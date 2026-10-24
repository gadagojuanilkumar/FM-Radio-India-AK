package com.gadagojuanilkumar.fmradioindiaak

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
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

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        // Keep up to PAUSE_BUFFER_MS of the stream buffered while paused, so
        // pressing Play continues from where you paused instead of jumping to live.
        // A live stream only delivers audio at its normal speed, so this does not
        // download extra data while you are listening - only while paused, and
        // never more than PAUSE_BUFFER_MS worth.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                PAUSE_BUFFER_MS,   // min buffer
                PAUSE_BUFFER_MS,   // max buffer
                2_500,             // buffer needed to start playing
                5_000              // buffer needed to resume after a stall
            )
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
                } else if (state == Player.STATE_ENDED &&
                    player.mediaItemCount > 0 &&
                    reconnectAttempts < MAX_RECONNECTS
                ) {
                    // The server closed the stream (for example after a long pause
                    // once the saved audio ran out). Reconnect to the live stream.
                    reconnectAttempts++
                    player.seekToDefaultPosition()
                    player.prepare()
                    player.play()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    player.seekToDefaultPosition()
                    player.prepare()
                }
            }
        })

        // Tapping the media notification / lock-screen controls opens the app.
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
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
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private companion object {
        // How much audio is kept while paused (5 minutes). Raise for longer pauses,
        // lower to use less data while paused.
        const val PAUSE_BUFFER_MS = 5 * 60 * 1000
        const val MAX_RECONNECTS = 3
    }
}
