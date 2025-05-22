package com.ubicate.ubicate.utils

import com.google.android.gms.maps.model.PolylineOptions
import com.ubicate.ubicate.service.RouteProvider

object RouteUtils {

    // Obtener la ruta del bus desde el servicio RouteProvider
    suspend fun getRoute(): PolylineOptions {
        val routeProvider = RouteProvider()  // Crear una instancia de RouteProvider
        return routeProvider.createPolyline() // Obtener el PolylineOptions desde RouteProvider
    }
}
