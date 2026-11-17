package com.example.wallpaperapp.ui.font

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.wallpaperapp.domain.model.Quote
import com.example.wallpaperapp.domain.model.QuoteFont
import com.example.wallpaperapp.domain.model.TextOrientation
import com.example.wallpaperapp.ui.dashboard.DashboardViewModel
import com.example.wallpaperapp.ui.zone.ZoneEditorViewModel
import java.io.File
import kotlin.math.roundToInt

@Composable
fun FontPreviewScreen(
    dashboardViewModel: DashboardViewModel,
    zoneEditorViewModel: ZoneEditorViewModel,
    onConfirmFont: () -> Unit
) {
    val context = LocalContext.current
    val currentSavedFontId by dashboardViewModel.quoteFontId.collectAsState()
    val savedIsBold by dashboardViewModel.isBoldState.collectAsState()
    val savedIsItalic by dashboardViewModel.isItalicState.collectAsState()

    var previewFontId by remember(currentSavedFontId) { mutableStateOf(currentSavedFontId) }
    var previewIsBold by remember(savedIsBold) { mutableStateOf(savedIsBold) }
    var previewIsItalic by remember(savedIsItalic) { mutableStateOf(savedIsItalic) }

    val rect by zoneEditorViewModel.normalizedRect.collectAsState()
    val orientation by zoneEditorViewModel.textOrientation.collectAsState()
    val sampleQuote by zoneEditorViewModel.sampleQuote.collectAsState()
    val imageUri by zoneEditorViewModel.sampleImageUri.collectAsState()

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

    val displayMetrics = context.resources.displayMetrics
    val screenAspectRatio = displayMetrics.widthPixels.toFloat() / displayMetrics.heightPixels.toFloat().coerceAtLeast(1f)

    val previewFontFamily = remember(previewFontId) {
        when (previewFontId) {
            "builtin_sans" -> FontFamily.SansSerif
            "builtin_serif" -> FontFamily.Serif
            "builtin_mono" -> FontFamily.Monospace
            "builtin_cursive" -> FontFamily.Cursive
            "builtin_casual" -> FontFamily.Cursive
            "builtin_sans_medium" -> FontFamily.SansSerif
            "builtin_serif_bold" -> FontFamily.Serif
            "builtin_mono_medium" -> FontFamily.Monospace
            "builtin_sans_black" -> FontFamily.SansSerif
            "builtin_sans_condensed" -> FontFamily.SansSerif
            "builtin_sans_light" -> FontFamily.SansSerif
            "builtin_serif_italic" -> FontFamily.Serif
            else -> {
                val filePath = if (previewFontId.startsWith("custom_")) {
                    previewFontId.removePrefix("custom_")
                } else {
                    previewFontId
                }
                try {
                    val file = File(filePath)
                    if (file.exists() && file.length() > 0) {
                        FontFamily(Font(file))
                    } else {
                        FontFamily.SansSerif
                    }
                } catch (_: Exception) {
                    FontFamily.SansSerif
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Full Typography Preview",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.Start)
        )

        // Built-in Font Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
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

        // Wallpaper Aspect Ratio Live Canvas Card
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
                val widthPx = rect.width * containerWidthPx
                val heightPx = rect.height * containerHeightPx

                val density = LocalDensity.current
                val widthDp = with(density) { widthPx.toDp() }
                val heightDp = with(density) { heightPx.toDp() }

                // Background
                if (imageUri != null) {
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
                            .background(Color(0xFF222222))
                    )
                }

                // Dimmed Outer Scrim
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scrimColor = Color.Black.copy(alpha = 0.50f)
                    drawRect(scrimColor, Offset(0f, 0f), Size(containerWidthPx, topPx))
                    drawRect(scrimColor, Offset(0f, topPx + heightPx), Size(containerWidthPx, containerHeightPx - (topPx + heightPx)))
                    drawRect(scrimColor, Offset(0f, topPx), Size(leftPx, heightPx))
                    drawRect(scrimColor, Offset(leftPx + widthPx, topPx), Size(containerWidthPx - (leftPx + widthPx), heightPx))
                }

                // Active Text Bounding Box
                Box(
                    modifier = Modifier
                        .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
                        .size(widthDp, heightDp)
                        .background(Color(0x2200E5FF)),
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
                            .graphicsLayer { rotationZ = rotationDegrees },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = activeQuote?.text ?: "Sample quote text renders here...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = previewFontFamily,
                            fontWeight = if (previewIsBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (previewIsItalic) FontStyle.Italic else FontStyle.Normal,
                            overflow = TextOverflow.Ellipsis
                        )
                        val currentQuote = activeQuote
                        if (currentQuote != null && !currentQuote.author.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "— ${currentQuote.author}",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = previewFontFamily,
                                fontWeight = if (previewIsBold) FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (previewIsItalic) FontStyle.Italic else FontStyle.Normal,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Use This Font Action Button
        Button(
            onClick = {
                dashboardViewModel.setQuoteFontId(previewFontId)
                dashboardViewModel.setIsBold(previewIsBold)
                dashboardViewModel.setIsItalic(previewIsItalic)
                onConfirmFont()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Use This Font Style", style = MaterialTheme.typography.titleMedium)
        }
    }
}
