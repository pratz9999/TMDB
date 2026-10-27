package com.tmdb.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.tmdb.R
import com.tmdb.data.api.TmdbApiConstants

@Composable
fun MoviePoster(
    posterPath: String?,
    title: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val imageUrl = posterPath
        ?.takeIf(String::isNotBlank)
        ?.let { "${TmdbApiConstants.IMAGE_BASE_URL}$it" }

    if (imageUrl == null) {
        Image(
            painter = painterResource(R.drawable.movie_poster_placeholder),
            contentDescription = stringResource(R.string.poster_unavailable),
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            modifier = modifier,
            contentScale = contentScale,
            placeholder = painterResource(R.drawable.movie_poster_placeholder),
            error = painterResource(R.drawable.movie_poster_placeholder)
        )
    }
}
