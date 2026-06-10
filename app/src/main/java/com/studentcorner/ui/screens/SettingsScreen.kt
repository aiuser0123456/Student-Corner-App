package com.studentcorner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentcorner.ui.theme.Accent500
import com.studentcorner.ui.theme.Brand500
import com.studentcorner.viewmodel.AuthViewModel
import com.studentcorner.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToContact: () -> Unit,
) {
    val authState     by authViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    var showUsernameDialog by remember { mutableStateOf(false) }
    var showSignOutDialog  by remember { mutableStateOf(false) }
    var newUsername        by remember { mutableStateOf("") }

    if (showUsernameDialog) {
        AlertDialog(
            onDismissRequest = { showUsernameDialog = false },
            title = { Text("Change Username") },
            text = { OutlinedTextField(value = newUsername, onValueChange = { newUsername = it },
                label = { Text("New username") }, singleLine = true,
                shape = RoundedCornerShape(10.dp)) },
            confirmButton = {
                TextButton(onClick = {
                    if (newUsername.isNotBlank()) { authViewModel.updateUsername(newUsername); showUsernameDialog = false }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showUsernameDialog = false }) { Text("Cancel") } }
        )
    }
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Sign Out?") },
            text = { Text("You'll need to sign in again to access your saved resources.") },
            confirmButton = {
                Button(onClick = { authViewModel.signOut(); onSignOut() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Sign Out")
                }
            },
            dismissButton = { TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
        ) {
            // ── Profile card ─────────────────────────────────────────────────
            authState.user?.let { user ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Avatar
                            Box(
                                modifier = Modifier.size(56.dp).clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(Brand500, Accent500))),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    user.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold, color = Color.White),
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.username,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(user.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (user.isAdmin) {
                                    Spacer(Modifier.height(2.dp))
                                    Surface(shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.errorContainer) {
                                        Text("Admin", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            IconButton(onClick = { newUsername = user.username; showUsernameDialog = true }) {
                                Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        authState.successMessage?.let {
                            Spacer(Modifier.height(8.dp))
                            Surface(shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF2E7D32).copy(alpha = 0.12f)) {
                                Text(it, modifier = Modifier.padding(8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF2E7D32))
                            }
                        }
                    }
                }
            }

            // ── Appearance ───────────────────────────────────────────────────
            SettingsGroupLabel("Appearance")
            SettingsCard {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    SettingsIconBox(
                        icon = if (settingsState.darkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        color = if (settingsState.darkMode) Color(0xFF4527A0) else Color(0xFFF57F17),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dark Mode", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                        Text(if (settingsState.darkMode) "Dark theme active" else "Light theme active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = settingsState.darkMode,
                        onCheckedChange = { settingsViewModel.setDarkMode(it) })
                }
            }

            // ── Support ───────────────────────────────────────────────────────
            SettingsGroupLabel("Support & Legal")
            SettingsCard {
                SettingsRow(Icons.Default.ContactSupport, Color(0xFF0277BD), "Contact Us", onNavigateToContact)
                HorizontalDivider(modifier = Modifier.padding(start = 58.dp))
                SettingsRow(Icons.Default.PrivacyTip, Color(0xFF2E7D32), "Privacy Policy", onNavigateToPrivacy)
                HorizontalDivider(modifier = Modifier.padding(start = 58.dp))
                SettingsRow(Icons.Default.Gavel, Color(0xFF6A1B9A), "Terms of Service", onNavigateToTerms, showDivider = false)
            }

            // ── Account ───────────────────────────────────────────────────────
            SettingsGroupLabel("Account")
            SettingsCard {
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    iconBg = MaterialTheme.colorScheme.error,
                    label = "Sign Out",
                    onClick = { showSignOutDialog = true },
                    textColor = MaterialTheme.colorScheme.error,
                    showDivider = false,
                )
            }

            Spacer(Modifier.height(32.dp))
            Text("Student Corner v1.1.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsGroupLabel(text: String) {
    Text(text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp))
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        content = { Column(content = content) }
    )
}

@Composable
private fun SettingsIconBox(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconBg: Color,
    label: String,
    onClick: () -> Unit,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    showDivider: Boolean = true,
) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            SettingsIconBox(icon, iconBg)
            Spacer(Modifier.width(14.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = textColor, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}
