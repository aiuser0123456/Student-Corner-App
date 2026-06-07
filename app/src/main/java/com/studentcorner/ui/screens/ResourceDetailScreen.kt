package com.studentcorner.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.studentcorner.viewmodel.ResourcesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceDetailScreen(
    resourceId: String,
    resourcesViewModel: ResourcesViewModel,
    onBack: () -> Unit,
) {
    val state by resourcesViewModel.uiState.collectAsState()
    val resource = state.allResources.find { it.id == resourceId }
    val isSaved = resourceId in state.savedResourceIds
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resource?.title ?: "Resource") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { resourcesViewModel.toggleSave(resourceId) }) {
                        Icon(
                            if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isSaved) "Unsave" else "Save",
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
            // Cover image
            if (resource.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = resource.imageUrl,
                    contentDescription = resource.imageHint,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
            }

            Column(modifier = Modifier.padding(20.dp)) {
                // Category + stream chips
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

                // PDF / Download buttons
                if (resource.pdfUrl != null || resource.downloadUrl != null) {
                    Spacer(Modifier.height(24.dp))
                    Text("Actions", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.height(12.dp))

                    resource.pdfUrl?.let { url ->
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("View PDF")
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    resource.downloadUrl?.let { url ->
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Download")
                        }
                    }
                }
            }
        }
    }
}
