package com.tmdb.ui.screens.results

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import com.tmdb.domain.model.Movie
import com.tmdb.ui.components.EmptyStateView
import com.tmdb.ui.components.ErrorStateView
import com.tmdb.ui.components.MoviePoster
import com.tmdb.ui.components.TmdbTopAppBar

@Composable
fun ResultsScreen(
    onBack: () -> Unit,
    onMovieClick: (Int) -> Unit,
    viewModel: ResultsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TmdbTopAppBar(
                title = if (state.query.isNotBlank()) {
                    stringResource(R.string.results_for_query, state.query)
                } else {
                    stringResource(R.string.search_results)
                },
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading && state.movies.isEmpty() -> {
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

            state.errorMessage != null && state.movies.isEmpty() -> {
                ErrorStateView(
                    message = state.errorMessage ?: stringResource(R.string.unexpected_error),
                    modifier = Modifier.padding(innerPadding),
                    isNetworkError = state.isNetworkError,
                    onRetry = viewModel::refresh
                )
            }

            state.movies.isEmpty() -> {
                EmptyStateView(
                    title = stringResource(R.string.no_movies_found),
                    message = if (state.query.isNotBlank()) {
                        stringResource(R.string.no_results_for_query, state.query)
                    } else {
                        stringResource(R.string.no_results_enter_query)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.movies, key = { it.id }) { movie ->
                        MovieListItem(movie = movie, onClick = { onMovieClick(movie.id) })
                    }

                    if (state.hasMorePages) {
                        item(key = "load_more_section") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (state.isLoadingMore) {
                                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                                } else {
                                    Button(
                                        onClick = viewModel::loadNextPage,
                                        modifier = Modifier.fillMaxWidth(0.6f)
                                    ) {
                                        Text(stringResource(R.string.load_more))
                                    }
                                }

                                if (state.loadMoreError != null) {
                                    Text(
                                        text = state.loadMoreError ?: stringResource(R.string.could_not_load_more),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieListItem(
    movie: Movie,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MoviePoster(
            posterPath = movie.posterPath,
            title = movie.title,
            modifier = Modifier
                .height(120.dp)
                .fillMaxWidth(0.3f),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = movie.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(
                    R.string.movie_release,
                    movie.releaseDate.ifBlank { stringResource(R.string.not_available) }
                )
            )
        }
    }
}
