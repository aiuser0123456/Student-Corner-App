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
        observePrefs()
        loadChatCommands()
        newConversation()
    }

    // ── Prefs (live) ──────────────────────────────────────────────────────────
    // Keys are read fresh from DataStore on each send — no stale snapshot bug
    private fun observePrefs() {
        viewModelScope.launch {
            combine(
                prefs.selectedProvider,
                prefs.geminiKey,
                prefs.openAiKey,
                prefs.claudeKey,
                prefs.openRouterKey,
            ) { provider, gemini, openAi, claude, openRouter ->
                _uiState.update { s ->
                    s.copy(
                        selectedProvider = provider,
                        geminiKey        = gemini,
                        openAiKey        = openAi,
                        claudeKey        = claude,
                        openRouterKey    = openRouter,
                    )
                }
            }.collect()
        }
    }

    fun saveApiSettings(provider: String, gemini: String, openAi: String, claude: String, openRouter: String) {
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

    // ── Conversations ─────────────────────────────────────────────────────────
    fun newConversation() {
        val conv = Conversation(id = UUID.randomUUID().toString(), title = "New Chat")
        _uiState.update { s ->
            s.copy(conversations = listOf(conv) + s.conversations, activeConversationId = conv.id)
        }
    }
    fun selectConversation(id: String) = _uiState.update { it.copy(activeConversationId = id) }
    fun renameConversation(id: String, title: String) = editConv(id) { it.copy(title = title) }
    fun clearCurrentChat() { _uiState.value.activeConversationId?.let { editConv(it) { c -> c.copy(messages = emptyList()) } } }
    fun deleteConversation(id: String) {
        val rest = _uiState.value.conversations.filter { it.id != id }
        val next = if (_uiState.value.activeConversationId == id) rest.firstOrNull()?.id else _uiState.value.activeConversationId
        _uiState.update { it.copy(conversations = rest, activeConversationId = next) }
        if (rest.isEmpty()) newConversation()
    }

    // ── Send ──────────────────────────────────────────────────────────────────
    fun sendMessage(input: String) {
        val convId = _uiState.value.activeConversationId ?: return
        val text = input.trim().ifBlank { return }

        val userMsg = ChatMessage(UUID.randomUUID().toString(), MessageRole.USER, text)
        editConv(convId) { c ->
            c.copy(messages = c.messages + userMsg,
                title = if (c.messages.isEmpty()) text.take(35) else c.title)
        }

        val lower = text.lowercase()
        when {
            lower in listOf("hi","hello","hlo","hey","hii") ->
                addBot(convId, "Hello! 👋 I'm **Study Buddy**, your AI assistant from Student Corner. Ask me anything about Physics, Chemistry, Maths, Biology or CS!")
            lower.contains("who made you") || lower.contains("who created you") || lower.contains("who built you") ->
                addBot(convId, "I'm **Study Buddy**, created by **Student Corner** to help Class 11 & 12 students prepare for NEET, JEE, and MHT-CET! 🎓")
            lower.contains("what is your name") || lower.contains("your name") ->
                addBot(convId, "I'm **Study Buddy** — your personal AI study assistant from Student Corner! 🤖")
            else -> {
                val cmd = matchCommand(text)
                if (cmd != null) addBot(convId, cmd) else callAiApi(convId)
            }
        }
    }

    fun stopGenerating() {
        streamCall?.cancel()
        streamCall = null
        _uiState.update { it.copy(isLoading = false) }
    }

    // ── AI API (SSE streaming) ────────────────────────────────────────────────
    private fun callAiApi(convId: String) {
        // Read keys fresh from current state (populated by observePrefs)
        val s = _uiState.value

        val provider = s.selectedProvider
        val key = when (provider) {
            "gemini"     -> s.geminiKey
            "openai"     -> s.openAiKey
            "claude"     -> s.claudeKey
            "openrouter" -> s.openRouterKey
            else         -> s.openRouterKey
        }

        // Validate key (openrouter free models don't need a key)
        val needsKey = provider != "openrouter"
        if (needsKey && key.isBlank()) {
            addBot(convId, "⚠️ No API key set for **${providerLabel(provider)}**.\n\nTap the **⚙ Settings** button in the top bar → enter your API key → Save.\n\n" +
                "**How to get keys:**\n" +
                "- **Gemini**: [aistudio.google.com](https://aistudio.google.com) → Get API Key (free)\n" +
                "- **OpenAI**: [platform.openai.com](https://platform.openai.com) → API keys\n" +
                "- **Claude**: [console.anthropic.com](https://console.anthropic.com) → API Keys\n" +
                "- **OpenRouter**: [openrouter.ai](https://openrouter.ai) → Keys (has free models!)")
            return
        }

        val botMsgId = UUID.randomUUID().toString()
        editConv(convId) { it.copy(messages = it.messages + ChatMessage(botMsgId, MessageRole.MODEL, "")) }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        val history = _uiState.value.activeConversation?.messages
            ?.filter { it.id != botMsgId }
            ?.map { mapOf("role" to if (it.role == MessageRole.USER) "user" else "assistant", "content" to it.content) }
            ?: emptyList()

        viewModelScope.launch {
            try {
                val (url, body, extraHeaders) = buildRequest(provider, key, history)
                val req = Request.Builder()
                    .url(url)
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .apply {
                        addHeader("Content-Type", "application/json")
                        extraHeaders.forEach { (k, v) -> addHeader(k, v) }
                    }
                    .build()

                streamCall = okHttpClient.newCall(req)
                val resp = streamCall!!.execute()

                if (!resp.isSuccessful) {
                    val errBody = resp.body?.string() ?: "HTTP ${resp.code}"
                    throw Exception("API error ${resp.code}: $errBody")
                }

                val reader = BufferedReader(InputStreamReader(resp.body!!.byteStream()))
                val sb = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val raw = line!!
                    if (raw == "data: [DONE]") break
                    if (!raw.startsWith("data: ")) continue
                    val data = raw.removePrefix("data: ").trim()
                    if (data.isBlank() || data == "[DONE]") continue
                    try {
                        val json = JSONObject(data)
                        val chunk = extractChunk(provider, json)
                        if (chunk.isNotEmpty()) {
                            sb.append(chunk)
                            val cur = sb.toString()
                            _uiState.update { st ->
                                st.copy(conversations = st.conversations.map { conv ->
                                    if (conv.id != convId) conv
                                    else conv.copy(messages = conv.messages.map { m ->
                                        if (m.id == botMsgId) m.copy(content = cur) else m
                                    })
                                })
                            }
                        }
                    } catch (_: Exception) { /* skip malformed chunks */ }
                }

                if (sb.isEmpty()) {
                    editConv(convId) { c -> c.copy(messages = c.messages.filter { it.id != botMsgId }) }
                    addBot(convId, "I received an empty response. Please try again.")
                }

            } catch (e: Exception) {
                val isCancelled = e.message?.contains("cancel", true) == true ||
                    e.message?.contains("closed", true) == true ||
                    e.message?.contains("Socket", true) == true
                val partial = _uiState.value.activeConversation?.messages?.find { it.id == botMsgId }?.content ?: ""
                if (isCancelled) {
                    if (partial.isEmpty()) {
                        editConv(convId) { c -> c.copy(messages = c.messages.filter { it.id != botMsgId }) }
                    } else {
                        editConv(convId) { c -> c.copy(messages = c.messages.map { m ->
                            if (m.id == botMsgId) m.copy(content = "$partial\n\n*[Stopped]*") else m
                        })}
                    }
                } else {
                    val msg = "❌ Error: ${e.message ?: "Unknown error"}"
                    editConv(convId) { c -> c.copy(messages = c.messages.map { m ->
                        if (m.id == botMsgId) m.copy(content = msg) else m
                    })}
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
                streamCall = null
            }
        }
    }

    /** Extract text chunk from SSE JSON based on provider format */
    private fun extractChunk(provider: String, json: JSONObject): String {
        return when (provider) {
            "gemini" -> {
                // Gemini: candidates[0].content.parts[0].text
                json.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text", "") ?: ""
            }
            "claude" -> {
                // Claude: delta.type == "text_delta", delta.text
                val delta = json.optJSONObject("delta")
                if (delta?.optString("type") == "text_delta") delta.optString("text", "") else ""
            }
            else -> {
                // OpenAI / OpenRouter: choices[0].delta.content
                json.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("delta")
                    ?.optString("content", "") ?: ""
            }
        }
    }

    private fun buildRequest(
        provider: String,
        key: String,
        history: List<Map<String, String>>,
    ): Triple<String, String, Map<String, String>> {
        val system = "You are Study Buddy, a helpful and encouraging AI study assistant created by Student Corner. " +
            "You help Indian Class 11 & 12 students with Physics, Chemistry, Mathematics, Biology, History, and Computer Science. " +
            "Focus on NEET, JEE, and MHT-CET exam preparation. " +
            "Format responses clearly using Markdown (bold for key terms, bullet points for lists). " +
            "Keep answers concise and student-friendly."

        return when (provider) {
            "gemini" -> {
                val contents = JSONArray()
                history.forEach { m ->
                    contents.put(JSONObject()
                        .put("role", if (m["role"] == "user") "user" else "model")
                        .put("parts", JSONArray().put(JSONObject().put("text", m["content"]))))
                }
                val body = JSONObject()
                    .put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
                    .put("contents", contents)
                    .put("generationConfig", JSONObject().put("temperature", 0.7))
                    .toString()
                Triple(
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:streamGenerateContent?alt=sse&key=$key",
                    body,
                    emptyMap()
                )
            }
            "claude" -> {
                val msgs = JSONArray()
                history.forEach { m -> msgs.put(JSONObject().put("role", m["role"]).put("content", m["content"])) }
                val body = JSONObject()
                    .put("model", "claude-sonnet-4-6")
                    .put("max_tokens", 2048)
                    .put("system", system)
                    .put("messages", msgs)
                    .put("stream", true)
                    .toString()
                Triple(
                    "https://api.anthropic.com/v1/messages",
                    body,
                    mapOf("x-api-key" to key, "anthropic-version" to "2023-06-01"),
                )
            }
            "openai" -> {
                val msgs = buildOpenAiMsgs(system, history)
                val body = JSONObject()
                    .put("model", "gpt-4o-mini")
                    .put("messages", msgs)
                    .put("stream", true)
                    .toString()
                Triple("https://api.openai.com/v1/chat/completions", body, mapOf("Authorization" to "Bearer $key"))
            }
            else -> { // openrouter
                val msgs = buildOpenAiMsgs(system, history)
                val body = JSONObject()
                    .put("model", "z-ai/glm-4.5-air:free")
                    .put("messages", msgs)
                    .put("stream", true)
                    .toString()
                Triple(
                    "https://openrouter.ai/api/v1/chat/completions",
                    body,
                    if (key.isNotBlank()) mapOf("Authorization" to "Bearer $key") else emptyMap()
                )
            }
        }
    }

    private fun buildOpenAiMsgs(system: String, history: List<Map<String, String>>): JSONArray {
        val arr = JSONArray()
        arr.put(JSONObject().put("role", "system").put("content", system))
        history.forEach { m -> arr.put(JSONObject().put("role", m["role"]).put("content", m["content"])) }
        return arr
    }

    private fun providerLabel(p: String) = when(p) {
        "gemini" -> "Gemini"; "openai" -> "OpenAI"; "claude" -> "Claude"
        else -> "OpenRouter"
    }

    private fun matchCommand(input: String): String? = chatCommands.firstOrNull { cmd ->
        val text    = if (cmd.caseSensitive) input else input.lowercase()
        val trigger = if (cmd.caseSensitive) cmd.trigger else cmd.trigger.lowercase()
        when (cmd.type) { "exact" -> text == trigger; "contains" -> text.contains(trigger); else -> false }
    }?.response

    private fun addBot(convId: String, content: String) {
        editConv(convId) { it.copy(messages = it.messages + ChatMessage(UUID.randomUUID().toString(), MessageRole.MODEL, content)) }
    }
    private fun editConv(id: String, transform: (Conversation) -> Conversation) {
        _uiState.update { s -> s.copy(conversations = s.conversations.map { if (it.id == id) transform(it) else it }) }
    }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}
