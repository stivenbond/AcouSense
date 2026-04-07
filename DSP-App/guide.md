# DSP Controller — User Guide

Welcome to the **DSP Controller** Android app project. This multi-module application is built natively with Kotlin and Jetpack Compose to interface with ESP32-based DSP hardware over a local Wi-Fi network.

This guide will walk you through setting up your development environment, building the project, and running the app.

---

## 1. Prerequisites

Before you begin, ensure you have the following installed on your machine:

- **Android Studio** (Koala Feature Drop | 2024.1.2 or newer recommended)
- **Java Development Kit (JDK) 17** (Required for Gradle 8.5+ and Kotlin 1.9.24)
- **Android SDK** (API 34 / Android 14)

---

## 2. Opening the Project

1. Launch **Android Studio**.
2. Click on **Open**.
3. Navigate to the `d:\DSP-App` directory and select it.
4. Click **OK**.

Android Studio will automatically detect the `settings.gradle.kts` and begin indexing the project.

---

## 3. Syncing Gradle

Because this is a multi-module project (comprising `app`, `data`, `domain`, and `network` modules), Gradle needs to download the dependencies defined in `gradle/libs.versions.toml`.

1. Android Studio usually triggers a sync automatically upon opening.
2. If it doesn't, or if you see an "Unsynced" warning at the top of the editor, click the **"Sync Project with Gradle Files"** icon in the top toolbar (the elephant icon with a blue arrow).
3. Wait for the build output window at the bottom to say **"BUILD SUCCESSFUL"**. This may take a few minutes the first time as it downloads dependencies like Jetpack Compose, Hilt, Room, and OkHttp.

---

## 4. Running the App

You can run the app on either a physical Android device or an Android Emulator.

### Running on a Physical Device
1. Enable **Developer Options** and **USB Debugging** on your Android device (Settings > About phone > Tap "Build number" 7 times).
2. Connect the device to your computer via USB (or pair it via Wi-Fi debugging).
3. In Android Studio, ensure your device is selected in the device dropdown menu in the top toolbar.
4. Click the **Run** button (the green play triangle) or press `Shift + F10`.

### Running on an Emulator
1. Open the **Device Manager** in Android Studio (Tools > Device Manager).
2. Click **Create Device** and create an emulator with at least **API level 24** (Android 7.0). API 30+ is recommended for the best Material 3 rendering.
3. Start the emulator.
4. Select it from the device dropdown and click **Run**.

---

## 5. Using the App

### Device Discovery (mDNS)
When the app launches, it opens the **Discovery Screen**. 
- The app uses `NsdManager` to scan the local Wi-Fi network for devices broadcasting the `_dsp._tcp` service.
- If your ESP32 is powered on, connected to the same Wi-Fi network, and advertising its existence, it will appear in the "Discovered Devices" list automatically.
- Tap the device in the list to connect.

### Manual Connection
If mDNS fails (which can happen on certain restrictive routers), you can manually enter the ESP32's IP address in the "Manual Connection" card and tap the connect (link) button.

### The Dashboard
Once connected, the app opens a persistent WebSocket connection to the DSP.
- **Telemetry**: You will see real-time updates for CPU usage, Heap memory, and Audio Signal RMS streaming in at ~4Hz.
- **Alerts**: If any metric crosses defined safety thresholds, a red alert banner will dynamically appear.

### DSP Parameters
Navigate to **Parameters** to adjust audio settings.
- The app automatically requests a full dump of the current DSP state upon opening this screen.
- Move any slider to change a value. The changes are immediately transmitted over the WebSocket (debounced by 150ms to prevent flooding the hardware).
- Watch for the green checkmark (✓) in the top right, indicating the hardware Acknowledged (ACK) your command.

### Presets
Navigate to **Presets** to save the current configuration or switch between saved states.
- Tap **Save Current** to take a snapshot of your current sliders and save it locally in the Room database.
- Tap the **Sync (Cloud)** floating action button to pull down presets stored on the ESP32 flash memory and sync them with your local database.

### Telemetry Charts
Navigate to **Telemetry** to view historical graphs of the DSP's health.
- The charts use `MPAndroidChart` to plot CPU, Heap, and RMS data over the last 10, 30, 60, or 120 seconds.
- You can pinch to zoom and drag to pan across the graphs.

---

## 6. Troubleshooting

- **WebSocket Fails to Connect**: Ensure your Android phone/emulator is on the *exact same Wi-Fi subnet* as the ESP32. On Android emulators, bridge adapter settings sometimes prevent local network routing.
- **"Unresolved Reference" Errors in Android Studio**: This usually means Hilt hasn't generated its dependency injection code yet. Simply go to **Build > Rebuild Project**.
- **mDNS Not Finding Device**: Make sure the ESP32 is correctly configured to broadcast `_dsp._tcp`. You can test this using third-party apps like "Service Browser" on Android or "Discovery" on iOS/macOS.
