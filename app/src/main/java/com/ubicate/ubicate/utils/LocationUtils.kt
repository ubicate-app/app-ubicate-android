package com.ubicate.ubicate.utils

import com.google.android.gms.maps.model.LatLng

object LocationUtils {

    // Calcular la distancia entre dos puntos (en metros)
    fun calculateDistance(from: LatLng, to: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results)
        return results[0]
    }

    // Calcular la distancia total de la ruta (acumulada)
    fun calculateTotalRouteDistance(routeCoordinates: List<LatLng>): Float {
        var totalDistance = 0f
        for (i in 0 until routeCoordinates.size - 1) {
            val start = routeCoordinates[i]
            val end = routeCoordinates[i + 1]
            val results = FloatArray(1)
            android.location.Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results)
            totalDistance += results[0]
        }
        return totalDistance
    }

    // Calcular la distancia recorrida por el bus a lo largo de la ruta
    fun calculateDistanceTravelled(busLocation: LatLng, routeCoordinates: List<LatLng>): Float {
        var distanceTravelled = 0f
        for (i in 0 until routeCoordinates.size - 1) {
            val start = routeCoordinates[i]
            val end = routeCoordinates[i + 1]

            // Si la ubicación del bus está más cerca de este segmento, agregamos la distancia
            val results = FloatArray(1)
            android.location.Location.distanceBetween(busLocation.latitude, busLocation.longitude, start.latitude, start.longitude, results)
            val distanceToStart = results[0]

            android.location.Location.distanceBetween(busLocation.latitude, busLocation.longitude, end.latitude, end.longitude, results)
            val distanceToEnd = results[0]

            // Acumulamos la distancia total recorrida a lo largo de la ruta
            val segmentDistance = Math.min(distanceToStart, distanceToEnd)
            if (segmentDistance < distanceToEnd) {
                distanceTravelled += segmentDistance
            }
        }
        return distanceTravelled
    }

    // Calcular el ETA en minutos basado en la distancia restante y la velocidad promedio
    fun calculateETA(distanceRemainingInMeters: Float, averageSpeedKmH: Float = 20f): String {
        val speedInMetersPerSecond = averageSpeedKmH * 1000 / 3600
        val etaInSeconds = (distanceRemainingInMeters / speedInMetersPerSecond).toInt()
        val etaInMinutes = etaInSeconds / 60
        return "$etaInMinutes min"
    }
}
