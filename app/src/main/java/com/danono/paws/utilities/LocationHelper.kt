package com.danono.paws.utilities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import kotlin.math.*

class LocationHelper(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    interface LocationCallback {
        fun onLocationReceived(location: Location)
        fun onLocationError(error: String)
    }

    fun getCurrentLocation(callback: LocationCallback) {
        if (!hasLocationPermission()) {
            callback.onLocationError("Location permission not granted")
            return
        }

        if (!isLocationEnabled()) {
            callback.onLocationError("Location services disabled")
            return
        }

        try {
            // Try GPS first
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestSingleUpdate(
                    LocationManager.GPS_PROVIDER,
                    object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            callback.onLocationReceived(location)
                        }

                        @Deprecated("Deprecated in API level 29")
                        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    },
                    null
                )
            }
            // Fallback to network provider
            else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestSingleUpdate(
                    LocationManager.NETWORK_PROVIDER,
                    object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            callback.onLocationReceived(location)
                        }

                        @Deprecated("Deprecated in API level 29")
                        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    },
                    null
                )
            } else {
                callback.onLocationError("No location providers available")
            }
        } catch (e: Exception) {
            callback.onLocationError("Error getting location: ${e.message}")
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    companion object {
        /**
         * Calculate distance between two points using Haversine formula
         */
        fun calculateDistance(
            lat1: Double, lon1: Double,
            lat2: Double, lon2: Double
        ): Double {
            val R = 6371 // Earth's radius in kilometers

            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)

            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)

            val c = 2 * atan2(sqrt(a), sqrt(1 - a))

            return R * c // Distance in kilometers
        }
    }
}