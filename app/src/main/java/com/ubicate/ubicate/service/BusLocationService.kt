package com.ubicate.ubicate.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.ubicate.ubicate.repository.BusLocationRepository

class BusLocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val handler = Handler(Looper.getMainLooper())
    private var isServiceRunning = false
    private val locationUpdateInterval: Long = 5000

    private lateinit var busLocationRepository: BusLocationRepository

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        busLocationRepository = BusLocationRepository()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (!isServiceRunning) {
            isServiceRunning = true
            startLocationUpdates()
        }
        return START_STICKY
    }


    private fun startLocationUpdates() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                getCurrentLocationAndUpdateBusLocation()
                handler.postDelayed(this, locationUpdateInterval)
            }
        }, locationUpdateInterval)
    }

    private fun getCurrentLocationAndUpdateBusLocation() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {

                    val latitude = it.latitude
                    val longitude = it.longitude

                    val busId = getBusId()
                    busId?.let {

                        busLocationRepository.updateBusLocation(busId, latitude, longitude)
                    }
                }
            }.addOnFailureListener {

            }
        } else {
            requestLocationPermission()
        }
    }

    private fun getBusId(): String? {

        val sharedPrefs = getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE)
        return sharedPrefs.getString("bus_id", null)
    }

    private fun requestLocationPermission() {

    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
    }
}
