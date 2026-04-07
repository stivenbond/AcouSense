# Arkitektura e Aplikacionit Android (Clean Architecture)

Ky diagram detajon shtresat (Clean Architecture) e rekomanduara nga roadmap-i i DSP-App-V2, si dhe ndarjet strikte mes ndërfaqes së përdoruesit, logjikës së biznesit dhe të dhënave.

```mermaid
graph TD
    subgraph UI Layer
        UI_Dash[Dashboard Screen]
        UI_Ins[Insights Screen]
        UI_Prov[Provisioning Screen]
        VM[ViewModels<br>StateFlow, RoomPresenceState]
    end

    subgraph Domain Layer
        UC[Use Cases<br>GetLiveFeed, ManageRoomPresence...]
        Models[Domain Models<br>NoiseReading, DeviceStatus...]
        Repo_Interfaces[Repository Interfaces]
    end

    subgraph Data Layer
        subgraph Remote
            Ktor[Ktor REST/WS Client]
            Repo_Remote[Remote Repositories]
        end
        subgraph Local
            RoomDB[Room SQL Database]
            Repo_Local[Cached Repositories]
        end
        subgraph Peripheral
            BLE[BluetoothLeScanner]
            Repo_Ble[BLE Presence Repository]
        end
    end

    UI_Dash --> VM
    UI_Ins --> VM
    UI_Prov --> VM
    VM --> UC
    UC --> Repo_Interfaces
    Repo_Interfaces <|-- Repo_Remote
    Repo_Interfaces <|-- Repo_Local
    Repo_Interfaces <|-- Repo_Ble
    Repo_Local --> RoomDB
    Repo_Remote --> Ktor
    Repo_Ble --> BLE
    
    Ktor <-->|HTTP/WS| ESP[ESP32 Server]
    BLE <-->|BLE Adv| ESP
```
