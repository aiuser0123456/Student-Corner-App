package com.studentcorner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentcorner.ui.theme.AccentPurple
import com.studentcorner.ui.theme.PrimaryBlue
import com.studentcorner.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    onNavigateToResources: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
) {
    val authState by authViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Corner") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Hero banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(colors = listOf(PrimaryBlue, AccentPurple)))
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    authState.user?.username?.let { name ->
                        Text("Welcome back, $name! 👋",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                        Spacer(Modifier.height(4.dp))
                    }
                    Text("Your Smart Study Companion",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text("NEET · JEE · MHT-CET preparation resources",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateToResources,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Browse Resources")
                    }
                }
            }

            // Quick actions grid (2×3)
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Quick Access", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp))
                val items = listOf(
                    Triple(Icons.Default.MenuBook,      "Resources",  onNavigateToResources),
                    Triple(Icons.Default.AutoAwesome,   "AI Chat",    onNavigateToAiChat),
                    Triple(Icons.Default.Bookmark,      "Saved",      onNavigateToSaved),
                    Triple(Icons.Default.DownloadDone,  "Downloads",  onNavigateToDownloads),
                    Triple(Icons.Default.Settings,      "Settings",   onNavigateToSettings),
                    Triple(Icons.Default.Info,          "About",      onNavigateToAbout),
                )
                items.chunked(3).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { (icon, label, action) ->
                            QuickActionCard(icon, label, action, Modifier.weight(1f))
                        }
                        // Fill remaining space if row has < 3 items
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            // Features section
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text("What We Offer", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp))
                listOf(
                    Triple(Icons.Default.Quiz, "Question Banks",
                        "Thousands of practice questions tailored to NEET, JEE & MHT-CET."),
                    Triple(Icons.Default.LibraryBooks, "Textbook Solutions",
                        "Step-by-step solutions for all standard textbook exercises."),
                    Triple(Icons.Default.Notes, "Study Notes",
                        "Concise, expert-curated notes for quick revision."),
                    Triple(Icons.Default.SmartToy, "AI Study Assistant",
                        "Instant answers using GPT-4, Gemini, Claude, or free models."),
                    Triple(Icons.Default.DownloadDone, "Offline PDFs",
                        "Download PDFs and read them anytime, even without internet."),
                ).forEach { (icon, title, desc) -> FeatureRow(icon, title, desc) }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(80.dp).clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, desc: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(8.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
