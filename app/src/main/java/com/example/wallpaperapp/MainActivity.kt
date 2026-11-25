package com.example.wallpaperapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wallpaperapp.domain.model.AppThemeMode
import com.example.wallpaperapp.ui.color.ColorSchemePreviewScreen
import com.example.wallpaperapp.ui.dashboard.DashboardScreen
import com.example.wallpaperapp.ui.dashboard.DashboardViewModel
import com.example.wallpaperapp.ui.font.FontPreviewScreen
import com.example.wallpaperapp.ui.pools.PoolsScreen
import com.example.wallpaperapp.ui.pools.PoolsViewModel
import com.example.wallpaperapp.ui.settings.SettingsScreen
import com.example.wallpaperapp.ui.zone.ZoneEditorScreen
import com.example.wallpaperapp.ui.zone.ZoneEditorViewModel
import kotlinx.coroutines.delay

sealed class Screen(val title: String, val icon: ImageVector) {
    object Dashboard : Screen("Dashboard", Icons.Default.Home)
    object ZoneEditor : Screen("Zone Editor", Icons.Default.Edit)
    object Pools : Screen("Pools", Icons.AutoMirrored.Filled.List)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as WallpaperApplication
        val container = app.container

        val dashboardViewModel: DashboardViewModel by viewModels {
            DashboardViewModel.Factory(
                settingsRepository = container.settingsRepository,
                wallpaperScheduler = container.wallpaperScheduler,
                rotateWallpaperUseCase = container.rotateWallpaperUseCase,
                libraryBackupRepository = container.libraryBackupRepository
            )
        }

        val zoneEditorViewModel: ZoneEditorViewModel by viewModels {
            ZoneEditorViewModel.Factory(
                settingsRepository = container.settingsRepository,
                quoteRepository = container.quoteRepository,
                imageRepository = container.imageRepository
            )
        }

        val poolsViewModel: PoolsViewModel by viewModels {
            PoolsViewModel.Factory(
                quoteRepository = container.quoteRepository,
                imageRepository = container.imageRepository,
                settingsRepository = container.settingsRepository
            )
        }

        setContent {
            val themeMode by dashboardViewModel.themeModeState.collectAsStateWithLifecycle()
            val systemInDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> systemInDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            val colorScheme = if (useDarkTheme) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme = colorScheme) {
                MainAppScreen(
                    dashboardViewModel = dashboardViewModel,
                    zoneEditorViewModel = zoneEditorViewModel,
                    poolsViewModel = poolsViewModel
                )
            }
        }
    }
}

@Composable
fun MainAppScreen(
    dashboardViewModel: DashboardViewModel,
    zoneEditorViewModel: ZoneEditorViewModel,
    poolsViewModel: PoolsViewModel
) {
    var selectedScreenIndex by remember { mutableIntStateOf(0) }
    val backStack = remember { mutableStateListOf<Int>(0) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showFontPreviewScreen by remember { mutableStateOf(false) }
    var showColorSchemePreviewScreen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    var isBottomNavVisible by remember { mutableStateOf(true) }

    LaunchedEffect(selectedScreenIndex) {
        if (selectedScreenIndex == 1) {
            delay(1500)
            isBottomNavVisible = false
        } else {
            isBottomNavVisible = true
        }
    }

    val screens = listOf(
        Screen.Dashboard,
        Screen.ZoneEditor,
        Screen.Pools
    )

    BackHandler(
        enabled = backStack.size > 1 && !showFontPreviewScreen && !showColorSchemePreviewScreen && !showSettingsScreen
    ) {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            selectedScreenIndex = backStack.last()
        }
    }

    if (showSettingsScreen) {
        SettingsScreen(
            viewModel = dashboardViewModel,
            onNavigateBack = { showSettingsScreen = false }
        )
    } else if (showFontPreviewScreen) {
        FontPreviewScreen(
            dashboardViewModel = dashboardViewModel,
            zoneEditorViewModel = zoneEditorViewModel,
            onConfirmFont = { showFontPreviewScreen = false }
        )
    } else if (showColorSchemePreviewScreen) {
        ColorSchemePreviewScreen(
            dashboardViewModel = dashboardViewModel,
            zoneEditorViewModel = zoneEditorViewModel,
            onConfirmScheme = { showColorSchemePreviewScreen = false }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                AnimatedVisibility(
                    visible = isBottomNavVisible,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    NavigationBar {
                        screens.forEachIndexed { index, screen ->
                            NavigationBarItem(
                                selected = selectedScreenIndex == index,
                                onClick = {
                                    if (selectedScreenIndex != index) {
                                        selectedScreenIndex = index
                                        if (backStack.lastOrNull() != index) {
                                            backStack.add(index)
                                        }
                                    }
                                },
                                icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            val contentPadding = if (selectedScreenIndex == 1 && !isBottomNavVisible) {
                PaddingValues(
                    start = innerPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    top = innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    bottom = 0.dp
                )
            } else {
                innerPadding
            }

            Box(modifier = Modifier.padding(contentPadding)) {
                when (selectedScreenIndex) {
                    0 -> DashboardScreen(
                        viewModel = dashboardViewModel,
                        snackbarHostState = snackbarHostState,
                        onNavigateToFontPreview = { showFontPreviewScreen = true },
                        onNavigateToColorPreview = { showColorSchemePreviewScreen = true },
                        onNavigateToSettings = { showSettingsScreen = true }
                    )
                    1 -> ZoneEditorScreen(
                        viewModel = zoneEditorViewModel,
                        onToggleBottomNav = { isBottomNavVisible = !isBottomNavVisible }
                    )
                    2 -> PoolsScreen(
                        viewModel = poolsViewModel
                    )
                }
            }
        }
    }
}
