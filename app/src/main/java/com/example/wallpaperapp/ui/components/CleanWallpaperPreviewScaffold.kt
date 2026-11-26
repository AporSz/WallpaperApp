package com.example.wallpaperapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun CleanWallpaperPreviewScaffold(
    modifier: Modifier = Modifier,
    isCleanMode: Boolean? = null,
    onCleanModeChange: ((Boolean) -> Unit)? = null,
    onTap: (() -> Unit)? = null,
    topControls: @Composable (() -> Unit)? = null,
    bottomControls: @Composable (() -> Unit)? = null,
    previewContent: @Composable BoxWithConstraintsScope.(isClean: Boolean) -> Unit
) {
    var internalIsCleanMode by remember { mutableStateOf(false) }
    val effectiveIsClean = isCleanMode ?: internalIsCleanMode

    val context = LocalContext.current
    val displayMetrics = context.resources.displayMetrics
    val screenAspectRatio = displayMetrics.widthPixels.toFloat() / displayMetrics.heightPixels.toFloat().coerceAtLeast(1f)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Center Preview Container locked to Device Screen Aspect Ratio
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(screenAspectRatio),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                            val upOrCancel = withTimeoutOrNull(longPressTimeout) {
                                waitForUpOrCancellation()
                            }

                            if (upOrCancel == null) {
                                // Long-press threshold exceeded -> Activate Hold-to-Peek!
                                internalIsCleanMode = true
                                onCleanModeChange?.invoke(true)

                                // Keep peeking while finger stays pressed down
                                do {
                                    val event = awaitPointerEvent()
                                } while (event.changes.any { it.pressed })

                                // Release/Up -> Instantly restore UI elements!
                                internalIsCleanMode = false
                                onCleanModeChange?.invoke(false)
                            } else {
                                // Short Tap (<500ms)
                                if (effectiveIsClean) {
                                    internalIsCleanMode = false
                                    onCleanModeChange?.invoke(false)
                                } else {
                                    onTap?.invoke()
                                }
                            }
                        }
                    }
            ) {
                previewContent(effectiveIsClean)
            }
        }

        // Animated Top Controls Overlay
        if (topControls != null) {
            AnimatedVisibility(
                visible = !effectiveIsClean,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                topControls()
            }
        }

        // Animated Bottom Controls Overlay
        if (bottomControls != null) {
            AnimatedVisibility(
                visible = !effectiveIsClean,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                bottomControls()
            }
        }
    }
}
