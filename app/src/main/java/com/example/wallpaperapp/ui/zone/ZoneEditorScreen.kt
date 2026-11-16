package com.example.wallpaperapp.ui.zone

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.wallpaperapp.domain.model.NormalizedRect
import kotlin.math.roundToInt

@Composable
fun ZoneEditorScreen(
    viewModel: ZoneEditorViewModel
) {
    val rect by viewModel.normalizedRect.collectAsState()
    val quote by viewModel.sampleQuote.collectAsState()
    val imageUri by viewModel.sampleImageUri.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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

        // Live Draggable Bounding Box Preview Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                val containerWidthPx = this.constraints.maxWidth.toFloat()
                val containerHeightPx = this.constraints.maxHeight.toFloat()

                // Background preview image
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

                // Calculate pixel bounds of bounding box
                val leftPx = rect.left * containerWidthPx
                val topPx = rect.top * containerHeightPx
                val widthPx = rect.width * containerWidthPx
                val heightPx = rect.height * containerHeightPx

                val density = LocalDensity.current
                val widthDp = with(density) { widthPx.toDp() }
                val heightDp = with(density) { heightPx.toDp() }

                // Interactive Bounding Box Overlay
                Box(
                    modifier = Modifier
                        .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
                        .size(widthDp, heightDp)
                        .background(Color(0x5500E5FF))
                        .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                        .pointerInput(containerWidthPx, containerHeightPx, rect) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val dxNorm = dragAmount.x / containerWidthPx
                                val dyNorm = dragAmount.y / containerHeightPx

                                val newLeft = (rect.left + dxNorm).coerceIn(0.0f, 1.0f - rect.width)
                                val newTop = (rect.top + dyNorm).coerceIn(0.0f, 1.0f - rect.height)
                                val newRight = newLeft + rect.width
                                val newBottom = newTop + rect.height

                                try {
                                    viewModel.updateRect(
                                        NormalizedRect(newLeft, newTop, newRight, newBottom)
                                    )
                                } catch (e: Exception) {
                                    // Keep within bounds safely
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
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
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
