package com.tmdb.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tmdb.R
import com.tmdb.ui.components.EmptyStateView
import com.tmdb.ui.components.ErrorStateView
import com.tmdb.ui.components.MoviePoster
import com.tmdb.ui.components.TmdbTopAppBar

@Composable
fun DetailScreen(
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TmdbTopAppBar(
                title = state.movie?.title ?: stringResource(R.string.movie_detail),
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }

            state.errorMessage != null -> {
                ErrorStateView(
                    message = state.errorMessage ?: stringResource(R.string.could_not_load_movie_details),
                    modifier = Modifier.padding(innerPadding),
                    isNetworkError = state.isNetworkError,
                    onRetry = viewModel::loadDetails
                )
            }

            state.movie != null -> {
                val movie = state.movie!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MoviePoster(
                        posterPath = movie.posterPath,
                        title = movie.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        contentScale = ContentScale.Crop
                    )

                    Text(text = movie.title, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = stringResource(
                            R.string.release_date,
                            movie.releaseDate.ifBlank { stringResource(R.string.not_available) }
                        )
                    )
                    Text(
                        text = if (movie.rating > 0) {
                            stringResource(R.string.movie_rating, movie.rating)
                        } else {
                            stringResource(
                                R.string.movie_rating_unavailable,
                                stringResource(R.string.not_available)
                            )
                        }
                    )
                    Text(text = movie.overview.ifBlank { stringResource(R.string.not_available) })
                }
            }

            else -> {
                EmptyStateView(
                    title = stringResource(R.string.movie_not_found),
                    message = stringResource(R.string.no_movie_details_found),
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
