# Lidhjet Harduerike dhe Nivelet e Logjikës (Hardware Wiring & Logic Levels)

Diagram që përshkruan arkitekturën inxhinierike, posaçërisht sfidat e furnizimit me energji dhe përkthimit të nivelit të tensionit (Voltage Level Translation) gjatë komunikimit SPI.

```mermaid
graph LR
    subgraph Power
        USB1[Laptop USB 1]
        USB2[Laptop USB 2]
    end

    subgraph 5V Logic Zone
        Nano[Arduino Nano<br>5V Logic]
        Mic[KY-038 Mic<br>VCC 5V]
        LCD[I2C LCD + Backpack<br>VCC 5V]
        Buzz[Passive Buzzer]
    end

    subgraph Logic Translation
        LLS[4-Ch Logic Level Shifter<br>Bidirectional]
    end

    subgraph 3.3V Logic Zone
        ESP[ESP32 Gateway<br>3.3V Logic]
        SD[SD Card Module<br>3.3V Logic]
    end

    USB1 -- 5V / GND --> Nano
    USB2 -- 5V / GND --> ESP
    
    Nano -- 5V --> Mic
    Nano -- A0 --> Mic
    
    Nano -- 5V --> LCD
    Nano -- A4 / A5 (SDA/SCL) --> LCD
    
    Nano -- D9 (PWM) --> Buzz
    
    Nano -- D10/11/12/13 --> LLS
    ESP -- GPIO12/13/14/15 --> LLS
    LLS -- 5V Ref --> Nano
    LLS -- 3.3V Ref --> ESP
    
    ESP -- GPIO5/18/19/23 (VSPI) --> SD
    
    %% Common Ground
    Nano -.- G[Common Ground]
    ESP -.- G
    LLS -.- G
    Mic -.- G
    LCD -.- G
    SD -.- G
```
