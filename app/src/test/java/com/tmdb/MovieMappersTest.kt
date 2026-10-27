package com.tmdb

import com.tmdb.data.api.dto.MovieDetailDto
import com.tmdb.data.api.dto.MovieDto
import com.tmdb.data.mapper.toEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieMappersTest {
    @Test
    fun searchMovieMapsMissingOptionalFieldsToDefaults() {
        val entity = MovieDto(id = 1, title = "Movie").toEntity(updatedAt = 123L)

        assertEquals("", entity.overview)
        assertEquals("", entity.releaseDate)
        assertEquals(0.0, entity.rating, 0.0)
        assertEquals(123L, entity.updatedAt)
        assertFalse(entity.hasDetails)
    }

    @Test
    fun movieDetailIsMarkedAsDetailedData() {
        val entity = MovieDetailDto(
            id = 1,
            title = "Movie",
            overview = "Full overview"
        ).toEntity(updatedAt = 456L)

        assertTrue(entity.hasDetails)
        assertEquals("Full overview", entity.overview)
    }
}
