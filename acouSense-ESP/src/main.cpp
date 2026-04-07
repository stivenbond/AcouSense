/**
 * AcouSense - ESP32 Firmware
 * Role: SPI Master, IoT Gateway, BLE Scanner, Web Server, DB Manager
 *
 * Responsibilities:
 *  - SPI Master on VSPI → talks to Arduino Nano (slave)
 *  - SPI Master on HSPI → talks to SD card module
 *  - Periodic poll of Arduino every 10s → receive aggregated noise data
 *  - Push config changes to Arduino on demand
 *  - Store noise records in SQLite DB on SD card
 *  - Serve WebUI (static files from SD card) via HTTP
 *  - BLE advertising + scanning for user presence detection
 * (check-in/check-out)
 *  - Admin config panel via WebUI with auth + last-5 config rollback
 *
 * SPI Bus Assignment:
 *  VSPI (default):  SD Card   — SS=5,  SCK=18, MOSI=23, MISO=19
 *  HSPI (manual):   Arduino   — SS=15, SCK=14, MOSI=13, MISO=12
 *
 * Libraries required (install via Arduino Library Manager or platformio.ini):
 *  - ESP32 Arduino Core
 *  - ArduinoJson (bblanchon)
 *  - ESPAsyncWebServer + AsyncTCP
 *  - sqlite3 (siara-cc/sqlite3_esp32)
 *  - SD (built-in ESP32 core)
 *  - NimBLE-Arduino (for BLE — lighter than built-in BLE)
 */

#include "sqlite3.h"
#include <Arduino.h>
#include <ArduinoJson.h>
#include <ESPAsyncWebServer.h>
#include <NimBLEDevice.h>
#include <Preferences.h> // NVS — store WiFi creds, admin password
#include <SD.h>
#include <SPI.h>
#include <WiFi.h>

#include <map>


// ─── Pin Definitions
// ────────────────────────────────────────────────────────── VSPI — SD Card
#define SD_CS 5
#define SD_SCK 18
#define SD_MOSI 23
#define SD_MISO 19

// HSPI — Arduino Nano SPI Slave
#define NANO_CS 15
#define NANO_SCK 14
#define NANO_MOSI 13
#define NANO_MISO 12

// ─── Packet Protocol (must match Arduino firmware) ───────────────────────────
#define PKT_CONFIG 0xCF
#define PKT_DATA 0xDA
#define PKT_HEARTBEAT 0xBE
#define FRAME_SIZE 8

// ─── Timing
// ───────────────────────────────────────────────────────────────────
#define NANO_POLL_INTERVAL_MS 10000 // poll Arduino every 10s
#define BLE_SCAN_INTERVAL_MS 15000  // BLE scan every 15s
#define BLE_SCAN_DURATION_S 3       // scan for 3 seconds

// ─── Config Defaults
// ──────────────────────────────────────────────────────────
#define DEFAULT_ADMIN_USER "admin"
#define DEFAULT_ADMIN_PASS "acousense2024"
#define MAX_CONFIG_HISTORY 5
#define DB_PATH "/sd/acousense.db"
#define WEB_ROOT "/sd/www"

// ─── Global Objects
// ───────────────────────────────────────────────────────────
SPIClass hspiBus(HSPI);
AsyncWebServer server(80);
Preferences prefs;
sqlite3 *db = nullptr;

// BLE
NimBLEScan *bleScan = nullptr;
std::map<std::string, unsigned long> presentDevices; // MAC → last seen ms
std::map<std::string, std::string> registeredUsers;  // MAC → user label

// ─── Device Config (loaded from NVS, pushed to Nano) ─────────────────────────
struct DeviceConfig {
  uint8_t mode; // 0=Office 1=Home 2=Sleep 3=Custom
  uint8_t warnDb;
  uint8_t alertDb;
  uint8_t dangerDb;
  char roomName[32];
};

DeviceConfig activeConfig = {0, 55, 65, 75, "Room"};

// Config history ring buffer (in-memory, also persisted to NVS)
DeviceConfig configHistory[MAX_CONFIG_HISTORY];
uint8_t configHistoryCount = 0;

// ─── Utility: XOR Checksum
// ────────────────────────────────────────────────────
uint8_t xorChecksum(uint8_t *buf, uint8_t len) {
  uint8_t cs = 0;
  for (uint8_t i = 0; i < len; i++)
    cs ^= buf[i];
  return cs;
}

// ─── SPI: Send Config to Arduino ─────────────────────────────────────────────
void pushConfigToNano(DeviceConfig &cfg) {
  uint8_t tx[FRAME_SIZE] = {
      PKT_CONFIG, cfg.mode, cfg.warnDb, cfg.alertDb, cfg.dangerDb,
      0x00,       0x00,     0x00 // reserved + checksum placeholder
  };
  tx[7] = xorChecksum(tx, 7);

  hspiBus.beginTransaction(SPISettings(1000000, MSBFIRST, SPI_MODE0));
  digitalWrite(NANO_CS, LOW);
  delayMicroseconds(10);
  for (uint8_t i = 0; i < FRAME_SIZE; i++) {
    hspiBus.transfer(tx[i]);
    delayMicroseconds(5);
  }
  digitalWrite(NANO_CS, HIGH);
  hspiBus.endTransaction();

  Serial.println("[SPI] Config pushed to Nano");
}

// ─── SPI: Poll Arduino for Data
// ───────────────────────────────────────────────
struct NoiseRecord {
  bool valid;
  uint16_t avg;
  uint16_t min;
  uint16_t max;
};

NoiseRecord pollNano() {
  NoiseRecord rec = {false, 0, 0, 0};

  // Send heartbeat request
  uint8_t tx[FRAME_SIZE] = {0xBE, 0, 0, 0, 0, 0, 0, 0};
  tx[7] = xorChecksum(tx, 7);
  uint8_t rx[FRAME_SIZE] = {0};

  hspiBus.beginTransaction(SPISettings(1000000, MSBFIRST, SPI_MODE0));
  digitalWrite(NANO_CS, LOW);
  delayMicroseconds(50); // give Nano time to prepare MISO

  for (uint8_t i = 0; i < FRAME_SIZE; i++) {
    rx[i] = hspiBus.transfer(tx[i]);
    delayMicroseconds(5);
  }

  digitalWrite(NANO_CS, HIGH);
  hspiBus.endTransaction();

  // Validate response
  if (rx[0] != PKT_DATA) {
    Serial.printf("[SPI] Unexpected packet type: 0x%02X\n", rx[0]);
    return rec;
  }
  uint8_t cs = xorChecksum(rx, 7);
  if (cs != rx[7]) {
    Serial.println("[SPI] Checksum mismatch — discarding frame");
    return rec;
  }

  rec.valid = true;
  rec.avg = ((uint16_t)rx[1] << 8) | rx[2];
  rec.min = ((uint16_t)rx[3] << 8) | rx[4];
  rec.max = ((uint16_t)rx[5] << 8) | rx[6];

  Serial.printf("[SPI] Received: avg=%u min=%u max=%u\n", rec.avg, rec.min,
                rec.max);
  return rec;
}

// ─── SQLite: Init DB
// ──────────────────────────────────────────────────────────
void initDatabase() {
  int rc = sqlite3_open(DB_PATH, &db);
  if (rc != SQLITE_OK) {
    Serial.printf("[DB] Failed to open: %s\n", sqlite3_errmsg(db));
    return;
  }

  const char *createNoise = R"sql(
    CREATE TABLE IF NOT EXISTS noise_records (
      id        INTEGER PRIMARY KEY AUTOINCREMENT,
      timestamp INTEGER NOT NULL,
      avg_db    REAL    NOT NULL,
      min_db    REAL    NOT NULL,
      max_db    REAL    NOT NULL,
      mode      INTEGER NOT NULL,
      room_name TEXT    NOT NULL
    );
  )sql";

  const char *createUsers = R"sql(
    CREATE TABLE IF NOT EXISTS room_presence (
      id         INTEGER PRIMARY KEY AUTOINCREMENT,
      mac        TEXT    NOT NULL,
      user_label TEXT,
      checked_in INTEGER NOT NULL,
      checked_out INTEGER
    );
  )sql";

  const char *createConfigs = R"sql(
    CREATE TABLE IF NOT EXISTS config_history (
      id        INTEGER PRIMARY KEY AUTOINCREMENT,
      timestamp INTEGER NOT NULL,
      mode      INTEGER NOT NULL,
      warn_db   INTEGER NOT NULL,
      alert_db  INTEGER NOT NULL,
      danger_db INTEGER NOT NULL,
      room_name TEXT    NOT NULL
    );
  )sql";

  char *errMsg = nullptr;
  sqlite3_exec(db, createNoise, nullptr, nullptr, &errMsg);
  sqlite3_exec(db, createUsers, nullptr, nullptr, &errMsg);
  sqlite3_exec(db, createConfigs, nullptr, nullptr, &errMsg);

  if (errMsg) {
    Serial.printf("[DB] Schema error: %s\n", errMsg);
    sqlite3_free(errMsg);
  } else {
    Serial.println("[DB] Schema ready");
  }
}

// ─── SQLite: Insert Noise Record
// ──────────────────────────────────────────────
void insertNoiseRecord(NoiseRecord &rec) {
  if (!db || !rec.valid)
    return;

  char sql[256];
  snprintf(sql, sizeof(sql),
           "INSERT INTO noise_records (timestamp, avg_db, min_db, max_db, "
           "mode, room_name) "
           "VALUES (%lld, %u, %u, %u, %u, '%s');",
           (long long)time(nullptr), rec.avg, rec.min, rec.max,
           activeConfig.mode, activeConfig.roomName);

  char *errMsg = nullptr;
  int rc = sqlite3_exec(db, sql, nullptr, nullptr, &errMsg);
  if (rc != SQLITE_OK) {
    Serial.printf("[DB] Insert error: %s\n", errMsg);
    sqlite3_free(errMsg);
  }
}

// ─── SQLite: Save Config to History ──────────────────────────────────────────
void saveConfigHistory(DeviceConfig &cfg) {
  if (!db)
    return;

  char sql[256];
  snprintf(sql, sizeof(sql),
           "INSERT INTO config_history (timestamp, mode, warn_db, alert_db, "
           "danger_db, room_name) "
           "VALUES (%lld, %u, %u, %u, %u, '%s');",
           (long long)time(nullptr), cfg.mode, cfg.warnDb, cfg.alertDb,
           cfg.dangerDb, cfg.roomName);

  char *errMsg = nullptr;
  sqlite3_exec(db, sql, nullptr, nullptr, &errMsg);

  // Keep only last 5
  const char *trim =
      "DELETE FROM config_history WHERE id NOT IN "
      "(SELECT id FROM config_history ORDER BY id DESC LIMIT 5);";
  sqlite3_exec(db, trim, nullptr, nullptr, &errMsg);

  if (errMsg)
    sqlite3_free(errMsg);
}

// ─── BLE: Presence Detection
// ──────────────────────────────────────────────────
class PresenceCallbacks : public NimBLEAdvertisedDeviceCallbacks {
  void onResult(NimBLEAdvertisedDevice *dev) override {
    std::string mac = dev->getAddress().toString();
    int rssi = dev->getRSSI();

    // Only consider devices with strong signal (same room heuristic)
    // Room boundary RSSI threshold — configurable per room during setup
    int rssiThreshold = prefs.getInt("rssi_thresh", -70);

    if (rssi >= rssiThreshold) {
      bool wasPresent = presentDevices.count(mac) > 0;
      presentDevices[mac] = millis();

      if (!wasPresent && registeredUsers.count(mac)) {
        // Check-in event
        Serial.printf("[BLE] Check-in: %s (%s)\n", mac.c_str(),
                      registeredUsers[mac].c_str());
        char sql[256];
        snprintf(sql, sizeof(sql),
                 "INSERT INTO room_presence (mac, user_label, checked_in) "
                 "VALUES ('%s', '%s', %lld);",
                 mac.c_str(), registeredUsers[mac].c_str(),
                 (long long)time(nullptr));
        char *err = nullptr;
        if (db)
          sqlite3_exec(db, sql, nullptr, nullptr, &err);
        if (err)
          sqlite3_free(err);
      }
    }
  }
};

void checkOutStaleDevices() {
  unsigned long now = millis();
  const unsigned long TIMEOUT_MS = 30000; // 30s no signal = checked out

  for (auto it = presentDevices.begin(); it != presentDevices.end();) {
    if (now - it->second > TIMEOUT_MS) {
      std::string mac = it->first;
      Serial.printf("[BLE] Check-out: %s\n", mac.c_str());

      char sql[256];
      snprintf(sql, sizeof(sql),
               "UPDATE room_presence SET checked_out = %lld "
               "WHERE mac = '%s' AND checked_out IS NULL;",
               (long long)time(nullptr), mac.c_str());
      char *err = nullptr;
      if (db)
        sqlite3_exec(db, sql, nullptr, nullptr, &err);
      if (err)
        sqlite3_free(err);

      it = presentDevices.erase(it);
    } else {
      ++it;
    }
  }
}

// ─── Web API Handlers
// ─────────────────────────────────────────────────────────

// Basic session token (stateless for simplicity — use JWT in production
// upgrade)
String adminSessionToken = "";

bool isAuthenticated(AsyncWebServerRequest *req) {
  if (!req->hasHeader("X-Auth-Token"))
    return false;
  return req->getHeader("X-Auth-Token")->value() == adminSessionToken;
}

void setupWebServer() {

  // ── Static files from SD ──────────────────────────────────────────────────
  server.serveStatic("/", SD, WEB_ROOT).setDefaultFile("index.html");

  // ── POST /api/auth/login ──────────────────────────────────────────────────
  server.on(
      "/api/auth/login", HTTP_POST, [](AsyncWebServerRequest *req) {}, nullptr,
      [](AsyncWebServerRequest *req, uint8_t *data, size_t len, size_t index,
         size_t total) {
        DynamicJsonDocument doc(256);
        deserializeJson(doc, (char *)data, len);

        String user = doc["username"] | "";
        String pass = doc["password"] | "";

        String storedUser = prefs.getString("admin_user", DEFAULT_ADMIN_USER);
        String storedPass = prefs.getString("admin_pass", DEFAULT_ADMIN_PASS);

        if (user == storedUser && pass == storedPass) {
          // Generate simple token
          adminSessionToken =
              String(esp_random(), HEX) + String(esp_random(), HEX);
          DynamicJsonDocument resp(128);
          resp["token"] = adminSessionToken;
          String body;
          serializeJson(resp, body);
          req->send(200, "application/json", body);
        } else {
          req->send(401, "application/json",
                    "{\"error\":\"Invalid credentials\"}");
        }
      });

  // ── GET /api/noise?limit=100&offset=0 ────────────────────────────────────
  server.on("/api/noise", HTTP_GET, [](AsyncWebServerRequest *req) {
    if (!db) {
      req->send(503, "application/json", "{\"error\":\"DB unavailable\"}");
      return;
    }

    int limit =
        req->hasParam("limit") ? req->getParam("limit")->value().toInt() : 100;
    int offset =
        req->hasParam("offset") ? req->getParam("offset")->value().toInt() : 0;

    char sql[256];
    snprintf(sql, sizeof(sql),
             "SELECT timestamp, avg_db, min_db, max_db, mode, room_name "
             "FROM noise_records ORDER BY timestamp DESC LIMIT %d OFFSET %d;",
             limit, offset);

    DynamicJsonDocument doc(8192);
    JsonArray arr = doc.createNestedArray("records");

    sqlite3_stmt *stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, nullptr) == SQLITE_OK) {
      while (sqlite3_step(stmt) == SQLITE_ROW) {
        JsonObject row = arr.createNestedObject();
        row["ts"] = sqlite3_column_int64(stmt, 0);
        row["avg"] = sqlite3_column_int(stmt, 1);
        row["min"] = sqlite3_column_int(stmt, 2);
        row["max"] = sqlite3_column_int(stmt, 3);
        row["mode"] = sqlite3_column_int(stmt, 4);
        row["room"] = (const char *)sqlite3_column_text(stmt, 5);
      }
      sqlite3_finalize(stmt);
    }

    String body;
    serializeJson(doc, body);
    req->send(200, "application/json", body);
  });

  // ── GET /api/presence ─────────────────────────────────────────────────────
  server.on("/api/presence", HTTP_GET, [](AsyncWebServerRequest *req) {
    DynamicJsonDocument doc(2048);
    JsonArray arr = doc.createNestedArray("present");
    for (auto &kv : presentDevices) {
      JsonObject entry = arr.createNestedObject();
      entry["mac"] = kv.first.c_str();
      entry["label"] = registeredUsers.count(kv.first)
                           ? registeredUsers[kv.first].c_str()
                           : "Unknown";
      entry["last_seen_ms"] = kv.second;
    }
    String body;
    serializeJson(doc, body);
    req->send(200, "application/json", body);
  });

  // ── GET /api/config/history ───────────────────────────────────────────────
  server.on("/api/config/history", HTTP_GET, [](AsyncWebServerRequest *req) {
    if (!isAuthenticated(req)) {
      req->send(401);
      return;
    }
    if (!db) {
      req->send(503);
      return;
    }

    const char *sql =
        "SELECT id, timestamp, mode, warn_db, alert_db, danger_db, room_name "
        "FROM config_history ORDER BY id DESC LIMIT 5;";

    DynamicJsonDocument doc(2048);
    JsonArray arr = doc.createNestedArray("history");

    sqlite3_stmt *stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, nullptr) == SQLITE_OK) {
      while (sqlite3_step(stmt) == SQLITE_ROW) {
        JsonObject row = arr.createNestedObject();
        row["id"] = sqlite3_column_int(stmt, 0);
        row["ts"] = sqlite3_column_int64(stmt, 1);
        row["mode"] = sqlite3_column_int(stmt, 2);
        row["warn_db"] = sqlite3_column_int(stmt, 3);
        row["alert_db"] = sqlite3_column_int(stmt, 4);
        row["danger_db"] = sqlite3_column_int(stmt, 5);
        row["room_name"] = (const char *)sqlite3_column_text(stmt, 6);
      }
      sqlite3_finalize(stmt);
    }

    String body;
    serializeJson(doc, body);
    req->send(200, "application/json", body);
  });

  // ── POST /api/config/apply ────────────────────────────────────────────────
  server.on(
      "/api/config/apply", HTTP_POST, [](AsyncWebServerRequest *req) {},
      nullptr,
      [](AsyncWebServerRequest *req, uint8_t *data, size_t len, size_t index,
         size_t total) {
        if (!isAuthenticated(req)) {
          req->send(401);
          return;
        }

        DynamicJsonDocument doc(512);
        deserializeJson(doc, (char *)data, len);

        DeviceConfig newCfg;
        newCfg.mode = doc["mode"] | activeConfig.mode;
        newCfg.warnDb = doc["warn_db"] | activeConfig.warnDb;
        newCfg.alertDb = doc["alert_db"] | activeConfig.alertDb;
        newCfg.dangerDb = doc["danger_db"] | activeConfig.dangerDb;
        strlcpy(newCfg.roomName, doc["room_name"] | activeConfig.roomName, 32);

        saveConfigHistory(activeConfig); // save current before overwriting
        activeConfig = newCfg;
        pushConfigToNano(activeConfig);

        req->send(200, "application/json", "{\"status\":\"applied\"}");
      });

  // ── POST /api/config/rollback ─────────────────────────────────────────────
  server.on(
      "/api/config/rollback", HTTP_POST, [](AsyncWebServerRequest *req) {},
      nullptr,
      [](AsyncWebServerRequest *req, uint8_t *data, size_t len, size_t index,
         size_t total) {
        if (!isAuthenticated(req)) {
          req->send(401);
          return;
        }

        DynamicJsonDocument doc(128);
        deserializeJson(doc, (char *)data, len);
        int targetId = doc["id"] | -1;
        if (targetId < 0) {
          req->send(400);
          return;
        }

        char sql[256];
        snprintf(sql, sizeof(sql),
                 "SELECT mode, warn_db, alert_db, danger_db, room_name "
                 "FROM config_history WHERE id = %d;",
                 targetId);

        sqlite3_stmt *stmt;
        if (sqlite3_prepare_v2(db, sql, -1, &stmt, nullptr) == SQLITE_OK &&
            sqlite3_step(stmt) == SQLITE_ROW) {
          DeviceConfig rollback;
          rollback.mode = sqlite3_column_int(stmt, 0);
          rollback.warnDb = sqlite3_column_int(stmt, 1);
          rollback.alertDb = sqlite3_column_int(stmt, 2);
          rollback.dangerDb = sqlite3_column_int(stmt, 3);
          strlcpy(rollback.roomName, (const char *)sqlite3_column_text(stmt, 4),
                  32);
          sqlite3_finalize(stmt);

          saveConfigHistory(activeConfig);
          activeConfig = rollback;
          pushConfigToNano(activeConfig);

          req->send(200, "application/json", "{\"status\":\"rolled_back\"}");
        } else {
          req->send(404, "application/json",
                    "{\"error\":\"Config ID not found\"}");
        }
      });

  // ── POST /api/device/register-user ───────────────────────────────────────
  server.on(
      "/api/device/register-user", HTTP_POST, [](AsyncWebServerRequest *req) {},
      nullptr,
      [](AsyncWebServerRequest *req, uint8_t *data, size_t len, size_t index,
         size_t total) {
        DynamicJsonDocument doc(256);
        deserializeJson(doc, (char *)data, len);
        std::string mac = std::string(doc["mac"] | "");
        std::string label = std::string(doc["label"] | "User");
        if (mac.empty()) {
          req->send(400);
          return;
        }
        registeredUsers[mac] = label;
        req->send(200, "application/json", "{\"status\":\"registered\"}");
      });

  // ── GET /api/status ───────────────────────────────────────────────────────
  server.on("/api/status", HTTP_GET, [](AsyncWebServerRequest *req) {
    DynamicJsonDocument doc(512);
    doc["uptime_s"] = millis() / 1000;
    doc["free_heap"] = esp_get_free_heap_size();
    doc["active_mode"] = activeConfig.mode;
    doc["room_name"] = activeConfig.roomName;
    doc["present_count"] = presentDevices.size();
    String body;
    serializeJson(doc, body);
    req->send(200, "application/json", body);
  });

  server.begin();
  Serial.println("[Web] Server started on port 80");
}

// ─── WiFi Setup
// ───────────────────────────────────────────────────────────────
void setupWiFi() {
  String ssid = prefs.getString("wifi_ssid", "");
  String pass = prefs.getString("wifi_pass", "");

  if (ssid.isEmpty()) {
    // No WiFi configured — start AP mode for initial setup
    WiFi.softAP("AcouSense-Setup", "acousense");
    Serial.printf("[WiFi] AP mode: AcouSense-Setup  IP: %s\n",
                  WiFi.softAPIP().toString().c_str());
  } else {
    WiFi.begin(ssid.c_str(), pass.c_str());
    Serial.print("[WiFi] Connecting");
    uint8_t attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 20) {
      delay(500);
      Serial.print(".");
      attempts++;
    }
    if (WiFi.status() == WL_CONNECTED) {
      Serial.printf("\n[WiFi] Connected: %s\n",
                    WiFi.localIP().toString().c_str());
    } else {
      Serial.println("\n[WiFi] Failed — falling back to AP mode");
      WiFi.softAP("AcouSense-Setup", "acousense");
    }
  }
}

// ─── Setup
// ────────────────────────────────────────────────────────────────────
void setup() {
  Serial.begin(115200);
  Serial.println("[AcouSense ESP32] Booting...");

  prefs.begin("acousense", false);

  // ── HSPI for Arduino Nano ─────────────────────────────────────────────────
  pinMode(NANO_CS, OUTPUT);
  digitalWrite(NANO_CS, HIGH);
  hspiBus.begin(NANO_SCK, NANO_MISO, NANO_MOSI, NANO_CS);
  Serial.println("[HSPI] Arduino bus ready");

  // ── VSPI for SD Card ──────────────────────────────────────────────────────
  if (!SD.begin(SD_CS)) {
    Serial.println("[SD] Mount failed — check wiring or card format (FAT32)");
  } else {
    Serial.println("[SD] Mounted");
  }

  // ── SQLite ────────────────────────────────────────────────────────────────
  sqlite3_initialize();
  initDatabase();

  // ── WiFi ──────────────────────────────────────────────────────────────────
  setupWiFi();

  // ── Web Server ────────────────────────────────────────────────────────────
  setupWebServer();

  // ── BLE ───────────────────────────────────────────────────────────────────
  NimBLEDevice::init("AcouSense");
  bleScan = NimBLEDevice::getScan();
  bleScan->setAdvertisedDeviceCallbacks(new PresenceCallbacks(), true);
  bleScan->setActiveScan(
      false); // passive scan — less power, sufficient for RSSI
  bleScan->setInterval(100);
  bleScan->setWindow(99);
  Serial.println("[BLE] Scanner ready");

  // Push initial config to Nano
  pushConfigToNano(activeConfig);

  Serial.println("[AcouSense ESP32] Ready.");
}

// ─── Main Loop
// ────────────────────────────────────────────────────────────────
unsigned long lastNanoPoll = 0;
unsigned long lastBleScan = 0;

void loop() {
  unsigned long now = millis();

  // ── Poll Arduino every 10s ───────────────────────────────────────────────
  if (now - lastNanoPoll >= NANO_POLL_INTERVAL_MS) {
    lastNanoPoll = now;
    NoiseRecord rec = pollNano();
    if (rec.valid) {
      insertNoiseRecord(rec);
    }
  }

  // ── BLE scan every 15s ───────────────────────────────────────────────────
  if (now - lastBleScan >= BLE_SCAN_INTERVAL_MS) {
    lastBleScan = now;
    bleScan->start(BLE_SCAN_DURATION_S, false);
    checkOutStaleDevices();
  }

  delay(10);
}
