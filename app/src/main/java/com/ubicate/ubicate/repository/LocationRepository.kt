package com.ubicate.ubicate.repository

import android.content.Context
import android.location.Location

import com.google.android.gms.location.LocationServices

class LocationRepository(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fun getLastKnownLocation(hasLocationPermission: Boolean, onLocationResult: (Location?) -> Unit) {
        if (!hasLocationPermission) {

            onLocationResult(null)
            return
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location -> onLocationResult(location) }
                .addOnFailureListener { onLocationResult(null) }
        } catch (e: SecurityException) {

            onLocationResult(null)
        }
    }


}
