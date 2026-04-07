/**
 * AcouSense - Arduino Nano Firmware
 * Role: SPI Slave, Sensor Hub, UI Controller
 *
 * Responsibilities:
 *  - Sample KY-038 analog mic every 100ms
 *  - Maintain a 10-second rolling window → compute min/max/avg
 *  - Drive 16x2 I2C LCD with current dB level + mode
 *  - Trigger buzzer on threshold breach (mode-dependent)
 *  - Receive config packets from ESP32 via SPI
 *  - Send aggregated noise packets to ESP32 via SPI
 *
 * SPI Role: SLAVE (ESP32 is master, controls clock)
 * I2C: LCD on A4 (SDA), A5 (SCL)
 * Analog: KY-038 AO on A0
 * Buzzer: D9 (PWM-capable)
 * SPI Slave Select: D10 (SS), D11 (MOSI), D12 (MISO), D13 (SCK)
 *
 * Packet Protocol (8-byte fixed frames):
 *  ESP→Nano (CONFIG):  [0xCF, mode, thr_low, thr_med, thr_high, reserved, reserved, checksum]
 *  Nano→ESP  (DATA):   [0xDA, avg_hi, avg_lo, min_hi, min_lo, max_hi, max_lo, checksum]
 *  Heartbeat request:  [0xBE, ...]  → Nano replies with current snapshot
 */

#include <SPI.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>

// ─── Pin Definitions ──────────────────────────────────────────────────────────
#define MIC_PIN         A0
#define BUZZER_PIN      9
#define SPI_SS_PIN      10   // SS/CS — driven LOW by ESP32 to select this slave

// ─── Packet Markers ───────────────────────────────────────────────────────────
#define PKT_CONFIG      0xCF
#define PKT_DATA        0xDA
#define PKT_HEARTBEAT   0xBE
#define PKT_ACK         0xAC
#define FRAME_SIZE      8

// ─── WHO Noise Modes ──────────────────────────────────────────────────────────
// Mode 0: Office/Study   — warn at 55dB,  alert at 65dB,  danger at 75dB
// Mode 1: Home/Rest      — warn at 45dB,  alert at 55dB,  danger at 65dB
// Mode 2: Sleep          — warn at 35dB,  alert at 45dB,  danger at 55dB
// Mode 3: Custom         — thresholds set via config packet from ESP32

struct NoiseMode {
  const char* label;
  uint8_t warnDb;
  uint8_t alertDb;
  uint8_t dangerDb;
};

NoiseMode MODES[] = {
  { "Office",  55, 65, 75 },
  { "Home",    45, 55, 65 },
  { "Sleep",   35, 45, 55 },
  { "Custom",  55, 65, 75 }   // overwritten by config packet
};

// ─── State ────────────────────────────────────────────────────────────────────
LiquidCrystal_I2C lcd(0x27, 16, 2);   // adjust address to 0x3F if 0x27 doesn't work

volatile uint8_t currentMode = 0;
volatile bool    configPending = false;
volatile bool    dataRequested = false;

// Rolling 10-second window: sample every 100ms → 100 samples
#define WINDOW_SIZE     100
#define SAMPLE_INTERVAL 100  // ms

uint16_t sampleWindow[WINDOW_SIZE];
uint8_t  windowIndex = 0;
bool     windowFull  = false;

uint16_t currentDb   = 0;   // live reading (scaled)
uint16_t windowMin   = 0;
uint16_t windowMax   = 0;
uint16_t windowAvg   = 0;

unsigned long lastSampleTime = 0;
unsigned long lastLcdUpdate  = 0;

// SPI receive buffer
volatile uint8_t spiRxBuf[FRAME_SIZE];
volatile uint8_t spiTxBuf[FRAME_SIZE];
volatile uint8_t spiByteIndex = 0;
volatile bool    spiFrameReady = false;

// ─── dB Estimation ────────────────────────────────────────────────────────────
// KY-038 AO gives 0-1023. We map this to an estimated dB range 30-100dB.
// This is NOT acoustically calibrated — it is a relative estimation suitable
// for threshold-based alerting. For a calibrated version, a reference measurement
// against a known sound source would be needed.
uint16_t analogToDb(uint16_t raw) {
  // Clamp to avoid log(0)
  if (raw < 1) raw = 1;
  // Map 1–1023 → 30–100 dB (logarithmic feel via float, cast back)
  float db = 30.0f + (70.0f * (float)raw / 1023.0f);
  return (uint16_t)db;
}

// ─── Window Stats ─────────────────────────────────────────────────────────────
void computeWindowStats() {
  uint8_t count = windowFull ? WINDOW_SIZE : windowIndex;
  if (count == 0) return;

  uint32_t sum = 0;
  uint16_t mn = 65535, mx = 0;

  for (uint8_t i = 0; i < count; i++) {
    sum += sampleWindow[i];
    if (sampleWindow[i] < mn) mn = sampleWindow[i];
    if (sampleWindow[i] > mx) mx = sampleWindow[i];
  }

  windowMin = mn;
  windowMax = mx;
  windowAvg = (uint16_t)(sum / count);
}

// ─── Buzzer ───────────────────────────────────────────────────────────────────
void buzzAlert(uint8_t level) {
  // level 1=warn, 2=alert, 3=danger — different tones
  uint16_t freq = (level == 1) ? 1000 : (level == 2) ? 1500 : 2500;
  uint16_t dur  = (level == 1) ? 100  : (level == 2) ? 200  : 400;
  tone(BUZZER_PIN, freq, dur);
}

// ─── LCD Display ──────────────────────────────────────────────────────────────
void updateLcd() {
  NoiseMode& m = MODES[currentMode];

  // Line 1: current reading + mode label
  lcd.setCursor(0, 0);
  lcd.print("Now:");
  lcd.print(currentDb);
  lcd.print("dB  ");
  lcd.print(m.label);
  // pad to 16 chars
  lcd.print("    ");

  // Line 2: avg / status
  lcd.setCursor(0, 1);
  if (currentDb >= m.dangerDb) {
    lcd.print("!! DANGER  ");
    lcd.print(currentDb);
    lcd.print("dB");
  } else if (currentDb >= m.alertDb) {
    lcd.print("! ALERT    ");
    lcd.print(currentDb);
    lcd.print("dB");
  } else if (currentDb >= m.warnDb) {
    lcd.print("~ WARN     ");
    lcd.print(currentDb);
    lcd.print("dB");
  } else {
    lcd.print("Avg:");
    lcd.print(windowAvg);
    lcd.print("dB OK     ");
  }
}

// ─── SPI Packet Builder ───────────────────────────────────────────────────────
void buildDataPacket() {
  spiTxBuf[0] = PKT_DATA;
  spiTxBuf[1] = (windowAvg >> 8) & 0xFF;
  spiTxBuf[2] =  windowAvg       & 0xFF;
  spiTxBuf[3] = (windowMin >> 8) & 0xFF;
  spiTxBuf[4] =  windowMin       & 0xFF;
  spiTxBuf[5] = (windowMax >> 8) & 0xFF;
  spiTxBuf[6] =  windowMax       & 0xFF;
  // Simple XOR checksum over bytes 0-6
  uint8_t cs = 0;
  for (uint8_t i = 0; i < 7; i++) cs ^= spiTxBuf[i];
  spiTxBuf[7] = cs;
}

// ─── SPI Frame Parser ─────────────────────────────────────────────────────────
void parseConfigPacket() {
  // Verify checksum
  uint8_t cs = 0;
  for (uint8_t i = 0; i < 7; i++) cs ^= spiRxBuf[i];
  if (cs != spiRxBuf[7]) return;  // corrupt frame, discard

  if (spiRxBuf[0] == PKT_CONFIG) {
    uint8_t newMode = spiRxBuf[1];
    if (newMode < 4) {
      currentMode = newMode;
    }
    if (newMode == 3) {  // Custom mode — load thresholds
      MODES[3].warnDb   = spiRxBuf[2];
      MODES[3].alertDb  = spiRxBuf[3];
      MODES[3].dangerDb = spiRxBuf[4];
    }
  } else if (spiRxBuf[0] == 0xBE) {  // heartbeat / data request
    dataRequested = true;
  }
}

// ─── SPI ISR ──────────────────────────────────────────────────────────────────
// Called on each byte received via SPI hardware
ISR(SPI_STC_vect) {
  uint8_t received = SPDR;

  if (spiByteIndex < FRAME_SIZE) {
    spiRxBuf[spiByteIndex] = received;
    // Load next TX byte
    SPDR = spiTxBuf[spiByteIndex];
    spiByteIndex++;
  }

  if (spiByteIndex >= FRAME_SIZE) {
    spiByteIndex = 0;
    spiFrameReady = true;
  }
}

// ─── Setup ────────────────────────────────────────────────────────────────────
void setup() {
  Serial.begin(115200);
  Serial.println(F("[AcouSense Nano] Booting..."));

  // LCD
  lcd.init();
  lcd.backlight();
  lcd.setCursor(0, 0);
  lcd.print("AcouSense v1.0");
  lcd.setCursor(0, 1);
  lcd.print("Initializing...");

  // Buzzer
  pinMode(BUZZER_PIN, OUTPUT);
  tone(BUZZER_PIN, 1200, 150);  // boot beep

  // SPI Slave setup
  pinMode(MISO, OUTPUT);        // Nano drives MISO
  pinMode(SS, INPUT);           // SS is input (driven by ESP32)
  SPCR |= _BV(SPE);            // Enable SPI in slave mode
  SPI.attachInterrupt();        // Attach SPI ISR

  // Pre-load TX buffer with a data packet (ESP reads it on first clock)
  buildDataPacket();

  // ADC reference
  analogReference(DEFAULT);    // 5V reference on Nano

  delay(1500);
  lcd.clear();

  Serial.println(F("[AcouSense Nano] Ready."));
}

// ─── Main Loop ────────────────────────────────────────────────────────────────
void loop() {
  unsigned long now = millis();

  // ── 1. Sample microphone ──────────────────────────────────────────────────
  if (now - lastSampleTime >= SAMPLE_INTERVAL) {
    lastSampleTime = now;

    uint16_t raw = analogRead(MIC_PIN);
    currentDb = analogToDb(raw);

    sampleWindow[windowIndex] = currentDb;
    windowIndex = (windowIndex + 1) % WINDOW_SIZE;
    if (windowIndex == 0) windowFull = true;

    computeWindowStats();

    // Buzzer alert logic
    NoiseMode& m = MODES[currentMode];
    if      (currentDb >= m.dangerDb) buzzAlert(3);
    else if (currentDb >= m.alertDb)  buzzAlert(2);
    else if (currentDb >= m.warnDb)   buzzAlert(1);

    // Refresh TX buffer so ESP gets fresh data on next SPI read
    buildDataPacket();
  }

  // ── 2. Update LCD every 500ms ─────────────────────────────────────────────
  if (now - lastLcdUpdate >= 500) {
    lastLcdUpdate = now;
    updateLcd();
  }

  // ── 3. Process incoming SPI frame (set by ISR) ────────────────────────────
  if (spiFrameReady) {
    spiFrameReady = false;
    parseConfigPacket();
  }

  // ── 4. If data was requested via heartbeat, rebuild TX buffer ─────────────
  if (dataRequested) {
    dataRequested = false;
    buildDataPacket();
    Serial.print(F("[SPI TX] avg="));
    Serial.print(windowAvg);
    Serial.print(F(" min="));
    Serial.print(windowMin);
    Serial.print(F(" max="));
    Serial.println(windowMax);
  }
}
