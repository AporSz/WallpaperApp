package com.example.wallpaperapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.wallpaperapp.ui.dashboard.DashboardScreen
import com.example.wallpaperapp.ui.dashboard.DashboardViewModel
import com.example.wallpaperapp.ui.pools.PoolsScreen
import com.example.wallpaperapp.ui.pools.PoolsViewModel
import com.example.wallpaperapp.ui.zone.ZoneEditorScreen
import com.example.wallpaperapp.ui.zone.ZoneEditorViewModel

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
                rotateWallpaperUseCase = container.rotateWallpaperUseCase
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
                imageRepository = container.imageRepository
            )
        }

        setContent {
            MaterialTheme {
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
    val snackbarHostState = remember { SnackbarHostState() }

    val screens = listOf(
        Screen.Dashboard,
        Screen.ZoneEditor,
        Screen.Pools
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                screens.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        selected = selectedScreenIndex == index,
                        onClick = { selectedScreenIndex = index },
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedScreenIndex) {
                0 -> DashboardScreen(
                    viewModel = dashboardViewModel,
                    snackbarHostState = snackbarHostState
                )
                1 -> ZoneEditorScreen(
                    viewModel = zoneEditorViewModel
                )
                2 -> PoolsScreen(
                    viewModel = poolsViewModel
                )
            }
        }
    }
}
