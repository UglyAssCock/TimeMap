package com.timemap.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

object GeoJsonManager {

    private val memoryCache = mutableMapOf<Int, String>()

    // Palette of rich, high-contrast historical atlas colors
    private val EMPIRE_PALETTE = listOf(
        "#D9534F", "#428BCA", "#5CB85C", "#F0AD4E", "#9B59B6",
        "#1ABC9C", "#E67E22", "#3498DB", "#E74C3C", "#2ECC71",
        "#F39C12", "#8E44AD", "#16A085", "#D35400", "#2980B9",
        "#C0392B", "#27AE60", "#D4AC0D", "#7D3C98", "#138D75"
    )

    private fun getCacheDir(context: Context): File {
        val dir = File(context.filesDir, "geojson_cache")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun isYearCached(context: Context, year: Int): Boolean {
        val snapped = HistoricalData.snapToAvailableYear(year)
        if (memoryCache.containsKey(snapped)) return true
        val file = File(getCacheDir(context), HistoricalData.getFileNameForYear(snapped))
        return file.exists() && file.length() > 100
    }

    fun getCachedYearsCount(context: Context): Int {
        return HistoricalData.AVAILABLE_YEARS.count { isYearCached(context, it) }
    }

    suspend fun loadEnrichedGeoJson(context: Context, year: Int): Result<String> = withContext(Dispatchers.IO) {
        val snapped = HistoricalData.snapToAvailableYear(year)
        memoryCache[snapped]?.let { return@withContext Result.success(it) }

        val cacheFile = File(getCacheDir(context), HistoricalData.getFileNameForYear(snapped))
        if (cacheFile.exists() && cacheFile.length() > 100) {
            try {
                val cachedText = cacheFile.readText()
                memoryCache[snapped] = cachedText
                return@withContext Result.success(cachedText)
            } catch (_: Exception) {
            }
        }

        // Download from aourednik/historical-basemaps on GitHub
        val urlString = HistoricalData.getGeoJsonUrl(snapped)
        try {
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 20000
                readTimeout = 25000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.geo+json, application/json")
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val rawGeoJson = connection.inputStream.bufferedReader().use { it.readText() }
                val enriched = enrichWithColors(rawGeoJson)
                cacheFile.writeText(enriched)
                memoryCache[snapped] = enriched
                Result.success(enriched)
            } else {
                Result.failure(Exception("HTTP ${connection.responseCode} for $urlString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Injects deterministic COLOR_HEX and DISPLAY_NAME into each feature's properties
    private fun enrichWithColors(rawJson: String): String {
        return try {
            val root = JSONObject(rawJson)
            val features = root.optJSONArray("features") ?: return rawJson
            for (i in 0 until features.length()) {
                val feature = features.optJSONObject(i) ?: continue
                var props = feature.optJSONObject("properties")
                if (props == null) {
                    props = JSONObject()
                    feature.put("properties", props)
                }
                val name = props.optString("NAME", "").trim()
                val subjecto = props.optString("SUBJECTO", "").trim()
                val partof = props.optString("PARTOF", "").trim()

                val keyForColor = when {
                    subjecto.isNotEmpty() && subjecto != "null" -> subjecto
                    partof.isNotEmpty() && partof != "null" -> partof
                    name.isNotEmpty() && name != "null" -> name
                    else -> "Unclaimed Territory"
                }

                val displayTitle = if (name.isNotEmpty() && name != "null") name else keyForColor
                val colorIndex = abs(keyForColor.hashCode()) % EMPIRE_PALETTE.size
                props.put("COLOR_HEX", EMPIRE_PALETTE[colorIndex])
                props.put("DISPLAY_NAME", displayTitle)
                props.put("RULING_POWER", keyForColor)
            }
            root.toString()
        } catch (_: Exception) {
            rawJson
        }
    }

    // Builds a GeoJSON FeatureCollection for historical events near the active year
    fun buildEventsGeoJson(activeYear: Int): String {
        val threshold = when {
            activeYear < -500 -> 350
            activeYear < 1000 -> 120
            activeYear < 1800 -> 45
            else -> 18
        }
        val root = JSONObject()
        root.put("type", "FeatureCollection")
        val features = JSONArray()

        val filtered = HistoricalData.EVENTS.filter { abs(it.year - activeYear) <= threshold }
        for (ev in filtered) {
            val feat = JSONObject()
            feat.put("type", "Feature")

            val geom = JSONObject()
            geom.put("type", "Point")
            val coords = JSONArray()
            coords.put(ev.lon)
            coords.put(ev.lat)
            geom.put("coordinates", coords)
            feat.put("geometry", geom)

            val props = JSONObject()
            props.put("EVENT_ID", ev.id)
            props.put("EVENT_TITLE", "${ev.title} (${HistoricalData.formatYear(ev.year)})")
            props.put("EVENT_YEAR", ev.year)
            props.put("EVENT_CATEGORY", ev.category)
            props.put("EVENT_SUMMARY", ev.summary)
            feat.put("properties", props)

            features.put(feat)
        }
        root.put("features", features)
        return root.toString()
    }
}
