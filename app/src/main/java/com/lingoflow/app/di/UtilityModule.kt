package com.lingoflow.app.di

import com.lingoflow.app.data.repository.FavoritesRepositoryImpl
import com.lingoflow.app.data.repository.HistoryRepositoryImpl
import com.lingoflow.app.data.tts.AndroidTtsEngine
import com.lingoflow.app.data.tts.TtsEngine
import com.lingoflow.app.domain.repository.FavoritesRepository
import com.lingoflow.app.domain.repository.HistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UtilityModule {

    @Binds
    @Singleton
    abstract fun bindTtsEngine(impl: AndroidTtsEngine): TtsEngine

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository
}
