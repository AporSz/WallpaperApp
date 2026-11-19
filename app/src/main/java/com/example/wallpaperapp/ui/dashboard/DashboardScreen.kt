package com.example.wallpaperapp.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.wallpaperapp.domain.model.AppThemeMode
import com.example.wallpaperapp.domain.model.QuoteFont
import com.example.wallpaperapp.domain.model.TextColorScheme
import com.example.wallpaperapp.domain.model.WallpaperTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    snackbarHostState: SnackbarHostState,
    onNavigateToFontPreview: () -> Unit = {},
    onNavigateToColorPreview: () -> Unit = {}
) {
    val context = LocalContext.current

    val target by viewModel.targetState.collectAsState()
    val schedule by viewModel.scheduleState.collectAsState()
    val themeMode by viewModel.themeModeState.collectAsState()
    val activeFontId by viewModel.quoteFontId.collectAsState()
    val isBold by viewModel.isBoldState.collectAsState()
    val isItalic by viewModel.isItalicState.collectAsState()
    val shuffleFonts by viewModel.shuffleFontsState.collectAsState()
    val activeColorScheme by viewModel.textColorSchemeState.collectAsState()

    var isScheduleExpanded by remember { mutableStateOf(false) }
    var isFontDropdownExpanded by remember { mutableStateOf(false) }
    var isColorSchemeExpanded by remember { mutableStateOf(false) }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importCustomFont(context, uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.message.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val intervals = remember {
        listOf(
            15L to "15 Mins",
            30L to "30 Mins",
            60L to "1 Hour",
            120L to "2 Hours",
            180L to "3 Hours",
            240L to "4 Hours",
            360L to "6 Hours",
            480L to "8 Hours",
            720L to "12 Hours",
            1440L to "Daily (24 Hours)"
        )
    }

    val activeIntervalLabel = remember(schedule.intervalMinutes) {
        intervals.find { it.first == schedule.intervalMinutes }?.second ?: "${schedule.intervalMinutes} Mins"
    }

    val selectedFont = remember(activeFontId) { QuoteFont.findById(activeFontId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Wallpaper Dashboard",
            style = MaterialTheme.typography.headlineMedium
        )

        // 1. Target Screen Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Target Screen",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = target == WallpaperTarget.HOME_SCREEN,
                        onClick = { viewModel.setWallpaperTarget(WallpaperTarget.HOME_SCREEN) },
                        label = { Text("Home Only") }
                    )
                    FilterChip(
                        selected = target == WallpaperTarget.LOCK_SCREEN,
                        onClick = { viewModel.setWallpaperTarget(WallpaperTarget.LOCK_SCREEN) },
                        label = { Text("Lock Only") }
                    )
                    FilterChip(
                        selected = target == WallpaperTarget.BOTH,
                        onClick = { viewModel.setWallpaperTarget(WallpaperTarget.BOTH) },
                        label = { Text("Both") }
                    )
                }
            }
        }

        // 2. Compact / Collapsible Automated Rotation Accordion Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isScheduleExpanded = !isScheduleExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Automated Rotation",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (schedule.isEnabled) "Every $activeIntervalLabel (Tap to change)" else "Disabled (Tap to configure)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = schedule.isEnabled,
                        onCheckedChange = { viewModel.setScheduleEnabled(it) }
                    )
                }

                AnimatedVisibility(visible = schedule.isEnabled && isScheduleExpanded) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(text = "Select Update Interval", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(6.dp))

                        intervals.forEach { (mins, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setScheduleInterval(mins)
                                        isScheduleExpanded = false // Auto-collapse upon selection!
                                    }
                            ) {
                                RadioButton(
                                    selected = schedule.intervalMinutes == mins,
                                    onClick = {
                                        viewModel.setScheduleInterval(mins)
                                        isScheduleExpanded = false // Auto-collapse upon selection!
                                    }
                                )
                                Text(text = label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        // 3. Compact Dropdown Typography & Styling Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Quote Typography & Styling",
                    style = MaterialTheme.typography.titleMedium
                )

                // Compact Material 3 Exposed Dropdown Menu Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = isFontDropdownExpanded,
                        onExpandedChange = { isFontDropdownExpanded = !isFontDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedFont.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Active Font Family") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFontDropdownExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = isFontDropdownExpanded,
                            onDismissRequest = { isFontDropdownExpanded = false }
                        ) {
                            QuoteFont.BUILTIN_FONTS.forEach { font ->
                                DropdownMenuItem(
                                    text = { Text(font.name) },
                                    onClick = {
                                        viewModel.setQuoteFontId(font.id)
                                        isFontDropdownExpanded = false
                                    }
                                )
                            }
                        }
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

                // Bold, Italic & Shuffle Fonts Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = isBold,
                        onClick = { viewModel.setIsBold(!isBold) },
                        label = { Text("Bold") }
                    )
                    FilterChip(
                        selected = isItalic,
                        onClick = { viewModel.setIsItalic(!isItalic) },
                        label = { Text("Italic") }
                    )
                    FilterChip(
                        selected = shuffleFonts,
                        onClick = { viewModel.setShuffleFonts(!shuffleFonts) },
                        label = { Text("Shuffle Fonts") }
                    )
                }

                // Preview Fonts Full-Aspect Button
                OutlinedButton(
                    onClick = onNavigateToFontPreview,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Full Font Preview Screen")
                }
            }
        }

        // 4. Text Color Scheme Accordion Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isColorSchemeExpanded = !isColorSchemeExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Text Color Scheme",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${activeColorScheme.displayName} (Tap to change)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(visible = isColorSchemeExpanded) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "Select Color Strategy", style = MaterialTheme.typography.labelLarge)

                        TextColorScheme.entries.forEach { scheme ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setTextColorScheme(scheme)
                                        isColorSchemeExpanded = false // Auto-collapse upon selection!
                                    }
                            ) {
                                RadioButton(
                                    selected = activeColorScheme == scheme,
                                    onClick = {
                                        viewModel.setTextColorScheme(scheme)
                                        isColorSchemeExpanded = false // Auto-collapse upon selection!
                                    }
                                )
                                Column {
                                    Text(text = scheme.displayName, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = scheme.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedButton(
                            onClick = onNavigateToColorPreview,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Full Color Preview Screen")
                        }
                    }
                }
            }
        }

        // 5. App Theme Mode
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "App Theme Mode",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = themeMode == AppThemeMode.SYSTEM,
                        onClick = { viewModel.setAppThemeMode(AppThemeMode.SYSTEM) },
                        label = { Text("System") }
                    )
                    FilterChip(
                        selected = themeMode == AppThemeMode.LIGHT,
                        onClick = { viewModel.setAppThemeMode(AppThemeMode.LIGHT) },
                        label = { Text("Light") }
                    )
                    FilterChip(
                        selected = themeMode == AppThemeMode.DARK,
                        onClick = { viewModel.setAppThemeMode(AppThemeMode.DARK) },
                        label = { Text("Dark") }
                    )
                }
            }
        }

        // 6. Manual Shuffle & Apply Now
        Button(
            onClick = { viewModel.rotateNow() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Shuffle & Apply Now",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
