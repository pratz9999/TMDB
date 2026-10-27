package com.tmdb.ui.navigation

import android.net.Uri

object NavRoutes {
    const val SEARCH = "search"
    const val RESULTS = "results/{query}"
    const val DETAIL = "detail/{movieId}"

    fun results(query: String): String = "results/${Uri.encode(query)}"
    fun detail(movieId: Int): String = "detail/$movieId"
}

