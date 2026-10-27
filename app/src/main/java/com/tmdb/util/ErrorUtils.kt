package com.tmdb.util

import android.content.res.Resources
import com.tmdb.R
import retrofit2.HttpException
import java.io.IOException
import java.net.UnknownHostException

fun Throwable.isNetworkError(): Boolean =
    this is UnknownHostException || this is IOException

fun Throwable.toUserFriendlyMessage(resources: Resources): String {
    return when (this) {
        is UnknownHostException, is IOException -> {
            resources.getString(R.string.no_internet_connection)
        }
        is HttpException -> {
            when (code()) {
                401, 403 -> resources.getString(R.string.unauthorized_request)
                404 -> resources.getString(R.string.movie_not_found_error)
                429 -> resources.getString(R.string.too_many_requests)
                in 500..599 -> resources.getString(R.string.tmdb_server_error, code())
                else -> resources.getString(R.string.server_error, code())
            }
        }
        else -> resources.getString(R.string.unexpected_error)
    }
}
