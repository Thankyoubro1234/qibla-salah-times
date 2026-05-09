package com.prayerpilot.app.prayer

import kotlin.math.*

object Qibla {
    private const val MECCA_LAT = 21.4225
    private const val MECCA_LNG = 39.8262

    fun bearingFrom(lat: Double, lng: Double): Double {
        val phi1 = Math.toRadians(lat)
        val phi2 = Math.toRadians(MECCA_LAT)
        val deltaLng = Math.toRadians(MECCA_LNG - lng)
        val y = sin(deltaLng) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLng)
        val brng = Math.toDegrees(atan2(y, x))
        return (brng + 360) % 360
    }

    fun distanceKm(lat: Double, lng: Double): Double {
        val r = 6371.0
        val phi1 = Math.toRadians(lat)
        val phi2 = Math.toRadians(MECCA_LAT)
        val dPhi = Math.toRadians(MECCA_LAT - lat)
        val dLng = Math.toRadians(MECCA_LNG - lng)
        val a = sin(dPhi/2).pow(2) + cos(phi1)*cos(phi2)*sin(dLng/2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}
