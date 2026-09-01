# Music1Chat Design Specification — Version 2.0 — Part 5

## Chapter 26: Developer Implementation Notes
*   **SwiftUI**: Use declarative views to observe the central state.
*   **Decoupling**: Keep the Navigation Engine logic separate from the UI views.
*   **Logging**: Meaningful logging to track state changes and recovery events.

---

## Chapter 27: Future Enhancements
*   CarPlay support.
*   Apple Watch remote.
*   Local file/folder support.
*   Advanced search filters (Bitrate, Language, etc.).

---

## Chapter 28: Design Rationale
*   **Why Hands-Free?**: To solve the specific problem of safe control while moving (biking/driving).
*   **Why Navigation vs Favorites?**: "Navigation Enabled" describes a behavior (participating in a sequence), not just an emotion (liking it).
*   **Why Automatic Recovery?**: A rider should not have to reach for their phone because a remote stream failed.

---

## Chapter 29: Glossary
*   **Category**: A collection of stations.
*   **Station**: A playable source.
*   **Membership**: A station's placement in a category.
*   **Navigation Enabled**: Participating in sequential Previous/Next.
*   **Wraparound**: Returning to the start/end of a list.

---

## Appendix A: Core Workflows
1.  **Search & Play**: Search -> Create Category -> Select Station -> Play.
2.  **Next Station**: Check eligibility -> Update current -> Start audio.
3.  **Station Failure**: Detect -> Mark -> Find next -> Play.
4.  **Copy**: Select destinations -> Create memberships -> Preserve source.
