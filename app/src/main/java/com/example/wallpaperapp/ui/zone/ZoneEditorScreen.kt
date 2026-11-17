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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.wallpaperapp.domain.model.NormalizedRect
import com.example.wallpaperapp.domain.model.TextOrientation
import com.example.wallpaperapp.ui.zone.CropBoxGeometry.resizeOrMove
import kotlin.math.roundToInt

@Composable
fun ZoneEditorScreen(
    viewModel: ZoneEditorViewModel
) {
    val rect by viewModel.normalizedRect.collectAsState()
    val orientation by viewModel.textOrientation.collectAsState()
    val quote by viewModel.sampleQuote.collectAsState()
    val imageUri by viewModel.sampleImageUri.collectAsState()

    var isDragging by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                selected = rect == NormalizedRect.UPPER_THIRD,
                onClick = { viewModel.applyPreset(NormalizedRect.UPPER_THIRD) },
                label = { Text("Upper Third") }
            )
            FilterChip(
                selected = rect == NormalizedRect.CENTER,
                onClick = { viewModel.applyPreset(NormalizedRect.CENTER) },
                label = { Text("Center") }
            )
            FilterChip(
                selected = rect == NormalizedRect.LOWER_THIRD,
                onClick = { viewModel.applyPreset(NormalizedRect.LOWER_THIRD) },
                label = { Text("Lower Third") }
            )
        }

        // Text Orientation Selector row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilterChip(
                selected = orientation == TextOrientation.HORIZONTAL,
                onClick = { viewModel.setTextOrientation(TextOrientation.HORIZONTAL) },
                label = { Text("Horizontal (0°)") }
            )
            FilterChip(
                selected = orientation == TextOrientation.VERTICAL_UP,
                onClick = { viewModel.setTextOrientation(TextOrientation.VERTICAL_UP) },
                label = { Text("Vertical (-90°)") }
            )
            FilterChip(
                selected = orientation == TextOrientation.VERTICAL_DOWN,
                onClick = { viewModel.setTextOrientation(TextOrientation.VERTICAL_DOWN) },
                label = { Text("Vertical (+90°)") }
            )
        }

        // Screenshot-Style Interactive Bounding Box Preview Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                val containerWidthPx = this.constraints.maxWidth.toFloat()
                val containerHeightPx = this.constraints.maxHeight.toFloat()

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

                // Pixel bounds of normalized rect
                val leftPx = rect.left * containerWidthPx
                val topPx = rect.top * containerHeightPx
                val widthPx = rect.width * containerWidthPx
                val heightPx = rect.height * containerHeightPx

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
                        val guideColor = Color.White.copy(alpha = 0.4f)
                        val strokeW = 1.dp.toPx()
                        // Vertical third lines
                        drawLine(guideColor, Offset(leftPx + widthPx / 3f, topPx), Offset(leftPx + widthPx / 3f, topPx + heightPx), strokeWidth = strokeW)
                        drawLine(guideColor, Offset(leftPx + 2 * widthPx / 3f, topPx), Offset(leftPx + 2 * widthPx / 3f, topPx + heightPx), strokeWidth = strokeW)
                        // Horizontal third lines
                        drawLine(guideColor, Offset(leftPx, topPx + heightPx / 3f), Offset(leftPx + widthPx, topPx + heightPx / 3f), strokeWidth = strokeW)
                        drawLine(guideColor, Offset(leftPx, topPx + 2 * heightPx / 3f), Offset(leftPx + widthPx, topPx + 2 * heightPx / 3f), strokeWidth = strokeW)
                    }
                }

                // 4. Active Bounding Box Overlay Window + Interior Drag
                Box(
                    modifier = Modifier
                        .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
                        .size(widthDp, heightDp)
                        .background(Color(0x3300E5FF))
                        .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(4.dp))
                        .pointerInput(containerWidthPx, containerHeightPx, rect) {
                            detectDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = { isDragging = false },
                                onDragCancel = { isDragging = false }
                            ) { change, dragAmount ->
                                change.consume()
                                val dxNorm = dragAmount.x / containerWidthPx
                                val dyNorm = dragAmount.y / containerHeightPx
                                val newRect = rect.resizeOrMove(HandleType.CENTER, dxNorm, dyNorm)
                                viewModel.updateRect(newRect)
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
                            .padding(8.dp)
                            .graphicsLayer { rotationZ = rotationDegrees },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = quote?.text ?: "Sample quote text renders here...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 4
                        )
                        if (!quote?.author.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "— ${quote?.author}",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.labelSmall
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
                        onDragEnd = { isDragging = false },
                        onDrag = { dxPx, dyPx ->
                            val dxNorm = dxPx / containerWidthPx
                            val dyNorm = dyPx / containerHeightPx
                            val newRect = rect.resizeOrMove(handleType, dxNorm, dyNorm)
                            viewModel.updateRect(newRect)
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
    val touchSizeDp = 36.dp
    val density = LocalDensity.current
    val touchSizePx = with(density) { touchSizeDp.toPx() }

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
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                ) { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
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
            val pillW = if (isHorizontalEdge) 22.dp else 8.dp
            val pillH = if (isHorizontalEdge) 8.dp else 22.dp

            Box(
                modifier = Modifier
                    .size(pillW, pillH)
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(4.dp))
            )
        }
    }
}
