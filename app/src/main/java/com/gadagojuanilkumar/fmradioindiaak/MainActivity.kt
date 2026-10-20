package com.gadagojuanilkumar.fmradioindiaak

import android.Manifest
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.async
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.gadagojuanilkumar.fmradioindiaak.ui.theme.FMRadioIndiaAKTheme
import com.google.common.util.concurrent.MoreExecutors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FMRadioIndiaAKTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RadioScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioScreen() {
    val context = LocalContext.current

    var selectedState by remember { mutableStateOf("Telangana") }
    var menuOpen by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var stations by remember { mutableStateOf<List<Station>>(emptyList()) }
    var currentStation by remember { mutableStateOf<Station?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var searchText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DisposableEffect(Unit) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val c = future.get()
            c.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
            isPlaying = c.isPlaying
            controller = c
        }, MoreExecutors.directExecutor())
        onDispose { MediaController.releaseFuture(future) }
    }

    LaunchedEffect(selectedState, searchQuery) {
        loading = true
        val customJob = async { StationRepository.fetchCustom() }
        val apiJob = async {
            if (searchQuery.isBlank()) {
                StationRepository.fetchByState(selectedState)
            } else {
                StationRepository.searchByName(searchQuery)
            }
        }
        val custom = customJob.await()
        val api = apiJob.await()
        stations = if (searchQuery.isBlank()) {
            custom.filter { it.state.equals(selectedState, ignoreCase = true) } + api
        } else {
            custom.filter { it.name.contains(searchQuery, ignoreCase = true) } + api
        }
        loading = false
    }

    fun play(station: Station) {
        val c = controller ?: return
        currentStation = station
        c.stop()
        c.clearMediaItems()
        val item = MediaItem.Builder()
            .setUri(station.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(station.state)
                    .build()
            )
            .build()
        c.setMediaItem(item)
        c.prepare()
        c.play()
    }

    fun playRelative(step: Int) {
        if (stations.isEmpty()) return
        val index = stations.indexOfFirst { it.id == currentStation?.id }
        val newIndex = if (index == -1) 0 else (index + step + stations.size) % stations.size
        play(stations[newIndex])
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("FM Radio India (AK)") }) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentStation?.name ?: "Pick a station",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            enabled = currentStation != null,
                            onClick = { playRelative(-1) }
                        ) { Text("Previous") }
                        Button(
                            enabled = currentStation != null,
                            onClick = {
                                val c = controller
                                if (c != null) {
                                    if (c.isPlaying) c.pause() else c.play()
                                }
                            }
                        ) { Text(if (isPlaying) "Pause" else "Play") }
                        Button(
                            enabled = currentStation != null,
                            onClick = { playRelative(1) }
                        ) { Text("Next") }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                OutlinedButton(onClick = { menuOpen = true }) {
                    Text("State: $selectedState")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    IndianStates.all.forEach { state ->
                        DropdownMenuItem(
                            text = { Text(state) },
                            onClick = {
                                selectedState = state
                                searchText = ""
                                searchQuery = ""
                                menuOpen = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Name or frequency, e.g. 91.1") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { searchQuery = searchText.trim() },
                    modifier = Modifier.padding(start = 8.dp)
                ) { Text("Search") }
            }
            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (stations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No stations found for $selectedState")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(stations) { station ->
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { play(station) }
                                .padding(16.dp)
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}