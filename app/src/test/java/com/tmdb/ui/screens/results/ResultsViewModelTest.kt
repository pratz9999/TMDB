package com.tmdb.ui.screens.results

import androidx.lifecycle.SavedStateHandle
import com.tmdb.R
import com.tmdb.data.repository.SearchPageResult
import com.tmdb.domain.model.Movie
import com.tmdb.testing.FakeMovieRepository
import com.tmdb.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class ResultsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun initializationObservesRouteQueryAndRefreshesFirstPage() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeMovieRepository()
        val viewModel = createViewModel(repository, "  dune  ")

        assertTrue(viewModel.uiState.value.isLoading)
        runCurrent()

        assertEquals("  dune  ", viewModel.uiState.value.query)
        assertEquals(listOf("  dune  "), repository.observedQueries)
        assertEquals(listOf("  dune  "), repository.refreshQueries)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun refreshSuccessPublishesPageMetadataAndClearsErrors() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeMovieRepository().apply {
            refreshHandler = {
                Result.success(SearchPageResult(2, 4, hasMorePages = true))
            }
        }
        val viewModel = createViewModel(repository, "movie")

        runCurrent()

        assertEquals(2, viewModel.uiState.value.currentPage)
        assertEquals(4, viewModel.uiState.value.totalPages)
        assertTrue(viewModel.uiState.value.hasMorePages)
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun refreshFailureUsesGenericLocalizedMessageAndMarksNetworkError() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeMovieRepository().apply {
            refreshHandler = { Result.failure(IOException("raw transport detail")) }
        }
        val viewModel = createViewModel(repository, "movie")

        runCurrent()

        assertEquals(
            context.getString(R.string.no_internet_connection),
            viewModel.uiState.value.errorMessage
        )
        assertTrue(viewModel.uiState.value.isNetworkError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun refreshFailureForUnknownExceptionDoesNotExposeRawExceptionText() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeMovieRepository().apply {
            refreshHandler = { Result.failure(IllegalStateException("private implementation detail")) }
        }
        val viewModel = createViewModel(repository, "movie")

        runCurrent()

        assertEquals(
            context.getString(R.string.unexpected_error),
            viewModel.uiState.value.errorMessage
        )
        assertFalse(viewModel.uiState.value.isNetworkError)
    }

    @Test
    fun observedDatabaseChangesUpdateScreenState() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeMovieRepository()
        val viewModel = createViewModel(repository, "movie")
        runCurrent()

        repository.searchResults.value = listOf(movie)
        runCurrent()

        assertEquals(listOf(movie), viewModel.uiState.value.movies)
    }

    @Test
    fun paginationIsIgnoredDuringRefresh() = runTest(mainDispatcherRule.dispatcher) {
        val refresh = CompletableDeferred<Result<SearchPageResult>>()
        val repository = FakeMovieRepository().apply {
            refreshHandler = { refresh.await() }
        }
        val viewModel = createViewModel(repository, "movie")
        runCurrent()

        viewModel.loadNextPage()
        runCurrent()

        assertTrue(repository.loadMoreRequests.isEmpty())
        refresh.complete(Result.success(SearchPageResult(1, 2, hasMorePages = true)))
        runCurrent()
    }

    @Test
    fun loadMoreIsDeduplicatedAndUpdatesPaginationState() = runTest(mainDispatcherRule.dispatcher) {
        val page = CompletableDeferred<Result<SearchPageResult>>()
        val repository = FakeMovieRepository().apply {
            refreshHandler = { Result.success(SearchPageResult(1, 3, hasMorePages = true)) }
            loadMoreHandler = { _, _ -> page.await() }
        }
        val viewModel = createViewModel(repository, "movie")
        runCurrent()

        viewModel.loadNextPage()
        runCurrent()
        viewModel.loadNextPage()
        runCurrent()

        assertEquals(listOf("movie" to 2), repository.loadMoreRequests)
        assertTrue(viewModel.uiState.value.isLoadingMore)

        page.complete(Result.success(SearchPageResult(2, 3, hasMorePages = true)))
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoadingMore)
        assertEquals(2, viewModel.uiState.value.currentPage)
        assertTrue(viewModel.uiState.value.hasMorePages)
    }

    @Test
    fun loadMoreFailureClearsLoadingAndReportsFriendlyMessage() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeMovieRepository().apply {
            refreshHandler = { Result.success(SearchPageResult(1, 2, hasMorePages = true)) }
            loadMoreHandler = { _, _ -> Result.failure(IOException("offline")) }
        }
        val viewModel = createViewModel(repository, "movie")
        runCurrent()

        viewModel.loadNextPage()
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoadingMore)
        assertEquals(
            context.getString(R.string.no_internet_connection),
            viewModel.uiState.value.loadMoreError
        )
    }

    @Test
    fun refreshCancelsPaginationAndResetsLoadingAndErrors() = runTest(mainDispatcherRule.dispatcher) {
        var pageCancelled = false
        val repository = FakeMovieRepository().apply {
            refreshHandler = { Result.success(SearchPageResult(1, 2, hasMorePages = true)) }
            loadMoreHandler = { _, _ ->
                try {
                    CompletableDeferred<Result<SearchPageResult>>().await()
                } catch (cancellation: kotlinx.coroutines.CancellationException) {
                    pageCancelled = true
                    throw cancellation
                }
            }
        }
        val viewModel = createViewModel(repository, "movie")
        runCurrent()
        viewModel.loadNextPage()
        runCurrent()

        viewModel.refresh()
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoadingMore)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, repository.refreshQueries.size)
        assertTrue(pageCancelled)
    }

    @Test
    fun newerRefreshSupersedesAnOlderPendingRefresh() = runTest(mainDispatcherRule.dispatcher) {
        val firstRefresh = CompletableDeferred<Result<SearchPageResult>>()
        var refreshCount = 0
        var firstRefreshCancelled = false
        val repository = FakeMovieRepository().apply {
            refreshHandler = {
                refreshCount++
                if (refreshCount == 1) {
                    try {
                        firstRefresh.await()
                    } catch (cancellation: CancellationException) {
                        firstRefreshCancelled = true
                        throw cancellation
                    }
                } else {
                    Result.success(SearchPageResult(2, 2, hasMorePages = false))
                }
            }
        }
        val viewModel = createViewModel(repository, "movie")
        runCurrent()

        viewModel.refresh()
        runCurrent()

        assertEquals(2, refreshCount)
        assertEquals(2, viewModel.uiState.value.currentPage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(firstRefreshCancelled)
    }

    @Test
    fun loadMoreDoesNothingWhenThereAreNoMorePages() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeMovieRepository()
        val viewModel = createViewModel(repository, "movie")
        runCurrent()

        viewModel.loadNextPage()
        runCurrent()

        assertTrue(repository.loadMoreRequests.isEmpty())
    }

    private fun createViewModel(
        repository: FakeMovieRepository,
        query: String
    ) = ResultsViewModel(
        savedStateHandle = SavedStateHandle(mapOf("query" to query)),
        repository = repository,
        context = context
    )

    private companion object {
        val movie = Movie(
            id = 1,
            title = "Dune",
            overview = "Overview",
            releaseDate = "2024-01-01",
            rating = 8.0,
            posterPath = null
        )
    }
}
