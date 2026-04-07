package com.dspcontroller.ui.navigation

/**
 * Sealed class defining all navigation routes in the application.
 *
 * Each screen has a unique [route] string used by the NavHost.
 */
sealed class Screen(val route: String) {

    /** Device discovery and connection screen. */
    data object Discovery : Screen("discovery")

    /** Main dashboard showing telemetry overview and alerts. */
    data object Dashboard : Screen("dashboard")

    /** DSP parameter control screen with sliders. */
    data object Parameters : Screen("parameters")

    /** Preset management screen (load, save, sync, delete). */
    data object Presets : Screen("presets")

    /** Telemetry chart and statistics screen. */
    data object Telemetry : Screen("telemetry")

    /** Application settings screen. */
    data object Settings : Screen("settings")
}
