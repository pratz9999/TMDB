package com.tmdb.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val overview: String,
    val releaseDate: String,
    val rating: Double,
    val posterPath: String?,
    val updatedAt: Long,
    @ColumnInfo(defaultValue = "0") val hasDetails: Boolean = false
)

@Entity(
    tableName = "search_results",
    primaryKeys = ["query", "movieId"],
    indices = [Index(value = ["query", "position"])]
)
data class SearchResultEntity(
    val query: String,
    val movieId: Int,
    val position: Int
)
