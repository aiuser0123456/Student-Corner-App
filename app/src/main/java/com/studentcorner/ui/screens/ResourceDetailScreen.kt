package com.studentcorner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val state by resourcesViewModel.uiState.collectAsState()
    val resource = state.allResources.find { it.id == resourceId }
    val isSaved = resourceId in state.savedResourceIds
    val isDownloaded = resourceId in state.downloadedIds
    val isDownloading = resourceId in state.downloadingIds
    val downloadProgress = state.downloadProgress[resourceId] ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resource?.title ?: "Resource") },
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
                }
            )
        }
    ) { padding ->
        if (resource == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (resource.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = resource.imageUrl,
                    contentDescription = resource.imageHint,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(resource.category) })
                    AssistChip(onClick = {}, label = { Text(resource.stream) })
                    AssistChip(onClick = {}, label = { Text(resource.classLevel.replace("class", "Class ")) })
                }

                Spacer(Modifier.height(12.dp))
                Text(resource.title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(4.dp))
                Text(resource.subject, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                Text("Description", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(8.dp))
                Text(resource.description, style = MaterialTheme.typography.bodyMedium)

                if (resource.content.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Content Overview", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.height(8.dp))
                    Text(resource.content, style = MaterialTheme.typography.bodyMedium)
                }

                // ── PDF / Download buttons ──────────────────────────────────────
                val hasPdf = resource.pdfUrl != null || resource.downloadUrl != null
                if (hasPdf) {
                    Spacer(Modifier.height(24.dp))
                    Text("Actions", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.height(12.dp))

                    // VIEW PDF — opens in-app viewer (from download cache if available, else downloads first)
                    if (resource.pdfUrl != null || resource.downloadUrl != null) {
                        val url = resource.pdfUrl ?: resource.downloadUrl!!
                        if (isDownloaded) {
                            // Open locally
                            Button(
                                onClick = {
                                    val f = resourcesViewModel.getLocalPdfFile(resourceId)
                                    onOpenPdf(f, resource.title)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("View PDF (Downloaded)")
                            }
                        } else {
                            // Must download first to view
                            OutlinedButton(
                                onClick = { resourcesViewModel.downloadPdf(resource) },
                                enabled = !isDownloading,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (isDownloading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Downloading $downloadProgress%…")
                                } else {
                                    Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Download & View PDF")
                                }
                            }
                        }
                    }

                    // DOWNLOAD button (if not already downloaded)
                    if (!isDownloaded && resource.isDownloadable) {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { resourcesViewModel.downloadPdf(resource) },
                            enabled = !isDownloading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (isDownloading) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    LinearProgressIndicator(
                                        progress = { downloadProgress / 100f },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Text("Downloading $downloadProgress%", style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Download PDF")
                            }
                        }
                    }

                    if (isDownloaded) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Downloaded — available offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}
