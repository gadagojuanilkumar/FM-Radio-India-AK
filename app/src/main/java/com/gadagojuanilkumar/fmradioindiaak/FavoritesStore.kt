package com.gadagojuanilkumar.fmradioindiaak

import android.content.Context

object FavoritesStore {
    private const val PREFS = "favorites"
    private const val KEY = "ids"

    fun load(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY, emptySet())?.toSet() ?: emptySet()
    }

    fun save(context: Context, ids: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY, ids)
            .apply()
    }
}