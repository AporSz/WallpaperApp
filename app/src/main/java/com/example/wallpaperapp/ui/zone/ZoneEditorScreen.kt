package com.example.wallpaperapp.ui.zone

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.wallpaperapp.domain.engine.ContrastResult
import com.example.wallpaperapp.domain.model.Quote
import com.example.wallpaperapp.domain.model.TextOrientation
import com.example.wallpaperapp.graphics.PaletteContrastAnalyzer
import com.example.wallpaperapp.graphics.QuoteTextCanvasDrawer
import com.example.wallpaperapp.ui.components.CleanWallpaperPreviewScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ZoneEditorScreen(
    viewModel: ZoneEditorViewModel,
    onToggleBottomNav: () -> Unit = {}
) {
    val context = LocalContext.current

    val savedRect by viewModel.normalizedRect.collectAsState()
    var localRect by remember(savedRect) { mutableStateOf(savedRect) }

    val orientation by viewModel.textOrientation.collectAsState()
    val activeFontId by viewModel.quoteFontId.collectAsState()
    val isBold by viewModel.isBoldState.collectAsState()
    val isItalic by viewModel.isItalicState.collectAsState()
    val activeColorScheme by viewModel.textColorSchemeState.collectAsState()
    val textGlowEnabled by viewModel.textGlowEnabledState.collectAsState()

    val sampleQuote by viewModel.sampleQuote.collectAsState()
    val imageUri by viewModel.sampleImageUri.collectAsState()

    var contrastResult by remember {
        mutableStateOf(
            ContrastResult(
                textColorArgb = android.graphics.Color.WHITE,
                shadowColorArgb = android.graphics.Color.BLACK,
                isDarkBackground = true
            )
        )
    }

    LaunchedEffect(imageUri, localRect, activeColorScheme) {
        withContext(Dispatchers.IO) {
            val analyzer = PaletteContrastAnalyzer()
            val bitmap = if (!imageUri.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(imageUri)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }

            if (bitmap != null) {
                val computed = analyzer.analyzeContrast(bitmap, localRect, activeColorScheme)
                withContext(Dispatchers.Main) {
                    contrastResult = computed
                }
                bitmap.recycle()
            } else {
                withContext(Dispatchers.Main) {
                    contrastResult = ContrastResult(
                        textColorArgb = android.graphics.Color.WHITE,
                        shadowColorArgb = android.graphics.Color.BLACK,
                        isDarkBackground = true
                    )
                }
            }
        }
    }

    var isDragging by remember { mutableStateOf(false) }
    var useLongQuotePreview by remember { mutableStateOf(false) }
    var isCleanPreviewMode by remember { mutableStateOf(false) }

    val longQuote = remember {
        Quote(
            id = "long_preview",
            text = "The unexamined life is not worth living. Human behavior flows from three main sources: desire, emotion, and knowledge. Wise men speak because they have something to say; Fools because they have to say something.",
            author = "Plato"
        )
    }

    val activeQuote = if (useLongQuotePreview) longQuote else sampleQuote

    CleanWallpaperPreviewScaffold(
        isCleanMode = isCleanPreviewMode,
        onCleanModeChange = { isCleanPreviewMode = it },
        onTap = onToggleBottomNav,
        topControls = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = orientation == TextOrientation.HORIZONTAL,
                        onClick = { viewModel.setTextOrientation(TextOrientation.HORIZONTAL) },
                        label = { Text("Horizontal") }
                    )
                    FilterChip(
                        selected = orientation == TextOrientation.VERTICAL_UP,
                        onClick = { viewModel.setTextOrientation(TextOrientation.VERTICAL_UP) },
                        label = { Text("Vert Up") }
                    )
                    FilterChip(
                        selected = orientation == TextOrientation.VERTICAL_DOWN,
                        onClick = { viewModel.setTextOrientation(TextOrientation.VERTICAL_DOWN) },
                        label = { Text("Vert Down") }
                    )
                }

                FilterChip(
                    selected = useLongQuotePreview,
                    onClick = { useLongQuotePreview = !useLongQuotePreview },
                    label = { Text("Long Text") }
                )
            }
        }
    ) { isClean ->
        val containerWidthPx = this.constraints.maxWidth.toFloat()
        val containerHeightPx = this.constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        val leftPx = localRect.left * containerWidthPx
        val topPx = localRect.top * containerHeightPx
        val widthPx = localRect.width * containerWidthPx
        val heightPx = localRect.height * containerHeightPx

        val widthDp = with(density) { widthPx.toDp() }
        val heightDp = with(density) { heightPx.toDp() }

        // Magnetic snap thresholds
        val centerSnapThreshold = 0.015f
        val isNearVerticalCenter = abs(localRect.left + localRect.width / 2f - 0.5f) < centerSnapThreshold
        val isNearHorizontalCenter = abs(localRect.top + localRect.height / 2f - 0.5f) < centerSnapThreshold

        // 1. Background Image
        if (!imageUri.isNullOrEmpty()) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Sample Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2C3E50))
            )
        }

        // 2. Dimmed Scrim & Canvas Text Render
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (!isClean) {
                val scrimColor = Color.Black.copy(alpha = 0.45f)
                drawRect(scrimColor, Offset(0f, 0f), Size(containerWidthPx, topPx))
                drawRect(scrimColor, Offset(0f, topPx + heightPx), Size(containerWidthPx, containerHeightPx - (topPx + heightPx)))
                drawRect(scrimColor, Offset(0f, topPx), Size(leftPx, heightPx))
                drawRect(scrimColor, Offset(leftPx + widthPx, topPx), Size(containerWidthPx - (leftPx + widthPx), heightPx))

                // Dynamic Alignment Guiding Lines
                if (isDragging) {
                    val strokeW = 1.dp.toPx()
                    val vertCenterColor = if (isNearVerticalCenter) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.5f)
                    val vertStrokeW = if (isNearVerticalCenter) 2.dp.toPx() else strokeW
                    drawLine(vertCenterColor, Offset(containerWidthPx * 0.5f, 0f), Offset(containerWidthPx * 0.5f, containerHeightPx), strokeWidth = vertStrokeW)

                    val horizCenterColor = if (isNearHorizontalCenter) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.5f)
                    val horizStrokeW = if (isNearHorizontalCenter) 2.dp.toPx() else strokeW
                    drawLine(horizCenterColor, Offset(0f, containerHeightPx * 0.5f), Offset(containerWidthPx, containerHeightPx * 0.5f), strokeWidth = horizStrokeW)

                    // Rule of Thirds Guides
                    val thirdColor = Color.White.copy(alpha = 0.3f)
                    drawLine(thirdColor, Offset(containerWidthPx * 0.333f, 0f), Offset(containerWidthPx * 0.333f, containerHeightPx), strokeWidth = strokeW)
                    drawLine(thirdColor, Offset(containerWidthPx * 0.666f, 0f), Offset(containerWidthPx * 0.666f, containerHeightPx), strokeWidth = strokeW)
                    drawLine(thirdColor, Offset(0f, containerHeightPx * 0.333f), Offset(containerWidthPx, containerHeightPx * 0.333f), strokeWidth = strokeW)
                    drawLine(thirdColor, Offset(0f, containerHeightPx * 0.666f), Offset(containerWidthPx, containerHeightPx * 0.666f), strokeWidth = strokeW)
                }
            }

            // 1:1 Bounding-Box-Filling Native Canvas Render with Dynamic Contrast & Glow
            drawIntoCanvas { composeCanvas ->
                val nativeCanvas = composeCanvas.nativeCanvas
                val activeQuoteObj = activeQuote ?: Quote("preview", "The journey of a thousand miles begins with a single step.", "Lao Tzu")
                QuoteTextCanvasDrawer.drawQuoteOnCanvas(
                    canvas = nativeCanvas,
                    quote = activeQuoteObj,
                    leftPx = leftPx,
                    topPx = topPx,
                    rightPx = leftPx + widthPx,
                    bottomPx = topPx + heightPx,
                    orientation = orientation,
                    fontId = activeFontId,
                    isBold = isBold,
                    isItalic = isItalic,
                    textColorArgb = contrastResult.textColorArgb,
                    shadowColorArgb = contrastResult.shadowColorArgb,
                    scaledDensity = density.density,
                    textGlowEnabled = textGlowEnabled
                )
            }
        }

        if (!isClean) {
            // 4. Visual Active Bounding Box Overlay Window
            Box(
                modifier = Modifier
                    .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
                    .size(widthDp, heightDp)
                    .background(Color(0x2200E5FF))
                    .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(4.dp))
                    .clipToBounds()
            )

            // 5. Visual Corner & Edge Handles (8 cyan circles)
            val handleSizeDp = 24.dp
            val handleOffsetDp = 12.dp

            val handlePositions = listOf(
                HandleType.TOP_LEFT to IntOffset((leftPx - with(density) { handleOffsetDp.toPx() }).roundToInt(), (topPx - with(density) { handleOffsetDp.toPx() }).roundToInt()),
                HandleType.TOP_RIGHT to IntOffset((leftPx + widthPx - with(density) { handleOffsetDp.toPx() }).roundToInt(), (topPx - with(density) { handleOffsetDp.toPx() }).roundToInt()),
                HandleType.BOTTOM_LEFT to IntOffset((leftPx - with(density) { handleOffsetDp.toPx() }).roundToInt(), (topPx + heightPx - with(density) { handleOffsetDp.toPx() }).roundToInt()),
                HandleType.BOTTOM_RIGHT to IntOffset((leftPx + widthPx - with(density) { handleOffsetDp.toPx() }).roundToInt(), (topPx + heightPx - with(density) { handleOffsetDp.toPx() }).roundToInt()),
                HandleType.TOP to IntOffset((leftPx + widthPx / 2f - with(density) { handleOffsetDp.toPx() }).roundToInt(), (topPx - with(density) { handleOffsetDp.toPx() }).roundToInt()),
                HandleType.BOTTOM to IntOffset((leftPx + widthPx / 2f - with(density) { handleOffsetDp.toPx() }).roundToInt(), (topPx + heightPx - with(density) { handleOffsetDp.toPx() }).roundToInt()),
                HandleType.LEFT to IntOffset((leftPx - with(density) { handleOffsetDp.toPx() }).roundToInt(), (topPx + heightPx / 2f - with(density) { handleOffsetDp.toPx() }).roundToInt()),
                HandleType.RIGHT to IntOffset((leftPx + widthPx - with(density) { handleOffsetDp.toPx() }).roundToInt(), (topPx + heightPx / 2f - with(density) { handleOffsetDp.toPx() }).roundToInt())
            )

            handlePositions.forEach { (_, offset) ->
                Box(
                    modifier = Modifier
                        .offset { offset }
                        .size(handleSizeDp)
                        .background(Color(0xFF00E5FF), CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                )
            }

            // 6. Unified Anchor-Based Touch Hit-Test & Drag/Resize Overlay
            val currentLocalRectState = rememberUpdatedState(localRect)
            val cornerHitRadiusPx = with(density) { 40.dp.toPx() }
            val edgeHitBandPx = with(density) { 32.dp.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val hitHandle = CropBoxHitTester.hitTest(
                                rect = currentLocalRectState.value,
                                containerWidthPx = containerWidthPx,
                                containerHeightPx = containerHeightPx,
                                touchPx = down.position,
                                cornerRadiusPx = cornerHitRadiusPx,
                                edgeBandPx = edgeHitBandPx
                            )

                            if (hitHandle != null) {
                                down.consume()
                                isDragging = true
                                val initialRectOnDown = currentLocalRectState.value
                                var accumulatedDxNorm = 0f
                                var accumulatedDyNorm = 0f
                                val pointerId = down.id

                                try {
                                    do {
                                        val event = awaitPointerEvent()
                                        val pointerChange = event.changes.firstOrNull { it.id == pointerId } ?: event.changes.firstOrNull()
                                        if (pointerChange != null && pointerChange.pressed) {
                                            val dragAmount = pointerChange.position - pointerChange.previousPosition
                                            if (dragAmount.x != 0f || dragAmount.y != 0f) {
                                                pointerChange.consume()
                                                accumulatedDxNorm += dragAmount.x / containerWidthPx
                                                accumulatedDyNorm += dragAmount.y / containerHeightPx

                                                val dragResult = CropBoxDragEngine.computeDrag(
                                                    initialRect = initialRectOnDown,
                                                    handle = hitHandle,
                                                    accumulatedDxNorm = accumulatedDxNorm,
                                                    accumulatedDyNorm = accumulatedDyNorm
                                                )

                                                localRect = dragResult.displayedRect
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })
                                } finally {
                                    isDragging = false
                                    viewModel.updateRect(currentLocalRectState.value)
                                }
                            }
                        }
                    }
            )
        }
    }
}
