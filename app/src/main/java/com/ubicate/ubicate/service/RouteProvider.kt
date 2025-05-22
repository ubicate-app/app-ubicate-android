package com.ubicate.ubicate.service

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class RouteProvider {

    private val apiKey = "AIzaSyB1xJSLK1YYe2X31DDMD1JlB8KN63HqR_U"

    private val busRouteCoordinates = listOf(
        LatLng(-8.145546782448399, -79.05081480127104),
        LatLng(-8.13609944766984, -79.04284531255807),
        LatLng(-8.134928536882555, -79.03624969003528),
        LatLng(-8.131669654027455, -79.03365076503539),
        LatLng(-8.127381649721473, -79.04118256014931),
        LatLng(-8.116979224725338, -79.03809442648347),
        LatLng(-8.112677345368397, -79.03442928899199),
        LatLng(-8.118976801935707, -79.0256443973289),
        LatLng(-8.10417038846796, -79.01035837066703),
        LatLng(-8.101023204586303, -79.01210543569688),
        LatLng(-8.093692127367936, -79.0242848315085),
        LatLng(-8.097463344231686, -79.02967344131885),
        LatLng(-8.050201865117678, -79.05629006393579),
        LatLng(-8.049346177004681, -79.05383068032637)
    )

    private val client = OkHttpClient()

    suspend fun createPolyline(): PolylineOptions = suspendCancellableCoroutine { cont ->
        if (busRouteCoordinates.size < 2) {
            cont.resumeWithException(IllegalArgumentException("Se necesitan al menos dos puntos para trazar la ruta"))
            return@suspendCancellableCoroutine
        }

        val origin = busRouteCoordinates.first()
        val destination = busRouteCoordinates.last()
        val waypoints = busRouteCoordinates.subList(1, busRouteCoordinates.size - 1)

        val url = buildDirectionsUrl(origin, destination, waypoints)

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        if (cont.isActive) cont.resumeWithException(IOException("Error en respuesta HTTP: ${response.code}"))
                        return
                    }

                    val body = response.body?.string()
                    if (body == null) {
                        if (cont.isActive) cont.resumeWithException(IOException("Respuesta vacía"))
                        return
                    }

                    try {
                        val json = JSONObject(body)
                        val status = json.getString("status")
                        if (status != "OK") {
                            if (cont.isActive) cont.resumeWithException(IOException("Google Directions API status: $status"))
                            return
                        }

                        val routes = json.getJSONArray("routes")
                        if (routes.length() == 0) {
                            if (cont.isActive) cont.resumeWithException(IOException("No se encontraron rutas"))
                            return
                        }

                        val overviewPolyline = routes.getJSONObject(0).getJSONObject("overview_polyline")
                        val encodedPolyline = overviewPolyline.getString("points")
                        val decodedPath = decodePolyline(encodedPolyline)

                        val polylineOptions = PolylineOptions()
                            .addAll(decodedPath)
                            .width(10f)
                            .color(0xFF0000FF.toInt()) // Azul

                        if (cont.isActive) cont.resume(polylineOptions)

                    } catch (ex: Exception) {
                        if (cont.isActive) cont.resumeWithException(ex)
                    }
                }
            }
        })
    }

    private fun buildDirectionsUrl(origin: LatLng, destination: LatLng, waypoints: List<LatLng>): String {
        val originParam = "origin=${origin.latitude},${origin.longitude}"
        val destinationParam = "destination=${destination.latitude},${destination.longitude}"

        val waypointsParam = if (waypoints.isNotEmpty()) {
            val points = waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }
            "waypoints=$points"
        } else {
            ""
        }

        return "https://maps.googleapis.com/maps/api/directions/json?$originParam&$destinationParam&$waypointsParam&key=$apiKey"
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }
}
