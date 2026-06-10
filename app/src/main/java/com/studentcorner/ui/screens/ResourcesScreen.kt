package com.studentcorner.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
                title = { Text("Resources", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    BadgedBox(badge = {
                        val activeFilters = listOf(
                            state.filter.category != Category.ALL,
                            state.filter.subject   != Subject.ALL,
                            state.filter.stream    != Stream.ALL,
                            state.filter.classLevel != ClassLevel.ALL,
                        ).count { it }
                        if (activeFilters > 0) Badge { Text("$activeFilters") }
                    }) {
                        IconButton(onClick = { showFilters = !showFilters }) {
                            Icon(
                                if (showFilters) Icons.Default.FilterListOff else Icons.Default.FilterList,
                                "Filter"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
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
                    if (state.filter.query.isNotEmpty())
                        IconButton(onClick = { viewModel.updateFilter(state.filter.copy(query = "")) }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Filter chips
            AnimatedVisibility(visible = showFilters) {
                Column {
                    Text("Category", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(Category.entries) { cat ->
                            FilterChip(
                                selected = state.filter.category == cat,
                                onClick = { viewModel.updateFilter(state.filter.copy(category = cat)) },
                                label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                    Text("Subject", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(Subject.entries) { sub ->
                            FilterChip(
                                selected = state.filter.subject == sub,
                                onClick = { viewModel.updateFilter(state.filter.copy(subject = sub)) },
                                label = { Text(sub.label, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(Stream.entries) { stream ->
                            FilterChip(
                                selected = state.filter.stream == stream,
                                onClick = { viewModel.updateFilter(state.filter.copy(stream = stream)) },
                                label = { Text(stream.label, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                        items(ClassLevel.entries) { cl ->
                            FilterChip(
                                selected = state.filter.classLevel == cl,
                                onClick = { viewModel.updateFilter(state.filter.copy(classLevel = cl)) },
                                label = { Text(cl.label, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            // Count row
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${state.filteredResources.size} result${if (state.filteredResources.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (state.filter != ResourceFilter()) {
                    TextButton(
                        onClick = { viewModel.updateFilter(ResourceFilter()) },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear filters", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Loading resources…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                state.errorMessage != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Default.WifiOff, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Failed to load", style = MaterialTheme.typography.titleMedium)
                        Text(state.errorMessage!!, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = viewModel::loadResources) { Text("Retry") }
                    }
                }
                state.filteredResources.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Text("No results found", style = MaterialTheme.typography.titleMedium)
                        Text("Try different search terms or filters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.filteredResources, key = { it.id }) { resource ->
                        ResourceCard(
                            resource      = resource,
                            isSaved       = resource.id in state.savedResourceIds,
                            isDownloaded  = resource.id in state.downloadedIds,
                            onClick       = { onResourceClick(resource.id) },
                            onSaveToggle  = { viewModel.toggleSave(resource.id) },
                        )
                    }
                }
            }
        }
    }
}

// ── Resource Card ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceCard(
    resource: Resource,
    isSaved: Boolean,
    isDownloaded: Boolean = false,
    onClick: () -> Unit,
    onSaveToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val categoryColor = when (resource.category) {
        "Question Bank"       -> Color(0xFF1565C0)
        "Textbook Solutions"  -> Color(0xFF2E7D32)
        "Study Notes"         -> Color(0xFFE65100)
        "Video Tutorial"      -> Color(0xFFAD1457)
        "AI"                  -> Color(0xFF6A1B9A)
        else                  -> MaterialTheme.colorScheme.primary
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column {
            // Cover image
            if (resource.imageUrl.isNotBlank()) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    AsyncImage(
                        model = resource.imageUrl,
                        contentDescription = resource.imageHint,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    )
                    // Category badge overlay
                    Surface(
                        modifier = Modifier.padding(10.dp).align(Alignment.TopStart),
                        shape = RoundedCornerShape(6.dp),
                        color = categoryColor,
                    ) {
                        Text(resource.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White)
                    }
                    if (isDownloaded) {
                        Surface(
                            modifier = Modifier.padding(10.dp).align(Alignment.TopEnd),
                            shape = CircleShape,
                            color = Color(0xFF2E7D32),
                        ) {
                            Icon(Icons.Default.DownloadDone, null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp).padding(4.dp))
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                if (resource.imageUrl.isBlank()) {
                    // Show chip inline if no image
                    Surface(shape = RoundedCornerShape(6.dp), color = categoryColor.copy(alpha = 0.12f)) {
                        Text(resource.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall, color = categoryColor)
                    }
                    Spacer(Modifier.height(6.dp))
                }

                Row(verticalAlignment = Alignment.Top) {
                    Text(resource.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    IconButton(
                        onClick = onSaveToggle,
                        modifier = Modifier.size(32.dp).offset(x = 6.dp, y = (-4).dp),
                    ) {
                        Icon(
                            if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            if (isSaved) "Unsave" else "Save",
                            tint = if (isSaved) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(resource.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SubjectChip(resource.subject)
                    SubjectChip(resource.stream)
                    if (resource.isDownloadable)
                        SubjectChip("📄 PDF")
                }
            }
        }
    }
}

@Composable
private fun SubjectChip(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
