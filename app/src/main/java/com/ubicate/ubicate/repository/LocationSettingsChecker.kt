package com.ubicate.ubicate.repository

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.location.LocationManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

class LocationSettingsChecker(private val activity: Activity) {

    private val settingsClient: SettingsClient = LocationServices.getSettingsClient(activity)

    private val locationRequest: LocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
        .setWaitForAccurateLocation(false)
        .build()

    private val locationSettingsRequest = LocationSettingsRequest.Builder()
        .addLocationRequest(locationRequest)
        .build()

    companion object {
        const val REQUEST_CHECK_SETTINGS = 1001
    }

    fun checkLocationSettings(
        onLocationEnabled: () -> Unit,
        onLocationDisabled: () -> Unit
    ) {
        val task = settingsClient.checkLocationSettings(locationSettingsRequest)

        task.addOnSuccessListener {
            onLocationEnabled()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    onLocationDisabled()
                }
            } else {
                onLocationDisabled()
            }
        }
    }

    // Aquí agregas el método isLocationEnabled para chequear si la ubicación está activa
    fun isLocationEnabled(): Boolean {
        val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}
