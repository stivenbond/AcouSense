# Protokollet e Komunikimit dhe Fluksi (Communication Protocols)

Ky diagram sekuencial tregon se si kalojnë të dhënat nga mikrofoni (hardueri) tek sensori Nano, më pas tek serveri Gateway (ESP32) dhe në fund në aplikacionin e përdoruesit (DSP-App V2).

```mermaid
sequenceDiagram
    participant Mic as Mic (KY-038)
    participant Nano as Arduino Nano
    participant ESP as ESP32
    participant App as Android App (V2)

    Note over Mic, Nano: 1. Nxjerrja e të Dhënave
    Mic->>Nano: Sinjale Analoge (A0)
    
    Note over Nano, ESP: 2. Komunikimi MCU (SPI)
    loop Çdo interval i caktuar
        ESP->>Nano: SPI Request (ID 0xDA DATA)
        Nano-->>ESP: Frame 8-Byte (Avg, Min, Max dB + Checksum)
    end
    
    Note over ESP, App: 3. Komunikimi i Rrjetit (Wi-Fi)
    App->>ESP: WebSocket /api/v1/readings/live
    ESP-->>App: WS Stream: Transmetimi i Zhurmës
    
    App->>ESP: GET /api/v1/readings/history
    ESP-->>App: Të dhënat historike nga DB SQLite
    
    App->>ESP: POST /setup/room
    ESP-->>App: Konfirmim (Acknowledge) i Konfigurimit
    
    Note over App, ESP: 4. Afërsia dhe Prania (BLE)
    App->>ESP: BLE Advertise (GATT 0000ACOU-...)
    ESP->>App: Skanimi dhe Përputhja e UUID për Praninë
```
