package com.timemap.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

data class SelectedMapSelection(
    val title: String,
    val rulingPower: String,
    val year: Int,
    val summary: String,
    val isEvent: Boolean,
    val category: String = ""
)

data class CameraMoveCommand(
    val lat: Double,
    val lon: Double,
    val zoom: Double,
    val timestamp: Long = System.currentTimeMillis()
)

private const val BORDERS_SOURCE_ID = "historical-borders-source"
private const val BORDERS_FILL_LAYER_ID = "historical-borders-fill"
private const val BORDERS_LINE_LAYER_ID = "historical-borders-line"
private const val EVENTS_SOURCE_ID = "historical-events-source"
private const val EVENTS_CIRCLE_LAYER_ID = "historical-events-circle"
private const val EVENTS_SYMBOL_LAYER_ID = "historical-events-symbol"
private const val BASE_STYLE_URL = "https://demotiles.maplibre.org/style.json"

@Composable
fun TimeMapViewComposable(
    year: Int,
    showEvents: Boolean,
    cameraCommand: CameraMoveCommand?,
    onSelection: (SelectedMapSelection) -> Unit,
    onMapCameraRead: ((Double, Double, Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentYearState by rememberUpdatedState(year)
    val currentOnSelection by rememberUpdatedState(onSelection)
    val currentOnCameraRead by rememberUpdatedState(onMapCameraRead)

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }

    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    // Load GeoJSON when year changes
    LaunchedEffect(year, styleReady) {
        if (!styleReady) return@LaunchedEffect
        val map = mapInstance ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect

        isLoading = true
        errorMessage = null
        val result = GeoJsonManager.loadEnrichedGeoJson(context, year)
        result.onSuccess { geoJsonString ->
            val borderSource = style.getSourceAs<GeoJsonSource>(BORDERS_SOURCE_ID)
            borderSource?.setGeoJson(geoJsonString)
            val eventSource = style.getSourceAs<GeoJsonSource>(EVENTS_SOURCE_ID)
            eventSource?.setGeoJson(GeoJsonManager.buildEventsGeoJson(year))
            isLoading = false
        }.onFailure { err ->
            errorMessage = err.localizedMessage ?: "Network error loading year $year"
            isLoading = false
        }
    }

    // Toggle events layer visibility
    LaunchedEffect(showEvents, styleReady) {
        if (!styleReady) return@LaunchedEffect
        val style = mapInstance?.style ?: return@LaunchedEffect
        val vis = if (showEvents) "visible" else "none"
        style.getLayer(EVENTS_CIRCLE_LAYER_ID)?.setProperties(PropertyFactory.visibility(vis))
        style.getLayer(EVENTS_SYMBOL_LAYER_ID)?.setProperties(PropertyFactory.visibility(vis))
    }

    // Move camera when requested by search, bookmark, or quiz
    LaunchedEffect(cameraCommand, styleReady) {
        val cmd = cameraCommand ?: return@LaunchedEffect
        val map = mapInstance ?: return@LaunchedEffect
        val position = CameraPosition.Builder()
            .target(LatLng(cmd.lat, cmd.lon))
            .zoom(cmd.zoom)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1200)
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                mapView.getMapAsync { map ->
                    mapInstance = map
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(30.0, 15.0))
                        .zoom(1.8)
                        .build()

                    map.addOnCameraIdleListener {
                        val pos = map.cameraPosition
                        val target = pos.target
                        if (target != null) {
                            currentOnCameraRead?.invoke(target.latitude, target.longitude, pos.zoom)
                        }
                    }

                    map.setStyle(Style.Builder().fromUri(BASE_STYLE_URL)) { loadedStyle ->
                        // 1. Historical Borders Source + Fill & Line Layers
                        val emptyCollection = """{"type":"FeatureCollection","features":[]}"""
                        val bordersSource = GeoJsonSource(BORDERS_SOURCE_ID, emptyCollection)
                        loadedStyle.addSource(bordersSource)

                        val fillLayer = FillLayer(BORDERS_FILL_LAYER_ID, BORDERS_SOURCE_ID).withProperties(
                            PropertyFactory.fillColor(
                                Expression.coalesce(
                                    Expression.toColor(Expression.get("COLOR_HEX")),
                                    Expression.color(AndroidColor.parseColor("#5C6BC0"))
                                )
                            ),
                            PropertyFactory.fillOpacity(0.55f)
                        )
                        loadedStyle.addLayer(fillLayer)

                        val lineLayer = LineLayer(BORDERS_LINE_LAYER_ID, BORDERS_SOURCE_ID).withProperties(
                            PropertyFactory.lineColor(AndroidColor.parseColor("#222222")),
                            PropertyFactory.lineWidth(1.2f),
                            PropertyFactory.lineOpacity(0.85f)
                        )
                        loadedStyle.addLayer(lineLayer)

                        // 2. Historical Events Source + Circle & Label Layers
                        val eventsSource = GeoJsonSource(EVENTS_SOURCE_ID, emptyCollection)
                        loadedStyle.addSource(eventsSource)

                        val circleLayer = CircleLayer(EVENTS_CIRCLE_LAYER_ID, EVENTS_SOURCE_ID).withProperties(
                            PropertyFactory.circleRadius(7.5f),
                            PropertyFactory.circleColor(AndroidColor.parseColor("#FF5722")),
                            PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
                            PropertyFactory.circleStrokeWidth(2.0f)
                        )
                        loadedStyle.addLayer(circleLayer)

                        val symbolLayer = SymbolLayer(EVENTS_SYMBOL_LAYER_ID, EVENTS_SOURCE_ID).withProperties(
                            PropertyFactory.textField(Expression.get("EVENT_TITLE")),
                            PropertyFactory.textSize(11f),
                            PropertyFactory.textOffset(arrayOf(0f, 1.4f)),
                            PropertyFactory.textColor(AndroidColor.parseColor("#111111")),
                            PropertyFactory.textHaloColor(AndroidColor.WHITE),
                            PropertyFactory.textHaloWidth(1.5f)
                        )
                        loadedStyle.addLayer(symbolLayer)

                        styleReady = true
                    }

                    map.addOnMapClickListener { latLng ->
                        val screenPoint = map.projection.toScreenLocation(latLng)

                        // Check if user tapped an Event Pin first
                        val eventFeatures = map.queryRenderedFeatures(screenPoint, EVENTS_CIRCLE_LAYER_ID)
                        if (eventFeatures.isNotEmpty()) {
                            val feat = eventFeatures.first()
                            val title = feat.getStringProperty("EVENT_TITLE") ?: "Historical Event"
                            val cat = feat.getStringProperty("EVENT_CATEGORY") ?: "Event"
                            val evYear = feat.getNumberProperty("EVENT_YEAR")?.toInt() ?: currentYearState
                            val summary = feat.getStringProperty("EVENT_SUMMARY") ?: ""
                            currentOnSelection(
                                SelectedMapSelection(
                                    title = title,
                                    rulingPower = cat,
                                    year = evYear,
                                    summary = summary,
                                    isEvent = true,
                                    category = cat
                                )
                            )
                            return@addOnMapClickListener true
                        }

                        // Otherwise check if user tapped a Historical Border Polygon
                        val borderFeatures = map.queryRenderedFeatures(screenPoint, BORDERS_FILL_LAYER_ID)
                        if (borderFeatures.isNotEmpty()) {
                            val feat = borderFeatures.first()
                            val displayName = feat.getStringProperty("DISPLAY_NAME")
                                ?: feat.getStringProperty("NAME")
                                ?: "Historical Territory"
                            val ruling = feat.getStringProperty("RULING_POWER")
                                ?: feat.getStringProperty("SUBJECTO")
                                ?: displayName
                            val snappedYear = HistoricalData.snapToAvailableYear(currentYearState)
                            currentOnSelection(
                                SelectedMapSelection(
                                    title = displayName,
                                    rulingPower = ruling,
                                    year = snappedYear,
                                    summary = HistoricalData.buildHistoricalSummary(
                                        regionName = displayName,
                                        subjectTo = ruling,
                                        year = snappedYear
                                    ),
                                    isEvent = false
                                )
                            )
                            return@addOnMapClickListener true
                        }
                        false
                    }
                }
                mapView
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 10.dp),
                        strokeWidth = 2.5.dp
                    )
                    Text(
                        text = "${stringResource(R.string.loading_map)} (${HistoricalData.formatYear(year)})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        errorMessage?.let { msg ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
            ) {
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}
