package com.tmdb.ui.screens.search

import com.tmdb.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun queryStartsEmpty() {
        val viewModel = SearchViewModel()

        assertEquals("", viewModel.query.value)
    }

    @Test
    fun queryChangesArePublishedAndCanBeCleared() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = SearchViewModel()

        viewModel.onQueryChange("Dune")
        assertEquals("Dune", viewModel.query.value)

        viewModel.onQueryChange("")
        assertEquals("", viewModel.query.value)
    }
}
