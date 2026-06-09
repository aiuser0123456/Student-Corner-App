package com.studentcorner.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        val DARK_MODE       = booleanPreferencesKey("dark_mode")
        val API_KEY_GEMINI  = stringPreferencesKey("api_key_gemini")
        val API_KEY_OPENAI  = stringPreferencesKey("api_key_openai")
        val API_KEY_CLAUDE  = stringPreferencesKey("api_key_claude")
        val API_KEY_OPENROUTER = stringPreferencesKey("api_key_openrouter")
        val SELECTED_PROVIDER = stringPreferencesKey("selected_provider")  // gemini | openai | claude | openrouter
    }

    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE] ?: false }
    val geminiKey: Flow<String> = context.dataStore.data.map { it[API_KEY_GEMINI] ?: "" }
    val openAiKey: Flow<String> = context.dataStore.data.map { it[API_KEY_OPENAI] ?: "" }
    val claudeKey: Flow<String> = context.dataStore.data.map { it[API_KEY_CLAUDE] ?: "" }
    val openRouterKey: Flow<String> = context.dataStore.data.map { it[API_KEY_OPENROUTER] ?: "" }
    val selectedProvider: Flow<String> = context.dataStore.data.map { it[SELECTED_PROVIDER] ?: "gemini" }

    suspend fun setDarkMode(enabled: Boolean) = context.dataStore.edit { it[DARK_MODE] = enabled }
    suspend fun setGeminiKey(key: String)     = context.dataStore.edit { it[API_KEY_GEMINI] = key }
    suspend fun setOpenAiKey(key: String)     = context.dataStore.edit { it[API_KEY_OPENAI] = key }
    suspend fun setClaudeKey(key: String)     = context.dataStore.edit { it[API_KEY_CLAUDE] = key }
    suspend fun setOpenRouterKey(key: String) = context.dataStore.edit { it[API_KEY_OPENROUTER] = key }
    suspend fun setSelectedProvider(p: String)= context.dataStore.edit { it[SELECTED_PROVIDER] = p }
}
