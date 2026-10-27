package com.tmdb.data.repository

import com.tmdb.domain.model.Movie
import kotlinx.coroutines.flow.Flow

data class SearchPageResult(
    val currentPage: Int,
    val totalPages: Int,
    val hasMorePages: Boolean
)

interface MovieRepository {
    fun observeSearchResults(query: String): Flow<List<Movie>>
    suspend fun refreshSearchResults(query: String): Result<SearchPageResult>
    suspend fun loadMoreSearchResults(query: String, page: Int): Result<SearchPageResult>
    suspend fun getMovieDetail(movieId: Int): Result<Movie>
}
