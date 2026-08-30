package com.lingoflow.app.di

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.lingoflow.app.data.repository.SettingsRepositoryImpl
import com.lingoflow.app.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    companion object {

        @Provides
        @Singleton
        fun provideSettingsDataStore(
            @ApplicationContext context: Context
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("lingoflow_settings") }
        )

        @Provides
        @Singleton
        fun provideEncryptedSharedPreferences(
            @ApplicationContext context: Context
        ): SharedPreferences {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            return try {
                createSecretsPrefs(context, masterKeyAlias)
            } catch (e: Exception) {
                // The secrets file is unrecoverable when the Keystore master
                // key is gone (e.g. ciphertext restored from another device
                // via backup, or a corrupted pref entry) and would otherwise
                // throw on every launch. Keys are re-enterable; a crash loop
                // is not. Reset the file and start fresh.
                context.deleteSharedPreferences(SECRETS_FILE_NAME)
                createSecretsPrefs(context, masterKeyAlias)
            }
        }

        private fun createSecretsPrefs(
            context: Context,
            masterKeyAlias: String
        ): SharedPreferences = EncryptedSharedPreferences.create(
            SECRETS_FILE_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        private const val SECRETS_FILE_NAME = "lingoflow_secrets"
    }
}
