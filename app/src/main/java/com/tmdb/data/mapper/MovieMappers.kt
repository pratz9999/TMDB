package com.tmdb.data.mapper

import com.tmdb.data.api.dto.MovieDetailDto
import com.tmdb.data.api.dto.MovieDto
import com.tmdb.data.db.MovieEntity
import com.tmdb.domain.model.Movie

fun MovieDto.toEntity(updatedAt: Long): MovieEntity = MovieEntity(
    id = id,
    title = title,
    overview = overview,
    releaseDate = releaseDate.orEmpty(),
    rating = rating ?: 0.0,
    posterPath = posterPath,
    updatedAt = updatedAt
)

fun MovieDetailDto.toEntity(updatedAt: Long): MovieEntity = MovieEntity(
    id = id,
    title = title,
    overview = overview,
    releaseDate = releaseDate.orEmpty(),
    rating = rating ?: 0.0,
    posterPath = posterPath,
    updatedAt = updatedAt,
    hasDetails = true
)

fun MovieEntity.toDomain(): Movie = Movie(
    id = id,
    title = title,
    overview = overview,
    releaseDate = releaseDate,
    rating = rating,
    posterPath = posterPath
)
