package com.studentcorner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.studentcorner.viewmodel.ResourcesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    viewModel: ResourcesViewModel,
    onResourceClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    // Observe the live savedIds flow so unbookmark reflects instantly
    val savedIds by viewModel.savedIds.collectAsState()
    val state    by viewModel.uiState.collectAsState()

    val savedResources = remember(savedIds, state.allResources) {
        state.allResources.filter { it.id in savedIds }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Resources (${savedResources.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        }
    ) { padding ->
        if (savedResources.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.BookmarkBorder, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("No saved resources", style = MaterialTheme.typography.titleMedium)
                    Text("Tap the bookmark icon on any resource to save it here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(savedResources, key = { it.id }) { resource ->
                    ResourceCard(
                        resource = resource,
                        isSaved = true,   // everything here is saved
                        onClick = { onResourceClick(resource.id) },
                        onSaveToggle = { viewModel.toggleSave(resource.id) },
                    )
                }
            }
        }
    }
}
