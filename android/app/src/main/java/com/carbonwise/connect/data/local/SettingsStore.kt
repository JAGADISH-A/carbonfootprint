package com.carbonwise.connect.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ACCOUNT_EMAIL = stringPreferencesKey("account_email")
        val ACCOUNT_NAME = stringPreferencesKey("account_name")
        val ACCOUNT_USER_ID = stringPreferencesKey("account_user_id")
        val CONNECTED = booleanPreferencesKey("connected")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val NOTIFICATION_SYNC_ENABLED = booleanPreferencesKey("notification_sync_enabled")
        val SMS_SYNC_ENABLED = booleanPreferencesKey("sms_sync_enabled")
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time") // Legacy, keeping for compatibility if needed elsewhere
        val LAST_SMS_SCAN_TIMESTAMP = longPreferencesKey("last_sms_scan_timestamp")
        val LAST_SUCCESSFUL_UPLOAD_TIMESTAMP = longPreferencesKey("last_successful_upload_timestamp")
    }

    val accountEmail: Flow<String> = context.dataStore.data.map { it[Keys.ACCOUNT_EMAIL] ?: "" }
    val accountName: Flow<String> = context.dataStore.data.map { it[Keys.ACCOUNT_NAME] ?: "" }
    val accountUserId: Flow<String> = context.dataStore.data.map { it[Keys.ACCOUNT_USER_ID] ?: "" }
    val isConnected: Flow<Boolean> = context.dataStore.data.map { it[Keys.CONNECTED] ?: false }
    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }
    val notificationSyncEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFICATION_SYNC_ENABLED] ?: true }
    val smsSyncEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.SMS_SYNC_ENABLED] ?: false }
    val lastSyncTime: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_SYNC_TIME] ?: 0L }
    val lastSmsScanTimestamp: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_SMS_SCAN_TIMESTAMP] ?: 0L }
    val lastSuccessfulUploadTimestamp: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_SUCCESSFUL_UPLOAD_TIMESTAMP] ?: 0L }

    suspend fun setConnected(connected: Boolean) {
        context.dataStore.edit { it[Keys.CONNECTED] = connected }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setAccountInfo(userId: String, email: String, name: String) {
        context.dataStore.edit {
            it[Keys.ACCOUNT_USER_ID] = userId
            it[Keys.ACCOUNT_EMAIL] = email
            it[Keys.ACCOUNT_NAME] = name
        }
    }

    suspend fun setNotificationSync(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATION_SYNC_ENABLED] = enabled }
    }

    suspend fun setSmsSync(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SMS_SYNC_ENABLED] = enabled }
    }

    suspend fun setLastSyncTime(time: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC_TIME] = time }
    }

    suspend fun setLastSmsScanTimestamp(time: Long) {
        context.dataStore.edit { it[Keys.LAST_SMS_SCAN_TIMESTAMP] = time }
    }

    suspend fun setLastSuccessfulUploadTimestamp(time: Long) {
        android.util.Log.d("UploadPipeline", "SettingsStore.setLastSuccessfulUploadTimestamp(time=$time) called")
        context.dataStore.edit { it[Keys.LAST_SUCCESSFUL_UPLOAD_TIMESTAMP] = time }
        android.util.Log.d("UploadPipeline", "SettingsStore.setLastSuccessfulUploadTimestamp() completed")
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
