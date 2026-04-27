package com.wardcompanion.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wardcompanion.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("ward_companion")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {
    private val ds = ctx.dataStore

    val token: Flow<String?> = ds.data.map { it[KEY_TOKEN] }
    val userId: Flow<String?> = ds.data.map { it[KEY_USER_ID] }
    val username: Flow<String?> = ds.data.map { it[KEY_USERNAME] }
    val serverUrl: Flow<String> = ds.data.map { it[KEY_SERVER] ?: BuildConfig.DEFAULT_SERVER_URL }

    suspend fun saveAuth(token: String, userId: String, username: String) {
        ds.edit {
            it[KEY_TOKEN] = token
            it[KEY_USER_ID] = userId
            it[KEY_USERNAME] = username
        }
    }

    suspend fun clearAuth() {
        ds.edit {
            it.remove(KEY_TOKEN)
            it.remove(KEY_USER_ID)
            it.remove(KEY_USERNAME)
        }
    }

    suspend fun setServerUrl(url: String) = ds.edit { it[KEY_SERVER] = url.trimEnd('/') }

    suspend fun currentToken(): String? = ds.data.first()[KEY_TOKEN]
    suspend fun currentServer(): String =
        ds.data.first()[KEY_SERVER] ?: BuildConfig.DEFAULT_SERVER_URL

    private companion object {
        val KEY_TOKEN = stringPreferencesKey("token")
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_USERNAME = stringPreferencesKey("username")
        val KEY_SERVER = stringPreferencesKey("server_url")
    }
}
