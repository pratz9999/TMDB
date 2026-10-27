package com.tmdb.di

import com.tmdb.data.repository.DefaultMovieRepository
import com.tmdb.data.repository.MovieRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMovieRepository(
        repository: DefaultMovieRepository
    ): MovieRepository
}

