package com.lingoflow.app.domain.repository

import com.lingoflow.app.domain.model.settings.AppSettings
import com.lingoflow.app.domain.model.translation.TranslationMemory
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    suspend fun getSettings(): AppSettings

    suspend fun saveSettings(settings: AppSettings)

    /** Emits the current settings and every subsequent change. */
    fun observeSettings(): Flow<AppSettings>

    /**
     * The last language pair + mode the user translated with, or null when
     * never persisted (fresh install). Restored on app launch so the home
     * screen resumes where the user left off.
     */
    suspend fun translationMemory(): TranslationMemory?

    /** Persists the last-used language pair + mode (fire-and-forget friendly). */
    suspend fun saveTranslationMemory(memory: TranslationMemory)
}
