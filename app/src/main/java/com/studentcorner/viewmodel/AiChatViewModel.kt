package com.studentcorner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentcorner.data.model.ChatCommand
import com.studentcorner.data.model.ChatMessage
import com.studentcorner.data.model.Conversation
import com.studentcorner.data.model.MessageRole
import com.studentcorner.data.repository.FirebaseRepository
import com.studentcorner.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AiChatUiState(
    val conversations: List<Conversation> = emptyList(),
    val activeConversationId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val activeConversation: Conversation?
        get() = conversations.find { it.id == activeConversationId }
}

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val repository: FirebaseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private var chatCommands: List<ChatCommand> = emptyList()

    // Gemini API key should be stored securely - replace with BuildConfig value
    // In production: store in local.properties and access via BuildConfig.GEMINI_API_KEY
    private val geminiApiKey: String = "AIzaSyA9t9EjcchJGZIfq2l3_iqE7691yvDPMFI"
    private val geminiEndpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$geminiApiKey"

    init {
        loadChatCommands()
        newConversation()
    }

    private fun loadChatCommands() {
        viewModelScope.launch {
            when (val result = repository.getAllChatCommands()) {
                is Result.Success -> chatCommands = result.data
                else -> {}
            }
        }
    }

    fun newConversation() {
        val conv = Conversation(
            id = UUID.randomUUID().toString(),
            title = "New Chat",
        )
        _uiState.value = _uiState.value.copy(
            conversations = listOf(conv) + _uiState.value.conversations,
            activeConversationId = conv.id,
        )
    }

    fun selectConversation(id: String) {
        _uiState.value = _uiState.value.copy(activeConversationId = id)
    }

    fun renameConversation(id: String, newTitle: String) {
        val updated = _uiState.value.conversations.map {
            if (it.id == id) it.copy(title = newTitle) else it
        }
        _uiState.value = _uiState.value.copy(conversations = updated)
    }

    fun deleteConversation(id: String) {
        val remaining = _uiState.value.conversations.filter { it.id != id }
        val newActive = if (_uiState.value.activeConversationId == id)
            remaining.firstOrNull()?.id else _uiState.value.activeConversationId
        _uiState.value = _uiState.value.copy(
            conversations = remaining,
            activeConversationId = newActive,
        )
    }

    fun sendMessage(input: String) {
        val convId = _uiState.value.activeConversationId ?: return
        val trimmed = input.trim().ifBlank { return }

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = trimmed,
        )

        // Add user message
        updateConversation(convId) { conv ->
            val updatedTitle = if (conv.messages.isEmpty()) trimmed.take(40) else conv.title
            conv.copy(
                messages = conv.messages + userMsg,
                title = updatedTitle,
            )
        }

        // Check for a matching chat command first (admin-configured auto-responses)
        val commandResponse = matchCommand(trimmed)
        if (commandResponse != null) {
            val botMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = MessageRole.MODEL,
                content = commandResponse,
            )
            updateConversation(convId) { it.copy(messages = it.messages + botMsg) }
            return
        }

        // Otherwise call Gemini API
        askGemini(convId)
    }

    private fun matchCommand(input: String): String? {
        return chatCommands.firstOrNull { cmd ->
            val text = if (cmd.caseSensitive) input else input.lowercase()
            val trigger = if (cmd.caseSensitive) cmd.trigger else cmd.trigger.lowercase()
            when (cmd.type) {
                "exact" -> text == trigger
                "contains" -> text.contains(trigger)
                else -> false
            }
        }?.response
    }

    private fun askGemini(convId: String) {
        val conv = _uiState.value.conversations.find { it.id == convId } ?: return

        // Build conversation history for the API
        val history = conv.messages.map { msg ->
            mapOf(
                "role" to if (msg.role == MessageRole.USER) "user" else "model",
                "parts" to listOf(mapOf("text" to msg.content)),
            )
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val requestBody = buildGeminiRequest(history)
                val response = callGeminiApi(requestBody)
                val text = parseGeminiResponse(response)

                val botMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.MODEL,
                    content = text,
                )
                updateConversation(convId) { it.copy(messages = it.messages + botMsg) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to get response: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // Build JSON body for Gemini REST API
    private fun buildGeminiRequest(history: List<Map<String, Any>>): String {
        val systemInstruction =
            "You are a helpful AI study assistant for Indian high school students (Class 11 & 12). " +
            "You help with subjects like Physics, Chemistry, Mathematics, Biology, and Computer Science. " +
            "Focus on NEET, JEE, and MHT-CET exam preparation. Be concise and clear."

        val contents = history.joinToString(",") { entry ->
            val role = entry["role"]
            @Suppress("UNCHECKED_CAST")
            val parts = (entry["parts"] as List<Map<String, String>>)
                .joinToString(",") { part -> """{"text":"${part["text"]?.replace("\"", "\\\"")}"}""" }
            """{"role":"$role","parts":[$parts]}"""
        }

        return """{
            "system_instruction": {"parts": [{"text": "$systemInstruction"}]},
            "contents": [$contents]
        }"""
    }

    // In a real app, use Retrofit or OkHttp from an injected service
    private suspend fun callGeminiApi(body: String): String {
        // Stub: replace with actual OkHttp/Retrofit call
        // Example:
        // val client = OkHttpClient()
        // val request = Request.Builder().url(geminiEndpoint)
        //     .post(body.toRequestBody("application/json".toMediaType())).build()
        // val response = client.newCall(request).execute()
        // return response.body!!.string()
        return """{"candidates":[{"content":{"parts":[{"text":"I'm your Student Corner AI assistant. Please configure your Gemini API key to enable AI responses."}]}}]}"""
    }

    private fun parseGeminiResponse(json: String): String {
        // Simple extraction — replace with Gson/Moshi in production
        val marker = "\"text\":\""
        val start = json.indexOf(marker)
        if (start == -1) return "Sorry, I couldn't understand the response."
        val textStart = start + marker.length
        val textEnd = json.indexOf("\"", textStart)
        return if (textEnd == -1) "Error parsing response." else json.substring(textStart, textEnd)
    }

    private fun updateConversation(id: String, transform: (Conversation) -> Conversation) {
        _uiState.value = _uiState.value.copy(
            conversations = _uiState.value.conversations.map { conv ->
                if (conv.id == id) transform(conv) else conv
            }
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
