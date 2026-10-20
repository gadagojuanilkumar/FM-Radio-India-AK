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
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.SubcomposeAsyncImage
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
    var allStations by remember { mutableStateOf<List<Station>>(emptyList()) }
    var currentStation by remember { mutableStateOf<Station?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var searchText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showDirectory by remember { mutableStateOf(false) }
    var directory by remember { mutableStateOf<List<FmEntry>>(emptyList()) }
    var directoryLoading by remember { mutableStateOf(false) }
    var directoryState by remember { mutableStateOf("Telangana") }
    var directoryMenuOpen by remember { mutableStateOf(false) }
    var directoryQuery by remember { mutableStateOf("") }
    var directoryMessage by remember { mutableStateOf("") }
    var onlineOnly by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("All languages") }
    var languageMenuOpen by remember { mutableStateOf(false) }
    var favoriteIds by remember { mutableStateOf(FavoritesStore.load(context)) }
    var favoritesOnly by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                override fun onEvents(player: Player, events: Player.Events) {
                    isPlaying = player.playWhenReady &&
                            player.playbackState != Player.STATE_IDLE &&
                            player.playbackState != Player.STATE_ENDED
                }
            })
            isPlaying = c.playWhenReady
            controller = c
        }, MoreExecutors.directExecutor())
        onDispose { MediaController.releaseFuture(future) }
    }

    LaunchedEffect(Unit) {
        loading = true
        val customJob = async { StationRepository.fetchCustom() }
        val apiJob = async { StationRepository.fetchAllIndia() }
        val fixJob = async { StationRepository.fetchStateFixes() }
        val fixes = fixJob.await()
        val api = apiJob.await().map { s ->
            val fixed = fixes[s.id]
            if (s.state == "India" && fixed != null) s.copy(state = fixed) else s
        }
        allStations = customJob.await() + api
        loading = false
    }

    val languages = listOf(
        "All languages", "Telugu", "Hindi", "Tamil", "Kannada", "Malayalam",
        "Punjabi", "Bengali", "Marathi", "Odia", "Urdu", "English", "Bhojpuri"
    )
    val stations = allStations.filter { s ->
        val query = searchQuery.trim()
        val languageOk = selectedLanguage == "All languages" ||
                s.language.contains(selectedLanguage, ignoreCase = true) ||
                (selectedLanguage == "Bengali" && s.language.contains("bangla", ignoreCase = true))
        val textOrStateOk = if (query.isNotBlank()) {
            s.name.contains(query, ignoreCase = true)
        } else {
            selectedState == "All India" ||
                    s.state.equals(selectedState, ignoreCase = true)
        }
        if (favoritesOnly) favoriteIds.contains(s.id) else languageOk && textOrStateOk
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

    fun toggleFavorite(station: Station) {
        favoriteIds = if (favoriteIds.contains(station.id)) {
            favoriteIds - station.id
        } else {
            favoriteIds + station.id
        }
        FavoritesStore.save(context, favoriteIds)
    }

    fun playRelative(step: Int) {
        if (stations.isEmpty()) return
        val index = stations.indexOfFirst { it.id == currentStation?.id }
        val newIndex = if (index == -1) 0 else (index + step + stations.size) % stations.size
        play(stations[newIndex])
    }

    LaunchedEffect(showDirectory) {
        if (showDirectory && directory.isEmpty()) {
            directoryLoading = true
            directory = StationRepository.fetchDirectory()
            directoryLoading = false
            if (directory.isEmpty()) {
                directoryMessage = "Could not load the FM list. Check your internet."
            }
        }
    }

    fun playFromDirectory(entry: FmEntry) {
        if (entry.streamUrl.isBlank()) {
            directoryMessage = "No online stream for ${entry.name} " +
                    "${entry.frequency} (${entry.city})"
            return
        }
        directoryMessage = "Playing ${entry.streamName.ifBlank { entry.name }}"
        play(
            Station(
                id = "fm-${entry.city}-${entry.frequency}",
                name = "${entry.name} ${entry.frequency} ${entry.city}",
                state = entry.state,
                streamUrl = entry.streamUrl
            )
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FM Radio India (AK)") },
                actions = {
                    TextButton(onClick = { showDirectory = !showDirectory }) {
                        Text(if (showDirectory) "Stations" else "FM Directory")
                    }
                }
            )
        },
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
                                    if (c.playWhenReady) c.pause() else c.play()
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
        if (showDirectory) {
            val states = listOf("All India") +
                    directory.map { it.state }.distinct().sorted()
            val q = directoryQuery.trim()
            val shown = directory.filter { e ->
                (!onlineOnly || e.streamUrl.isNotBlank()) &&
                (directoryState == "All India" ||
                        e.state.equals(directoryState, ignoreCase = true)) &&
                        (q.isBlank() ||
                                e.city.contains(q, ignoreCase = true) ||
                                e.name.contains(q, ignoreCase = true) ||
                                e.remarks.contains(q, ignoreCase = true) ||
                                e.frequency.contains(q))
            }
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    OutlinedButton(onClick = { directoryMenuOpen = true }) {
                        Text("State: $directoryState")
                    }
                    DropdownMenu(
                        expanded = directoryMenuOpen,
                        onDismissRequest = { directoryMenuOpen = false }
                    ) {
                        states.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s) },
                                onClick = {
                                    directoryState = s
                                    directoryMenuOpen = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = directoryQuery,
                    onValueChange = { directoryQuery = it },
                    label = { Text("City, name or frequency") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Online only (can play)")
                    Switch(checked = onlineOnly, onCheckedChange = { onlineOnly = it })
                }
                if (directoryMessage.isNotBlank()) {
                    Text(
                        text = directoryMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                if (directoryLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(shown) { e ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { playFromDirectory(e) }
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "${e.frequency} MHz  ${e.name}" +
                                            if (e.streamUrl.isNotBlank()) "   (Online)" else "",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = e.city + if (e.remarks.isNotBlank()) {
                                        " - " + e.remarks
                                    } else {
                                        ""
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
            return@Scaffold
        }
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
                    (listOf("All India") + IndianStates.all).forEach { state ->
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
            Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                OutlinedButton(onClick = { languageMenuOpen = true }) {
                    Text("Language: $selectedLanguage")
                }
                DropdownMenu(
                    expanded = languageMenuOpen,
                    onDismissRequest = { languageMenuOpen = false }
                ) {
                    languages.forEach { l ->
                        DropdownMenuItem(
                            text = { Text(l) },
                            onClick = {
                                selectedLanguage = l
                                languageMenuOpen = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Favorites only")
                Switch(checked = favoritesOnly, onCheckedChange = { favoritesOnly = it })
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
                        StationRow(
                            station = station,
                            onClick = { play(station) },
                            isFavorite = favoriteIds.contains(station.id),
                            onToggleFavorite = { toggleFavorite(station) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun StationRow(
    station: Station,
    onClick: () -> Unit,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {}
) {
    val placeholder: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = station.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (station.logoUrl.startsWith("http")) {
                SubcomposeAsyncImage(
                    model = station.logoUrl,
                    contentDescription = station.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(56.dp),
                    loading = { placeholder() },
                    error = { placeholder() }
                )
            } else {
                placeholder()
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOf(station.state, station.language)
                    .filter { it.isNotBlank() }
                    .joinToString(" - "),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        TextButton(onClick = onToggleFavorite) {
            Text(
                text = if (isFavorite) "\u2665" else "\u2661",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}