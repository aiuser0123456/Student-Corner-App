package com.studentcorner.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentcorner.data.local.DownloadedPdfEntity
import com.studentcorner.viewmodel.ResourcesViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DownloadsScreen(
    viewModel: ResourcesViewModel,
    onOpenPdf: (file: File, title: String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val pdfs = state.downloadedPdfs

    // Multi-select state
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isSelecting = selectedIds.isNotEmpty()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Reset selection when pdfs change (after delete)
    LaunchedEffect(pdfs.size) { selectedIds = emptySet() }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete ${selectedIds.size} PDF${if (selectedIds.size > 1) "s" else ""}?") },
            text = { Text("This will permanently delete the selected downloaded PDFs. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePdfs(selectedIds.toList())
                    selectedIds = emptySet()
                    showDeleteDialog = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            if (isSelecting) {
                // Selection mode top bar
                TopAppBar(
                    title = { Text("${selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, "Cancel selection")
                        }
                    },
                    actions = {
                        // Select all
                        IconButton(onClick = {
                            selectedIds = if (selectedIds.size == pdfs.size) emptySet()
                            else pdfs.map { it.resourceId }.toSet()
                        }) {
                            Icon(
                                if (selectedIds.size == pdfs.size) Icons.Default.Deselect else Icons.Default.SelectAll,
                                "Select all"
                            )
                        }
                        // Delete selected
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            } else {
                TopAppBar(
                    title = { Text("Downloads") },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    },
                    actions = {
                        if (pdfs.isNotEmpty()) {
                            IconButton(onClick = {
                                // enter select mode on action tap
                                selectedIds = setOf(pdfs.first().resourceId)
                            }) {
                                Icon(Icons.Default.Checklist, "Select")
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        if (pdfs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DownloadDone, null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("No downloads yet", style = MaterialTheme.typography.titleMedium)
                    Text("Downloaded PDFs appear here", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        "${pdfs.size} PDF${if (pdfs.size > 1) "s" else ""} · Long-press to select",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(pdfs, key = { it.resourceId }) { pdf ->
                    val isSelected = pdf.resourceId in selectedIds
                    DownloadCard(
                        pdf = pdf,
                        isSelected = isSelected,
                        isSelecting = isSelecting,
                        onClick = {
                            if (isSelecting) {
                                selectedIds = if (isSelected) selectedIds - pdf.resourceId
                                else selectedIds + pdf.resourceId
                            } else {
                                val file = viewModel.getLocalPdfFile(pdf.resourceId)
                                if (file.exists()) onOpenPdf(file, pdf.title)
                            }
                        },
                        onLongClick = {
                            selectedIds = selectedIds + pdf.resourceId
                        },
                        onDelete = {
                            selectedIds = setOf(pdf.resourceId)
                            showDeleteDialog = true
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadCard(
    pdf: DownloadedPdfEntity,
    isSelected: Boolean,
    isSelecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val sizeKb = pdf.fileSizeBytes / 1024

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 0.dp else 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Checkbox or PDF icon
            if (isSelecting) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null, // handled by combinedClickable
                    modifier = Modifier.padding(end = 8.dp),
                )
            } else {
                Icon(
                    Icons.Default.PictureAsPdf,
                    null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp).padding(end = 8.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    pdf.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(pdf.subject, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text("·", style = MaterialTheme.typography.labelSmall)
                    Text(
                        if (sizeKb > 1024) "${"%.1f".format(sizeKb / 1024f)} MB"
                        else "$sizeKb KB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("·", style = MaterialTheme.typography.labelSmall)
                    Text(
                        sdf.format(Date(pdf.downloadedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!isSelecting) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
