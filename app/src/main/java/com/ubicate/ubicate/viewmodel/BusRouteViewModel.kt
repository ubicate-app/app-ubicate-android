package com.ubicate.ubicate.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ubicate.ubicate.repository.RouteRepository
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.launch

class BusRouteViewModel(private val repository: RouteRepository) : ViewModel() {

    private val _busRoute = MutableLiveData<PolylineOptions>()
    val busRoute: LiveData<PolylineOptions> = _busRoute

    fun loadSnappedRoute() {
        viewModelScope.launch {
            val polyline = repository.fetchSnappedBusRoute()
            _busRoute.postValue(polyline)
        }
    }
}
