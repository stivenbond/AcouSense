# AcouSense Compatibility & Protocol Report

This report evaluates the current state of communication protocols across the **Arduino Nano** (Sensor Hub), **ESP32** (Gateway/Server), and the **DSP Android App** (V2 Roadmap).

---

## 1. Physical Layer & SPI Protocol (ESP32 ↔ Arduino Nano)
**Status: 🟢 FULLY COMPATIBLE**

The SPI communication between the ESP32 (Master) and Arduino Nano (Slave) is correctly implemented and synchronized.

| Parameter | ESP32 Implementation | Arduino Implementation | Consistency |
| :--- | :--- | :--- | :--- |
| **Bus/Pins** | HSPI (15, 14, 13, 12) | Standard SPI (10, 11, 12, 13) | **Match** |
| **Baud Rate** | 1.0 MHz | Hardware-Driven (Slave) | **Match** |
| **Frame Size** | 8 Bytes (Fixed) | 8 Bytes (Fixed) | **Match** |
| **Checksum** | XOR (Bytes 0-6 at Index 7) | XOR (Bytes 0-6 at Index 7) | **Match** |
| **Protocol IDs** | `0xCF`, `0xDA`, `0xBE` | `0xCF`, `0xDA`, `0xBE` | **Match** |

### Data Mapping (DATA Packet `0xDA`)
- `Payload[1-2]`: Average dB (Big-Endian)
- `Payload[3-4]`: Minimum dB (Big-Endian)
- `Payload[5-6]`: Maximum dB (Big-Endian)
Both sides use bit-shifting (`<< 8`) and masking (`0xFF`) consistently.

---

## 2. Network Layer (ESP32 ↔ DSP-App)
**Status: 🔴 CRITICAL MISMATCH (PROTOCOL & VERSIONING)**

There is a significant divergence between the REST API implemented on the ESP32 and the expectations of both the legacy App (V1) and the V2 Roadmap.

### A. Endpoint Mismatches
| Endpoint Purpose | ESP32 Implementation (`main.cpp`) | App Roadmap Expectation (`ROADMAP.md`) | Status |
| :--- | :--- | :--- | :--- |
| **Live Data** | `GET /api/noise` (Polling) | `WS /api/v1/readings/live` (WebSocket) | **❌ Missing WS** |
| **Historical Data** | `GET /api/noise?limit=...` | `GET /api/v1/readings/history` | **⚠️ Path Mismatch** |
| **Status/Info** | `GET /api/status` | `GET /status` or `GET /api/info` | **❌ Path Mismatch** |
| **WiFi Setup** | *Not Implemented* | `POST /setup/wifi` | **❌ Missing** |
| **Room Config** | `POST /api/config/apply` | `POST /setup/room` | **❌ Logic Mismatch** |

### B. Functional Gaps
1. **WebSocket Server**: The `ESPAsyncWebServer` in `main.cpp` lacks any WebSocket setup, while the App V2 expects a live stream of data frames.
2. **Setup Wizard**: The `setupWiFi()` function in the ESP32 code effectively handles boot-time connection but provides no API-driven way for the App to push new credentials (SSID/Password) to the ESP32.

---

## 3. Proximity Layer (BLE Presence Detection)
**Status: 🟡 PARTIALLY COMPATIBLE / LOGIC MISMATCH**

The ESP32 currently uses `NimBLE` to scan for generic MAC addresses, while the App Roadmap specifies a more secure, privacy-preserving UUID-based method.

- **Current ESP32 Logic**: Scans for any device, checks RSSI, and matches MAC against a hardcoded list.
- **V2 App Roadmap**: Advertises a specific GATT Service UUID (`0000ACOU-...`).
- **Conflict**: If the Android App uses randomized MAC addresses (standard on Android 10+), the ESP32 will fail to identify users because it is looking for MACs, not Service UUIDs.

---

## 4. Recommendations for Protocol Alignment

To resolve these issues, the following upgrades are necessary:

1. **ESP32 Firmware**:
   - Implement `AsyncWebSocket` handler at `/api/v1/readings/live`.
   - Add `/setup/wifi` and `/setup/room` endpoints to support the provisioning wizard.
   - Update the BLE scanner to look for the service UUID `0000ACOU-0000-1000-8000-00805F9B34FB` instead of just the MAC address.

2. **DSP-App**:
   - Synchronize the `RestApiService.kt` interface to use the correct ESP32 base URL and paths once the firmware is updated.

---
*Report Generated: 2026-04-07*
