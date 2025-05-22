package com.ubicate.ubicate.repository

import android.content.Context
import android.location.Location

import com.google.android.gms.location.LocationServices

class LocationRepository(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fun getLastKnownLocation(hasLocationPermission: Boolean, onLocationResult: (Location?) -> Unit) {
        if (!hasLocationPermission) {
            // No hay permiso, no intentamos obtener la ubicación
            onLocationResult(null)
            return
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location -> onLocationResult(location) }
                .addOnFailureListener { onLocationResult(null) }
        } catch (e: SecurityException) {
            // Captura caso donde el permiso no está realmente concedido (por ejemplo cambio en runtime)
            onLocationResult(null)
        }
    }
}
