package com.ubicate.ubicate.repository

import com.google.android.gms.maps.model.LatLng
import com.ubicate.ubicate.utils.LocationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class ETARepository(private val apiKey: String) {

    // Obtener ETA y distancia
    suspend fun getETA(userLocation: LatLng, busLocation: LatLng): Pair<String, String> {
        return withContext(Dispatchers.IO) {
            val distance = LocationUtils.calculateDistance(userLocation, busLocation)
            val eta = LocationUtils.calculateETA(distance)

            return@withContext Pair(eta, "$distance m")
        }
    }
}
