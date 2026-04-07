# AcouSense — Schematic & Wiring Guide
## Build Order (Sequential Steps)

---

## STEP 1 — Power Rails (do this first, verify before connecting anything)

### Arduino Nano (USB from laptop)
- USB-A/Mini-B → Laptop
- Nano provides: 5V on VIN/5V pin, 3.3V on 3V3 pin, GND

### ESP32 (USB from laptop, separate cable)
- USB micro/C → Laptop (second port)
- ESP32 provides: 3.3V on 3V3 pin, GND
- ⚠️ ESP32 GPIO pins are 3.3V logic — never connect directly to 5V signals

---

## STEP 2 — KY-038 Microphone Module → Arduino Nano

| KY-038 Pin | Arduino Nano Pin | Notes                              |
|------------|------------------|------------------------------------|
| VCC        | 5V               | Module runs on 5V                  |
| GND        | GND              | Common ground                      |
| AO         | A0               | Analog output — raw sound level    |
| DO         | NOT CONNECTED    | Digital out not used               |

**Add:** 100nF ceramic capacitor between KY-038 VCC and GND (place close to module)
— this decouples supply noise from affecting mic readings.

---

## STEP 3 — I2C LCD (16x2 + PCF8574 backpack) → Arduino Nano

| LCD Backpack Pin | Arduino Nano Pin | Notes                            |
|------------------|------------------|----------------------------------|
| VCC              | 5V               |                                  |
| GND              | GND              |                                  |
| SDA              | A4               | I2C Data                         |
| SCL              | A5               | I2C Clock                        |

**I2C Address:** Try 0x27 first. If LCD does not initialize, try 0x3F.
You can scan with an I2C scanner sketch to confirm.

**Add:** 4.7kΩ pull-up resistors on SDA and SCL lines to 5V
(Many backpacks have these built in — check your module. If it does, skip this.)

---

## STEP 4 — Buzzer → Arduino Nano

| Buzzer Pin | Arduino Nano Pin | Notes                             |
|------------|------------------|-----------------------------------|
| + (VCC)    | D9               | PWM pin — drives tone() directly  |
| - (GND)    | GND              |                                   |

**Note:** If your buzzer is passive (2 pins, no integrated oscillator), D9 with tone()
works perfectly. If it is active (self-oscillating), connect + to D9 and it will just
turn on/off without pitch control — still functional for alerts.

---

## STEP 5 — SPI Bus: ESP32 (Master/HSPI) → Arduino Nano (Slave)

⚠️ VOLTAGE LEVEL WARNING:
- ESP32 outputs 3.3V logic
- Arduino Nano SPI inputs expect 5V logic
- Nano MISO output is 5V → will damage ESP32 input

You MUST use a logic level shifter (bidirectional, e.g. BSS138-based module)
on ALL four SPI lines. A cheap 4-channel bidirectional logic level shifter
(common on AliExpress/Amazon) handles all lines at once.

### Wiring through logic level shifter (LLS):

#### ESP32 side (3.3V, HSPI bus):
| ESP32 Pin | LLS HV Side | Notes                        |
|-----------|-------------|------------------------------|
| GPIO14    | HV-SCK      | HSPI Clock                   |
| GPIO13    | HV-MOSI     | HSPI MOSI (ESP→Nano)         |
| GPIO12    | HV-MISO     | HSPI MISO (Nano→ESP)         |
| GPIO15    | HV-CS       | Chip Select (active LOW)     |
| 3V3       | HV-VCC      | 3.3V reference for LLS       |
| GND       | HV-GND      |                              |

#### Arduino Nano side (5V):
| LLS LV Side | Arduino Nano Pin | Notes                    |
|-------------|------------------|--------------------------|
| LV-SCK      | D13 (SCK)        | SPI Clock                |
| LV-MOSI     | D11 (MOSI)       | SPI MOSI in              |
| LV-MISO     | D12 (MISO)       | SPI MISO out             |
| LV-CS       | D10 (SS)         | Slave select             |
| LV-VCC      | 5V               | 5V reference for LLS     |
| LV-GND      | GND              |                          |

**Common ground:** Connect Nano GND and ESP32 GND together.
This is essential for SPI to work correctly across the two USB power supplies.

---

## STEP 6 — SD Card Module → ESP32 (VSPI bus)

The SD card module runs on 3.3V logic — connects directly to ESP32, no shifter needed.

| SD Module Pin | ESP32 Pin | Notes                             |
|---------------|-----------|-----------------------------------|
| VCC           | 3V3       | SD module VCC (check: some need 5V→use 3V3 regulator on module) |
| GND           | GND       |                                   |
| CS            | GPIO5     | VSPI Chip Select                  |
| SCK           | GPIO18    | VSPI Clock                        |
| MOSI          | GPIO23    | VSPI MOSI                         |
| MISO          | GPIO19    | VSPI MISO                         |

**SD Card format:** FAT32, max 32GB. Create these folders on the card:
```
/www/          ← WebUI static files (index.html, app.js, styles.css)
/acousense.db  ← SQLite DB (auto-created by firmware)
```

**Add:** 100nF decoupling cap on SD module VCC/GND.
The SD card has large current spikes during writes that can cause ESP32 resets
without decoupling.

---

## STEP 7 — Final Connections Checklist

### Common Ground Network
Connect these GND pins together with a wire (star topology or daisy chain):
- Arduino Nano GND
- ESP32 GND
- KY-038 GND
- LCD GND
- SD Module GND
- Logic Level Shifter GND (both sides)
- Buzzer GND

### Pull-down on ESP32 GPIO15 (NANO_CS)
GPIO15 on ESP32 has an internal pull-up that affects boot mode.
Add a 10kΩ resistor from GPIO15 to GND.
This ensures ESP32 boots correctly even when the SPI bus is connected.

### ESP32 EN/Boot pins
Leave EN floating (has internal pull-up). GPIO0 should be floating (not connected
to GND) for normal operation — only hold LOW for flashing if needed.

---

## Component Summary

| Component              | Qty | Connection Target       |
|------------------------|-----|-------------------------|
| Arduino Nano           | 1   | USB (laptop)            |
| ESP32-D                | 1   | USB (laptop)            |
| KY-038 Mic Module      | 1   | Nano A0                 |
| I2C LCD 16x2           | 1   | Nano A4/A5              |
| Passive Buzzer         | 1   | Nano D9                 |
| SD Card Module         | 1   | ESP32 VSPI              |
| Logic Level Shifter 4ch| 1   | SPI bus bridge          |
| 100nF ceramic caps     | 2   | KY-038 VCC, SD VCC      |
| 10kΩ resistor          | 1   | ESP32 GPIO15 pull-down  |
| 4.7kΩ resistors        | 2   | I2C SDA/SCL (if needed) |

---

## Libraries to Install (Arduino IDE / PlatformIO)

### For Arduino Nano:
- `LiquidCrystal_I2C` by Frank de Brabander (Library Manager)

### For ESP32:
- `ArduinoJson` by Benoit Blanchon
- `ESPAsyncWebServer` by ESP Async Web Server (GitHub: me-no-dev)
- `AsyncTCP` by dvarrel (dependency of ESPAsyncWebServer)
- `NimBLE-Arduino` by h2zero
- `sqlite3_esp32` by siara-cc (GitHub — manual install)
- `SD` — built into ESP32 Arduino core

---

## Verification Order (Power-On Sequence)

1. Flash Arduino Nano firmware first (USB). Verify serial output shows "Ready."
2. Flash ESP32 firmware (USB). Verify serial shows DB ready + WiFi AP or connected.
3. Open Serial Monitor on both (115200 baud) to watch SPI handshake.
4. Connect to "AcouSense-Setup" WiFi (if no creds stored) → navigate to 192.168.4.1
5. Verify /api/status returns JSON.
6. Make noise near KY-038 — verify /api/noise starts populating after 10s.
