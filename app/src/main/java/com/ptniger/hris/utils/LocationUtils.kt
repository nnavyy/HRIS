package com.ptniger.hris.utils

import android.location.Location

object LocationUtils {
    
    /**
     * Calculates the distance in meters between two geographical points.
     */
    fun calculateDistance(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, results)
        return results[0]
    }

    /**
     * Checks if a given location is within the allowed radius of an office location.
     */
    fun isWithinRadius(
        userLat: Double,
        userLng: Double,
        officeLat: Double,
        officeLng: Double,
        allowedRadiusMeters: Double
    ): Boolean {
        val distance = calculateDistance(userLat, userLng, officeLat, officeLng)
        return distance <= allowedRadiusMeters
    }
}
