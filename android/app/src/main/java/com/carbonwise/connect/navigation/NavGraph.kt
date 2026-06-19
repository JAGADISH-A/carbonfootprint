package com.carbonwise.connect.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.carbonwise.connect.data.local.SettingsStore
import com.carbonwise.connect.ui.permissions.PermissionsScreen
import com.carbonwise.connect.ui.settings.SettingsScreen
import com.carbonwise.connect.ui.status.StatusScreen
import com.carbonwise.connect.ui.welcome.WelcomeScreen
import kotlinx.coroutines.flow.first

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object Permissions : Screen("permissions")
    data object Status : Screen("status")
    data object Settings : Screen("settings")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    settingsStore: SettingsStore,
    startDestination: String = Screen.Welcome.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToPermissions = {
                    navController.navigate(Screen.Permissions.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Permissions.route) {
            PermissionsScreen(
                onComplete = {
                    navController.navigate(Screen.Status.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Status.route) {
            StatusScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onSignOut = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
