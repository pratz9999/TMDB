package com.tmdb.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tmdb.ui.screens.detail.DetailScreen
import com.tmdb.ui.screens.results.ResultsScreen
import com.tmdb.ui.screens.search.SearchScreen

@Composable
fun MovieAppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.SEARCH
    ) {
        composable(route = NavRoutes.SEARCH) {
            SearchScreen(
                onSearch = { query ->
                    navController.navigate(NavRoutes.results(query))
                }
            )
        }

        composable(
            route = NavRoutes.RESULTS,
            arguments = listOf(navArgument("query") { type = NavType.StringType })
        ) {
            ResultsScreen(
                onBack = { navController.popBackStack() },
                onMovieClick = { movieId -> navController.navigate(NavRoutes.detail(movieId)) }
            )
        }

        composable(
            route = NavRoutes.DETAIL,
            arguments = listOf(navArgument("movieId") { type = NavType.IntType })
        ) {
            DetailScreen(onBack = { navController.popBackStack() })
        }
    }
}

