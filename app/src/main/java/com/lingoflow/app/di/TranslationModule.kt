package com.lingoflow.app.di

import com.lingoflow.app.data.repository.TranslationRepositoryImpl
import com.lingoflow.app.data.translator.MlKitTranslator
import com.lingoflow.app.domain.Translator
import com.lingoflow.app.domain.repository.TranslationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TranslationModule {

    /**
     * Default production engine. FakeTranslator is kept for tests but is not
     * bound here.
     */
    @Binds
    @Singleton
    abstract fun bindTranslator(impl: MlKitTranslator): Translator

    @Binds
    @Singleton
    abstract fun bindTranslationRepository(impl: TranslationRepositoryImpl): TranslationRepository
}
