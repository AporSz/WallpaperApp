package com.example.wallpaperapp.ui.zone

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.wallpaperapp.domain.model.NormalizedRect
import com.example.wallpaperapp.domain.model.Quote
import com.example.wallpaperapp.domain.model.TextOrientation
import com.example.wallpaperapp.ui.zone.CropBoxGeometry.resizeOrMove
import kotlin.math.roundToInt

@Composable
fun ZoneEditorScreen(
    viewModel: ZoneEditorViewModel
) {
    val savedRect by viewModel.normalizedRect.collectAsState()
    var localRect by remember(savedRect) { mutableStateOf(savedRect) }

    val orientation by viewModel.textOrientation.collectAsState()
    val sampleQuote by viewModel.sampleQuote.collectAsState()
    val imageUri by viewModel.sampleImageUri.collectAsState()

    var isDragging by remember { mutableStateOf(false) }
    var useLongQuotePreview by remember { mutableStateOf(false) }

    val longQuote = remember {
        Quote(
            id = "long_preview",
            text = "The unexamined life is not worth living. Human behavior flows from three main sources: desire, emotion, and knowledge. Wise men speak because they have something to say; Fools because they have to say something.",
            author = "Plato"
        )
    }

    val activeQuote = if (useLongQuotePreview) longQuote else sampleQuote

    val displayMetrics = LocalContext.current.resources.displayMetrics
    val screenAspectRatio = displayMetrics.widthPixels.toFloat() / displayMetrics.heightPixels.toFloat().coerceAtLeast(1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Quote Zone Editor",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.Start)
        )

        // Presets row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilterChip(
                selected = localRect == NormalizedRect.UPPER_THIRD,
                onClick = {
                    localRect = NormalizedRect.UPPER_THIRD
                    viewModel.applyPreset(NormalizedRect.UPPER_THIRD)
                },
                label = { Text("Upper Third") }
            )
            FilterChip(
                selected = localRect == NormalizedRect.CENTER,
                onClick = {
                    localRect = NormalizedRect.CENTER
                    viewModel.applyPreset(NormalizedRect.CENTER)
                },
                label = { Text("Center") }
            )
            FilterChip(
                selected = localRect == NormalizedRect.LOWER_THIRD,
                onClick = {
                    localRect = NormalizedRect.LOWER_THIRD
                    viewModel.applyPreset(NormalizedRect.LOWER_THIRD)
                },
                label = { Text("Lower Third") }
            )
        }

        // Text Orientation Selector & Long Quote Toggle row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilterChip(
                selected = orientation == TextOrientation.HORIZONTAL,
                onClick = { viewModel.setTextOrientation(TextOrientation.HORIZONTAL) },
                label = { Text("Horizontal") }
            )
            FilterChip(
                selected = orientation == TextOrientation.VERTICAL_UP,
                onClick = { viewModel.setTextOrientation(TextOrientation.VERTICAL_UP) },
                label = { Text("Vert (-90°)") }
            )
            FilterChip(
                selected = orientation == TextOrientation.VERTICAL_DOWN,
                onClick = { viewModel.setTextOrientation(TextOrientation.VERTICAL_DOWN) },
                label = { Text("Vert (+90°)") }
            )
            FilterChip(
                selected = useLongQuotePreview,
                onClick = { useLongQuotePreview = !useLongQuotePreview },
                label = { Text("Long Text") }
            )
        }

        // Aspect-Ratio-Locked Screenshot-Style Bounding Box Editor
        Card(
            modifier = Modifier
                .weight(1f)
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

                // Remember updated states for drag callbacks to prevent pointerInput re-keying interruptions
                val currentLocalRect by rememberUpdatedState(localRect)
                val currentContainerW by rememberUpdatedState(containerWidthPx)
                val currentContainerH by rememberUpdatedState(containerHeightPx)

                // 1. Background preview image
                if (imageUri != null) {
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
                            .background(Color(0xFF222222))
                    )
                }

                // Pixel bounds of localRect
                val leftPx = localRect.left * containerWidthPx
                val topPx = localRect.top * containerHeightPx
                val widthPx = localRect.width * containerWidthPx
                val heightPx = localRect.height * containerHeightPx

                val density = LocalDensity.current
                val widthDp = with(density) { widthPx.toDp() }
                val heightDp = with(density) { heightPx.toDp() }

                // 2. Dimmed Outer Scrim Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scrimColor = Color.Black.copy(alpha = 0.55f)
                    // Top scrim
                    drawRect(scrimColor, Offset(0f, 0f), Size(containerWidthPx, topPx))
                    // Bottom scrim
                    drawRect(scrimColor, Offset(0f, topPx + heightPx), Size(containerWidthPx, containerHeightPx - (topPx + heightPx)))
                    // Left scrim
                    drawRect(scrimColor, Offset(0f, topPx), Size(leftPx, heightPx))
                    // Right scrim
                    drawRect(scrimColor, Offset(leftPx + widthPx, topPx), Size(containerWidthPx - (leftPx + widthPx), heightPx))

                    // 3. Rule of Thirds Guide Lines inside box (shown while dragging)
                    if (isDragging) {
                        val guideColor = Color.White.copy(alpha = 0.45f)
                        val strokeW = 1.dp.toPx()
                        drawLine(guideColor, Offset(leftPx + widthPx / 3f, topPx), Offset(leftPx + widthPx / 3f, topPx + heightPx), strokeWidth = strokeW)
                        drawLine(guideColor, Offset(leftPx + 2 * widthPx / 3f, topPx), Offset(leftPx + 2 * widthPx / 3f, topPx + heightPx), strokeWidth = strokeW)
                        drawLine(guideColor, Offset(leftPx, topPx + heightPx / 3f), Offset(leftPx + widthPx, topPx + heightPx / 3f), strokeWidth = strokeW)
                        drawLine(guideColor, Offset(leftPx, topPx + 2 * heightPx / 3f), Offset(leftPx + widthPx, topPx + 2 * heightPx / 3f), strokeWidth = strokeW)
                    }
                }

                // 4. Active Bounding Box Overlay Window + Ephemeral Center Drag
                Box(
                    modifier = Modifier
                        .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
                        .size(widthDp, heightDp)
                        .background(Color(0x3300E5FF))
                        .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(4.dp))
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = {
                                    isDragging = false
                                    viewModel.updateRect(currentLocalRect)
                                },
                                onDragCancel = {
                                    isDragging = false
                                    viewModel.updateRect(currentLocalRect)
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                val dxNorm = dragAmount.x / currentContainerW
                                val dyNorm = dragAmount.y / currentContainerH
                                localRect = currentLocalRect.resizeOrMove(HandleType.CENTER, dxNorm, dyNorm)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val rotationDegrees = when (orientation) {
                        TextOrientation.HORIZONTAL -> 0f
                        TextOrientation.VERTICAL_UP -> -90f
                        TextOrientation.VERTICAL_DOWN -> 90f
                    }

                    Column(
                        modifier = Modifier
                            .padding(6.dp)
                            .graphicsLayer { rotationZ = rotationDegrees }
                            .clipToBounds(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = activeQuote?.text ?: "Sample quote text renders here...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            overflow = TextOverflow.Ellipsis
                        )
                        val currentQuote = activeQuote
                        if (currentQuote != null && !currentQuote.author.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "— ${currentQuote.author}",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.labelSmall,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 5. 8 Grab Handles (4 Corners + 4 Edges)
                val handles = listOf(
                    HandleType.TOP_LEFT to Offset(leftPx, topPx),
                    HandleType.TOP to Offset(leftPx + widthPx / 2f, topPx),
                    HandleType.TOP_RIGHT to Offset(leftPx + widthPx, topPx),
                    HandleType.RIGHT to Offset(leftPx + widthPx, topPx + heightPx / 2f),
                    HandleType.BOTTOM_RIGHT to Offset(leftPx + widthPx, topPx + heightPx),
                    HandleType.BOTTOM to Offset(leftPx + widthPx / 2f, topPx + heightPx),
                    HandleType.BOTTOM_LEFT to Offset(leftPx, topPx + heightPx),
                    HandleType.LEFT to Offset(leftPx, topPx + heightPx / 2f)
                )

                handles.forEach { (handleType, pos) ->
                    GrabHandleOverlay(
                        handleType = handleType,
                        centerX = pos.x,
                        centerY = pos.y,
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            viewModel.updateRect(currentLocalRect)
                        },
                        onDrag = { dxPx, dyPx ->
                            val dxNorm = dxPx / currentContainerW
                            val dyNorm = dyPx / currentContainerH
                            localRect = currentLocalRect.resizeOrMove(handleType, dxNorm, dyNorm)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GrabHandleOverlay(
    handleType: HandleType,
    centerX: Float,
    centerY: Float,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (dxPx: Float, dyPx: Float) -> Unit
) {
    val touchSizeDp = 38.dp
    val density = LocalDensity.current
    val touchSizePx = with(density) { touchSizeDp.toPx() }

    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    val isCorner = handleType in listOf(
        HandleType.TOP_LEFT, HandleType.TOP_RIGHT,
        HandleType.BOTTOM_LEFT, HandleType.BOTTOM_RIGHT
    )

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (centerX - touchSizePx / 2f).roundToInt(),
                    (centerY - touchSizePx / 2f).roundToInt()
                )
            }
            .size(touchSizeDp)
            .pointerInput(handleType) {
                detectDragGestures(
                    onDragStart = { currentOnDragStart() },
                    onDragEnd = { currentOnDragEnd() },
                    onDragCancel = { currentOnDragEnd() }
                ) { change, dragAmount ->
                    change.consume()
                    currentOnDrag(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (isCorner) {
            // Circle handle for corners
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color.White, CircleShape)
                    .border(2.dp, Color(0xFF00E5FF), CircleShape)
            )
        } else {
            // Pill handle for edges
            val isHorizontalEdge = handleType == HandleType.TOP || handleType == HandleType.BOTTOM
            val pillW = if (isHorizontalEdge) 24.dp else 8.dp
            val pillH = if (isHorizontalEdge) 8.dp else 24.dp

            Box(
                modifier = Modifier
                    .size(pillW, pillH)
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(4.dp))
            )
        }
    }
}
