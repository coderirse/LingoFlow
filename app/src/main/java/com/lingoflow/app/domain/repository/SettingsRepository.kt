package com.lingoflow.app.domain.repository

import com.lingoflow.app.domain.model.settings.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    suspend fun getSettings(): AppSettings

    suspend fun saveSettings(settings: AppSettings)

    /** Emits the current settings and every subsequent change. */
    fun observeSettings(): Flow<AppSettings>
}
