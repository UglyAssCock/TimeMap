package com.timemap.app

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private val DarkSlateScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    secondary = Color(0xFFF59E0B),
    tertiary = Color(0xFF10B981),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onPrimary = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF1F5F9)
)

private val LightAtlasScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    secondary = Color(0xFFD97706),
    tertiary = Color(0xFF059669),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize MapLibre SDK before rendering any MapView
        MapLibre.getInstance(applicationContext)

        setContent {
            TimeMapAppRoot()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeMapAppRoot() {
    val baseContext = LocalContext.current
    var isDarkMode by remember { mutableStateOf(true) }
    var currentLang by remember { mutableStateOf("en") } // "en", "fr", "ar"

    val localizedContext = remember(baseContext, currentLang) {
        val locale = Locale(currentLang)
        Locale.setDefault(locale)
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        baseContext.createConfigurationContext(config)
    }

    val layoutDir = if (currentLang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalLayoutDirection provides layoutDir
    ) {
        MaterialTheme(
            colorScheme = if (isDarkMode) DarkSlateScheme else LightAtlasScheme
        ) {
            TimeMapMainScreen(
                isDarkMode = isDarkMode,
                onToggleDarkMode = { isDarkMode = !isDarkMode },
                currentLang = currentLang,
                onCycleLanguage = {
                    currentLang = when (currentLang) {
                        "en" -> "fr"
                        "fr" -> "ar"
                        else -> "en"
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeMapMainScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    currentLang: String,
    onCycleLanguage: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("timemap_prefs", Context.MODE_PRIVATE) }

    var selectedTab by remember { mutableIntStateOf(0) }

    // Primary Map Year Index (snapped to AVAILABLE_YEARS)
    val years = HistoricalData.AVAILABLE_YEARS
    var yearIndex by remember { mutableIntStateOf(years.indexOf(1500).coerceAtLeast(0)) }
    val activeYear = years[yearIndex]

    // Timelapse Play Controls
    var isPlaying by remember { mutableStateOf(false) }
    var playDirection by remember { mutableIntStateOf(1) } // 1 = forward, -1 = backward
    var playSpeedMultiplier by remember { mutableFloatStateOf(1f) } // 1x, 2x, 4x
    var showEventsLayer by remember { mutableStateOf(true) }

    // Map selection card & camera state
    var selectedFeature by remember { mutableStateOf<SelectedMapSelection?>(null) }
    var cameraCommand by remember { mutableStateOf<CameraMoveCommand?>(null) }
    var lastMapLat by remember { mutableStateOf(30.0) }
    var lastMapLon by remember { mutableStateOf(15.0) }
    var lastMapZoom by remember { mutableStateOf(2.0) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }

    // Compare Mode state
    var compareLeftIndex by remember { mutableIntStateOf(years.indexOf(100).coerceAtLeast(0)) }
    var compareRightIndex by remember { mutableIntStateOf(years.indexOf(1914).coerceAtLeast(0)) }
    var compareSelection by remember { mutableStateOf<SelectedMapSelection?>(null) }

    // Bookmarks & Custom Timeline state
    val bookmarks = remember {
        mutableStateListOf<BookmarkItem>().apply {
            val saved = prefs.getString("saved_bookmarks", null)
            if (!saved.isNullOrBlank()) {
                saved.split(";").forEach { entry ->
                    val parts = entry.split("|")
                    if (parts.size == 5) {
                        val y = parts[1].toIntOrNull() ?: 1500
                        val lat = parts[2].toDoubleOrNull() ?: 30.0
                        val lon = parts[3].toDoubleOrNull() ?: 15.0
                        val zm = parts[4].toDoubleOrNull() ?: 3.0
                        add(BookmarkItem(parts[0], y, lat, lon, zm))
                    }
                }
            } else {
                add(BookmarkItem("Pax Romana Peak", 100, 41.90, 12.49, 3.3))
                add(BookmarkItem("Fall of Constantinople", 1400, 41.01, 28.98, 4.2))
                add(BookmarkItem("Regency of Algiers", 1700, 36.75, 3.05, 4.5))
                add(BookmarkItem("Eve of World War I", 1914, 48.85, 10.0, 3.2))
            }
        }
    }

    fun persistBookmarks() {
        val serialized = bookmarks.joinToString(";") {
            "${it.title}|${it.year}|${it.lat}|${it.lon}|${it.zoom}"
        }
        prefs.edit().putString("saved_bookmarks", serialized).apply()
    }

    // Timelapse coroutine loop
    LaunchedEffect(isPlaying, playDirection, playSpeedMultiplier) {
        while (isPlaying) {
            val delayMs = (1800L / playSpeedMultiplier).toLong().coerceAtLeast(350L)
            delay(delayMs)
            val nextIdx = yearIndex + playDirection
            if (nextIdx in years.indices) {
                yearIndex = nextIdx
            } else {
                isPlaying = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = HistoricalData.formatYear(activeYear),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onCycleLanguage) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = stringResource(R.string.language))
                            Text(
                                text = currentLang.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = onToggleDarkMode) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = stringResource(R.string.dark_mode),
                            tint = if (isDarkMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_map)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Compare, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_compare)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_today)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_offline)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Quiz, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_quiz)) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                // TAB 0: MAIN HISTORICAL ATLAS
                0 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search Bar + Instant Suggestions
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(stringResource(R.string.search_hint), style = MaterialTheme.typography.bodySmall) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = null)
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (searchQuery.trim().isNotEmpty()) {
                                val q = searchQuery.trim().lowercase()
                                val matchedPlaces = HistoricalData.SEARCH_PLACES.filter {
                                    it.name.lowercase().contains(q) || it.description.lowercase().contains(q)
                                }.take(4)
                                val matchedEvents = HistoricalData.EVENTS.filter {
                                    it.title.lowercase().contains(q) || it.summary.lowercase().contains(q)
                                }.take(3)

                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(170.dp)
                                        .padding(top = 4.dp)
                                ) {
                                    items(matchedPlaces) { place ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                                .clickable {
                                                    val snapped = HistoricalData.snapToAvailableYear(place.year)
                                                    yearIndex = years.indexOf(snapped).coerceAtLeast(0)
                                                    cameraCommand = CameraMoveCommand(place.lat, place.lon, place.zoom)
                                                    selectedFeature = SelectedMapSelection(
                                                        title = place.name,
                                                        rulingPower = place.name,
                                                        year = snapped,
                                                        summary = place.description,
                                                        isEvent = false
                                                    )
                                                    searchQuery = ""
                                                }
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    text = "${place.name} (${HistoricalData.formatYear(place.year)})",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = place.description,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                    items(matchedEvents) { ev ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                                .clickable {
                                                    val snapped = HistoricalData.snapToAvailableYear(ev.year)
                                                    yearIndex = years.indexOf(snapped).coerceAtLeast(0)
                                                    cameraCommand = CameraMoveCommand(ev.lat, ev.lon, 4.5)
                                                    selectedFeature = SelectedMapSelection(
                                                        title = ev.title,
                                                        rulingPower = ev.category,
                                                        year = ev.year,
                                                        summary = ev.summary,
                                                        isEvent = true,
                                                        category = ev.category
                                                    )
                                                    searchQuery = ""
                                                }
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    text = "Event: ${ev.title} (${HistoricalData.formatYear(ev.year)})",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = ev.summary,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Map Container + Floating Region/Event Info Card
                        Box(modifier = Modifier.weight(1f)) {
                            TimeMapViewComposable(
                                year = activeYear,
                                showEvents = showEventsLayer,
                                cameraCommand = cameraCommand,
                                onSelection = { selectedFeature = it },
                                onMapCameraRead = { lat, lon, zoom ->
                                    lastMapLat = lat
                                    lastMapLon = lon
                                    lastMapZoom = zoom
                                }
                            )

                            // Region / Event Summary Card when user taps the map
                            selectedFeature?.let { sel ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = sel.title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${stringResource(R.string.ruling_power)}: ${sel.rulingPower} • ${HistoricalData.formatYear(sel.year)}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(onClick = { selectedFeature = null }) {
                                                Icon(Icons.Default.Close, contentDescription = null)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = sel.summary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Timeline Controls: Year Slider + Play/Pause/Direction/Speed + Events Toggle
                        Surface(
                            tonalElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "3000 BC",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = "${HistoricalData.formatYear(activeYear)} (${yearIndex + 1}/${years.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "2010 AD",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                                Slider(
                                    value = yearIndex.toFloat(),
                                    onValueChange = { newVal ->
                                        yearIndex = newVal.roundToInt().coerceIn(0, years.lastIndex)
                                    },
                                    valueRange = 0f..years.lastIndex.toFloat(),
                                    steps = (years.size - 2).coerceAtLeast(0),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Direction & Play/Pause
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                playDirection = -1
                                                if (!isPlaying && yearIndex > 0) yearIndex -= 1
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.FastRewind,
                                                contentDescription = stringResource(R.string.reverse),
                                                tint = if (playDirection == -1) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Button(
                                            onClick = { isPlaying = !isPlaying }
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play))
                                        }
                                        IconButton(
                                            onClick = {
                                                playDirection = 1
                                                if (!isPlaying && yearIndex < years.lastIndex) yearIndex += 1
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.FastForward,
                                                contentDescription = stringResource(R.string.forward),
                                                tint = if (playDirection == 1) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // Speed Chip (1x / 2x / 4x)
                                    FilterChip(
                                        selected = playSpeedMultiplier > 1f,
                                        onClick = {
                                            playSpeedMultiplier = when (playSpeedMultiplier) {
                                                1f -> 2f
                                                2f -> 4f
                                                else -> 1f
                                            }
                                        },
                                        label = { Text("${playSpeedMultiplier.toInt()}x") }
                                    )

                                    // Events Layer Switch + Quick Bookmark Button
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stringResource(R.string.events_layer),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Switch(
                                            checked = showEventsLayer,
                                            onCheckedChange = { showEventsLayer = it }
                                        )
                                        IconButton(
                                            onClick = {
                                                val title = selectedFeature?.title ?: "Saved Era ${HistoricalData.formatYear(activeYear)}"
                                                bookmarks.add(
                                                    0,
                                                    BookmarkItem(
                                                        title = title,
                                                        year = activeYear,
                                                        lat = lastMapLat,
                                                        lon = lastMapLon,
                                                        zoom = lastMapZoom
                                                    )
                                                )
                                                persistBookmarks()
                                            }
                                        ) {
                                            Icon(Icons.Default.Bookmark, contentDescription = stringResource(R.string.bookmark_add))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 1: COMPARE MODE (SPLIT SCREEN WITH TWO INDEPENDENT YEARS)
                1 -> {
                    val leftYear = years[compareLeftIndex]
                    val rightYear = years[compareRightIndex]

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Map (Year A)
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${stringResource(R.string.compare_left)}: ${HistoricalData.formatYear(leftYear)}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(150.dp)
                                    )
                                    Slider(
                                        value = compareLeftIndex.toFloat(),
                                        onValueChange = { compareLeftIndex = it.roundToInt().coerceIn(0, years.lastIndex) },
                                        valueRange = 0f..years.lastIndex.toFloat(),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            TimeMapViewComposable(
                                year = leftYear,
                                showEvents = true,
                                cameraCommand = null,
                                onSelection = { compareSelection = it },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )

                        // Bottom Map (Year B)
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${stringResource(R.string.compare_right)}: ${HistoricalData.formatYear(rightYear)}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(150.dp)
                                    )
                                    Slider(
                                        value = compareRightIndex.toFloat(),
                                        onValueChange = { compareRightIndex = it.roundToInt().coerceIn(0, years.lastIndex) },
                                        valueRange = 0f..years.lastIndex.toFloat(),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            TimeMapViewComposable(
                                year = rightYear,
                                showEvents = true,
                                cameraCommand = null,
                                onSelection = { compareSelection = it },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        compareSelection?.let { sel ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Text(
                                        text = "${sel.title} (${HistoricalData.formatYear(sel.year)}): ${sel.rulingPower}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { compareSelection = null }) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 2: "ON THIS DAY" FEED LINKED TO THE MAP
                2 -> {
                    val calendar = remember { Calendar.getInstance() }
                    val currentMonth = calendar.get(Calendar.MONTH) + 1
                    val sortedEvents = remember {
                        HistoricalData.EVENTS.sortedBy {
                            val monthDiff = kotlin.math.abs(it.month - currentMonth)
                            monthDiff * 100 + it.day
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.on_this_day_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Events matched to the current calendar season & full historical chronicle",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(sortedEvents) { ev ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "${ev.month}/${ev.day} • ${HistoricalData.formatYear(ev.year)}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = ev.category,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ev.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ev.summary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            val snapped = HistoricalData.snapToAvailableYear(ev.year)
                                            yearIndex = years.indexOf(snapped).coerceAtLeast(0)
                                            cameraCommand = CameraMoveCommand(ev.lat, ev.lon, 4.5)
                                            selectedFeature = SelectedMapSelection(
                                                title = ev.title,
                                                rulingPower = ev.category,
                                                year = ev.year,
                                                summary = ev.summary,
                                                isEvent = true,
                                                category = ev.category
                                            )
                                            selectedTab = 0
                                        }
                                    ) {
                                        Text(stringResource(R.string.jump_to_map))
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 3: OFFLINE ERA PACKS, CACHE MANAGER, BOOKMARKS & SHAREABLE TIMELINES
                3 -> {
                    var cachedCount by remember { mutableIntStateOf(GeoJsonManager.getCachedYearsCount(context)) }
                    var downloadingPackId by remember { mutableStateOf<String?>(null) }
                    var downloadProgress by remember { mutableFloatStateOf(0f) }
                    var importCodeInput by remember { mutableStateOf("") }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.era_packs_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.cached_years_count, cachedCount, years.size),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        items(HistoricalData.ERA_PACKS) { pack ->
                            val packTitle = when (currentLang) {
                                "fr" -> pack.titleFr
                                "ar" -> pack.titleAr
                                else -> pack.titleEn
                            }
                            val allCached = pack.years.all { GeoJsonManager.isYearCached(context, it) }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = packTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = pack.years.joinToString(", ") { HistoricalData.formatYear(it) },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (downloadingPackId == pack.id) {
                                        LinearProgressIndicator(
                                            progress = { downloadProgress },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    Button(
                                        enabled = downloadingPackId == null && !allCached,
                                        onClick = {
                                            downloadingPackId = pack.id
                                            downloadProgress = 0f
                                            coroutineScope.launch {
                                                pack.years.forEachIndexed { idx, y ->
                                                    GeoJsonManager.loadEnrichedGeoJson(context, y)
                                                    downloadProgress = (idx + 1).toFloat() / pack.years.size
                                                    cachedCount = GeoJsonManager.getCachedYearsCount(context)
                                                }
                                                downloadingPackId = null
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            if (allCached) stringResource(R.string.downloaded)
                                            else stringResource(R.string.download_pack)
                                        )
                                    }
                                }
                            }
                        }

                        // Bookmarks & Custom Shareable Timelines Section
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Bookmarks & Custom Timelines",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedButton(
                                    onClick = {
                                        val shareCode = bookmarks.joinToString(";") {
                                            "${it.title}|${it.year}|${"%.2f".format(Locale.US, it.lat)}|${"%.2f".format(Locale.US, it.lon)}|${"%.1f".format(Locale.US, it.zoom)}"
                                        }
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "TimeMap Custom Timeline:\n$shareCode"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Timeline"))
                                    }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.share_timeline))
                                }
                            }
                        }

                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = importCodeInput,
                                    onValueChange = { importCodeInput = it },
                                    placeholder = { Text(stringResource(R.string.import_timeline)) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val cleaned = importCodeInput.replace("TimeMap Custom Timeline:", "").trim()
                                        cleaned.split(";").forEach { entry ->
                                            val parts = entry.split("|")
                                            if (parts.size == 5) {
                                                val y = parts[1].toIntOrNull() ?: 1500
                                                val lat = parts[2].toDoubleOrNull() ?: 30.0
                                                val lon = parts[3].toDoubleOrNull() ?: 15.0
                                                val zm = parts[4].toDoubleOrNull() ?: 3.0
                                                bookmarks.add(0, BookmarkItem(parts[0], y, lat, lon, zm))
                                            }
                                        }
                                        persistBookmarks()
                                        importCodeInput = ""
                                    }
                                ) {
                                    Text("Import")
                                }
                            }
                        }

                        items(bookmarks) { bm ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val snapped = HistoricalData.snapToAvailableYear(bm.year)
                                        yearIndex = years.indexOf(snapped).coerceAtLeast(0)
                                        cameraCommand = CameraMoveCommand(bm.lat, bm.lon, bm.zoom)
                                        selectedTab = 0
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bm.title,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "${HistoricalData.formatYear(bm.year)} • (${"%.1f".format(bm.lat)}, ${"%.1f".format(bm.lon)})",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            bookmarks.remove(bm)
                                            persistBookmarks()
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 4: HISTORICAL EMPIRE QUIZ MODE
                4 -> {
                    val questions = HistoricalData.QUIZ_QUESTIONS
                    var qIndex by remember { mutableIntStateOf(0) }
                    var score by remember { mutableIntStateOf(0) }
                    var attempts by remember { mutableIntStateOf(0) }
                    var selectedOption by remember { mutableStateOf<String?>(null) }

                    val currentQ = questions[qIndex % questions.size]

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.quiz_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.quiz_score, score, attempts),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(
                                        R.string.quiz_prompt,
                                        currentQ.areaName,
                                        HistoricalData.formatYear(currentQ.year)
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                currentQ.options.forEach { option ->
                                    val isChosen = selectedOption == option
                                    val isCorrect = option == currentQ.correctEmpire
                                    val buttonColor = when {
                                        selectedOption == null -> MaterialTheme.colorScheme.surfaceVariant
                                        isCorrect -> Color(0xFF16A34A)
                                        isChosen -> Color(0xFFDC2626)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }

                                    Surface(
                                        color = buttonColor,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable(enabled = selectedOption == null) {
                                                selectedOption = option
                                                attempts += 1
                                                if (option == currentQ.correctEmpire) {
                                                    score += 1
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = option,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(14.dp)
                                        )
                                    }
                                }

                                if (selectedOption != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = currentQ.explanation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val snapped = HistoricalData.snapToAvailableYear(currentQ.year)
                                                yearIndex = years.indexOf(snapped).coerceAtLeast(0)
                                                cameraCommand = CameraMoveCommand(currentQ.lat, currentQ.lon, 4.0)
                                                selectedTab = 0
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(stringResource(R.string.quiz_jump_map))
                                        }
                                        Button(
                                            onClick = {
                                                selectedOption = null
                                                qIndex = (qIndex + 1) % questions.size
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(stringResource(R.string.quiz_next))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
