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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.border
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.CornerRadius
import coil.compose.AsyncImage
import kotlin.math.abs
import kotlin.math.sin
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Canvas
import androidx.compose.material3.IconButton
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
    var showPlayer by remember { mutableStateOf(false) }
    var countries by remember { mutableStateOf<List<Country>>(emptyList()) }
    var selectedCountry by remember { mutableStateOf(Country("India", "IN", 0)) }
    var countryMenuOpen by remember { mutableStateOf(false) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var countryQuery by remember { mutableStateOf("") }
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
        countries = StationRepository.fetchCountries()
    }

    LaunchedEffect(selectedCountry.code) {
        loading = true
        if (selectedCountry.code == "IN") {
            val customJob = async { StationRepository.fetchCustom() }
            val apiJob = async { StationRepository.fetchAllIndia() }
            val fixJob = async { StationRepository.fetchStateFixes() }
            val fixes = fixJob.await()
            val api = apiJob.await().map { s ->
                val fixed = fixes[s.id]
                if (s.state == "India" && fixed != null) s.copy(state = fixed) else s
            }
            allStations = customJob.await() + api
        } else {
            allStations = StationRepository.fetchCountry(selectedCountry.code)
        }
        loading = false
    }

    val indianLanguages = listOf(
        "All languages", "Telugu", "Hindi", "Tamil", "Kannada", "Malayalam",
        "Punjabi", "Bengali", "Marathi", "Odia", "Urdu", "English", "Bhojpuri"
    )
    val languages = if (selectedCountry.code == "IN") {
        indianLanguages
    } else {
        listOf("All languages") + allStations
            .flatMap { it.language.split(",") }
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(25)
            .map { e -> e.key.replaceFirstChar { ch -> ch.uppercase() } }
    }
    val stations = allStations.filter { s ->
        val query = searchQuery.trim()
        val languageOk = selectedLanguage == "All languages" ||
                s.language.contains(selectedLanguage, ignoreCase = true) ||
                (selectedLanguage == "Bengali" && s.language.contains("bangla", ignoreCase = true))
        val textOrStateOk = if (query.isNotBlank()) {
            s.name.contains(query, ignoreCase = true)
        } else {
            selectedState.startsWith("All") ||
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
    if (showCountryPicker) {
        val cq = countryQuery.trim()
        val shownCountries = (if (countries.isEmpty()) listOf(selectedCountry) else countries)
            .sortedBy { it.name.lowercase() }
            .filter {
                cq.isBlank() ||
                        it.name.contains(cq, ignoreCase = true) ||
                        it.code.equals(cq, ignoreCase = true)
            }
        Dialog(
            onDismissRequest = {
                showCountryPicker = false
                countryQuery = ""
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Choose country", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = {
                            showCountryPicker = false
                            countryQuery = ""
                        }) { Text("Close") }
                    }
                    OutlinedTextField(
                        value = countryQuery,
                        onValueChange = { countryQuery = it },
                        label = { Text("Search country") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(shownCountries) { c ->
                            Text(
                                text = if (c.count > 0) "${c.name}  (${c.count})" else c.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCountry = c
                                        selectedState =
                                            if (c.code == "IN") "All India" else "All regions"
                                        selectedLanguage = "All languages"
                                        searchText = ""
                                        searchQuery = ""
                                        favoritesOnly = false
                                        showCountryPicker = false
                                        countryQuery = ""
                                    }
                                    .padding(vertical = 14.dp, horizontal = 8.dp)
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
    val playing = currentStation
    if (showPlayer && playing != null) {
        NowPlayingDialog(
            station = playing,
            isPlaying = isPlaying,
            isFavorite = favoriteIds.contains(playing.id),
            onToggle = {
                val c = controller
                if (c != null) {
                    if (c.playWhenReady) c.pause() else c.play()
                }
            },
            onPrevious = { playRelative(-1) },
            onNext = { playRelative(1) },
            onToggleFavorite = { toggleFavorite(playing) },
            onClose = { showPlayer = false }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FM Radio Worldwide") },
                actions = {
                    TextButton(onClick = { showDirectory = !showDirectory }) {
                        Text(if (showDirectory) "Stations" else "FM Directory")
                    }
                }
            )
        },
        bottomBar = {
            val current = currentStation
            val barTint = if (current != null) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
            }
            Surface(
                tonalElevation = 6.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = current != null) { showPlayer = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StationLogo(current, 48.dp)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = current?.name ?: "Pick a station",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (current == null) {
                                    "Tap a station to start"
                                } else {
                                    listOf(if (isPlaying) "Playing" else "Paused", current.state)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" - ")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        enabled = current != null,
                        onClick = { playRelative(-1) },
                        modifier = Modifier.size(44.dp)
                    ) { ControlIcon("previous", barTint, Modifier.size(26.dp)) }
                    IconButton(
                        enabled = current != null,
                        onClick = {
                            val c = controller
                            if (c != null) {
                                if (c.playWhenReady) c.pause() else c.play()
                            }
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
                        ControlIcon(
                            kind = if (isPlaying) "pause" else "play",
                            tint = barTint,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    IconButton(
                        enabled = current != null,
                        onClick = { playRelative(1) },
                        modifier = Modifier.size(44.dp)
                    ) { ControlIcon("next", barTint, Modifier.size(26.dp)) }
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
                Row {
                    Box {
                        OutlinedButton(
                            onClick = { showCountryPicker = true },
                            modifier = Modifier.widthIn(max = 170.dp)
                        ) {
                            Text(
                                text = selectedCountry.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = countryMenuOpen,
                            onDismissRequest = { countryMenuOpen = false }
                        ) {
                            val countryList = if (countries.isEmpty()) {
                                listOf(selectedCountry)
                            } else {
                                countries
                            }
                            countryList.forEach { c ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (c.count > 0) "${c.name} (${c.count})" else c.name
                                        )
                                    },
                                    onClick = {
                                        selectedCountry = c
                                        selectedState =
                                            if (c.code == "IN") "All India" else "All regions"
                                        selectedLanguage = "All languages"
                                        searchText = ""
                                        searchQuery = ""
                                        favoritesOnly = false
                                        countryMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = { menuOpen = true }) {
                        Text(
                            text = "State: $selectedState",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    val stateList = if (selectedCountry.code == "IN") {
                        listOf("All India") + IndianStates.all
                    } else {
                        listOf("All regions") + allStations.map { it.state.trim() }
                            .filter { it.isNotBlank() }
                            .distinct()
                            .sorted()
                    }
                    stateList.forEach { state ->
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

@Composable
fun ControlIcon(kind: String, tint: Color, modifier: Modifier = Modifier.size(32.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (kind) {
            "play" -> {
                val p = Path().apply {
                    moveTo(w * 0.25f, h * 0.12f)
                    lineTo(w * 0.88f, h * 0.5f)
                    lineTo(w * 0.25f, h * 0.88f)
                    close()
                }
                drawPath(p, tint)
            }
            "pause" -> {
                drawRect(tint, Offset(w * 0.2f, h * 0.15f), Size(w * 0.22f, h * 0.7f))
                drawRect(tint, Offset(w * 0.58f, h * 0.15f), Size(w * 0.22f, h * 0.7f))
            }
            "next" -> {
                val p = Path().apply {
                    moveTo(w * 0.12f, h * 0.15f)
                    lineTo(w * 0.68f, h * 0.5f)
                    lineTo(w * 0.12f, h * 0.85f)
                    close()
                }
                drawPath(p, tint)
                drawRect(tint, Offset(w * 0.74f, h * 0.15f), Size(w * 0.14f, h * 0.7f))
            }
            "previous" -> {
                val p = Path().apply {
                    moveTo(w * 0.88f, h * 0.15f)
                    lineTo(w * 0.32f, h * 0.5f)
                    lineTo(w * 0.88f, h * 0.85f)
                    close()
                }
                drawPath(p, tint)
                drawRect(tint, Offset(w * 0.12f, h * 0.15f), Size(w * 0.14f, h * 0.7f))
            }
        }
    }
}

@Composable
fun NowPlayingDialogOld(
    station: Station,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit
) {
    val letter: @Composable () -> Unit = {
        Text(
            text = station.name.take(1).uppercase(),
            style = MaterialTheme.typography.displayLarge
        )
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onClose) { Text("Close") }
                    TextButton(onClick = onToggleFavorite) {
                        Text(
                            text = if (isFavorite) "\u2665" else "\u2661",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .size(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (station.logoUrl.startsWith("http")) {
                        SubcomposeAsyncImage(
                            model = station.logoUrl,
                            contentDescription = station.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(280.dp),
                            loading = { Box(contentAlignment = Alignment.Center) { letter() } },
                            error = { Box(contentAlignment = Alignment.Center) { letter() } }
                        )
                    } else {
                        letter()
                    }
                }
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 24.dp)
                )
                Text(
                    text = listOf(station.state, station.language)
                        .filter { it.isNotBlank() }
                        .joinToString(" - "),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tint = MaterialTheme.colorScheme.onSurface
                    IconButton(onClick = onPrevious, modifier = Modifier.size(72.dp)) {
                        ControlIcon("previous", tint, Modifier.size(44.dp))
                    }
                    IconButton(onClick = onToggle, modifier = Modifier.size(88.dp)) {
                        ControlIcon(
                            kind = if (isPlaying) "pause" else "play",
                            tint = tint,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(72.dp)) {
                        ControlIcon("next", tint, Modifier.size(44.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StationLogo(station: Station?, size: Dp) {
    val letter = station?.name?.take(1)?.uppercase() ?: "\u266B"
    val placeholder: @Composable () -> Unit = {
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            Text(text = letter, style = MaterialTheme.typography.titleMedium)
        }
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val url = station?.logoUrl ?: ""
        if (url.startsWith("http")) {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = station?.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size),
                loading = { placeholder() },
                error = { placeholder() }
            )
        } else {
            placeholder()
        }
    }
}

@Composable
fun EqualizerBars(active: Boolean, color: Color) {
    val transition = rememberInfiniteTransition(label = "eq")
    val bars = listOf(0.4f to 520, 0.7f to 430, 0.5f to 610, 0.9f to 480, 0.6f to 560)
    Row(
        modifier = Modifier.height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEachIndexed { i, (start, ms) ->
            val h by transition.animateFloat(
                initialValue = start,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(ms), RepeatMode.Reverse),
                label = "bar$i"
            )
            val fraction = if (active) h else 0.25f
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height((32 * fraction).dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun NowPlayingDialogV2(
    station: Station,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val letter: @Composable () -> Unit = {
        Text(
            text = station.name.take(1).uppercase(),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onClose) { Text("Close") }
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 2.sp
                    )
                    TextButton(onClick = onToggleFavorite) {
                        Text(
                            text = if (isFavorite) "\u2665" else "\u2661",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    shadowElevation = 16.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                        if (station.logoUrl.startsWith("http")) {
                            SubcomposeAsyncImage(
                                model = station.logoUrl,
                                contentDescription = station.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(300.dp),
                                loading = { Box(contentAlignment = Alignment.Center) { letter() } },
                                error = { Box(contentAlignment = Alignment.Center) { letter() } }
                            )
                        } else {
                            letter()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
                EqualizerBars(active = isPlaying, color = accent)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = listOf(station.state, station.language)
                        .filter { it.isNotBlank() }
                        .joinToString(" - "),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tint = MaterialTheme.colorScheme.onSurface
                    IconButton(onClick = onPrevious, modifier = Modifier.size(72.dp)) {
                        ControlIcon("previous", tint, Modifier.size(40.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(accent)
                            .clickable { onToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        ControlIcon(
                            kind = if (isPlaying) "pause" else "play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(72.dp)) {
                        ControlIcon("next", tint, Modifier.size(40.dp))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun WaveBars(active: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "wave")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "t"
    )
    Canvas(modifier = modifier) {
        val count = 28
        val gap = size.width / count
        val barW = gap * 0.55f
        for (i in 0 until count) {
            val phase = i * 0.55f
            val level = if (active) {
                0.2f + 0.8f * abs(sin(t * (1 + (i % 3)) + phase))
            } else {
                0.12f
            }
            val h = size.height * level
            val x = i * gap + (gap - barW) / 2
            val color = lerp(Color(0xFF22D3EE), Color(0xFFFF8A00), i / (count - 1f))
            drawRoundRect(
                color = color,
                topLeft = Offset(x, size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2, barW / 2)
            )
        }
    }
}

@Composable
fun GlowFrame(active: Boolean, content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "glow")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "pulse"
    )
    val p = if (active) pulse else 0f
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(270.dp)
                .graphicsLayer {
                    scaleX = 1f + 0.14f * p
                    scaleY = 1f + 0.14f * p
                    alpha = 0.6f - 0.35f * p
                }
                .clip(RoundedCornerShape(40.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF22D3EE), Color(0xFF7B2FF7), Color(0xFFFF8A00))
                    )
                )
        )
        content()
    }
}

@Composable
fun InfoChip(text: String) {
    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.14f)) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun GlassButton(size: Dp, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun NowPlayingDialog(
    station: Station,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit
) {
    val hasLogo = station.logoUrl.startsWith("http")
    val letter: @Composable () -> Unit = {
        Text(
            text = station.name.take(1).uppercase(),
            style = MaterialTheme.typography.displayLarge,
            color = Color.White
        )
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B1030))) {
            if (hasLogo && Build.VERSION.SDK_INT >= 31) {
                AsyncImage(
                    model = station.logoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(60.dp)
                        .alpha(0.5f)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xCC0B1030), Color(0xE61B1448), Color(0xF20B0A24))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassButton(44.dp, onClose) {
                        Canvas(modifier = Modifier.size(18.dp)) {
                            val sw = 3.dp.toPx()
                            drawLine(
                                Color.White,
                                Offset(size.width * 0.1f, size.height * 0.3f),
                                Offset(size.width * 0.5f, size.height * 0.7f),
                                sw, StrokeCap.Round
                            )
                            drawLine(
                                Color.White,
                                Offset(size.width * 0.5f, size.height * 0.7f),
                                Offset(size.width * 0.9f, size.height * 0.3f),
                                sw, StrokeCap.Round
                            )
                        }
                    }
                    Text(
                        text = "NOW PLAYING",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 3.sp
                    )
                    GlassButton(44.dp, onToggleFavorite) {
                        Text(
                            text = if (isFavorite) "\u2665" else "\u2661",
                            color = if (isFavorite) Color(0xFFFF4D6D) else Color.White,
                            fontSize = 22.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                GlowFrame(active = isPlaying) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        shadowElevation = 20.dp,
                        color = Color(0xFF1E1A4D)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(250.dp)
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.25f),
                                    RoundedCornerShape(28.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasLogo) {
                                SubcomposeAsyncImage(
                                    model = station.logoUrl,
                                    contentDescription = station.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(250.dp),
                                    loading = { Box(contentAlignment = Alignment.Center) { letter() } },
                                    error = { Box(contentAlignment = Alignment.Center) { letter() } }
                                )
                            } else {
                                letter()
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFFF3B30).copy(alpha = 0.25f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3B30))
                            )
                            Text(
                                text = "  LIVE",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (station.state.isNotBlank()) InfoChip(station.state)
                    if (station.language.isNotBlank()) InfoChip(station.language.take(18))
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = station.name,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(16.dp))
                WaveBars(
                    active = isPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassButton(68.dp, onPrevious) {
                        ControlIcon("previous", Color.White, Modifier.size(30.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF22D3EE), Color(0xFF7B2FF7))
                                )
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                            .clickable { onToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        ControlIcon(
                            kind = if (isPlaying) "pause" else "play",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    GlassButton(68.dp, onNext) {
                        ControlIcon("next", Color.White, Modifier.size(30.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}