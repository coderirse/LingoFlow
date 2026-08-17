package com.lingoflow.app.domain.repository

import com.lingoflow.app.domain.model.settings.AppSettings

// TODO: Prompt 4 接入持久化实现
interface SettingsRepository {

    suspend fun getSettings(): AppSettings

    suspend fun saveSettings(settings: AppSettings)
}
