package com.tmdb.ui.screens.results

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tmdb.data.repository.MovieRepository
import com.tmdb.domain.model.Movie
import com.tmdb.util.isNetworkError
import com.tmdb.util.toUserFriendlyMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
class ResultsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val query: String = savedStateHandle.get<String>("query").orEmpty()

    private val _uiState = MutableStateFlow(ResultsUiState(query = query))
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()
    private var refreshJob: Job? = null
    private var loadMoreJob: Job? = null

    init {
        observeResults()
        refresh()
    }

    fun refresh() {
        loadMoreJob?.cancel()
        refreshJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = true,
                isLoadingMore = false,
                errorMessage = null,
                loadMoreError = null,
                isNetworkError = false,
                currentPage = 1
            )
        }
        refreshJob = viewModelScope.launch {
            repository.refreshSearchResults(query)
                .onSuccess { pageResult ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            currentPage = pageResult.currentPage,
                            totalPages = pageResult.totalPages,
                            hasMorePages = pageResult.hasMorePages,
                            errorMessage = null,
                            isNetworkError = false
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = throwable.toUserFriendlyMessage(context.resources),
                            isNetworkError = throwable.isNetworkError()
                        )
                    }
                }
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isLoadingMore || !currentState.hasMorePages) return

        val nextPage = currentState.currentPage + 1
        _uiState.update {
            it.copy(isLoadingMore = true, loadMoreError = null)
        }

        loadMoreJob = viewModelScope.launch {
            repository.loadMoreSearchResults(query, page = nextPage)
                .onSuccess { pageResult ->
                    _uiState.update { current ->
                        current.copy(
                            isLoadingMore = false,
                            currentPage = pageResult.currentPage,
                            totalPages = pageResult.totalPages,
                            hasMorePages = pageResult.hasMorePages,
                            loadMoreError = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { current ->
                        current.copy(
                            isLoadingMore = false,
                            loadMoreError = throwable.toUserFriendlyMessage(context.resources)
                        )
                    }
                }
        }
    }

    private fun observeResults() {
        viewModelScope.launch {
            repository.observeSearchResults(query).collect { movies ->
                _uiState.update { current ->
                    current.copy(movies = movies)
                }
            }
        }
    }
}

data class ResultsUiState(
    val query: String,
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val hasMorePages: Boolean = false,
    val errorMessage: String? = null,
    val isNetworkError: Boolean = false,
    val loadMoreError: String? = null
)
