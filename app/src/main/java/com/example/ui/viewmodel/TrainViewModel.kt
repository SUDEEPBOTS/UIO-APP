package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SavedTrain
import com.example.data.TrainCatalogItem
import com.example.data.TrainDatabase
import com.example.data.TrainRepository
import com.example.network.TrainApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val response: com.example.network.TrainResponse) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

sealed interface RouteSearchUiState {
    object Idle : RouteSearchUiState
    object Loading : RouteSearchUiState
    data class Success(val trains: List<TrainCatalogItem>) : RouteSearchUiState
    data class Error(val message: String) : RouteSearchUiState
}

class TrainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: TrainRepository
    
    init {
        val database = TrainDatabase.getDatabase(application)
        repository = TrainRepository(database.trainDao(), TrainApiService.create())
    }

    val savedTrains: StateFlow<List<SavedTrain>> = repository.savedTrains
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    // Route search states ("To & From")
    private val _fromQuery = MutableStateFlow("")
    val fromQuery: StateFlow<String> = _fromQuery.asStateFlow()

    private val _toQuery = MutableStateFlow("")
    val toQuery: StateFlow<String> = _toQuery.asStateFlow()

    private val _routeSearchUiState = MutableStateFlow<RouteSearchUiState>(RouteSearchUiState.Idle)
    val routeSearchUiState: StateFlow<RouteSearchUiState> = _routeSearchUiState.asStateFlow()

    fun updateFromQuery(query: String) {
        _fromQuery.value = query
    }

    fun updateToQuery(query: String) {
        _toQuery.value = query
    }

    fun swapStations() {
        val temp = _fromQuery.value
        _fromQuery.value = _toQuery.value
        _toQuery.value = temp
    }

    fun searchRoutes(from: String, to: String) {
        val cleanedFrom = from.trim()
        val cleanedTo = to.trim()
        if (cleanedFrom.isEmpty() || cleanedTo.isEmpty()) {
            _routeSearchUiState.value = RouteSearchUiState.Error("Please enter both Source and Destination stations.")
            return
        }

        _routeSearchUiState.value = RouteSearchUiState.Loading

        viewModelScope.launch {
            // Simulate realistic network API latency for delightful UX
            delay(800)
            try {
                val results = repository.searchTrainsBetweenStations(cleanedFrom, cleanedTo)
                _routeSearchUiState.value = RouteSearchUiState.Success(results)
            } catch (e: Exception) {
                _routeSearchUiState.value = RouteSearchUiState.Error(
                    e.localizedMessage ?: "Failed to find trains. Please check station names."
                )
            }
        }
    }

    fun clearRouteSearch() {
        _fromQuery.value = ""
        _toQuery.value = ""
        _routeSearchUiState.value = RouteSearchUiState.Idle
    }

    fun getFullTrainCatalog(): List<TrainCatalogItem> {
        return repository.getTrainCatalog()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun searchTrain(trainNo: String) {
        val trimmed = trainNo.trim()
        if (trimmed.isEmpty()) return
        
        _searchQuery.value = trimmed
        _searchUiState.value = SearchUiState.Loading

        viewModelScope.launch {
            try {
                val response = repository.getTrainStatus(trimmed)
                if (response.success == true && response.data != null) {
                    _searchUiState.value = SearchUiState.Success(response)
                    
                    // Save to local database (history)
                    val name = response.trainName ?: "$trimmed Train"
                    repository.saveTrain(trimmed, name)
                } else {
                    val msg = response.message ?: "Failed to find train details for $trimmed. Please check the train number."
                    _searchUiState.value = SearchUiState.Error(msg)
                }
            } catch (e: Exception) {
                val isOffline = e is java.net.UnknownHostException || 
                                e is java.net.ConnectException || 
                                e is java.net.SocketTimeoutException || 
                                e is java.io.IOException || 
                                e.message?.contains("Unable to resolve host", ignoreCase = true) == true
                
                _searchUiState.value = SearchUiState.Error(
                    if (isOffline) "offline_error" else (e.localizedMessage ?: "Network error. Please check your connection and try again.")
                )
            }
        }
    }

    fun toggleFavorite(train: SavedTrain) {
        viewModelScope.launch {
            repository.toggleFavorite(train)
        }
    }

    fun deleteSavedTrain(trainNo: String) {
        viewModelScope.launch {
            repository.deleteTrain(trainNo)
        }
    }
    
    fun clearSearchState() {
        _searchUiState.value = SearchUiState.Idle
        _searchQuery.value = ""
    }
}
