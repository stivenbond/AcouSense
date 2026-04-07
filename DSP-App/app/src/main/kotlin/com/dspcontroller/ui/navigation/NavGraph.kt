package com.dspcontroller.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dspcontroller.ui.dashboard.DashboardScreen
import com.dspcontroller.ui.discovery.DeviceDiscoveryScreen
import com.dspcontroller.ui.parameter.ParameterScreen
import com.dspcontroller.ui.preset.PresetScreen
import com.dspcontroller.ui.settings.SettingsScreen
import com.dspcontroller.ui.telemetry.TelemetryScreen

/**
 * Top-level navigation graph for the DSP Controller application.
 *
 * Starts at the [Screen.Discovery] screen and navigates forward
 * through the device control flow.
 */
@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Discovery.route
    ) {
        composable(route = Screen.Discovery.route) {
            DeviceDiscoveryScreen(
                onDeviceConnected = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Discovery.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToParameters = { navController.navigate(Screen.Parameters.route) },
                onNavigateToPresets = { navController.navigate(Screen.Presets.route) },
                onNavigateToTelemetry = { navController.navigate(Screen.Telemetry.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onDisconnect = {
                    navController.navigate(Screen.Discovery.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Parameters.route) {
            ParameterScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Presets.route) {
            PresetScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Telemetry.route) {
            TelemetryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
