package com.robbiebedford.bakebook.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.robbiebedford.bakebook.data.repository.BakeBookRepository

class BakeBookViewModelFactory(private val repository: BakeBookRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BakeBookViewModel(repository) as T
    }
}
