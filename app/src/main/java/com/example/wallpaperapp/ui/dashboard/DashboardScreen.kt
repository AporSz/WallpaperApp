package com.example.wallpaperapp.ui.dashboard

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TextButton
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
import androidx.core.content.ContextCompat
import com.example.wallpaperapp.domain.model.AppThemeMode
import com.example.wallpaperapp.domain.model.BackupScope
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
    val textGlowEnabled by viewModel.textGlowEnabledState.collectAsState()

    val showExportDialog by viewModel.showExportDialog.collectAsState()
    val showImportDialog by viewModel.showImportDialog.collectAsState()
    val exportScope by viewModel.exportScopeState.collectAsState()
    val importScope by viewModel.importScopeState.collectAsState()
    val isBackupInProgress by viewModel.isBackupInProgress.collectAsState()

    var isScheduleExpanded by remember { mutableStateOf(false) }
    var isFontDropdownExpanded by remember { mutableStateOf(false) }
    var isColorSchemeExpanded by remember { mutableStateOf(false) }
    var showPermissionSettingsDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            showPermissionSettingsDialog = true
        }
    }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importCustomFont(context, uri)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.exportLibrary(uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importLibrary(uri)
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

    fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }

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
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                checkAndRequestNotificationPermission()
                            }
                            viewModel.setScheduleEnabled(enabled)
                        }
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
                                        isScheduleExpanded = false
                                    }
                                )
                                Text(text = label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        // 3. Compact Dropdown Font Selector & Style Controls Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Font Selection & Style",
                    style = MaterialTheme.typography.titleMedium
                )

                // Material 3 Dropdown Menu for 12 Built-in + Custom Fonts
                ExposedDropdownMenuBox(
                    expanded = isFontDropdownExpanded,
                    onExpandedChange = { isFontDropdownExpanded = !isFontDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedFont.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Active Font Family") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFontDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
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

                // Style Controls Row (Bold, Italic, Shuffle Fonts, + Import)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                        label = { Text("Shuffle") }
                    )
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

                OutlinedButton(
                    onClick = onNavigateToFontPreview,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Full Font Preview Screen")
                }
            }
        }

        // 4. Collapsible Text Color Scheme Accordion Card
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
                            text = activeColorScheme.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (isColorSchemeExpanded) "▲" else "▼",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                AnimatedVisibility(visible = isColorSchemeExpanded) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextColorScheme.entries.forEach { scheme ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setTextColorScheme(scheme)
                                        isColorSchemeExpanded = false
                                    }
                            ) {
                                RadioButton(
                                    selected = activeColorScheme == scheme,
                                    onClick = {
                                        viewModel.setTextColorScheme(scheme)
                                        isColorSchemeExpanded = false
                                    }
                                )
                                Column {
                                    Text(text = scheme.displayName, style = MaterialTheme.typography.bodyMedium)
                                    Text(text = scheme.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setTextGlowEnabled(!textGlowEnabled) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Highlight Glow",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = textGlowEnabled,
                                onCheckedChange = { viewModel.setTextGlowEnabled(it) }
                            )
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

        // 6. Library Backup & Restore (Export & Import) Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Library Backup & Restore",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Export or import your quotes and wallpaper images as a portable zip backup archive.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.openExportDialog() },
                        modifier = Modifier.weight(1f),
                        enabled = !isBackupInProgress
                    ) {
                        Text("Export")
                    }

                    OutlinedButton(
                        onClick = { viewModel.openImportDialog() },
                        modifier = Modifier.weight(1f),
                        enabled = !isBackupInProgress
                    ) {
                        Text("Import")
                    }
                }

                if (isBackupInProgress) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Processing backup...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // 7. Manual Shuffle & Apply Now
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

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissExportDialog() },
            title = { Text("Export Library Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select the data scope you want to export into a zip file:")
                    BackupScope.entries.forEach { scope ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setExportScope(scope) }
                        ) {
                            RadioButton(
                                selected = exportScope == scope,
                                onClick = { viewModel.setExportScope(scope) }
                            )
                            Text(text = scope.displayName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissExportDialog()
                        val fileName = "wallpaper_backup_${System.currentTimeMillis()}.zip"
                        exportLauncher.launch(fileName)
                    }
                ) {
                    Text("Choose Destination & Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissExportDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportDialog() },
            title = { Text("Import Library Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select the data scope you want to restore from a backup zip file:")
                    BackupScope.entries.forEach { scope ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setImportScope(scope) }
                        ) {
                            RadioButton(
                                selected = importScope == scope,
                                onClick = { viewModel.setImportScope(scope) }
                            )
                            Text(text = scope.displayName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissImportDialog()
                        importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                    }
                ) {
                    Text("Select Backup File")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissImportDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPermissionSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionSettingsDialog = false },
            title = { Text("Permission Required") },
            text = { Text("Notification and storage permissions were denied. Please open App Settings to grant permissions for automated wallpaper updates.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionSettingsDialog = false
                        launchAppSettings(context)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun launchAppSettings(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("DashboardScreen", "Failed to launch app settings", e)
    }
}
