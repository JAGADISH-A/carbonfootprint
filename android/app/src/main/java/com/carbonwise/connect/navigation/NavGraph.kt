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
import com.carbonwise.connect.ui.status.CompanionStatusScreen
import kotlinx.coroutines.flow.first

sealed class Screen(val route: String) {
    data object Pairing : Screen("pairing")
    data object Permissions : Screen("permissions")
    data object CompanionStatus : Screen("companion_status")
    data object Settings : Screen("settings")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    settingsStore: SettingsStore,
    startDestination: String = Screen.Pairing.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        composable(Screen.Pairing.route) {
            com.carbonwise.connect.ui.pairing.PairDeviceScreen(
                onPairedSuccessfully = {
                    navController.navigate(Screen.CompanionStatus.route + "?justPaired=true") {
                        popUpTo(Screen.Pairing.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Permissions.route) {
            PermissionsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CompanionStatus.route + "?justPaired={justPaired}",
            arguments = listOf(
                androidx.navigation.navArgument("justPaired") {
                    type = androidx.navigation.NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val justPaired = backStackEntry.arguments?.getBoolean("justPaired") ?: false
            CompanionStatusScreen(
                justPaired = justPaired,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onUnpaired = {
                    navController.navigate(Screen.Pairing.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onSignOut = {
                    navController.navigate(Screen.Pairing.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
