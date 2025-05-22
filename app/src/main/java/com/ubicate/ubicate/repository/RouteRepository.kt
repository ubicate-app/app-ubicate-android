package com.ubicate.ubicate.repository

import com.google.android.gms.maps.model.PolylineOptions
import com.ubicate.ubicate.service.RouteProvider


class RouteRepository(private val routeProvider: RouteProvider) {

    suspend fun fetchSnappedBusRoute(): PolylineOptions {
        return routeProvider.createPolyline()
    }
}

