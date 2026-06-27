package com.ptniger.hris.utils

import android.location.Location
import android.os.Build

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

    /**
     * Detects if a Location object is from a mock/fake GPS provider.
     * Works on both old and new Android API levels.
     */
    fun isMockLocation(location: Location): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }
    }

    /**
     * Checks if developer mock location setting is enabled on the device.
     */
    fun isMockLocationEnabled(context: android.content.Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val mockLocationApp = android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    "mock_location"
                )
                mockLocationApp != null && mockLocationApp != "0"
            } else {
                @Suppress("DEPRECATION")
                android.provider.Settings.Secure.getInt(
                    context.contentResolver,
                    android.provider.Settings.Secure.ALLOW_MOCK_LOCATION, 0
                ) != 0
            }
        } catch (e: Exception) {
            false
        }
    }
}
