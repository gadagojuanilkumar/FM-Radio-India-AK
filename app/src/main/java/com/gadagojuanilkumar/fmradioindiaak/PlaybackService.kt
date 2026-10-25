package com.gadagojuanilkumar.fmradioindiaak

import android.app.PendingIntent
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var reconnectAttempts = 0
    private var retryAttempts = 0
    private val handler = Handler(Looper.getMainLooper())
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(PAUSE_BUFFER_MS, PAUSE_BUFFER_MS, 2_500, 5_000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    reconnectAttempts = 0
                    retryAttempts = 0
                } else if (state == Player.STATE_ENDED &&
                    player.mediaItemCount > 0 &&
                    reconnectAttempts < MAX_RECONNECTS
                ) {
                    reconnectAttempts++
                    player.seekToDefaultPosition()
                    player.prepare()
                    player.play()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // A new station was chosen: forget any retries for the old one
                handler.removeCallbacksAndMessages(null)
                retryAttempts = 0
                reconnectAttempts = 0
            }

            override fun onPlayerError(error: PlaybackException) {
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    player.seekToDefaultPosition()
                    player.prepare()
                    return
                }
                val isNetworkProblem =
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                val maxTries = if (isNetworkProblem) MAX_NETWORK_RETRIES else MAX_OTHER_RETRIES
                // Try again after 2s, 4s, 6s ... while the user still wants to listen
                if (player.playWhenReady && retryAttempts < maxTries) {
                    retryAttempts++
                    handler.removeCallbacksAndMessages(null)
                    handler.postDelayed({ player.prepare() }, retryAttempts * 2000L)
                }
            }
        })

        // When the internet comes back (or switches between Wi-Fi and mobile data),
        // restart a stream that had stopped because of an error.
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                handler.post {
                    if (player.playWhenReady && player.playerError != null) {
                        retryAttempts = 0
                        player.prepare()
                    }
                }
            }
        }
        try {
            getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(callback)
            networkCallback = callback
        } catch (e: Exception) {
            // Auto-restart on network return is a bonus; playback works without it
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openApp = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        networkCallback?.let {
            try {
                getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(it)
            } catch (e: Exception) {
            }
        }
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private companion object {
        const val PAUSE_BUFFER_MS = 5 * 60 * 1000
        const val MAX_RECONNECTS = 3
        const val MAX_NETWORK_RETRIES = 6
        const val MAX_OTHER_RETRIES = 2
    }
}