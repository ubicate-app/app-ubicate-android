package com.ubicate.ubicate.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.ubicate.ubicate.repository.ETARepository
import kotlinx.coroutines.launch

class ETAViewModel(private val etaRepository: ETARepository) : ViewModel() {

    private val _etaData = MutableLiveData<Pair<String, String>>()
    val etaData: LiveData<Pair<String, String>> = _etaData

    // Método para cargar el tiempo estimado de llegada y la distancia
    fun fetchETA(userLocation: LatLng, busLocation: LatLng) {
        viewModelScope.launch {
            val result = etaRepository.getETA(userLocation, busLocation)
            _etaData.postValue(result) // Actualiza el LiveData con ETA y distancia
        }
    }
}
