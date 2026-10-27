package com.tmdb.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import com.tmdb.R
import com.tmdb.domain.model.Movie
import com.tmdb.testing.FakeMovieRepository
import com.tmdb.testing.MainDispatcherRule
import java.io.IOException
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
class DetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun initializationLoadsMovieFromSavedStateHandle() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeMovieRepository().apply {
            detailHandler = { Result.success(movie) }
        }
        val viewModel = createViewModel(repository, 42)

        assertTrue(viewModel.uiState.value.isLoading)
        runCurrent()

        assertEquals(listOf(42), repository.detailRequests)
        assertEquals(movie, viewModel.uiState.value.movie)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun missingMovieIdShowsLocalizedValidationErrorWithoutRepositoryCall() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeMovieRepository()
        val viewModel = createViewModel(repository, null)
        runCurrent()

        assertEquals(context.getString(R.string.invalid_movie_id), viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(repository.detailRequests.isEmpty())
    }

    @Test
    fun failedNetworkLoadPublishesFriendlyErrorAndNetworkFlag() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeMovieRepository().apply {
            detailHandler = { Result.failure(IOException("raw network message")) }
        }
        val viewModel = createViewModel(repository, 42)
        runCurrent()

        assertEquals(
            context.getString(R.string.no_internet_connection),
            viewModel.uiState.value.errorMessage
        )
        assertTrue(viewModel.uiState.value.isNetworkError)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.movie)
    }

    @Test
    fun unknownFailureDoesNotExposeRawExceptionText() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeMovieRepository().apply {
            detailHandler = {
                Result.failure(IllegalStateException("internal implementation detail"))
            }
        }
        val viewModel = createViewModel(repository, 42)
        runCurrent()

        assertEquals(
            context.getString(R.string.unexpected_error),
            viewModel.uiState.value.errorMessage
        )
        assertFalse(viewModel.uiState.value.isNetworkError)
    }

    @Test
    fun retryClearsPreviousErrorAndReloadsMovie() = runTest(mainDispatcherRule.dispatcher) {
        var failLoad = true
        val retryResult = CompletableDeferred<Result<Movie>>()
        val repository = FakeMovieRepository().apply {
            detailHandler = {
                if (failLoad) Result.failure(IOException("offline"))
                else retryResult.await()
            }
        }
        val viewModel = createViewModel(repository, 42)
        runCurrent()
        assertTrue(viewModel.uiState.value.isNetworkError)

        failLoad = false
        viewModel.loadDetails()
        runCurrent()
        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)

        retryResult.complete(Result.success(movie))
        runCurrent()
        assertEquals(movie, viewModel.uiState.value.movie)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isNetworkError)
        assertEquals(listOf(42, 42), repository.detailRequests)
    }

    private fun createViewModel(repository: FakeMovieRepository, movieId: Int?) =
        DetailViewModel(
            savedStateHandle = SavedStateHandle(
                movieId?.let { mapOf("movieId" to it) } ?: emptyMap()
            ),
            repository = repository,
            context = context
        )

    private companion object {
        val movie = Movie(
            id = 42,
            title = "Dune",
            overview = "Overview",
            releaseDate = "2024-01-01",
            rating = 8.0,
            posterPath = "/poster.jpg"
        )
    }
}
