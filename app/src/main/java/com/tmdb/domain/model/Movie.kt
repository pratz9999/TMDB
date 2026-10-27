package com.tmdb.domain.model

data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val releaseDate: String,
    val rating: Double,
    val posterPath: String?
)

