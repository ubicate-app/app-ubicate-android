package com.ubicate.ubicate.utils

import com.google.android.gms.maps.model.LatLng

object LocationUtils {

    // Calcular la distancia entre dos puntos (en metros)
    fun calculateDistance(from: LatLng, to: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results)
        return results[0]
    }

    // Calcular el ETA basado en la distancia y la velocidad del bus
    fun calculateETA(distanceInMeters: Float, averageSpeed: Float = 20f): String {
        val speedInMetersPerSecond = averageSpeed * 1000 / 3600
        val etaInSeconds = (distanceInMeters / speedInMetersPerSecond).toInt()
        val etaInMinutes = etaInSeconds / 60
        return "$etaInMinutes min"
    }

    // Encontrar el punto más cercano en la ruta
    fun findNearestPointInRoute(userLocation: LatLng, routeCoordinates: List<LatLng>): LatLng {
        var nearestPoint: LatLng = routeCoordinates[0]
        var shortestDistance = Float.MAX_VALUE

        for (point in routeCoordinates) {
            val distance = calculateDistance(userLocation, point)
            if (distance < shortestDistance) {
                shortestDistance = distance
                nearestPoint = point
            }
        }
        return nearestPoint
    }
}
