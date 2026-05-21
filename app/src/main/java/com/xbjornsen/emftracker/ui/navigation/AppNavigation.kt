package com.xbjornsen.emftracker.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xbjornsen.emftracker.ui.screens.AboutScreen
import com.xbjornsen.emftracker.ui.screens.ChartScreen
import com.xbjornsen.emftracker.ui.screens.CompassScreen
import com.xbjornsen.emftracker.ui.screens.LiveScreen
import com.xbjornsen.emftracker.ui.screens.SessionsScreen
import com.xbjornsen.emftracker.viewmodel.MainViewModel

private sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Live     : Screen("live",     "Live",     Icons.Default.Home)
    object Compass  : Screen("compass",  "Compass",  Icons.Default.Explore)
    object Chart    : Screen("chart",    "Chart",    Icons.Default.ShowChart)
    object Sessions : Screen("sessions", "Sessions", Icons.Default.DateRange)
    object About    : Screen("about",    "About",    Icons.Default.Info)
}

private val screens = listOf(Screen.Live, Screen.Compass, Screen.Chart, Screen.Sessions, Screen.About)

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()

    Scaffold(
        // Top (status bar) inset applied to content; bottom handled by NavigationBar itself
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
        bottomBar = {
            NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController,
            startDestination = Screen.Live.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Live.route) {
                LiveScreen(viewModel)
            }
            composable(Screen.Compass.route) { CompassScreen(viewModel) }
            composable(Screen.Chart.route) { ChartScreen(viewModel) }
            composable(Screen.Sessions.route) { SessionsScreen(viewModel) }
            composable(Screen.About.route) { AboutScreen() }
        }
    }
}
