package com.rahmatulula.motocare.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat

/** Lokasi terakhir perangkat; bila izin ditolak dipakai lokasi bawaan (Jakarta). */
object LocationHelper {
    data class Coord(val lat: Double, val lon: Double, val isDefault: Boolean)

    private val DEFAULT = Coord(-6.2088, 106.8456, true)

    fun hasPermission(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun current(ctx: Context): Coord {
        if (!hasPermission(ctx)) return DEFAULT
        return try {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = lm.getProviders(true)
                .mapNotNull { lm.getLastKnownLocation(it) }
                .maxByOrNull { it.time }
            if (loc != null) Coord(loc.latitude, loc.longitude, false) else DEFAULT
        } catch (e: SecurityException) {
            DEFAULT
        }
    }
}
