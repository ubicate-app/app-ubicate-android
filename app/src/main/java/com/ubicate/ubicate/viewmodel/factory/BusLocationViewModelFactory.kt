package com.ubicate.ubicate.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ubicate.ubicate.repository.BusLocationRepository
import com.ubicate.ubicate.viewmodel.BusLocationViewModel

class BusLocationViewModelFactory(private val repository: BusLocationRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BusLocationViewModel::class.java)) {
            return BusLocationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
