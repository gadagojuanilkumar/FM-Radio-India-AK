package com.gadagojuanilkumar.fmradioindiaak

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class Station(
    val id: String,
    val name: String,
    val state: String,
    val streamUrl: String,
    val logoUrl: String = "",
    val language: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val online: Boolean = true,
    val bitrate: Int = 0
)

fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val p = Math.PI / 180
    val a = sin((lat2 - lat1) * p / 2).pow(2) +
            cos(lat1 * p) * cos(lat2 * p) * sin((lon2 - lon1) * p / 2).pow(2)
    return 12742 * asin(sqrt(a))
}