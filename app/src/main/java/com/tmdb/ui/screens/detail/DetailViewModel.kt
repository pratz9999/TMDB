package com.tmdb.ui.screens.detail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tmdb.R
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
import kotlinx.coroutines.launch

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val movieId: Int = savedStateHandle.get<Int>("movieId") ?: -1

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
    }

    fun loadDetails() {
        if (movieId == -1) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = context.getString(R.string.invalid_movie_id),
                    isNetworkError = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, isNetworkError = false)
            }

            repository.getMovieDetail(movieId)
                .onSuccess { movie ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            movie = movie,
                            errorMessage = null,
                            isNetworkError = false
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.toUserFriendlyMessage(context.resources),
                            isNetworkError = throwable.isNetworkError()
                        )
                    }
                }
        }
    }
}

data class DetailUiState(
    val isLoading: Boolean = true,
    val movie: Movie? = null,
    val errorMessage: String? = null,
    val isNetworkError: Boolean = false
)
