package com.studentcorner.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Neutral viewer bg that works in both light & dark — like a real PDF reader
private val ViewerBg     = Color(0xFF2B2D3A)
private val ViewerBgDark = Color(0xFF1A1C28)
private val PageShadow   = Color(0x55000000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdfFile: File,
    title: String,
    onBack: () -> Unit,
) {
    var pages       by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(true) }
    var error       by remember { mutableStateOf<String?>(null) }
    var scale       by remember { mutableStateOf(1f) }
    val listState   = rememberLazyListState()
    val scope       = rememberCoroutineScope()

    // Current visible page indicator
    val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }

    LaunchedEffect(pdfFile) {
        isLoading = true
        error = null
        try {
            val rendered = withContext(Dispatchers.IO) {
                val fd       = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                val bitmaps  = mutableListOf<Bitmap>()
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    // 2× density for crisp rendering
                    val w  = page.width * 2
                    val h  = page.height * 2
                    val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    // Fill white so text is readable (PdfRenderer renders transparent by default)
                    bm.eraseColor(android.graphics.Color.WHITE)
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
            error = "Could not open PDF: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Free bitmaps when leaving screen
    DisposableEffect(Unit) { onDispose { pages.forEach { it.recycle() } } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1, color = Color.White)
                        if (pages.isNotEmpty())
                            Text("${pages.size} pages", style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                },
                actions = {
                    IconButton(onClick = { scale = (scale - 0.2f).coerceAtLeast(0.4f) }) {
                        Icon(Icons.Default.ZoomOut, "Zoom out", tint = Color.White)
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.15f)) {
                        Text("${(scale * 100).toInt()}%",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium, color = Color.White)
                    }
                    IconButton(onClick = { scale = (scale + 0.2f).coerceAtMost(4f) }) {
                        Icon(Icons.Default.ZoomIn, "Zoom in", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ViewerBg),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ViewerBgDark),
        ) {
            when {
                isLoading -> {
                    Column(modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(16.dp))
                        Text("Loading PDF…", color = Color.White,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                error != null -> {
                    Column(modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFEF5350),
                            modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Failed to open PDF", color = Color.White,
                            style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(error!!, color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                pages.isNotEmpty() -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.4f, 4f)
                                }
                            },
                    ) {
                        itemsIndexed(pages, key = { index, _ -> index }) { index, bitmap ->
                            Box(modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Page ${index + 1}",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth(scale.coerceAtMost(1f))
                                        .shadow(8.dp, RoundedCornerShape(4.dp))
                                        .clip(RoundedCornerShape(4.dp))
                                        // Scale > 1 applied via graphicsLayer for zoom
                                        .then(
                                            if (scale > 1f)
                                                Modifier.wrapContentSize(unbounded = true)
                                            else Modifier
                                        ),
                                )
                            }
                        }
                    }

                    // Page indicator pill — bottom center
                    if (pages.size > 1) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp),
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.65f),
                        ) {
                            Text("$currentPage / ${pages.size}",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    // Scroll to top FAB
                    val showScrollTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }
                    AnimatedVisibility(
                        visible = showScrollTop,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        enter = fadeIn() + scaleIn(),
                        exit  = fadeOut() + scaleOut(),
                    ) {
                        FloatingActionButton(
                            onClick = { scope.launch { listState.animateScrollToItem(0) } },
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, "Scroll to top",
                                tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
