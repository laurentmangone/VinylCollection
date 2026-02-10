package com.example.vinylcollection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class VinylViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VinylRepository(VinylDatabase.getInstance(application).vinylDao())
    private val query = MutableStateFlow("")

    val vinyls: StateFlow<List<Vinyl>> = query
        .flatMapLatest { repository.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(text: String) {
        query.value = text.trim()
    }

    fun add(vinyl: Vinyl) {
        viewModelScope.launch {
            repository.insert(vinyl)
        }
    }

    fun update(vinyl: Vinyl) {
        viewModelScope.launch {
            repository.update(vinyl)
        }
    }

    fun delete(vinyl: Vinyl) {
        viewModelScope.launch {
            repository.delete(vinyl)
        }
    }
}
