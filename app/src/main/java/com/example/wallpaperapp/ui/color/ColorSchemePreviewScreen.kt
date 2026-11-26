package com.example.wallpaperapp.ui.color

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.wallpaperapp.domain.engine.ContrastResult
import com.example.wallpaperapp.domain.model.Quote
import com.example.wallpaperapp.domain.model.TextColorScheme
import com.example.wallpaperapp.graphics.PaletteContrastAnalyzer
import com.example.wallpaperapp.graphics.QuoteTextCanvasDrawer
import com.example.wallpaperapp.ui.components.CleanWallpaperPreviewScaffold
import com.example.wallpaperapp.ui.dashboard.DashboardViewModel
import com.example.wallpaperapp.ui.zone.ZoneEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ColorSchemePreviewScreen(
    dashboardViewModel: DashboardViewModel,
    zoneEditorViewModel: ZoneEditorViewModel,
    onConfirmScheme: () -> Unit
) {
    BackHandler(enabled = true) {
        onConfirmScheme()
    }

    val context = LocalContext.current

    val currentSavedScheme by dashboardViewModel.textColorSchemeState.collectAsState()
    val currentSavedGlow by dashboardViewModel.textGlowEnabledState.collectAsState()

    var previewScheme by remember(currentSavedScheme) { mutableStateOf(currentSavedScheme) }
    var previewTextGlow by remember(currentSavedGlow) { mutableStateOf(currentSavedGlow) }

    val rect by zoneEditorViewModel.normalizedRect.collectAsState()
    val orientation by zoneEditorViewModel.textOrientation.collectAsState()
    val fontId by dashboardViewModel.quoteFontId.collectAsState()
    val isBold by dashboardViewModel.isBoldState.collectAsState()
    val isItalic by dashboardViewModel.isItalicState.collectAsState()

    val sampleQuote by zoneEditorViewModel.sampleQuote.collectAsState()
    val imageUriString by zoneEditorViewModel.sampleImageUri.collectAsState()

    var contrastResult by remember {
        mutableStateOf(
            ContrastResult(
                textColorArgb = android.graphics.Color.WHITE,
                shadowColorArgb = android.graphics.Color.BLACK,
                isDarkBackground = true
            )
        )
    }

    // Recalculate dynamic text color and drop shadow on IO thread
    LaunchedEffect(imageUriString, rect, previewScheme) {
        withContext(Dispatchers.IO) {
            val analyzer = PaletteContrastAnalyzer()
            val bitmap = if (!imageUriString.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(imageUriString)
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
                val computed = analyzer.analyzeContrast(bitmap, rect, previewScheme)
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

    CleanWallpaperPreviewScaffold(
        topControls = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = onConfirmScheme,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Color Scheme Preview",
                        tint = Color.White
                    )
                }
            }
        },
        bottomControls = {
            Card(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Text Color Scheme Preview",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row(
                            modifier = Modifier.clickable { previewTextGlow = !previewTextGlow },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Glow", style = MaterialTheme.typography.labelLarge)
                            Switch(
                                checked = previewTextGlow,
                                onCheckedChange = { previewTextGlow = it }
                            )
                        }
                    }

                    // Horizontal Scheme Selector Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextColorScheme.entries.forEach { scheme ->
                            FilterChip(
                                selected = previewScheme == scheme,
                                onClick = { previewScheme = scheme },
                                label = { Text(scheme.displayName) }
                            )
                        }
                    }

                    Text(
                        text = previewScheme.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Use This Color Scheme Button
                    Button(
                        onClick = {
                            dashboardViewModel.setTextColorScheme(previewScheme)
                            dashboardViewModel.setTextGlowEnabled(previewTextGlow)
                            onConfirmScheme()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Use This Color Scheme", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    ) { isClean ->
        val containerWidthPx = this.constraints.maxWidth.toFloat()
        val containerHeightPx = this.constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        // Background Image
        if (!imageUriString.isNullOrEmpty()) {
            AsyncImage(
                model = imageUriString,
                contentDescription = "Color Preview Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E293B))
            )
        }

        // Semi-translucent Overlay Canvas + Dynamic Contrast Quote Render
        Canvas(modifier = Modifier.fillMaxSize()) {
            val leftPx = rect.left * containerWidthPx
            val topPx = rect.top * containerHeightPx
            val rightPx = rect.right * containerWidthPx
            val bottomPx = rect.bottom * containerHeightPx

            if (!isClean) {
                val scrimColor = Color.Black.copy(alpha = 0.40f)
                drawRect(scrimColor, Offset(0f, 0f), Size(containerWidthPx, topPx))
                drawRect(scrimColor, Offset(0f, bottomPx), Size(containerWidthPx, containerHeightPx - bottomPx))
                drawRect(scrimColor, Offset(0f, topPx), Size(leftPx, bottomPx - topPx))
                drawRect(scrimColor, Offset(rightPx, topPx), Size(containerWidthPx - rightPx, bottomPx - topPx))

                // Border overlay around bounding box
                drawRect(
                    color = Color(0x8800E5FF),
                    topLeft = Offset(leftPx, topPx),
                    size = Size(rightPx - leftPx, bottomPx - topPx),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }

            // 1:1 Native Canvas Render with Dynamic Contrast Color & Highlight Glow
            drawIntoCanvas { composeCanvas ->
                val nativeCanvas = composeCanvas.nativeCanvas
                val activeQuoteObj = sampleQuote ?: Quote("preview", "The unexamined life is not worth living.", "Socrates")
                QuoteTextCanvasDrawer.drawQuoteOnCanvas(
                    canvas = nativeCanvas,
                    quote = activeQuoteObj,
                    leftPx = leftPx,
                    topPx = topPx,
                    rightPx = rightPx,
                    bottomPx = bottomPx,
                    orientation = orientation,
                    fontId = fontId,
                    isBold = isBold,
                    isItalic = isItalic,
                    textColorArgb = contrastResult.textColorArgb,
                    shadowColorArgb = contrastResult.shadowColorArgb,
                    scaledDensity = density.density,
                    textGlowEnabled = previewTextGlow
                )
            }
        }
    }
}
