package com.example.wallpaperapp.ui.font

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.wallpaperapp.domain.model.QuoteFont
import com.example.wallpaperapp.graphics.PaletteContrastAnalyzer
import com.example.wallpaperapp.graphics.QuoteTextCanvasDrawer
import com.example.wallpaperapp.ui.components.CleanWallpaperPreviewScaffold
import com.example.wallpaperapp.ui.dashboard.DashboardViewModel
import com.example.wallpaperapp.ui.zone.ZoneEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FontPreviewScreen(
    dashboardViewModel: DashboardViewModel,
    zoneEditorViewModel: ZoneEditorViewModel,
    onConfirmFont: () -> Unit
) {
    BackHandler(enabled = true) {
        onConfirmFont()
    }

    val context = LocalContext.current
    val currentSavedFontId by dashboardViewModel.quoteFontId.collectAsState()
    val savedIsBold by dashboardViewModel.isBoldState.collectAsState()
    val savedIsItalic by dashboardViewModel.isItalicState.collectAsState()
    val activeColorScheme by dashboardViewModel.textColorSchemeState.collectAsState()
    val textGlowEnabled by dashboardViewModel.textGlowEnabledState.collectAsState()

    var previewFontId by remember(currentSavedFontId) { mutableStateOf(currentSavedFontId) }
    var previewIsBold by remember(savedIsBold) { mutableStateOf(savedIsBold) }
    var previewIsItalic by remember(savedIsItalic) { mutableStateOf(savedIsItalic) }

    val rect by zoneEditorViewModel.normalizedRect.collectAsState()
    val orientation by zoneEditorViewModel.textOrientation.collectAsState()
    val sampleQuote by zoneEditorViewModel.sampleQuote.collectAsState()
    val imageUri by zoneEditorViewModel.sampleImageUri.collectAsState()

    var contrastResult by remember {
        mutableStateOf(
            ContrastResult(
                textColorArgb = android.graphics.Color.WHITE,
                shadowColorArgb = android.graphics.Color.BLACK,
                isDarkBackground = true
            )
        )
    }

    LaunchedEffect(imageUri, rect, activeColorScheme) {
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
                val computed = analyzer.analyzeContrast(bitmap, rect, activeColorScheme)
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

    var useLongQuotePreview by remember { mutableStateOf(false) }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            dashboardViewModel.importCustomFont(context, uri)
        }
    }

    val longQuote = remember {
        Quote(
            id = "long_preview",
            text = "The unexamined life is not worth living. Human behavior flows from three main sources: desire, emotion, and knowledge. Wise men speak because they have something to say; Fools because they have to say something.",
            author = "Plato"
        )
    }

    val activeQuote = if (useLongQuotePreview) longQuote else sampleQuote

    CleanWallpaperPreviewScaffold(
        topControls = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Built-in Font Presets Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuoteFont.BUILTIN_FONTS.forEach { font ->
                        FilterChip(
                            selected = previewFontId == font.id,
                            onClick = { previewFontId = font.id },
                            label = { Text(font.name) }
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            fontPickerLauncher.launch(
                                arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/octet-stream")
                            )
                        }
                    ) {
                        Text("+ Import")
                    }
                }

                // Style Toggles Row (Bold, Italic, Long Text Preview)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = previewIsBold,
                        onClick = { previewIsBold = !previewIsBold },
                        label = { Text("Bold") }
                    )
                    FilterChip(
                        selected = previewIsItalic,
                        onClick = { previewIsItalic = !previewIsItalic },
                        label = { Text("Italic") }
                    )
                    FilterChip(
                        selected = useLongQuotePreview,
                        onClick = { useLongQuotePreview = !useLongQuotePreview },
                        label = { Text("Long Text") }
                    )
                }
            }
        },
        bottomControls = {
            Button(
                onClick = {
                    dashboardViewModel.setQuoteFontId(previewFontId)
                    dashboardViewModel.setIsBold(previewIsBold)
                    dashboardViewModel.setIsItalic(previewIsItalic)
                    onConfirmFont()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp)
            ) {
                Text("Use This Font Style", style = MaterialTheme.typography.titleMedium)
            }
        }
    ) { isClean ->
        val containerWidthPx = this.constraints.maxWidth.toFloat()
        val containerHeightPx = this.constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        // Background Image
        if (!imageUri.isNullInEmpty()) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Preview Background",
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

        // Semi-translucent Overlay Canvas + Quote Render
        Canvas(modifier = Modifier.fillMaxSize()) {
            val leftPx = rect.left * containerWidthPx
            val topPx = rect.top * containerHeightPx
            val rightPx = rect.right * containerWidthPx
            val bottomPx = rect.bottom * containerHeightPx

            if (!isClean) {
                val scrimColor = Color.Black.copy(alpha = 0.45f)
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
            }

            // 1:1 Bounding-Box-Filling Native Canvas Render with Dynamic Contrast & Glow
            drawIntoCanvas { composeCanvas ->
                val nativeCanvas = composeCanvas.nativeCanvas
                val activeQuoteObj = activeQuote ?: Quote("preview", "The unexamined life is not worth living.", "Socrates")
                QuoteTextCanvasDrawer.drawQuoteOnCanvas(
                    canvas = nativeCanvas,
                    quote = activeQuoteObj,
                    leftPx = leftPx,
                    topPx = topPx,
                    rightPx = rightPx,
                    bottomPx = bottomPx,
                    orientation = orientation,
                    fontId = previewFontId,
                    isBold = previewIsBold,
                    isItalic = previewIsItalic,
                    textColorArgb = contrastResult.textColorArgb,
                    shadowColorArgb = contrastResult.shadowColorArgb,
                    scaledDensity = density.density,
                    textGlowEnabled = textGlowEnabled
                )
            }
        }
    }
}

private fun String?.isNullInEmpty(): Boolean = this == null || this.isEmpty()
