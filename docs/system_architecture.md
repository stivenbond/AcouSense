# Arkitektura e Përgjithshme e Sistemit (System Architecture)

Ky diagram tregon pamjen nga lart të gjithë ekosistemit AcouSense, ku tregohet se si komponentët kryesorë (nyja e sensorit dhe aplikacionet e klientit) ndërveprojnë me njëri-tjetrin.

```mermaid
graph TD
    subgraph AcouSense Node
        Nano[Arduino Nano<br>Sensor Hub]
        ESP[ESP32<br>Gateway Server]
        Mic[KY-038<br>Microphone]
        Buzzer[Passive Buzzer]
        LCD[I2C LCD 16x2]
        SD[SD Card Module<br>SQLite DB & Web files]
    end

    subgraph Client Applications
        App[DSP-App V2<br>Android App]
        Web[DSP-WebUI<br>Browser]
    end

    Mic -- Raw Sound (Analog) --> Nano
    Nano -- Display Text (I2C) --> LCD
    Nano -- Alerts (PWM) --> Buzzer
    Nano -- Sensor Data (SPI 8-Byte) --> ESP
    ESP -- Read/Write (SPI) --> SD

    ESP <==>|Wi-Fi: HTTP REST / WS| App
    ESP <==>|Wi-Fi: HTTP| Web
    ESP -.->|BLE: Presence Detection| App
```
