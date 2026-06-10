package com.studentcorner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studentcorner.ui.theme.Accent500
import com.studentcorner.ui.theme.Brand500
import com.studentcorner.ui.theme.Brand700
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Brand500, Accent500))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.School, null, tint = Color.White,
                                modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("Student Corner", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
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
            // ── Hero Banner ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(
                        listOf(Brand500, Brand700.copy(alpha = 0.9f), Accent500)
                    ))
                    .padding(horizontal = 24.dp, vertical = 32.dp),
            ) {
                Column {
                    // Greeting chip
                    authState.user?.username?.let { name ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Text(
                                "👋  Hey, $name!",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                            )
                        }
                    }
                    Text("Your Smart\nStudy Companion",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold, lineHeight = 36.sp),
                        color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text("NEET · JEE · MHT-CET",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f))
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onNavigateToResources,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor   = Brand500,
                            ),
                            elevation = ButtonDefaults.buttonElevation(4.dp),
                        ) {
                            Icon(Icons.Default.MenuBook, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Resources", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = onNavigateToAiChat,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("AI Chat")
                        }
                    }
                }
            }

            // ── Quick Access Grid ───────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
                SectionTitle("Quick Access")
                Spacer(Modifier.height(12.dp))
                val tiles = listOf(
                    QuickTile(Icons.Default.MenuBook,   "Resources",  Brand500,             onNavigateToResources),
                    QuickTile(Icons.Default.AutoAwesome,"AI Chat",    Color(0xFF7E57C2),    onNavigateToAiChat),
                    QuickTile(Icons.Default.Bookmark,   "Saved",      Color(0xFF00897B),    onNavigateToSaved),
                    QuickTile(Icons.Default.Download,   "Downloads",  Color(0xFFE65100),    onNavigateToDownloads),
                )
                tiles.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { tile ->
                            QuickAccessCard(tile, Modifier.weight(1f))
                        }
                        repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ── Features ────────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionTitle("What We Offer")
                Spacer(Modifier.height(12.dp))
                val features = listOf(
                    Triple(Icons.Default.Quiz,        "Question Banks",       "Practice questions for NEET, JEE & MHT-CET"),
                    Triple(Icons.Default.LibraryBooks,"Textbook Solutions",   "Step-by-step solved exercises"),
                    Triple(Icons.Default.Notes,       "Expert Study Notes",   "Concise notes for quick revision"),
                    Triple(Icons.Default.SmartToy,    "AI Study Assistant",   "GPT-4, Gemini, Claude & free OpenRouter"),
                    Triple(Icons.Default.DownloadDone,"Offline PDF Library",  "Download once, read anywhere — no internet needed"),
                )
                features.forEach { (icon, title, desc) ->
                    FeatureListItem(icon, title, desc)
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

private data class QuickTile(
    val icon: ImageVector, val label: String,
    val color: Color, val onClick: () -> Unit,
)

@Composable
private fun QuickAccessCard(tile: QuickTile, modifier: Modifier) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = tile.onClick)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tile.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tile.icon, null, tint = tile.color, modifier = Modifier.size(20.dp))
            }
            Text(tile.label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
private fun FeatureListItem(icon: ImageVector, title: String, desc: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                Text(desc, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
}

private fun androidx.compose.ui.graphics.Color.luminance(): Float {
    val r = red; val g = green; val b = blue
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}
