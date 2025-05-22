package com.ubicate.ubicate.viewmodel.factory


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ubicate.ubicate.repository.ETARepository
import com.ubicate.ubicate.viewmodel.ETAViewModel

class ETAViewModelFactory(private val etaRepository: ETARepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ETAViewModel::class.java)) {
            return ETAViewModel(etaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
