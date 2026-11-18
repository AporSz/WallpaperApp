package com.example.wallpaperapp.ui.pools

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.wallpaperapp.domain.model.LandscapeHandlingMode
import com.example.wallpaperapp.domain.model.NormalizedRect
import com.example.wallpaperapp.domain.model.Quote
import com.example.wallpaperapp.domain.model.TextOrientation
import com.example.wallpaperapp.domain.model.WallpaperImage
import com.example.wallpaperapp.graphics.LandscapeOrientationTransformation
import com.example.wallpaperapp.graphics.QuoteTextCanvasDrawer

@Composable
fun PoolsScreen(
    viewModel: PoolsViewModel
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddQuoteDialog by remember { mutableStateOf(false) }

    var previewingQuote by remember { mutableStateOf<Quote?>(null) }
    var previewingImage by remember { mutableStateOf<WallpaperImage?>(null) }

    val quotes by viewModel.quotesState.collectAsState()
    val images by viewModel.imagesState.collectAsState()

    val landscapeMode by viewModel.landscapeHandlingModeState.collectAsState()
    val rect by viewModel.rectState.collectAsState()
    val orientation by viewModel.orientationState.collectAsState()
    val fontId by viewModel.fontIdState.collectAsState()
    val isBold by viewModel.isBoldState.collectAsState()
    val isItalic by viewModel.isItalicState.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, flag)
                } catch (_: Exception) {
                    // Ignore if permission isn't persistable for this URI
                }
            }
            viewModel.addImages(uris)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Asset Pools",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Quotes (${quotes.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Images (${images.size})") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == 0) {
                // Quotes List
                if (quotes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No quotes added yet. Tap + to add quotes!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(
                            items = quotes,
                            key = { it.id },
                            contentType = { "pool_quote" }
                        ) { quote ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { previewingQuote = quote },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "“${quote.text}”",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        if (!quote.author.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "— ${quote.author}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteQuote(quote.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Quote",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { showAddQuoteDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Quote")
                }
            } else {
                // Images Tab (Landscape Mode Selector + Grid)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Landscape Mode Selector Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Landscape Image Handling Mode",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = landscapeMode == LandscapeHandlingMode.ROTATE_90_CW,
                                    onClick = { viewModel.setLandscapeHandlingMode(LandscapeHandlingMode.ROTATE_90_CW) },
                                    label = { Text("Rotate +90°") }
                                )
                                FilterChip(
                                    selected = landscapeMode == LandscapeHandlingMode.ROTATE_90_CCW,
                                    onClick = { viewModel.setLandscapeHandlingMode(LandscapeHandlingMode.ROTATE_90_CCW) },
                                    label = { Text("Rotate -90°") }
                                )
                                FilterChip(
                                    selected = landscapeMode == LandscapeHandlingMode.CROP,
                                    onClick = { viewModel.setLandscapeHandlingMode(LandscapeHandlingMode.CROP) },
                                    label = { Text("Crop") }
                                )
                            }

                            if (landscapeMode == LandscapeHandlingMode.CROP) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "⚠️ Warning: Cropping landscape images to fit a portrait screen zooms in heavily and may drastically reduce image quality.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF856404),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Images Grid with Fluid Off-Thread Coil Transformation & Caching
                    if (images.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No images added yet. Tap + to pick images!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(
                                items = images,
                                key = { it.id },
                                contentType = { "pool_image" }
                            ) { image ->
                                val imageRequest = remember(image.uri, landscapeMode) {
                                    ImageRequest.Builder(context)
                                        .data(image.uri)
                                        .transformations(LandscapeOrientationTransformation(landscapeMode))
                                        .crossfade(true)
                                        .build()
                                }

                                val badgeText = remember(landscapeMode) {
                                    when (landscapeMode) {
                                        LandscapeHandlingMode.ROTATE_90_CW -> "+90° Mode"
                                        LandscapeHandlingMode.ROTATE_90_CCW -> "-90° Mode"
                                        LandscapeHandlingMode.CROP -> "Crop Mode"
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.75f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.DarkGray)
                                        .clickable { previewingImage = image }
                                ) {
                                    AsyncImage(
                                        model = imageRequest,
                                        contentDescription = "Wallpaper Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Orientation Badge Indicator
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(topEnd = 6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )

                                    IconButton(
                                        onClick = { viewModel.deleteImage(image.id) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(bottomStart = 8.dp))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Image",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Images")
                }
            }
        }
    }

    if (showAddQuoteDialog) {
        AddQuoteDialog(
            onDismiss = { showAddQuoteDialog = false },
            onConfirm = { text, author ->
                viewModel.addQuote(text, author)
                showAddQuoteDialog = false
            }
        )
    }

    // 1:1 Wallpaper Item Click-to-Preview Modal
    if (previewingQuote != null || previewingImage != null) {
        PoolItemPreviewDialog(
            initialQuote = previewingQuote,
            initialImage = previewingImage,
            allQuotes = quotes,
            allImages = images,
            rect = rect,
            orientation = orientation,
            fontId = fontId,
            isBold = isBold,
            isItalic = isItalic,
            landscapeMode = landscapeMode,
            onDismiss = {
                previewingQuote = null
                previewingImage = null
            }
        )
    }
}

@Composable
fun PoolItemPreviewDialog(
    initialQuote: Quote?,
    initialImage: WallpaperImage?,
    allQuotes: List<Quote>,
    allImages: List<WallpaperImage>,
    rect: NormalizedRect,
    orientation: TextOrientation,
    fontId: String,
    isBold: Boolean,
    isItalic: Boolean,
    landscapeMode: LandscapeHandlingMode,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var selectedQuoteIndex by remember {
        mutableIntStateOf(
            if (initialQuote != null && allQuotes.isNotEmpty()) {
                allQuotes.indexOf(initialQuote).coerceAtLeast(0)
            } else {
                0
            }
        )
    }

    var selectedImageIndex by remember {
        mutableIntStateOf(
            if (initialImage != null && allImages.isNotEmpty()) {
                allImages.indexOf(initialImage).coerceAtLeast(0)
            } else {
                0
            }
        )
    }

    val activeQuote = remember(selectedQuoteIndex, allQuotes, initialQuote) {
        if (allQuotes.isNotEmpty()) {
            allQuotes[selectedQuoteIndex % allQuotes.size]
        } else {
            initialQuote ?: Quote("preview", "The journey of a thousand miles begins with a single step.", "Lao Tzu")
        }
    }

    val activeImageUri = remember(selectedImageIndex, allImages, initialImage) {
        if (allImages.isNotEmpty()) {
            allImages[selectedImageIndex % allImages.size].uri
        } else {
            initialImage?.uri
        }
    }

    val displayMetrics = context.resources.displayMetrics
    val screenAspectRatio = displayMetrics.widthPixels.toFloat() / displayMetrics.heightPixels.toFloat().coerceAtLeast(1f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialQuote != null) "Quote Preview" else "Image Preview",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Full Wallpaper Aspect Ratio Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(screenAspectRatio),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        val containerWidthPx = this.constraints.maxWidth.toFloat()
                        val containerHeightPx = this.constraints.maxHeight.toFloat()

                        val leftPx = rect.left * containerWidthPx
                        val topPx = rect.top * containerHeightPx
                        val rightPx = rect.right * containerWidthPx
                        val bottomPx = rect.bottom * containerHeightPx

                        val density = LocalDensity.current

                        val imageRequest = remember(activeImageUri, landscapeMode) {
                            if (activeImageUri != null) {
                                ImageRequest.Builder(context)
                                    .data(activeImageUri)
                                    .transformations(LandscapeOrientationTransformation(landscapeMode))
                                    .crossfade(true)
                                    .build()
                            } else null
                        }

                        // Background
                        if (imageRequest != null) {
                            AsyncImage(
                                model = imageRequest,
                                contentDescription = "Preview Background",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF222222))
                            )
                        }

                        // Outer Scrim & 1:1 Bounding Box Canvas Render
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val scrimColor = Color.Black.copy(alpha = 0.50f)
                            drawRect(scrimColor, Offset(0f, 0f), Size(containerWidthPx, topPx))
                            drawRect(scrimColor, Offset(0f, bottomPx), Size(containerWidthPx, containerHeightPx - bottomPx))
                            drawRect(scrimColor, Offset(0f, topPx), Size(leftPx, bottomPx - topPx))
                            drawRect(scrimColor, Offset(rightPx, topPx), Size(containerWidthPx - rightPx, bottomPx - topPx))

                            // Border overlay around bounding box
                            drawRect(
                                color = Color(0x6600E5FF),
                                topLeft = Offset(leftPx, topPx),
                                size = Size(rightPx - leftPx, bottomPx - topPx),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )

                            // 1:1 Bounding-Box-Filling Native Canvas Render
                            drawIntoCanvas { composeCanvas ->
                                val nativeCanvas = composeCanvas.nativeCanvas
                                QuoteTextCanvasDrawer.drawQuoteOnCanvas(
                                    canvas = nativeCanvas,
                                    quote = activeQuote,
                                    leftPx = leftPx,
                                    topPx = topPx,
                                    rightPx = rightPx,
                                    bottomPx = bottomPx,
                                    orientation = orientation,
                                    fontId = fontId,
                                    isBold = isBold,
                                    isItalic = isItalic,
                                    textColorArgb = android.graphics.Color.WHITE,
                                    shadowColorArgb = android.graphics.Color.BLACK,
                                    scaledDensity = density.density
                                )
                            }
                        }
                    }
                }

                // Cycling buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (initialQuote != null && allImages.size > 1) {
                        OutlinedButton(
                            onClick = { selectedImageIndex = (selectedImageIndex + 1) % allImages.size }
                        ) {
                            Text("Cycle Image")
                        }
                    }

                    if (initialImage != null && allQuotes.size > 1) {
                        OutlinedButton(
                            onClick = { selectedQuoteIndex = (selectedQuoteIndex + 1) % allQuotes.size }
                        ) {
                            Text("Cycle Quote")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun AddQuoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var quoteText by remember { mutableStateOf("") }
    var authorText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Quote") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quoteText,
                    onValueChange = { quoteText = it },
                    label = { Text("Quote Text") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = authorText,
                    onValueChange = { authorText = it },
                    label = { Text("Author (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(quoteText, authorText) },
                enabled = quoteText.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
