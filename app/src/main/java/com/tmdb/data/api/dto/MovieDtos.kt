package com.tmdb.data.api.dto

import com.google.gson.annotations.SerializedName

data class SearchResponseDto(
    @SerializedName("page")
    val page: Int = 1,
    @SerializedName("results")
    val results: List<MovieDto> = emptyList(),
    @SerializedName("total_pages")
    val totalPages: Int = 1,
    @SerializedName("total_results")
    val totalResults: Int = 0
)

data class MovieDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("overview")
    val overview: String = "",
    @SerializedName("release_date")
    val releaseDate: String? = null,
    @SerializedName("vote_average")
    val rating: Double? = null,
    @SerializedName("poster_path")
    val posterPath: String? = null
)

data class MovieDetailDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("overview")
    val overview: String = "",
    @SerializedName("release_date")
    val releaseDate: String? = null,
    @SerializedName("vote_average")
    val rating: Double? = null,
    @SerializedName("poster_path")
    val posterPath: String? = null
)
