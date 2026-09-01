# Music1Chat Design Specification — Version 2.0 — Part 4

## Chapter 21: Persistence
Restores the user's context on launch.
*   **Storage**: Core Data or SQLite for relational data; UserDefaults for simple settings.
*   **Restoration**: Remembers Current Category, Station, and search history.
*   **Startup**: The app restores context but does not "Autoplay" unless specifically requested.

---

## Chapter 22: Settings
*   **Appearance**: Support for System, Light, and Dark themes.
*   **Playback**: Adjustable timeouts and recovery toggles.
*   **Data**: Options to clear history or reset to standard defaults.

---

## Chapter 23: AirPlay and External Playback
*   **AirPlay**: Standard iOS AirPlay integration for home audio and Smart TVs.
*   **Cast**: Support for Google Cast and other remote receivers where applicable.
*   **State Authority**: The app remains the authority for navigation even when playing to a remote device.

---

## Chapter 24: Themes and Visual System
*   **Midnight Black**: Default dark theme optimized for high-visibility outdoors.
*   **Accessibility**: High contrast, large touch targets, and screen-reader compatibility.
*   **Dynamic Layout**: Responds to iOS font scaling and orientation changes.

---

## Chapter 15: Startup and Shutdown
*   **Launch**: Quick restoration of saved categories and stations.
*   **Backgrounding**: Audio continues playing in the background via `AVAudioSession`.
*   **Exit**: Explicit power-off stops audio and releases system resources.
