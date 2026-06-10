package com.studentcorner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.studentcorner.viewmodel.ResourcesViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceDetailScreen(
    resourceId: String,
    resourcesViewModel: ResourcesViewModel,
    onBack: () -> Unit,
    onOpenPdf: (file: File, title: String) -> Unit,
) {
    val state        by resourcesViewModel.uiState.collectAsState()
    val resource      = state.allResources.find { it.id == resourceId }
    val isSaved       = resourceId in state.savedResourceIds
    val isDownloaded  = resourceId in state.downloadedIds
    val isDownloading = resourceId in state.downloadingIds
    val dlProgress    = state.downloadProgress[resourceId] ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resource?.title ?: "Resource", maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { resourcesViewModel.toggleSave(resourceId) }) {
                        Icon(
                            if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            if (isSaved) "Unsave" else "Save",
                            tint = if (isSaved) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        }
    ) { padding ->
        if (resource == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {

            // Cover image
            if (resource.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = resource.imageUrl,
                    contentDescription = resource.imageHint,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                // Chips row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(resource.category, resource.stream,
                        resource.classLevel.replace("class", "Class ")).forEach { label ->
                        Surface(shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(resource.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))
                Spacer(Modifier.height(4.dp))
                Text(resource.subject, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                InfoSection("Description", resource.description)
                if (resource.content.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    InfoSection("Content Overview", resource.content)
                }

                // ── PDF Actions ──────────────────────────────────────────────
                val hasPdf = resource.pdfUrl != null || resource.downloadUrl != null
                if (hasPdf) {
                    Spacer(Modifier.height(24.dp))
                    Text("Actions", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.height(12.dp))

                    if (isDownloaded) {
                        // Already downloaded — open locally, no URL exposed
                        Button(
                            onClick = {
                                onOpenPdf(resourcesViewModel.getLocalPdfFile(resourceId), resource.title)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("View PDF", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        // Offline badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = Color(0xFF2E7D32), modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Downloaded — available offline",
                                style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                        }
                    } else if (isDownloading) {
                        // Progress
                        Card(shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Text("Downloading PDF… $dlProgress%",
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { dlProgress / 100f },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    } else {
                        // Not downloaded yet
                        Button(
                            onClick = { resourcesViewModel.downloadPdf(resource) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Download & View PDF", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("PDF is downloaded privately inside the app — your link stays secure.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoSection(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
    Spacer(Modifier.height(6.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}
