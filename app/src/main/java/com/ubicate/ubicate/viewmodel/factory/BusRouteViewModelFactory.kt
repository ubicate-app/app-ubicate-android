package com.ubicate.ubicate.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ubicate.ubicate.repository.RouteRepository
import com.ubicate.ubicate.viewmodel.BusRouteViewModel

class BusRouteViewModelFactory(private val repository: RouteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BusRouteViewModel::class.java)) {
            return BusRouteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
