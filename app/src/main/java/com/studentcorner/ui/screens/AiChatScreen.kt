package com.studentcorner.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
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
    val state    by viewModel.uiState.collectAsState()
    val conv      = state.activeConversation
    var input    by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val snackbar  = remember { SnackbarHostState() }

    var showHistory     by remember { mutableStateOf(false) }
    var showApiSettings by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var renameDialog    by remember { mutableStateOf<String?>(null) }
    var renameText      by remember { mutableStateOf("") }

    LaunchedEffect(conv?.messages?.size) {
        val n = conv?.messages?.size ?: 0
        if (n > 0) listState.animateScrollToItem(n - 1)
    }

    // Dialogs
    if (renameDialog != null) {
        AlertDialog(
            onDismissRequest = { renameDialog = null },
            title = { Text("Rename Chat") },
            text = { OutlinedTextField(value = renameText, onValueChange = { renameText = it },
                label = { Text("Chat name") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank()) viewModel.renameConversation(renameDialog!!, renameText)
                    renameDialog = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameDialog = null }) { Text("Cancel") } }
        )
    }
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Chat?") },
            text = { Text("All messages will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearCurrentChat(); showClearDialog = false }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } }
        )
    }
    if (showHistory) {
        ModalBottomSheet(onDismissRequest = { showHistory = false }) {
            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 40.dp)) {
                Text("Conversations", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(4.dp))
                Text("${state.conversations.size} chat${if (state.conversations.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                state.conversations.forEach { c ->
                    val isActive = c.id == state.activeConversationId
                    Surface(
                        onClick = { viewModel.selectConversation(c.id); showHistory = false },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isActive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Chat, null,
                                tint = if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(c.title.ifBlank { "New Chat" },
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal))
                            IconButton(onClick = {
                                renameText = c.title; renameDialog = c.id; showHistory = false
                            }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { viewModel.deleteConversation(c.id) },
                                modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, null,
                                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Study Assistant", fontWeight = FontWeight.Bold)
                        conv?.title?.let {
                            Text(it.ifBlank { "New Chat" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { viewModel.newConversation() }) { Icon(Icons.Default.Add, "New") }
                    IconButton(onClick = { showHistory = true }) { Icon(Icons.Default.History, "History") }
                    IconButton(onClick = { showClearDialog = true },
                        enabled = conv?.messages?.isNotEmpty() == true) {
                        Icon(Icons.Default.DeleteSweep, "Clear")
                    }
                    IconButton(onClick = { showApiSettings = true }) { Icon(Icons.Default.Settings, "API Settings") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    state.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            placeholder = { Text("Ask anything…") },
                            modifier = Modifier.weight(1f),
                            minLines = 1, maxLines = 5,
                            shape = RoundedCornerShape(22.dp),
                            enabled = !state.isLoading,
                        )
                        Spacer(Modifier.width(8.dp))
                        AnimatedContent(targetState = state.isLoading, label = "send_stop") { loading ->
                            if (loading) {
                                FilledIconButton(
                                    onClick = { viewModel.stopGenerating() },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.error),
                                ) { Icon(Icons.Default.Stop, "Stop") }
                            } else {
                                FilledIconButton(
                                    onClick = {
                                        if (input.isNotBlank()) { viewModel.sendMessage(input); input = "" }
                                    },
                                    enabled = input.isNotBlank(),
                                ) { Icon(Icons.AutoMirrored.Filled.Send, "Send") }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        val messages = conv?.messages ?: emptyList()

        if (messages.isEmpty() && !state.isLoading) {
            // Empty state
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.AutoAwesome, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Study Buddy AI",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Text("Your personal AI study assistant",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                val providerLabel = when (state.selectedProvider) {
                    "gemini" -> "Gemini"; "openai" -> "OpenAI"; "claude" -> "Claude"
                    else -> "OpenRouter (Free)"
                }
                AssistChip(
                    onClick = { showApiSettings = true },
                    label = { Text("Provider: $providerLabel") },
                    leadingIcon = { Icon(Icons.Default.SmartToy, null, modifier = Modifier.size(16.dp)) },
                )
                Spacer(Modifier.height(24.dp))
                Text("Try asking:", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                listOf(
                    "What is Beats frequency?",
                    "Explain Newton's laws of motion",
                    "What is electrochemistry?",
                    "Explain the CPU architecture",
                ).forEach { prompt ->
                    OutlinedButton(
                        onClick = { viewModel.sendMessage(prompt) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        shape = RoundedCornerShape(10.dp),
                    ) { Text(prompt, style = MaterialTheme.typography.bodySmall) }
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
                    items(messages, key = { it.id }) { msg ->
                        val isUser = msg.role == MessageRole.USER
                        if (msg.content.isEmpty() && !isUser) {
                            // Typing indicator
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BotAvatar()
                                Spacer(Modifier.width(8.dp))
                                Surface(shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp)
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
                            if (!isUser) { BotAvatar(); Spacer(Modifier.width(8.dp)) }
                            Column(modifier = Modifier.widthIn(max = 280.dp),
                                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(
                                            topStart = 18.dp, topEnd = 18.dp,
                                            bottomStart = if (isUser) 18.dp else 4.dp,
                                            bottomEnd   = if (isUser) 4.dp else 18.dp,
                                        ))
                                        .background(
                                            if (isUser) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .padding(12.dp),
                                ) {
                                    Text(msg.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isUser) Color.White
                                        else MaterialTheme.colorScheme.onSurface)
                                }
                                if (!isUser && msg.content.isNotEmpty()) {
                                    TextButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(msg.content))
                                            scope.launch { snackbar.showSnackbar("Copied!") }
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    ) {
                                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("Copy", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            if (isUser) {
                                Spacer(Modifier.width(8.dp))
                                Box(modifier = Modifier.size(30.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, null,
                                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
                // Scroll to bottom FAB
                val showDown by remember { derivedStateOf { listState.canScrollForward } }
                AnimatedVisibility(visible = showDown,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                    SmallFloatingActionButton(
                        onClick = { scope.launch { listState.animateScrollToItem(messages.size - 1) } },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ) { Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun BotAvatar() {
    Box(modifier = Modifier.size(30.dp).clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center) {
        Icon(Icons.Default.SmartToy, null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
    }
}

// ── AI Settings Sheet ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsSheet(
    state: com.studentcorner.viewmodel.AiChatUiState,
    onSave: (String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var provider      by remember { mutableStateOf(state.selectedProvider) }
    var geminiKey     by remember { mutableStateOf(state.geminiKey) }
    var openAiKey     by remember { mutableStateOf(state.openAiKey) }
    var claudeKey     by remember { mutableStateOf(state.claudeKey) }
    var openRouterKey by remember { mutableStateOf(state.openRouterKey) }
    var showKeys      by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp)) {

            Text("AI Provider Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text("Choose a provider and enter your API key",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))

            // Provider cards
            val providers = listOf(
                Triple("openrouter", "OpenRouter", "Free models available — no key needed to try!"),
                Triple("gemini",     "Google Gemini", "aistudio.google.com → Get API Key"),
                Triple("openai",     "OpenAI",        "platform.openai.com → API keys"),
                Triple("claude",     "Anthropic Claude", "console.anthropic.com → API Keys"),
            )
            providers.forEach { (key, label, hint) ->
                Surface(
                    onClick = { provider = key },
                    shape = RoundedCornerShape(12.dp),
                    color = if (provider == key) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = provider == key, onClick = { provider = key })
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(hint, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            // API Keys section
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, null, modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("API Keys", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1f))
                        TextButton(onClick = { showKeys = !showKeys },
                            contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text(if (showKeys) "Hide" else "Show / Edit",
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (showKeys) {
                        Spacer(Modifier.height(10.dp))
                        ApiKeyInput("OpenRouter Key", openRouterKey, "sk-or-v1-…") { openRouterKey = it }
                        Spacer(Modifier.height(8.dp))
                        ApiKeyInput("Gemini Key", geminiKey, "AIzaSy…") { geminiKey = it }
                        Spacer(Modifier.height(8.dp))
                        ApiKeyInput("OpenAI Key", openAiKey, "sk-…") { openAiKey = it }
                        Spacer(Modifier.height(8.dp))
                        ApiKeyInput("Claude Key", claudeKey, "sk-ant-…") { claudeKey = it }
                        Spacer(Modifier.height(8.dp))
                        Surface(shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Lock, null, modifier = Modifier.size(14.dp).padding(top = 2.dp),
                                    tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(6.dp))
                                Text("Keys are stored locally on your device and never sent anywhere except the AI provider you select.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = { onSave(provider, geminiKey, openAiKey, claudeKey, openRouterKey) },
                    modifier = Modifier.weight(1f)) { Text("Save") }
            }
        }
    }
}

@Composable
private fun ApiKeyInput(label: String, value: String, placeholder: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}
