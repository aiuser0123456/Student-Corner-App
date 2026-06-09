package com.studentcorner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentcorner.data.model.MessageRole
import com.studentcorner.viewmodel.AiChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val conv = state.activeConversation
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var showHistory by remember { mutableStateOf(false) }
    var showApiSettings by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<String?>(null) } // convId to rename
    var renameText by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }

    // Auto-scroll to bottom on new message
    LaunchedEffect(conv?.messages?.size) {
        val n = conv?.messages?.size ?: 0
        if (n > 0) listState.animateScrollToItem(n - 1)
    }

    // ── Rename dialog ─────────────────────────────────────────────────────────
    if (showRenameDialog != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename Chat") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Chat name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank()) viewModel.renameConversation(showRenameDialog!!, renameText)
                    showRenameDialog = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = null }) { Text("Cancel") } }
        )
    }

    // ── Clear dialog ──────────────────────────────────────────────────────────
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Conversation?") },
            text = { Text("All messages in this chat will be deleted.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearCurrentChat(); showClearDialog = false }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } }
        )
    }

    // ── API Settings sheet ────────────────────────────────────────────────────
    if (showApiSettings) {
        AiSettingsSheet(
            state = state,
            onSave = { provider, gemini, openAi, claude, openRouter ->
                viewModel.saveApiSettings(provider, gemini, openAi, claude, openRouter)
                showApiSettings = false
            },
            onDismiss = { showApiSettings = false },
        )
    }

    // ── History drawer (ModalDrawer) ──────────────────────────────────────────
    if (showHistory) {
        ModalBottomSheet(onDismissRequest = { showHistory = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Chat History", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(12.dp))
                if (state.conversations.isEmpty()) {
                    Text("No conversations yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.conversations.forEach { c ->
                    val isActive = c.id == state.activeConversationId
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = { viewModel.selectConversation(c.id); showHistory = false },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                c.title.ifBlank { "New Chat" },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        // Rename
                        IconButton(onClick = {
                            renameText = c.title
                            showRenameDialog = c.id
                            showHistory = false
                        }) { Icon(Icons.Default.Edit, "Rename", modifier = Modifier.size(18.dp)) }
                        // Delete
                        IconButton(onClick = { viewModel.deleteConversation(c.id) }) {
                            Icon(Icons.Default.Delete, "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp))
                        }
                    }
                    HorizontalDivider()
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Study Assistant")
                        conv?.title?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    // New chat
                    IconButton(onClick = { viewModel.newConversation() }) {
                        Icon(Icons.Default.Add, "New chat")
                    }
                    // History
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Default.History, "History")
                    }
                    // Clear
                    IconButton(
                        onClick = { showClearDialog = true },
                        enabled = conv?.messages?.isNotEmpty() == true,
                    ) {
                        Icon(Icons.Default.DeleteSweep, "Clear chat")
                    }
                    // API settings
                    IconButton(onClick = { showApiSettings = true }) {
                        Icon(Icons.Default.Settings, "API Settings")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                state.errorMessage?.let { err ->
                    Text(err, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp))
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask a study question… (Enter to send)") },
                        modifier = Modifier.weight(1f),
                        minLines = 1,
                        maxLines = 5,
                        shape = RoundedCornerShape(24.dp),
                        enabled = !state.isLoading,
                    )
                    Spacer(Modifier.width(8.dp))
                    if (state.isLoading) {
                        FilledIconButton(
                            onClick = { viewModel.stopGenerating() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Icon(Icons.Default.Stop, "Stop generating")
                        }
                    } else {
                        FilledIconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            enabled = inputText.isNotBlank(),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Send")
                        }
                    }
                }
            }
        }
    ) { padding ->
        val messages = conv?.messages ?: emptyList()

        if (messages.isEmpty() && !state.isLoading) {
            // Empty state with example prompts
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Default.AutoAwesome, null,
                    modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Study Buddy AI", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Text("Your personal AI study assistant", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                // Show current provider
                val providerLabel = when (state.selectedProvider) {
                    "gemini" -> "Gemini"
                    "openai" -> "OpenAI"
                    "claude" -> "Claude"
                    else -> "OpenRouter (Free)"
                }
                AssistChip(
                    onClick = { showApiSettings = true },
                    label = { Text("Provider: $providerLabel") },
                    leadingIcon = { Icon(Icons.Default.SmartToy, null, modifier = Modifier.size(16.dp)) },
                )
                Spacer(Modifier.height(24.dp))
                listOf(
                    "What is Beats frequency?",
                    "Explain electrochemistry",
                    "What is CPU?",
                    "Explain Newton's laws of motion",
                ).forEach { prompt ->
                    OutlinedButton(
                        onClick = { viewModel.sendMessage(prompt); inputText = "" },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Text(prompt, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(messages, key = { it.id }) { message ->
                        val isUser = message.role == MessageRole.USER

                        // Thinking indicator
                        if (message.content.isEmpty() && !isUser) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.SmartToy, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(12.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Thinking…", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            return@items
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                        ) {
                            if (!isUser) {
                                Icon(Icons.Default.SmartToy, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp).padding(end = 4.dp))
                            }
                            Column(modifier = Modifier.widthIn(max = 290.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(
                                            topStart = 16.dp, topEnd = 16.dp,
                                            bottomStart = if (isUser) 16.dp else 4.dp,
                                            bottomEnd = if (isUser) 4.dp else 16.dp,
                                        ))
                                        .background(
                                            if (isUser) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .padding(12.dp),
                                ) {
                                    Text(
                                        message.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isUser) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                // Copy button for AI messages
                                if (!isUser && message.content.isNotEmpty()) {
                                    TextButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(message.content))
                                            scope.launch { snackbarHost.showSnackbar("Copied!") }
                                        },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                    ) {
                                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Copy", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            if (isUser) {
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.AccountCircle, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }

                // Scroll to bottom FAB
                val showScrollBtn by remember {
                    derivedStateOf { listState.canScrollForward }
                }
                if (showScrollBtn) {
                    FloatingActionButton(
                        onClick = { scope.launch { listState.animateScrollToItem(messages.size - 1) } },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(40.dp),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, "Scroll to bottom",
                            modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// ── API Settings bottom sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsSheet(
    state: com.studentcorner.viewmodel.AiChatUiState,
    onSave: (String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var provider    by remember { mutableStateOf(state.selectedProvider) }
    var geminiKey   by remember { mutableStateOf(state.geminiKey) }
    var openAiKey   by remember { mutableStateOf(state.openAiKey) }
    var claudeKey   by remember { mutableStateOf(state.claudeKey) }
    var openRouterKey by remember { mutableStateOf(state.openRouterKey) }
    var showKeys    by remember { mutableStateOf(false) }

    val providers = listOf(
        "openrouter" to "OpenRouter (Free – no key needed for free models)",
        "gemini"     to "Google Gemini",
        "openai"     to "OpenAI",
        "claude"     to "Anthropic Claude",
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("AI Settings", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(16.dp))

            Text("Provider", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            providers.forEach { (key, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    RadioButton(selected = provider == key, onClick = { provider = key })
                    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("API Keys", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showKeys = !showKeys }) {
                    Text(if (showKeys) "Hide" else "Show / Edit")
                }
            }

            if (showKeys) {
                Spacer(Modifier.height(8.dp))
                ApiKeyField("OpenRouter Key", openRouterKey, "sk-or-…") { openRouterKey = it }
                Spacer(Modifier.height(8.dp))
                ApiKeyField("Gemini API Key", geminiKey, "AIza…") { geminiKey = it }
                Spacer(Modifier.height(8.dp))
                ApiKeyField("OpenAI API Key", openAiKey, "sk-…") { openAiKey = it }
                Spacer(Modifier.height(8.dp))
                ApiKeyField("Anthropic (Claude) Key", claudeKey, "sk-ant-…") { claudeKey = it }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Keys are stored locally on your device only.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = { onSave(provider, geminiKey, openAiKey, claudeKey, openRouterKey) },
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
        }
    }
}

@Composable
private fun ApiKeyField(label: String, value: String, placeholder: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Default.Key, null, modifier = Modifier.size(18.dp)) },
    )
}
