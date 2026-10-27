package com.tmdb.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    @Transaction
    @Query(
        """
        SELECT movies.* FROM movies
        INNER JOIN search_results ON movies.id = search_results.movieId
        WHERE search_results.query = :query
        ORDER BY search_results.position ASC
        """
    )
    fun observeMoviesForQuery(query: String): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE id = :movieId LIMIT 1")
    suspend fun getMovieById(movieId: Int): MovieEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMoviesIfAbsent(movies: List<MovieEntity>)

    @Query(
        """
        UPDATE movies
        SET title = :title,
            overview = :overview,
            releaseDate = :releaseDate,
            rating = :rating,
            posterPath = :posterPath,
            updatedAt = :updatedAt
        WHERE id = :id AND hasDetails = 0
        """
    )
    suspend fun updateMovieSummary(
        id: Int,
        title: String,
        overview: String,
        releaseDate: String,
        rating: Double,
        posterPath: String?,
        updatedAt: Long
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchResults(results: List<SearchResultEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: MovieEntity)

    @Query("DELETE FROM search_results WHERE query = :query")
    suspend fun clearSearchResultsForQuery(query: String)
}
