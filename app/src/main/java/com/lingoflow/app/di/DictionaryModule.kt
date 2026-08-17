package com.lingoflow.app.di

import com.lingoflow.app.data.dictionary.CachedDictionaryRepository
import com.lingoflow.app.data.dictionary.DictionaryRepositoryImpl
import com.lingoflow.app.domain.repository.DictionaryRepository
import com.lingoflow.app.domain.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object DictionaryModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient()

    @Provides
    @Singleton
    fun provideDictionaryRepositoryImpl(
        client: OkHttpClient,
        settingsRepository: SettingsRepository
    ): DictionaryRepositoryImpl = DictionaryRepositoryImpl(client, settingsRepository)

    @Provides
    @Singleton
    fun provideDictionaryRepository(
        impl: DictionaryRepositoryImpl
    ): DictionaryRepository = CachedDictionaryRepository(impl)
}
