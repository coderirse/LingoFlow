package com.lingoflow.app.di

import com.lingoflow.app.data.dictionary.CachedDictionaryRepository
import com.lingoflow.app.data.dictionary.DictionaryRepositoryImpl
import com.lingoflow.app.domain.repository.DictionaryRepository
import com.lingoflow.app.domain.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object DictionaryModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        // LLM completions are slow: a non-streaming answer or the gap before
        // the next SSE delta can easily exceed OkHttp's 10s default read
        // timeout on a slow network, which used to surface as a stuck
        // loading cursor or a spurious "Translation failed" error.
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

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
