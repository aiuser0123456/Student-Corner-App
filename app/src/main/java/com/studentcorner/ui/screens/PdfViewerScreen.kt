package com.studentcorner.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdfFile: File,
    title: String,
    onBack: () -> Unit,
) {
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var scale by remember { mutableStateOf(1f) }
    val listState = rememberLazyListState()

    // Render all pages in background
    LaunchedEffect(pdfFile) {
        isLoading = true
        try {
            val rendered = withContext(Dispatchers.IO) {
                val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                val bitmaps = mutableListOf<Bitmap>()
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val bm = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bm)
                    page.close()
                }
                renderer.close()
                fd.close()
                bitmaps
            }
            pages = rendered
        } catch (e: Exception) {
            error = "Failed to open PDF: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        if (pages.isNotEmpty()) {
                            Text("${pages.size} pages", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    // Zoom out
                    IconButton(onClick = { scale = (scale - 0.25f).coerceAtLeast(0.5f) }) {
                        Icon(Icons.Default.ZoomOut, "Zoom out")
                    }
                    Text("${(scale * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp))
                    // Zoom in
                    IconButton(onClick = { scale = (scale + 0.25f).coerceAtMost(4f) }) {
                        Icon(Icons.Default.ZoomIn, "Zoom in")
                    }
                },
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF444444)),  // dark grey like a PDF reader
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Text("Loading PDF…", color = Color.White)
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(error!!, color = Color.White)
                    }
                }
                pages.isNotEmpty() -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.5f, 4f)
                                }
                            },
                    ) {
                        itemsIndexed(pages) { index, bitmap ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Page ${index + 1}",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                        ),
                                )
                                // Page number badge
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Text(
                                        "Page ${index + 1} of ${pages.size}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
