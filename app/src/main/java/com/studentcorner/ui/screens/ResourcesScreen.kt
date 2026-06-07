package com.studentcorner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.studentcorner.data.model.*
import com.studentcorner.viewmodel.ResourcesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesScreen(
    viewModel: ResourcesViewModel,
    onResourceClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resources") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(if (showFilters) Icons.Default.FilterListOff else Icons.Default.FilterList, "Filter")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = state.filter.query,
                onValueChange = { viewModel.updateFilter(state.filter.copy(query = it)) },
                placeholder = { Text("Search resources…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (state.filter.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateFilter(state.filter.copy(query = "")) }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Filter chips
            if (showFilters) {
                FilterChipsRow(
                    filter = state.filter,
                    onFilterChange = viewModel::updateFilter,
                )
            }

            // Result count
            Text(
                "${state.filteredResources.size} resources found",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = viewModel::loadResources) { Text("Retry") }
                    }
                }
                state.filteredResources.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("No resources found", style = MaterialTheme.typography.titleMedium)
                        Text("Try adjusting your filters", style = MaterialTheme.typography.bodySmall)
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.filteredResources, key = { it.id }) { resource ->
                        ResourceCard(
                            resource = resource,
                            isSaved = resource.id in state.savedResourceIds,
                            onClick = { onResourceClick(resource.id) },
                            onSaveToggle = { viewModel.toggleSave(resource.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(filter: ResourceFilter, onFilterChange: (ResourceFilter) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Category
        items(Category.entries) { cat ->
            FilterChip(
                selected = filter.category == cat,
                onClick = { onFilterChange(filter.copy(category = cat)) },
                label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Subject
        items(Subject.entries) { sub ->
            FilterChip(
                selected = filter.subject == sub,
                onClick = { onFilterChange(filter.copy(subject = sub)) },
                label = { Text(sub.label, style = MaterialTheme.typography.labelSmall) },
            )
        }
        // Stream
        items(Stream.entries) { stream ->
            FilterChip(
                selected = filter.stream == stream,
                onClick = { onFilterChange(filter.copy(stream = stream)) },
                label = { Text(stream.label, style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceCard(
    resource: Resource,
    isSaved: Boolean,
    onClick: () -> Unit,
    onSaveToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column {
            if (resource.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = resource.imageUrl,
                    contentDescription = resource.imageHint,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        AssistChip(
                            onClick = {},
                            label = { Text(resource.category, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            resource.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onSaveToggle) {
                        Icon(
                            if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isSaved) "Unsave" else "Save",
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    resource.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SuggestionChip(onClick = {}, label = { Text(resource.subject, style = MaterialTheme.typography.labelSmall) })
                    SuggestionChip(onClick = {}, label = { Text(resource.stream, style = MaterialTheme.typography.labelSmall) })
                    if (resource.isDownloadable) {
                        SuggestionChip(onClick = {}, label = {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("PDF", style = MaterialTheme.typography.labelSmall)
                        })
                    }
                }
            }
        }
    }
}
