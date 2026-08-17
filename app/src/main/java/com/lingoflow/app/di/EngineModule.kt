package com.lingoflow.app.di

import com.lingoflow.app.data.engine.LlmTranslationEngine
import com.lingoflow.app.data.engine.TranslationRouter
import com.lingoflow.app.data.llm.OpenAiCompatibleProvider
import com.lingoflow.app.domain.Translator
import com.lingoflow.app.domain.engine.MlKitTranslationEngine
import com.lingoflow.app.domain.engine.TranslationEngine
import com.lingoflow.app.domain.repository.SettingsRepository
import com.lingoflow.app.domain.usecase.LookupWordUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    fun provideMlKitTranslationEngine(translator: Translator): MlKitTranslationEngine =
        MlKitTranslationEngine(translator)

    @Provides
    @Singleton
    fun provideLlmTranslationEngine(
        client: OkHttpClient,
        settingsRepository: SettingsRepository
    ): LlmTranslationEngine = LlmTranslationEngine(
        settingsRepository = settingsRepository,
        providerFactory = { config -> OpenAiCompatibleProvider(client, config) }
    )

    @Provides
    @Singleton
    fun provideTranslationEngine(
        mlKitEngine: MlKitTranslationEngine,
        llmEngine: LlmTranslationEngine,
        settingsRepository: SettingsRepository
    ): TranslationEngine = TranslationRouter(mlKitEngine, llmEngine, settingsRepository)

    @Provides
    @Singleton
    fun provideLookupWordUseCase(
        client: OkHttpClient,
        settingsRepository: SettingsRepository
    ): LookupWordUseCase = LookupWordUseCase(
        settingsRepository = settingsRepository,
        providerFactory = { config -> OpenAiCompatibleProvider(client, config) }
    )
}
