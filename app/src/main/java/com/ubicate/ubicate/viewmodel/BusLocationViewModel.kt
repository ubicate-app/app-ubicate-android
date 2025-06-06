package com.ubicate.ubicate.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ubicate.ubicate.model.BusLocation
import com.ubicate.ubicate.repository.BusLocationRepository
import com.google.firebase.database.ValueEventListener

class BusLocationViewModel(private val repository: BusLocationRepository) : ViewModel() {

    private val _busLocations = MutableLiveData<List<BusLocation>>()
    val busLocations: LiveData<List<BusLocation>> = _busLocations

    private var listener: ValueEventListener? = null

    fun startListening() {
        Log.d("BusLocationViewModel", "startListening() called")
        listener = repository.listenBusLocations(
            onUpdate = { buses ->
                Log.d("BusLocationViewModel", "Received bus list with size: ${buses.size}")
                _busLocations.postValue(buses)
            },
            onError = { error ->
                Log.e("BusLocationViewModel", "Error received", error.toException())
            }
        )
    }

    fun stopListening() {
        listener?.let { repository.stopListening(it) }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}