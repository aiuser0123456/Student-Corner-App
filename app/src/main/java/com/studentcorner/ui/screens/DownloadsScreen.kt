package com.studentcorner.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    val state          by viewModel.uiState.collectAsState()
    val pdfs            = state.downloadedPdfs
    var selectedIds    by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isSelecting     = selectedIds.isNotEmpty()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(pdfs.size) { if (pdfs.isEmpty()) selectedIds = emptySet() }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete ${selectedIds.size} PDF${if (selectedIds.size > 1) "s" else ""}?") },
            text = { Text("These files will be permanently removed from the app.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deletePdfs(selectedIds.toList())
                    selectedIds = emptySet()
                    showDeleteDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            AnimatedContent(targetState = isSelecting, label = "topbar") { selecting ->
                if (selecting) {
                    TopAppBar(
                        title = { Text("${selectedIds.size} selected", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { selectedIds = emptySet() }) {
                                Icon(Icons.Default.Close, "Cancel")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                selectedIds = if (selectedIds.size == pdfs.size) emptySet()
                                else pdfs.map { it.resourceId }.toSet()
                            }) {
                                Icon(if (selectedIds.size == pdfs.size) Icons.Default.Deselect
                                else Icons.Default.SelectAll, "Select all")
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer),
                    )
                } else {
                    TopAppBar(
                        title = { Text("Downloads", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                        },
                        actions = {
                            if (pdfs.isNotEmpty()) {
                                IconButton(onClick = { selectedIds = setOf(pdfs.first().resourceId) }) {
                                    Icon(Icons.Default.Checklist, "Select")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    )
                }
            }
        }
    ) { padding ->
        if (pdfs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.DownloadDone, null,
                        modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("No downloads yet", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Downloaded PDFs appear here. They're stored privately — only accessible within the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text("${pdfs.size} file${if (pdfs.size != 1) "s" else ""} · Long-press to select",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp))
                }
                items(pdfs, key = { it.resourceId }) { pdf ->
                    val isSelected = pdf.resourceId in selectedIds
                    DownloadCard(
                        pdf        = pdf,
                        isSelected = isSelected,
                        isSelecting = isSelecting,
                        onClick = {
                            if (isSelecting) {
                                selectedIds = if (isSelected) selectedIds - pdf.resourceId
                                else selectedIds + pdf.resourceId
                            } else {
                                val f = viewModel.getLocalPdfFile(pdf.resourceId)
                                if (f.exists()) onOpenPdf(f, pdf.title)
                            }
                        },
                        onLongClick = { selectedIds = selectedIds + pdf.resourceId },
                        onDelete = { selectedIds = setOf(pdf.resourceId); showDeleteDialog = true },
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
    val sdf   = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val sizeKb = pdf.fileSizeBytes / 1024

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(if (isSelected) 0.dp else 2.dp),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(targetState = isSelecting, label = "icon") { selecting ->
                if (selecting) {
                    Checkbox(checked = isSelected, onCheckedChange = null,
                        modifier = Modifier.padding(end = 8.dp))
                } else {
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFB71C1C).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.PictureAsPdf, null, tint = Color(0xFFB71C1C),
                            modifier = Modifier.size(24.dp))
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(pdf.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2)
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(pdf.subject,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Text(
                        if (sizeKb > 1024) "${"%.1f".format(sizeKb / 1024f)} MB" else "$sizeKb KB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall)
                    Text(sdf.format(Date(pdf.downloadedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!isSelecting) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
