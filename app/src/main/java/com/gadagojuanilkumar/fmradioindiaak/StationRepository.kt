package com.gadagojuanilkumar.fmradioindiaak

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

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

    private suspend fun request(path: String, fallbackState: String): List<Station> =
        withContext(Dispatchers.IO) {
            for (server in servers) {
                try {
                    val connection = URL(server + path).openConnection() as HttpURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 15000
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
                    logoUrl = item.optString("favicon")
                )
            )
        }
        return list
    }
}