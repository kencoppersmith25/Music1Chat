# Music1Chat Design Specification — Version 2.0 — Part 3

## Chapter 16: Station Model
*   **Station**: Global record (URL, Name, Artwork, Genre).
*   **Membership**: Links a Station to a Category with a specific sort order and navigation state.
*   **Normalization**: Stream URLs are normalized to prevent duplicate station records.

---

## Chapter 17: Playback Controller
Owns the execution of audio.
*   **Platform**: Uses `AVPlayer` on iOS.
*   **Stop vs Pause**: Live streams use "Stop" semantics to avoid implying a resume point that doesn't exist.
*   **Audio Focus**: Correctly handles interruptions (Phone calls, Siri, other apps).

---

## Chapter 18: Navigation Engine
The logic for selecting the next/previous item.
*   **Station Navigation**: Wraps within the current category; skips disabled stations.
*   **Category Navigation**: Wraps through the category list; skips disabled categories.
*   **Automatic Selection**: Choosing a new category while playing selects an eligible station and starts it immediately.

---

## Chapter 19: Siri and Media Buttons
*   **Siri Support**: Integrated with Siri intents for "Play", "Stop", "Next", and "Previous".
*   **Media Center**: Full integration with the iOS Control Center and Lock Screen.
*   **Headset Gestures**: Standard remote control events map to Station and Category navigation.

---

## Chapter 20: Failure Recovery
Ensures the music doesn't stop for the rider.
*   **Startup Timeout**: If a stream doesn't start in 4 seconds, it's marked "Failed for Session".
*   **Auto-Skip**: Automatically moves to the next station on failure.
*   **Category Advance**: If all stations in a category fail, the app moves to the next eligible category.
*   **Session Scope**: Failure status resets when the app is restarted.
