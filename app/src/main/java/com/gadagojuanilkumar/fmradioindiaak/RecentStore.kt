package com.gadagojuanilkumar.fmradioindiaak

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// Remembers the last stations the user played (newest first)
object RecentStore {
    private const val PREFS = "recent_stations"
    private const val KEY = "list"
    private const val MAX = 20

    fun load(context: Context): List<Station> {
        val text = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(text)
            val list = mutableListOf<Station>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    Station(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        state = o.optString("state"),
                        streamUrl = o.getString("streamUrl"),
                        logoUrl = o.optString("logoUrl"),
                        language = o.optString("language"),
                        lat = o.optDouble("lat", 0.0),
                        lon = o.optDouble("lon", 0.0),
                        online = o.optBoolean("online", true),
                        bitrate = o.optInt("bitrate", 0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Puts the station at the top of the list and returns the new list
    fun add(context: Context, station: Station): List<Station> {
        val list = (listOf(station) + load(context).filter { it.id != station.id }).take(MAX)
        save(context, list)
        return list
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }

    private fun save(context: Context, list: List<Station>) {
        val array = JSONArray()
        for (s in list) {
            array.put(
                JSONObject()
                    .put("id", s.id)
                    .put("name", s.name)
                    .put("state", s.state)
                    .put("streamUrl", s.streamUrl)
                    .put("logoUrl", s.logoUrl)
                    .put("language", s.language)
                    .put("lat", s.lat)
                    .put("lon", s.lon)
                    .put("online", s.online)
                    .put("bitrate", s.bitrate)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, array.toString())
            .apply()
    }
}