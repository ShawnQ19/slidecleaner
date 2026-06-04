package com.gallery.cleaner.di

import com.gallery.cleaner.data.repository.MediaRepositoryImpl
import com.gallery.cleaner.data.repository.ThemeRepositoryImpl
import com.gallery.cleaner.domain.repository.MediaRepository
import com.gallery.cleaner.domain.repository.ThemeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        impl: MediaRepositoryImpl
    ): MediaRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(
        impl: ThemeRepositoryImpl
    ): ThemeRepository
}
