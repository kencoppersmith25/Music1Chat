# Music1Chat Design Specification — Version 2.0 — Part 1

## Mission Statement
Music1Chat is designed to provide the safest, simplest, and most enjoyable hands-free audio experience possible.

Whether walking, cycling, driving, exercising, working, or relaxing at home, users should be able to discover, organize, and enjoy audio with minimal interaction and maximum reliability.

Every feature in Music1Chat is evaluated according to one guiding principle:
**Can the user continue enjoying audio without needing to look at or touch the phone?**

## Document Purpose
This document defines the intended design and behavior of Music1Chat for the Apple platform. It is 99% identical to the Android implementation to ensure a consistent experience across all devices.

## Revision History: Version 2.0 (July 2026)
*   **Navigation Model**: Replaced Favorites/Heart model with Navigation Enabled/Disabled.
*   **Category Navigation**: Defined independently from station navigation.
*   **Station Navigation**: Now a property of membership within a category.
*   **Category Types**: Standard, Search, and User-Defined are first-class types.
*   **Persistent Search**: Search categories persist until explicitly deleted.
*   **Hands-Free Priority**: Central design priority for drivers and bikers.
*   **External Control**: Full support for Siri and media button commands.

---

## Chapter 1: Introduction
Music1Chat is an audio application designed around reliable, low-attention, and hands-free listening. It assumes the phone may be in a pocket, mounted on a bike, or connected to a vehicle where visual interaction is unsafe or impossible.

### Design Priorities
1.  Safety
2.  Reliable continued playback
3.  Simple navigation
4.  Clear current-state feedback
5.  Easy organization

---

## Chapter 2: Design Philosophy
*   **User-First Design**: Start with the user's experience, not the data model.
*   **Hands-Free First**: Siri and media buttons are primary interfaces.
*   **One Source of Truth**: Centralized state for categories, stations, and playback.
*   **Safe Defaults**: Favor continued playback and automatic recovery.

---

## Chapter 3: Overall Architecture
The application is divided into logical layers:
1.  **User Interface**: SwiftUI views observing state.
2.  **State & ViewModels**: Managing observable UI state.
3.  **Navigation & Playback Coordination**: The engine driving content selection.
4.  **Repositories & Services**: Data access and Search.
5.  **Platform Playback**: AVPlayer and iOS-specific persistence.

---

## Chapter 4: Core Concepts
*   **Category**: An ordered collection of station memberships (Standard, Search, User-Defined).
*   **Station**: A global playable audio source (Internet radio stream).
*   **Membership**: The relationship between a Station and a Category (includes sort order and navigation state).
*   **Navigation Enabled**: Item participates in sequential "Next/Previous" commands.
*   **Navigation Disabled**: Item is visible/selectable but skipped in sequential navigation.
*   **Wraparound**: Navigation returns to the start/end of a list automatically.
