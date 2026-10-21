package com.gadagojuanilkumar.fmradioindiaak

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class PlaceInfo(
    val lat: Double,
    val lon: Double,
    val countryCode: String,
    val countryName: String,
    val region: String
)

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(context: Context): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = lm.getProviders(true)

        var best: Location? = null
        for (p in providers) {
            try {
                val l = lm.getLastKnownLocation(p)
                if (l != null && (best == null || l.time > best.time)) best = l
            } catch (e: Exception) {
                // ignore this provider
            }
        }
        if (best != null) return best

        val provider = when {
            providers.contains(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            providers.contains(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return null
        }

        return withTimeoutOrNull(15000) {
            suspendCancellableCoroutine<Location?> { cont ->
                try {
                    if (Build.VERSION.SDK_INT >= 30) {
                        lm.getCurrentLocation(provider, null, context.mainExecutor) { loc ->
                            if (cont.isActive) cont.resume(loc)
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        lm.requestSingleUpdate(
                            provider,
                            object : LocationListener {
                                override fun onLocationChanged(location: Location) {
                                    if (cont.isActive) cont.resume(location)
                                }

                                override fun onProviderEnabled(provider: String) {}
                                override fun onProviderDisabled(provider: String) {}

                                @Deprecated("Deprecated in Java")
                                override fun onStatusChanged(
                                    provider: String?,
                                    status: Int,
                                    extras: Bundle?
                                ) {
                                }
                            },
                            Looper.getMainLooper()
                        )
                    }
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }

    suspend fun describe(context: Context, loc: Location): PlaceInfo =
        withContext(Dispatchers.IO) {
            var code = ""
            var country = ""
            var region = ""
            try {
                if (Geocoder.isPresent()) {
                    @Suppress("DEPRECATION")
                    val list = Geocoder(context, Locale.ENGLISH)
                        .getFromLocation(loc.latitude, loc.longitude, 1)
                    val a = list?.firstOrNull()
                    if (a != null) {
                        code = a.countryCode ?: ""
                        country = a.countryName ?: ""
                        region = a.adminArea ?: ""
                    }
                }
            } catch (e: Exception) {
                // no place name available
            }
            PlaceInfo(loc.latitude, loc.longitude, code, country, region)
        }
}