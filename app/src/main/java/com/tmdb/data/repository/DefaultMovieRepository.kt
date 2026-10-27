package com.tmdb.data.repository

import androidx.room.withTransaction
import com.tmdb.data.api.TmdbApiConstants
import com.tmdb.data.api.TmdbApiService
import com.tmdb.data.db.AppDatabase
import com.tmdb.data.db.MovieEntity
import com.tmdb.data.db.SearchResultEntity
import com.tmdb.data.mapper.toDomain
import com.tmdb.data.mapper.toEntity
import com.tmdb.domain.model.Movie
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultMovieRepository @Inject constructor(
    private val apiService: TmdbApiService,
    private val database: AppDatabase
) : MovieRepository {

    private val movieDao = database.movieDao()

    override fun observeSearchResults(query: String): Flow<List<Movie>> {
        val normalizedQuery = query.trim()
        return movieDao.observeMoviesForQuery(normalizedQuery)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun refreshSearchResults(query: String): Result<SearchPageResult> {
        return try {
            val normalizedQuery = query.trim()
            if (normalizedQuery.isBlank()) {
                return Result.success(SearchPageResult(1, 1, hasMorePages = false))
            }

            val response = apiService.searchMovies(
                apiKey = TmdbApiConstants.TMDB_API_KEY,
                query = normalizedQuery,
                page = 1
            )
            val fetchedAt = System.currentTimeMillis()
            val movies = response.results.map { it.toEntity(updatedAt = fetchedAt) }
            val refs = response.results.mapIndexed { index, movie ->
                SearchResultEntity(
                    query = normalizedQuery,
                    movieId = movie.id,
                    position = index
                )
            }

            database.withTransaction {
                movieDao.clearSearchResultsForQuery(normalizedQuery)
                saveSearchMovies(movies)
                movieDao.insertSearchResults(refs)
            }

            Result.success(
                SearchPageResult(
                    currentPage = response.page,
                    totalPages = response.totalPages,
                    hasMorePages = response.page < response.totalPages
                )
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun loadMoreSearchResults(query: String, page: Int): Result<SearchPageResult> {
        return try {
            val normalizedQuery = query.trim()
            if (normalizedQuery.isBlank()) {
                return Result.success(SearchPageResult(1, 1, hasMorePages = false))
            }

            val response = apiService.searchMovies(
                apiKey = TmdbApiConstants.TMDB_API_KEY,
                query = normalizedQuery,
                page = page
            )
            val fetchedAt = System.currentTimeMillis()
            val movies = response.results.map { it.toEntity(updatedAt = fetchedAt) }
            val startPosition = (response.page - 1) * 20
            val refs = response.results.mapIndexed { index, movie ->
                SearchResultEntity(
                    query = normalizedQuery,
                    movieId = movie.id,
                    position = startPosition + index
                )
            }

            database.withTransaction {
                saveSearchMovies(movies)
                movieDao.insertSearchResults(refs)
            }

            Result.success(
                SearchPageResult(
                    currentPage = response.page,
                    totalPages = response.totalPages,
                    hasMorePages = response.page < response.totalPages
                )
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun getMovieDetail(movieId: Int): Result<Movie> {
        return try {
            val networkMovie = apiService.getMovieDetail(
                movieId = movieId,
                apiKey = TmdbApiConstants.TMDB_API_KEY
            )
            val entity = networkMovie.toEntity(updatedAt = System.currentTimeMillis())
            movieDao.insertMovie(entity)
            Result.success(entity.toDomain())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (networkException: Exception) {
            try {
                val cached = movieDao.getMovieById(movieId)
                if (cached != null) {
                    Result.success(cached.toDomain())
                } else {
                    Result.failure(networkException)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (cacheException: Exception) {
                networkException.addSuppressed(cacheException)
                Result.failure(networkException)
            }
        }
    }

    private suspend fun saveSearchMovies(movies: List<MovieEntity>) {
        movieDao.insertMoviesIfAbsent(movies)
        movies.forEach { movie ->
            movieDao.updateMovieSummary(
                id = movie.id,
                title = movie.title,
                overview = movie.overview,
                releaseDate = movie.releaseDate,
                rating = movie.rating,
                posterPath = movie.posterPath,
                updatedAt = movie.updatedAt
            )
        }
    }
}
