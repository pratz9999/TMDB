package com.tmdb.data.repository

import androidx.room.Room
import com.tmdb.data.api.TmdbApiService
import com.tmdb.data.api.dto.MovieDetailDto
import com.tmdb.data.api.dto.MovieDto
import com.tmdb.data.api.dto.SearchResponseDto
import com.tmdb.data.db.AppDatabase
import com.tmdb.domain.model.Movie
import com.tmdb.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DefaultMovieRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: AppDatabase
    private lateinit var api: FakeTmdbApiService
    private lateinit var repository: DefaultMovieRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        api = FakeTmdbApiService()
        repository = DefaultMovieRepository(api, database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun refreshTrimsQuerySavesResultsAndReturnsPageMetadata() = runTest(mainDispatcherRule.dispatcher) {
        api.searchResponses[1] = SearchResponseDto(
            page = 1,
            results = listOf(movieDto(1, "First"), movieDto(2, "Second")),
            totalPages = 3
        )

        val result = repository.refreshSearchResults("  dune  ").getOrThrow()

        assertEquals(SearchPageResult(1, 3, hasMorePages = true), result)
        assertEquals(listOf("dune" to 1), api.searchRequests.map { it.query to it.page })
        assertEquals(listOf(1, 2), repository.observeSearchResults("  dune  ").first().map { it.id })
        assertTrue(api.searchRequests.single().apiKey.isNotBlank())
    }

    @Test
    fun blankRefreshReturnsEmptyPaginationWithoutCallingApi() = runTest(mainDispatcherRule.dispatcher) {
        val result = repository.refreshSearchResults("  ").getOrThrow()

        assertEquals(SearchPageResult(1, 1, hasMorePages = false), result)
        assertTrue(api.searchRequests.isEmpty())
    }

    @Test
    fun refreshReplacesOnlyTheSelectedQueryReferences() = runTest(mainDispatcherRule.dispatcher) {
        api.searchResponses[1] = SearchResponseDto(
            page = 1,
            results = listOf(movieDto(1, "Old")),
            totalPages = 1
        )
        repository.refreshSearchResults("same").getOrThrow()
        repository.refreshSearchResults("other").getOrThrow()

        api.searchResponses[1] = SearchResponseDto(
            page = 1,
            results = listOf(movieDto(2, "Replacement")),
            totalPages = 1
        )
        repository.refreshSearchResults("same").getOrThrow()

        assertEquals(listOf(2), repository.observeSearchResults("same").first().map { it.id })
        assertEquals(listOf(1), repository.observeSearchResults("other").first().map { it.id })
    }

    @Test
    fun failedRefreshKeepsPreviouslyCachedSearchResults() = runTest(mainDispatcherRule.dispatcher) {
        api.searchResponses[1] = SearchResponseDto(
            page = 1,
            results = listOf(movieDto(1, "Cached")),
            totalPages = 1
        )
        repository.refreshSearchResults("query").getOrThrow()
        val failure = IOException("offline")
        api.searchFailure = failure

        val result = repository.refreshSearchResults("query")

        assertSame(failure, result.exceptionOrNull())
        assertEquals(listOf(1), repository.observeSearchResults("query").first().map { it.id })
    }

    @Test
    fun loadMoreAppendsItemsInPageOrderAndReturnsMetadata() = runTest(mainDispatcherRule.dispatcher) {
        api.searchResponses[1] = SearchResponseDto(
            page = 1,
            results = listOf(movieDto(1, "First")),
            totalPages = 2
        )
        repository.refreshSearchResults("query").getOrThrow()
        api.searchResponses[2] = SearchResponseDto(
            page = 2,
            results = listOf(movieDto(2, "Second"), movieDto(3, "Third")),
            totalPages = 2
        )

        val result = repository.loadMoreSearchResults("query", page = 2).getOrThrow()

        assertEquals(SearchPageResult(2, 2, hasMorePages = false), result)
        assertEquals(
            listOf(1, 2, 3),
            repository.observeSearchResults("query").first().map { it.id }
        )
        assertEquals(2, api.searchRequests.last().page)
    }

    @Test
    fun blankLoadMoreReturnsEmptyPaginationWithoutCallingApi() = runTest(mainDispatcherRule.dispatcher) {
        val result = repository.loadMoreSearchResults("", page = 5).getOrThrow()

        assertEquals(SearchPageResult(1, 1, hasMorePages = false), result)
        assertTrue(api.searchRequests.isEmpty())
    }

    @Test
    fun failedLoadMoreReturnsFailureWithoutChangingCachedResults() = runTest(mainDispatcherRule.dispatcher) {
        api.searchResponses[1] = SearchResponseDto(
            page = 1,
            results = listOf(movieDto(1, "Cached")),
            totalPages = 2
        )
        repository.refreshSearchResults("query").getOrThrow()
        val failure = IOException("offline")
        api.searchFailure = failure

        val result = repository.loadMoreSearchResults("query", page = 2)

        assertSame(failure, result.exceptionOrNull())
        assertEquals(listOf(1), repository.observeSearchResults("query").first().map { it.id })
    }

    @Test
    fun detailFetchStoresDetailedMovieInCache() = runTest(mainDispatcherRule.dispatcher) {
        api.detailResponse = MovieDetailDto(
            id = 42,
            title = "Full title",
            overview = "Full overview",
            releaseDate = "2024-01-02",
            rating = 8.5,
            posterPath = "/poster.jpg"
        )

        val result = repository.getMovieDetail(42).getOrThrow()
        val cached = database.movieDao().getMovieById(42)

        assertEquals(Movie(42, "Full title", "Full overview", "2024-01-02", 8.5, "/poster.jpg"), result)
        assertNotNull(cached)
        assertTrue(cached!!.hasDetails)
        assertEquals("Full overview", cached.overview)
    }

    @Test
    fun detailFetchFailureFallsBackToCachedMovie() = runTest(mainDispatcherRule.dispatcher) {
        api.searchResponses[1] = SearchResponseDto(
            page = 1,
            results = listOf(movieDto(7, "Cached summary")),
            totalPages = 1
        )
        repository.refreshSearchResults("query").getOrThrow()
        api.detailFailure = IOException("offline")

        val result = repository.getMovieDetail(7).getOrThrow()

        assertEquals("Cached summary", result.title)
        assertFalse(database.movieDao().getMovieById(7)!!.hasDetails)
    }

    @Test
    fun searchRefreshDoesNotReplacePreviouslyCachedDetailFields() = runTest(mainDispatcherRule.dispatcher) {
        api.searchResponses[1] = SearchResponseDto(
            page = 1,
            results = listOf(movieDto(7, "Search title")),
            totalPages = 1
        )
        repository.refreshSearchResults("query").getOrThrow()
        api.detailResponse = MovieDetailDto(
            id = 7,
            title = "Detailed title",
            overview = "Full overview"
        )
        repository.getMovieDetail(7).getOrThrow()

        api.searchResponses[1] = SearchResponseDto(
            page = 1,
            results = listOf(movieDto(7, "New search title")),
            totalPages = 1
        )
        repository.refreshSearchResults("query").getOrThrow()

        val cached = database.movieDao().getMovieById(7)!!
        assertEquals("Detailed title", cached.title)
        assertEquals("Full overview", cached.overview)
        assertTrue(cached.hasDetails)
    }

    @Test
    fun detailFailureWithoutCacheReturnsOriginalFailure() = runTest(mainDispatcherRule.dispatcher) {
        val failure = IOException("offline")
        api.detailFailure = failure

        val result = repository.getMovieDetail(99)

        assertSame(failure, result.exceptionOrNull())
    }

    @Test
    fun searchOperationsRethrowCancellation() = runTest(mainDispatcherRule.dispatcher) {
        api.searchFailure = CancellationException("cancelled")
        assertCancellation { repository.refreshSearchResults("query") }
        assertCancellation { repository.loadMoreSearchResults("query", 2) }
    }

    @Test
    fun detailOperationRethrowsCancellation() = runTest(mainDispatcherRule.dispatcher) {
        api.detailFailure = CancellationException("cancelled")

        assertCancellation { repository.getMovieDetail(1) }
    }

    private suspend fun assertCancellation(block: suspend () -> Any?) {
        try {
            block()
            fail("Expected CancellationException to be rethrown")
        } catch (_: CancellationException) {
            // Expected.
        }
    }

    private fun movieDto(id: Int, title: String) = MovieDto(
        id = id,
        title = title,
        overview = "$title overview"
    )

    private class FakeTmdbApiService : TmdbApiService {
        val searchRequests = mutableListOf<SearchRequest>()
        val searchResponses = mutableMapOf<Int, SearchResponseDto>()
        var searchFailure: Throwable? = null
        var detailResponse = MovieDetailDto(id = 1, title = "Detail")
        var detailFailure: Throwable? = null

        override suspend fun searchMovies(
            apiKey: String,
            query: String,
            page: Int
        ): SearchResponseDto {
            searchRequests += SearchRequest(apiKey, query, page)
            searchFailure?.let { throw it }
            return searchResponses[page] ?: SearchResponseDto(page = page)
        }

        override suspend fun getMovieDetail(movieId: Int, apiKey: String): MovieDetailDto {
            detailFailure?.let { throw it }
            return detailResponse.copy(id = movieId)
        }
    }

    private data class SearchRequest(
        val apiKey: String,
        val query: String,
        val page: Int
    )
}
