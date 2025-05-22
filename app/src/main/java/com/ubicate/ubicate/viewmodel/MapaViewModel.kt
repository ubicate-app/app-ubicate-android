package com.ubicate.ubicate.viewmodel


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.ubicate.ubicate.repository.LocationRepository

class MapaViewModel(private val locationRepository: LocationRepository) : ViewModel() {
    private val _userLocation = MutableLiveData<LatLng?>()
    val userLocation: LiveData<LatLng?> = _userLocation

    fun fetchUserLocation(hasPermission: Boolean) {
        if (!hasPermission) return
        locationRepository.getLastKnownLocation(true) { location ->
            if (location != null) {
                _userLocation.postValue(LatLng(location.latitude, location.longitude))
            } else {
                _userLocation.postValue(null)
            }
        }
    }
}

