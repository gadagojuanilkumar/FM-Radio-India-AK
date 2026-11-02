package com.gadagojuanilkumar.fmradioindiaak

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

// One pin on the map: a state and how many stations it has
private data class MapPin(val name: String, val lat: Double, val lon: Double, val count: Int)

// Free map style from OpenFreeMap (no account or key needed)
private const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/positron"

// Border lines are removed on purpose: India has strict rules about how its borders
// must be drawn, so the map shows no border lines at all.
private const val HIDE_BORDERS = true

private fun Context.findLifecycleOwner(): LifecycleOwner? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is LifecycleOwner) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun MapScreen(
    stations: List<Station>,
    isIndia: Boolean,
    startState: String,
    favoriteIds: Collection<String>,
    currentStationId: String?,
    onPlay: (Station) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    var selected by remember {
        mutableStateOf<String?>(
            startState.takeIf { !it.startsWith("All") && StateCenters.all.containsKey(it) }
        )
    }
    val pinColor = MaterialTheme.colorScheme.primary.toArgb()

    // Pins: one per state that has stations
    val pins = remember(stations, isIndia) {
        if (!isIndia) {
            emptyList()
        } else {
            val counts = HashMap<String, Int>()
            for (s in stations) {
                val name = IndianStates.all.firstOrNull {
                    it.equals(s.state.trim(), ignoreCase = true)
                } ?: continue
                counts[name] = (counts[name] ?: 0) + 1
            }
            counts.mapNotNull { (name, n) ->
                StateCenters.all[name]?.let { MapPin(name, it.first, it.second, n) }
            }
        }
    }

    val shown = remember(stations, selected) {
        val state = selected
        if (state == null) {
            emptyList()
        } else {
            stations
                .filter { it.state.trim().equals(state, ignoreCase = true) }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.trim() })
        }
    }

    // The map view must follow the screen's life cycle
    DisposableEffect(mapView) {
        val owner = context.findLifecycleOwner()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose {
            owner?.lifecycle?.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    // Load the map once
    LaunchedEffect(mapView) {
        mapView.getMapAsync { m ->
            map = m
            m.uiSettings.isRotateGesturesEnabled = false
            m.uiSettings.isTiltGesturesEnabled = false
            m.cameraPosition = CameraPosition.Builder()
                .target(LatLng(22.0, 80.5))
                .zoom(3.0)
                .build()
            m.setStyle(MAP_STYLE_URL) { style ->
                if (HIDE_BORDERS) {
                    style.layers
                        .filter { it.id.contains("boundary", ignoreCase = true) }
                        .forEach { style.removeLayer(it) }
                }
                style.addSource(GeoJsonSource("pins"))
                style.addLayer(
                    CircleLayer("pin-circles", "pins").withProperties(
                        PropertyFactory.circleColor(pinColor),
                        PropertyFactory.circleRadius(17f),
                        PropertyFactory.circleOpacity(0.92f),
                        PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
                        PropertyFactory.circleStrokeWidth(2f)
                    )
                )
                style.addLayer(
                    SymbolLayer("pin-counts", "pins").withProperties(
                        PropertyFactory.textField(Expression.get("label")),
                        PropertyFactory.textSize(11f),
                        PropertyFactory.textColor(AndroidColor.WHITE),
                        PropertyFactory.textFont(arrayOf("Noto Sans Bold")),
                        PropertyFactory.textAllowOverlap(true),
                        PropertyFactory.textIgnorePlacement(true)
                    )
                )
                styleReady = true
            }
            m.addOnMapClickListener { latLng ->
                val point = m.projection.toScreenLocation(latLng)
                val hit = m.queryRenderedFeatures(point, "pin-circles")
                if (hit.isNotEmpty()) {
                    selected = hit[0].getStringProperty("name")
                    true
                } else {
                    false
                }
            }
        }
    }

    // Put the pins on the map whenever the station list changes
    LaunchedEffect(map, styleReady, pins) {
        val m = map ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        m.getStyle { style ->
            val features = pins.map { p ->
                Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat)).also {
                    it.addStringProperty("name", p.name)
                    it.addStringProperty("label", p.count.toString())
                }
            }
            style.getSourceAs<GeoJsonSource>("pins")
                ?.setGeoJson(FeatureCollection.fromFeatures(features))
        }
    }

    // Fly to the state that was tapped
    LaunchedEffect(map, selected) {
        val m = map ?: return@LaunchedEffect
        val center = selected?.let { StateCenters.all[it] }
        if (center != null) {
            m.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(center.first, center.second), 5.2),
                800
            )
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            !isIndia -> "The map is available for India"
                            selected == null -> "Tap a pin to see its stations"
                            else -> selected + " - " + shown.size + " stations"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected != null) {
                        TextButton(onClick = {
                            selected = null
                            map?.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(LatLng(22.0, 80.5), 3.0),
                                800
                            )
                        }) { Text("Show all") }
                    }
                }
                if (isIndia && selected != null) {
                    LazyColumn {
                        items(shown) { s ->
                            StationRow(
                                station = s,
                                onClick = { onPlay(s) },
                                isFavorite = favoriteIds.contains(s.id),
                                onToggleFavorite = { onToggleFavorite(s) },
                                isCurrent = s.id == currentStationId
                            )
                        }
                    }
                }
            }
        }
    }
}