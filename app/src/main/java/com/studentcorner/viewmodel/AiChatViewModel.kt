package com.studentcorner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentcorner.data.local.AppPreferences
import com.studentcorner.data.model.ChatCommand
import com.studentcorner.data.model.ChatMessage
import com.studentcorner.data.model.Conversation
import com.studentcorner.data.model.MessageRole
import com.studentcorner.data.repository.FirebaseRepository
import com.studentcorner.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import javax.inject.Inject

data class AiChatUiState(
    val conversations: List<Conversation> = emptyList(),
    val activeConversationId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // API settings (loaded from DataStore)
    val selectedProvider: String = "openrouter",
    val geminiKey: String = "",
    val openAiKey: String = "",
    val claudeKey: String = "",
    val openRouterKey: String = "",
) {
    val activeConversation get() = conversations.find { it.id == activeConversationId }
}

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val repository: FirebaseRepository,
    private val prefs: AppPreferences,
    private val okHttpClient: OkHttpClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private var chatCommands: List<ChatCommand> = emptyList()
    private var streamCall: Call? = null

    init {
        loadPrefs()
        loadChatCommands()
        newConversation()
    }

    // ── Prefs ─────────────────────────────────────────────────────────────────

    private fun loadPrefs() {
        viewModelScope.launch {
            combine(
                prefs.selectedProvider,
                prefs.geminiKey,
                prefs.openAiKey,
                prefs.claudeKey,
                prefs.openRouterKey,
            ) { arr -> arr }.collect { arr ->
                _uiState.update { s ->
                    s.copy(
                        selectedProvider = arr[0] as String,
                        geminiKey        = arr[1] as String,
                        openAiKey        = arr[2] as String,
                        claudeKey        = arr[3] as String,
                        openRouterKey    = arr[4] as String,
                    )
                }
            }
        }
    }

    fun saveApiSettings(
        provider: String,
        gemini: String,
        openAi: String,
        claude: String,
        openRouter: String,
    ) {
        viewModelScope.launch {
            prefs.setSelectedProvider(provider)
            prefs.setGeminiKey(gemini)
            prefs.setOpenAiKey(openAi)
            prefs.setClaudeKey(claude)
            prefs.setOpenRouterKey(openRouter)
        }
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    private fun loadChatCommands() {
        viewModelScope.launch {
            when (val r = repository.getAllChatCommands()) {
                is Result.Success -> chatCommands = r.data
                else -> {}
            }
        }
    }

    // ── Conversation management ───────────────────────────────────────────────

    fun newConversation() {
        val conv = Conversation(id = UUID.randomUUID().toString(), title = "New Chat")
        _uiState.update { s ->
            s.copy(
                conversations = listOf(conv) + s.conversations,
                activeConversationId = conv.id,
            )
        }
    }

    fun selectConversation(id: String) = _uiState.update { it.copy(activeConversationId = id) }

    fun renameConversation(id: String, newTitle: String) =
        updateConversation(id) { it.copy(title = newTitle) }

    fun clearCurrentChat() {
        val id = _uiState.value.activeConversationId ?: return
        updateConversation(id) { it.copy(messages = emptyList()) }
    }

    fun deleteConversation(id: String) {
        val remaining = _uiState.value.conversations.filter { it.id != id }
        val newActive = if (_uiState.value.activeConversationId == id)
            remaining.firstOrNull()?.id else _uiState.value.activeConversationId
        _uiState.update { it.copy(conversations = remaining, activeConversationId = newActive) }
        if (remaining.isEmpty()) newConversation()
    }

    // ── Send message ──────────────────────────────────────────────────────────

    fun sendMessage(input: String) {
        val convId = _uiState.value.activeConversationId ?: return
        val trimmed = input.trim().ifBlank { return }

        val userMsg = ChatMessage(UUID.randomUUID().toString(), MessageRole.USER, trimmed)
        updateConversation(convId) { c ->
            val title = if (c.messages.isEmpty()) trimmed.take(30) else c.title
            c.copy(messages = c.messages + userMsg, title = title)
        }

        // 1. Hardcoded identity responses
        val lower = trimmed.lowercase()
        if (lower in listOf("hi", "hello", "hlo", "hey")) {
            addBotMessage(convId, "Hello! I'm your Study Buddy AI from Student Corner. How can I help you learn today? 😊")
            return
        }
        if (lower.contains("who made you") || lower.contains("who created you")) {
            addBotMessage(convId, "I'm **Study Buddy**, created by **Student Corner** to help you with your studies! 🎓")
            return
        }

        // 2. Admin chat commands from Firestore
        val cmdResp = matchCommand(trimmed)
        if (cmdResp != null) { addBotMessage(convId, cmdResp); return }

        // 3. AI API call
        callAiApi(convId, trimmed)
    }

    fun stopGenerating() {
        streamCall?.cancel()
        streamCall = null
        _uiState.update { it.copy(isLoading = false) }
    }

    // ── API call (SSE streaming) ───────────────────────────────────────────────

    private fun callAiApi(convId: String, latestUserText: String) {
        val s = _uiState.value
        val botMsgId = UUID.randomUUID().toString()
        val placeholderMsg = ChatMessage(botMsgId, MessageRole.MODEL, "")
        updateConversation(convId) { it.copy(messages = it.messages + placeholderMsg) }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        val history = _uiState.value.activeConversation?.messages
            ?.filter { it.id != botMsgId }
            ?.map { mapOf("role" to if (it.role == MessageRole.USER) "user" else "assistant", "content" to it.content) }
            ?: emptyList()

        viewModelScope.launch {
            try {
                val (url, requestBody, headers) = buildRequest(s, history)
                val req = Request.Builder().url(url).post(requestBody.toRequestBody("application/json".toMediaType()))
                    .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
                    .build()

                streamCall = okHttpClient.newCall(req)
                val resp = streamCall!!.execute()

                if (!resp.isSuccessful) {
                    val errBody = resp.body?.string() ?: "HTTP ${resp.code}"
                    throw Exception(errBody)
                }

                val reader = BufferedReader(InputStreamReader(resp.body!!.byteStream()))
                val sb = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val raw = line!!
                    if (!raw.startsWith("data: ")) continue
                    val data = raw.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    try {
                        val json = JSONObject(data)
                        val delta = when (s.selectedProvider) {
                            "claude" -> json.getJSONArray("delta").optJSONObject(0)?.optString("text", "") ?: ""
                            else -> json.getJSONArray("choices").getJSONObject(0)
                                .getJSONObject("delta").optString("content", "")
                        }
                        if (delta.isNotEmpty()) {
                            sb.append(delta)
                            val current = sb.toString()
                            // update message in-place
                            _uiState.update { st ->
                                st.copy(conversations = st.conversations.map { conv ->
                                    if (conv.id == convId) conv.copy(
                                        messages = conv.messages.map { m ->
                                            if (m.id == botMsgId) m.copy(content = current) else m
                                        }
                                    ) else conv
                                })
                            }
                        }
                    } catch (_: Exception) {}
                }

                // If stopped by user and empty, remove placeholder
                if (sb.isEmpty()) {
                    updateConversation(convId) { c -> c.copy(messages = c.messages.filter { it.id != botMsgId }) }
                }

            } catch (e: Exception) {
                if (e.message?.contains("cancel", ignoreCase = true) == true ||
                    e.message?.contains("closed", ignoreCase = true) == true) {
                    // user pressed stop — append note only if partial content exists
                    val partial = _uiState.value.activeConversation?.messages?.find { it.id == botMsgId }?.content ?: ""
                    if (partial.isNotEmpty()) {
                        updateConversation(convId) { c -> c.copy(
                            messages = c.messages.map { m ->
                                if (m.id == botMsgId) m.copy(content = "$partial\n\n*Generation stopped.*") else m
                            }
                        )}
                    } else {
                        updateConversation(convId) { c -> c.copy(messages = c.messages.filter { it.id != botMsgId }) }
                    }
                } else {
                    val errText = "Sorry, I couldn't get a response: ${e.message}"
                    updateConversation(convId) { c -> c.copy(
                        messages = c.messages.map { m ->
                            if (m.id == botMsgId) m.copy(content = errText) else m
                        }
                    )}
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
                streamCall = null
            }
        }
    }

    /** Returns Triple(url, bodyJson, extraHeaders) */
    private fun buildRequest(
        s: AiChatUiState,
        history: List<Map<String, String>>,
    ): Triple<String, String, Map<String, String>> {
        val systemPrompt = "You are a friendly and encouraging Study Buddy AI for Indian Class 11 & 12 students. " +
            "Your name is Study Buddy and you were created by Student Corner. " +
            "Help with Physics, Chemistry, Mathematics, Biology, Computer Science. " +
            "Focus on NEET, JEE and MHT-CET exam preparation. Format your responses in Markdown."

        return when (s.selectedProvider) {
            "gemini" -> {
                val contents = buildGeminiContents(history)
                val body = JSONObject().apply {
                    put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
                    put("contents", contents)
                }.toString()
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:streamGenerateContent?alt=sse&key=${s.geminiKey}"
                Triple(url, body, mapOf("Content-Type" to "application/json"))
            }
            "openai" -> {
                val msgs = buildOpenAiMessages(systemPrompt, history)
                val body = JSONObject().apply {
                    put("model", "gpt-4o-mini")
                    put("messages", msgs)
                    put("stream", true)
                }.toString()
                Triple(
                    "https://api.openai.com/v1/chat/completions",
                    body,
                    mapOf("Authorization" to "Bearer ${s.openAiKey}"),
                )
            }
            "claude" -> {
                val msgs = buildOpenAiMessages(systemPrompt, history)
                val body = JSONObject().apply {
                    put("model", "claude-sonnet-4-6")
                    put("max_tokens", 2048)
                    put("system", systemPrompt)
                    put("messages", msgs)
                    put("stream", true)
                }.toString()
                Triple(
                    "https://api.anthropic.com/v1/messages",
                    body,
                    mapOf(
                        "x-api-key" to s.claudeKey,
                        "anthropic-version" to "2023-06-01",
                    ),
                )
            }
            else -> { // openrouter (default, uses free z-ai model same as your website)
                val msgs = buildOpenAiMessages(systemPrompt, history)
                val body = JSONObject().apply {
                    put("model", "z-ai/glm-4.5-air:free")
                    put("messages", msgs)
                    put("stream", true)
                }.toString()
                Triple(
                    "https://openrouter.ai/api/v1/chat/completions",
                    body,
                    mapOf("Authorization" to "Bearer ${s.openRouterKey}"),
                )
            }
        }
    }

    private fun buildOpenAiMessages(system: String, history: List<Map<String, String>>): JSONArray {
        val arr = JSONArray()
        arr.put(JSONObject().put("role", "system").put("content", system))
        history.forEach { m -> arr.put(JSONObject().put("role", m["role"]).put("content", m["content"])) }
        return arr
    }

    private fun buildGeminiContents(history: List<Map<String, String>>): JSONArray {
        val arr = JSONArray()
        history.forEach { m ->
            val role = if (m["role"] == "user") "user" else "model"
            arr.put(JSONObject()
                .put("role", role)
                .put("parts", JSONArray().put(JSONObject().put("text", m["content"]))))
        }
        return arr
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun matchCommand(input: String): String? = chatCommands.firstOrNull { cmd ->
        val text    = if (cmd.caseSensitive) input else input.lowercase()
        val trigger = if (cmd.caseSensitive) cmd.trigger else cmd.trigger.lowercase()
        when (cmd.type) {
            "exact"    -> text == trigger
            "contains" -> text.contains(trigger)
            else -> false
        }
    }?.response

    private fun addBotMessage(convId: String, content: String) {
        val msg = ChatMessage(UUID.randomUUID().toString(), MessageRole.MODEL, content)
        updateConversation(convId) { it.copy(messages = it.messages + msg) }
    }

    private fun updateConversation(id: String, transform: (Conversation) -> Conversation) {
        _uiState.update { s ->
            s.copy(conversations = s.conversations.map { if (it.id == id) transform(it) else it })
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}
