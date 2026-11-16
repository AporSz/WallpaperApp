package com.example.wallpaperapp.ui.dashboard

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wallpaperapp.domain.model.AppThemeMode
import com.example.wallpaperapp.domain.model.WallpaperTarget

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    snackbarHostState: SnackbarHostState
) {
    val target by viewModel.targetState.collectAsState()
    val schedule by viewModel.scheduleState.collectAsState()
    val themeMode by viewModel.themeModeState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.message.collect { msg ->
            snackbarHostState.showSnackbar(msg)
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

        // 2. App Theme Mode
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
                        label = { Text("System Default") }
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

        // 3. Schedule Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Automated Rotation",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (schedule.isEnabled) "Scheduled background updates active" else "Disabled",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = schedule.isEnabled,
                        onCheckedChange = { viewModel.setScheduleEnabled(it) }
                    )
                }

                if (schedule.isEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Update Interval", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    val intervals = listOf(
                        15L to "15 Mins",
                        60L to "1 Hour",
                        360L to "6 Hours",
                        1440L to "Daily"
                    )

                    Column {
                        intervals.forEach { (mins, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = schedule.intervalMinutes == mins,
                                    onClick = { viewModel.setScheduleInterval(mins) }
                                )
                                Text(text = label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        // 4. Manual Shuffle & Apply Now
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
