package com.studentcorner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentcorner.data.local.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val darkMode: Boolean = false,
    val selectedProvider: String = "openrouter",
    val geminiKey: String = "",
    val openAiKey: String = "",
    val claudeKey: String = "",
    val openRouterKey: String = "",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        prefs.darkMode,
        prefs.selectedProvider,
        prefs.geminiKey,
        prefs.openAiKey,
        prefs.claudeKey,
        prefs.openRouterKey,
    ) { arr ->
        SettingsUiState(
            darkMode         = arr[0] as Boolean,
            selectedProvider = arr[1] as String,
            geminiKey        = arr[2] as String,
            openAiKey        = arr[3] as String,
            claudeKey        = arr[4] as String,
            openRouterKey    = arr[5] as String,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setDarkMode(v: Boolean)         { viewModelScope.launch { prefs.setDarkMode(v) } }
    fun setSelectedProvider(v: String)  { viewModelScope.launch { prefs.setSelectedProvider(v) } }
    fun setGeminiKey(v: String)         { viewModelScope.launch { prefs.setGeminiKey(v) } }
    fun setOpenAiKey(v: String)         { viewModelScope.launch { prefs.setOpenAiKey(v) } }
    fun setClaudeKey(v: String)         { viewModelScope.launch { prefs.setClaudeKey(v) } }
    fun setOpenRouterKey(v: String)     { viewModelScope.launch { prefs.setOpenRouterKey(v) } }
}
