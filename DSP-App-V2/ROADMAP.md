## AcouSense Android App — Build Roadmap

### Architectural Premise

Given your timeline, C# background, and thesis extensibility goal, the right call is **plain Android + Clean Architecture with strict layer boundaries from day one**. This is not a compromise — it's the same structure you'd use before a KMP migration. The rule is: **no Android SDK imports above the Data layer**. When thesis time comes, you lift the Domain and Data layers straight into a KMP shared module with minimal changes. The UI layer gets rewritten in Compose Multiplatform. Nothing is thrown away.

Think of it like a C# solution where you have a `.Core` project (no framework dependencies), a `.Infrastructure` project (network, DB), and a `.App` project (UI). Same idea here.

---

### Tech Stack Decisions

| Concern | Choice | Why |
|---|---|---|
| UI | Jetpack Compose | First-party, modern, no XML |
| Async | Kotlin Coroutines + Flow | Idiomatic; maps to C# async/await + IObservable |
| HTTP | Ktor Client | Swaps to KMP engine later with zero logic changes |
| Local DB | Room (not SQLDelight yet) | Faster to set up now; SQLDelight migration is mechanical for thesis |
| BLE | Android BluetoothLeScanner (wrapped behind an interface) | Direct control; interface means KMP expect/actual later |
| DI | Hilt | Industry standard for Android; Koin swap is trivial for KMP |
| State | ViewModel + StateFlow | Compose-native; identical mental model to KMP |
| Navigation | Navigation Compose | Single activity, screen-based nav |
| Charts | Vico (Compose-native) | Lighter than MPAndroidChart, Compose-first |

---

### Project Structure

```
app/
├── data/
│   ├── remote/          # Ktor client, ESP32 API, WebSocket
│   ├── local/           # Room DB, DAOs, entities
│   ├── ble/             # BLE scanner wrapper
│   └── repository/      # Repository implementations
├── domain/
│   ├── model/           # Pure Kotlin data classes (NoiseReading, Session, etc.)
│   ├── repository/      # Repository interfaces (no Android imports)
│   └── usecase/         # One class per use case
├── ui/
│   ├── dashboard/       # Live dBA screen
│   ├── insights/        # Charts + exposure analysis
│   ├── provisioning/    # Setup wizard
│   ├── settings/
│   └── theme/
└── di/                  # Hilt modules
```

The `domain/` package must stay pure Kotlin. Enforce this on yourself — if you find yourself importing `android.*` there, stop and move it to `data/`.

---

### Sequential Build Steps

#### Phase 0 — Project Bootstrap *(~half a day)*

1. Create a new Android project in Android Studio with **Empty Compose Activity**, min SDK 26 (Android 8.0 per SAD).
2. Add all Gradle dependencies in one go: Ktor, Room, Hilt, Vico, Navigation Compose, kotlinx-serialization, kotlinx-coroutines.
3. Set up the package structure above — create the empty folders and a `README.md` in each explaining what belongs there. This is discipline infrastructure, not wasted time.
4. Configure Hilt: annotate `Application`, add the Gradle plugin.
5. Commit. Every phase below ends with a commit.

---

#### Phase 1 — Domain Layer *(~half a day)*

Define your pure Kotlin models and repository interfaces. Nothing is implemented yet — you're drawing the contracts.

**Models to create** (`domain/model/`):
- `NoiseReading(id, recordedAt, dbaLevel, alertLevel, deviceId)`
- `PresenceSession(id, btUuid, checkedInAt, checkedOutAt, deviceId)`
- `DeviceStatus(dbaCurrent, modeId, alertLevel, uptimeSeconds)`
- `ExposureInsight(sessionAvgDba, peakDba, minutesAboveWarn, leq, trend)`
- `DeviceInfo(deviceId, roomName, fwVersion)`

**Repository interfaces to create** (`domain/repository/`):
- `ReadingsRepository` — `getReadings(from, to)`, `getLiveReading(): Flow<NoiseReading>`, `getSummary(from, to)`
- `PresenceRepository` — `getActiveSessions()`, `checkOut(btUuid)`
- `DeviceRepository` — `getStatus(): Flow<DeviceStatus>`, `getInfo()`
- `BleRepository` — `scanForDevices(): Flow<BleDevice>`, `getNearestDevice(): Flow<BleDevice?>`
- `ProvisioningRepository` — `sendWifiCredentials(ssid, pass)`, `sendRoomConfig(rssiThreshold, retention)`

**Use cases to create** (`domain/usecase/`):
- `GetLiveFeedUseCase` — wraps `ReadingsRepository.getLiveReading()`
- `GetHistoricalReadingsUseCase` — fetches + filters by time range
- `ComputeExposureInsightUseCase` — takes a list of readings, returns `ExposureInsight` (the LEq formula lives here)
- `DiscoverNearestDeviceUseCase` — wraps BLE scan, applies RSSI threshold logic
- `ManageRoomPresenceUseCase` — orchestrates check-in/check-out state machine
- `ProvisionDeviceUseCase` — orchestrates the provisioning wizard steps

The `ComputeExposureInsightUseCase` contains the LEq math: `LEq = 10 × log10((1/T) × Σ 10^(Li/10))`. Keep it here — pure Kotlin, fully unit-testable, zero Android dependency.

---

#### Phase 2 — Data Layer: REST + WebSocket *(~1 day)*

Implement the ESP32 API client using Ktor.

1. Create `ESP32ApiClient` in `data/remote/` with a base URL that takes the mDNS hostname or IP. Configure Ktor with the `ContentNegotiation` plugin using `kotlinx.serialization`.
2. Create `@Serializable` DTO classes mirroring the SAD API responses: `ReadingDto`, `StatusDto`, `PresenceDto`, `SummaryDto`, `DeviceInfoDto`.
3. Implement the WebSocket connection for `/api/v1/readings/live` — this should return a `Flow<NoiseReading>` that emits on every WS frame and closes cleanly when the coroutine scope is cancelled. This is your hottest path; get it right.
4. Implement `RemoteReadingsRepository` and `RemoteDeviceRepository` using the API client, mapping DTOs → domain models at the repository boundary. DTOs never leave the `data/` layer.
5. Implement `RemoteProvisioningRepository` for the setup endpoints.
6. Write a simple `EspDeviceDiscovery` helper that does mDNS resolution (use `NsdManager` on Android, wrapped behind an interface).

**At this point you can test everything against the real device** — wire up a temporary Compose screen with a button that fetches `/status` and displays the result. This is your integration smoke test.

---

#### Phase 3 — Data Layer: BLE *(~1 day)*

This is the trickiest part of the data layer.

1. Create `BlePresenceRepository` in `data/ble/` using `BluetoothLeScanner`. Wrap the callback-based Android BLE API in a `callbackFlow` so it emits `BleDevice(uuid, rssi, address)` objects as a `Flow`. This callback-flow pattern is the standard way to bridge legacy listeners to coroutines — it's the Kotlin equivalent of wrapping an event in a `Task<T>` in C#.
2. Implement the RSSI threshold + 3-consecutive-cycle debounce logic in `ManageRoomPresenceUseCase` (domain layer) — the BLE repo just emits raw scans, the use case applies the business rules.
3. Generate a persistent UUID v4 on first app launch and store it in `EncryptedSharedPreferences`. This UUID is what gets advertised in the BLE payload. The SAD's privacy model depends on this.
4. Implement BLE advertising so the ESP32 can detect the app. Use `BluetoothLeAdvertiser` with the GATT service UUID `0000ACOU-0000-1000-8000-00805F9B34FB` and embed the persistent UUID in the advertisement payload.
5. The `PresenceViewModel` should expose a `RoomPresenceState` sealed class: `Scanning`, `Connecting`, `InRoom(deviceInfo)`, `Departed`. This maps directly to the SAD Section 7.4 state machine.

---

#### Phase 4 — Data Layer: Local Cache *(~half a day)*

1. Set up Room with a single database class `AcouSenseDatabase`.
2. Create entities mirroring your domain models: `NoiseReadingEntity`, `PresenceSessionEntity`. Add a `last_synced_at` field to `NoiseReadingEntity`.
3. Create DAOs with the queries you need: `getByTimeRange(from, to)`, `insertAll(readings)`, `getLastSyncedAt()`.
4. Implement `CachedReadingsRepository` — a decorator over `RemoteReadingsRepository` that: fetches from remote, writes to Room, falls back to Room if remote throws. This is the offline-first pattern. The `ReadingsRepository` interface in domain sees none of this — it just gets readings.
5. On reconnection, query `getLastSyncedAt()` and fetch the gap from the ESP32 API.

---

#### Phase 5 — UI: Core Screens *(~2 days)*

Build screens in this order — each one exercises more of the stack.

**Screen 1: Device Not Found / Scanning**
- Simple animated scanning indicator
- Driven by `PresenceViewModel` in `Scanning` state
- Manual IP entry text field as fallback (stores in `DataStore`)

**Screen 2: Dashboard (Home)**
- Live dBA gauge — a large circular indicator that updates from the WebSocket Flow
- Current mode badge, alert status chip
- Today's session summary (avg, peak, time above warn)
- Driven by `DashboardViewModel` consuming `GetLiveFeedUseCase`

**Screen 3: Insights / History**
- Time range selector: 1h / 24h / 7d tabs
- Vico line chart rendering `NoiseReading` data
- WHO guideline reference lines drawn as horizontal annotations on the chart
- LEq and exposure insight cards below the chart
- Driven by `InsightsViewModel` consuming `GetHistoricalReadingsUseCase` + `ComputeExposureInsightUseCase`

**Screen 4: Tips & Recommendations**
- Static tip library (40+ tips as a sealed class or JSON asset bundled in the app)
- `TipGeneratorService` in domain selects ≤3 tips ranked by relevance to the last `ExposureInsight`
- Simple card list UI

**Screen 5: Settings**
- RSSI threshold slider (stored in `DataStore`, read by `ManageRoomPresenceUseCase`)
- Retention window preference
- App reset / clear cache

---

#### Phase 6 — Provisioning Wizard *(~half a day)*

This is a linear multi-step flow. Use a single `ProvisioningViewModel` with a `step: Int` state and a `ProvisioningWizardScreen` that renders different content per step.

1. **Step 1** — Scan for `AcouSense-Setup-[MAC]` Wi-Fi AP. Show found/not-found state.
2. **Step 2** — Enter premises Wi-Fi SSID + password. Call `POST /setup/wifi`.
3. **Step 3** — Walk the room perimeter. The app samples BLE RSSI every 2 seconds for 30 seconds, computes the min RSSI seen, and proposes that as the boundary. User can adjust with a slider.
4. **Step 4** — Send room config via `POST /setup/room`. Show success + redirect to Dashboard.

---

#### Phase 7 — Polish & Hardening *(~1 day)*

1. Add a `NetworkMonitor` (using `ConnectivityManager`) that triggers the offline banner and switches repositories to cache mode.
2. Add proper error states to every ViewModel — a `UiState` sealed class with `Loading`, `Success(data)`, `Error(message)` on every screen.
3. Add BLE + Location permission handling with rationale dialogs (BLE scan requires `ACCESS_FINE_LOCATION` on Android ≤11, `BLUETOOTH_SCAN` on Android 12+).
4. Add a `WorkManager` periodic task for the gap-sync on reconnection.
5. Implement the "Offline — showing cached data" banner in the Dashboard composable.

---

### Thesis Extensibility Notes (decisions to make now so you don't regret them later)

- **Keep all business logic in `domain/`** — this is the KMP migration. When the time comes, you create a `shared/` KMP module, copy `domain/` in unchanged, move `data/` models to `commonMain`, and swap Android-specific implementations with `expect/actual`.
- **Use Ktor now, not Retrofit** — Ktor is KMP-native. Retrofit is Android-only. Switching costs nothing now, saves a full rewrite later.
- **Interface every external dependency** — `BleRepository`, `ReadingsRepository`, etc. are interfaces in `domain/`. This means you can also mock them for unit tests and swap implementations for multi-room or cloud-sync features without touching the domain.
- **Use `DataStore` not `SharedPreferences`** — it's the current standard and has a KMP-compatible path.
- **Don't hardcode the ESP32 IP** — use the mDNS hostname as default, with IP override in settings. Multi-device support later just means a device selector screen feeding a different base URL to the same Ktor client.