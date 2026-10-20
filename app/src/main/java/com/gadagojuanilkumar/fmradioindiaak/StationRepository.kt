package com.gadagojuanilkumar.fmradioindiaak

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object StationRepository {

    private val servers = listOf(
        "https://de1.api.radio-browser.info",
        "https://de2.api.radio-browser.info",
        "https://fi1.api.radio-browser.info"
    )

    suspend fun fetchByState(state: String): List<Station> {
        val encodedState = URLEncoder.encode(state, "UTF-8")
        val path = "/json/stations/search?countrycode=IN&state=$encodedState" +
                "&hidebroken=true&order=clickcount&reverse=true&limit=100"
        return request(path, state)
    }

    suspend fun searchByName(query: String): List<Station> {
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val path = "/json/stations/search?countrycode=IN&name=$encodedQuery" +
                "&hidebroken=true&order=clickcount&reverse=true&limit=100"
        return request(path, "India")
    }

    suspend fun fetchCustom(): List<Station> = withContext(Dispatchers.IO) {
        try {
            val address = "https://raw.githubusercontent.com/gadagojuanilkumar/" +
                    "FM-Radio-India-AK/main/data/stations.json"
            val connection = URL(address).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", "FMRadioIndiaAK/1.0")
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val array = JSONArray(text)
            val list = mutableListOf<Station>()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val name = item.optString("name").trim()
                val url = item.optString("streamUrl").trim()
                if (name.isBlank() || url.isBlank()) continue
                list.add(
                    Station(
                        id = item.optString("id").ifBlank { "custom-$i" },
                        name = name,
                        state = item.optString("state").ifBlank { "India" },
                        streamUrl = url,
                        logoUrl = item.optString("logoUrl")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }


    suspend fun fetchAllIndia(): List<Station> {
        val path = "/json/stations/search?countrycode=IN&hidebroken=true" +
                "&order=clickcount&reverse=true&limit=10000"
        return request(path, "India")
    }
    suspend fun fetchStateFixes(): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val address = "https://raw.githubusercontent.com/gadagojuanilkumar/" +
                    "FM-Radio-India-AK/main/data/state_fix.json"
            val connection = URL(address).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", "FMRadioIndiaAK/1.0")
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val obj = JSONObject(text)
            val map = mutableMapOf<String, String>()
            for (key in obj.keys()) {
                map[key] = obj.getString(key)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }
    suspend fun fetchDirectory(): List<FmEntry> = withContext(Dispatchers.IO) {
        try {
            val address = "https://raw.githubusercontent.com/gadagojuanilkumar/" +
                    "FM-Radio-India-AK/main/data/fm_directory.json"
            val connection = URL(address).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            connection.setRequestProperty("User-Agent", "FMRadioIndiaAK/1.0")
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val array = JSONArray(text)
            val list = mutableListOf<FmEntry>()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                list.add(
                    FmEntry(
                        state = item.optString("state"),
                        city = item.optString("city"),
                        frequency = item.optString("frequency"),
                        name = item.optString("name"),
                        remarks = item.optString("remarks"),
                        streamUrl = item.optString("streamUrl"),
                        streamName = item.optString("streamName")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun findStream(entry: FmEntry): Station? {
        val isAir = entry.name.equals("Akashvani", ignoreCase = true)
        val city = entry.city.substringBefore("(").trim()
        val queries = (if (isAir) listOf(city) else listOf(entry.name, city))
            .filter { it.isNotBlank() }

        var best: Station? = null
        var bestScore = 0
        for (query in queries) {
            for (s in searchByName(query)) {
                val n = s.name.lowercase()
                var score = 0
                if (n.contains(entry.frequency)) score += 3
                val nameMatch = if (isAir) {
                    n.contains("akashvani") || n.contains("all india radio") ||
                            Regex("\\bair\\b").containsMatchIn(n)
                } else {
                    n.contains(entry.name.lowercase())
                }
                if (nameMatch) score += 2
                if (city.isNotBlank() && n.contains(city.lowercase())) score += 2
                if (s.state.equals(entry.state, ignoreCase = true)) score += 1
                if (score > bestScore) {
                    best = s
                    bestScore = score
                }
            }
            if (bestScore >= 4) break
        }
        return if (bestScore >= 4) best else null
    }
    private suspend fun request(path: String, fallbackState: String): List<Station> =
        withContext(Dispatchers.IO) {
            for (server in servers) {
                try {
                    val connection = URL(server + path).openConnection() as HttpURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 40000
                    connection.setRequestProperty("User-Agent", "FMRadioIndiaAK/1.0")
                    val text = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()
                    return@withContext parse(text, fallbackState)
                } catch (e: Exception) {
                    // Try the next server
                }
            }
            emptyList()
        }

    private fun parse(json: String, fallbackState: String): List<Station> {
        val array = JSONArray(json)
        val list = mutableListOf<Station>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val url = item.optString("url_resolved").ifBlank { item.optString("url") }
            val name = item.optString("name").trim()
            if (url.isBlank() || name.isBlank()) continue
            list.add(
                Station(
                    id = item.optString("stationuuid"),
                    name = name,
                    state = item.optString("state").ifBlank { fallbackState },
                    streamUrl = url,
                    logoUrl = item.optString("favicon"),
                    language = item.optString("language")
                )
            )
        }
        return list
    }
}