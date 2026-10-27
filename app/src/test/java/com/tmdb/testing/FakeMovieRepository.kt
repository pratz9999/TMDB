package com.tmdb.testing

import com.tmdb.data.repository.MovieRepository
import com.tmdb.data.repository.SearchPageResult
import com.tmdb.domain.model.Movie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeMovieRepository : MovieRepository {
    val searchResults = MutableStateFlow<List<Movie>>(emptyList())
    val observedQueries = mutableListOf<String>()
    val refreshQueries = mutableListOf<String>()
    val loadMoreRequests = mutableListOf<Pair<String, Int>>()
    val detailRequests = mutableListOf<Int>()

    var refreshHandler: suspend (String) -> Result<SearchPageResult> = {
        Result.success(SearchPageResult(1, 1, hasMorePages = false))
    }
    var loadMoreHandler: suspend (String, Int) -> Result<SearchPageResult> = { _, page ->
        Result.success(SearchPageResult(page, page, hasMorePages = false))
    }
    var detailHandler: suspend (Int) -> Result<Movie> = {
        Result.failure(IllegalStateException("No movie configured"))
    }

    override fun observeSearchResults(query: String): Flow<List<Movie>> {
        observedQueries += query
        return searchResults
    }

    override suspend fun refreshSearchResults(query: String): Result<SearchPageResult> {
        refreshQueries += query
        return refreshHandler(query)
    }

    override suspend fun loadMoreSearchResults(
        query: String,
        page: Int
    ): Result<SearchPageResult> {
        loadMoreRequests += query to page
        return loadMoreHandler(query, page)
    }

    override suspend fun getMovieDetail(movieId: Int): Result<Movie> {
        detailRequests += movieId
        return detailHandler(movieId)
    }
}
