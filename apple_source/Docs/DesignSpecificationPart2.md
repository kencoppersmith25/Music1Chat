# Music1Chat Design Specification — Version 2.0 — Part 2

## Chapter 5: Main Screen Design
The Main Screen is optimized for quick recognition and low-attention control.

### Vertical Arrangement
1.  **Top Bar**: Title, AirPlay/Cast, Settings, Power/Exit.
2.  **Search Field**: Single-line genre-oriented search.
3.  **Search Chips**: Horizontal row of recent/saved searches.
4.  **Current Category Card**: Displays active category and its navigation state.
5.  **Now Playing Card**: Displays station info, metadata, and copy action.
6.  **Primary Controls**: Large, distinct buttons for navigation.

---

## Chapter 6: Search System
Search Discovery becomes persistent organization.
*   **Genre List**: Comprehensive list of common genres (60s, Jazz, Rock, etc.).
*   **Search Category**: Created automatically from search results.
*   **Duplicate Handling**: Rerunning a search refreshes the existing category.

---

## Chapter 7: Current Category Section
Identifies the "listening context".
*   Displays name, navigation indicator, and station count.
*   Tapping the card opens the Category List.
*   Tapping the indicator toggles navigation eligibility.

---

## Chapter 8: Now Playing Section
*   **Marquee Text**: Displays station name and live metadata (Song/Artist).
*   **Station Indicator**: Toggles navigation for this station *within* this category.
*   **Copy Action**: Allows saving the station to other categories.

---

## Chapter 9: Primary Control Interactions
*   **Center Control**: Large Play/Stop button. Red when active.
*   **Single Chevrons**: Previous/Next Station within the current category.
*   **Double Chevrons**: Previous/Next Category (Skips disabled categories).
*   **Active Navigation**: Navigating while playing immediately starts the new selection.

---

## Chapter 10: Search Chips
*   Compact shortcuts for recent searches.
*   Tapping a chip activates/refreshes that Search Category.
*   Newest searches appear on the left.

---

## Chapter 11: Category List Screen
Displays all Standard, Search, and User-Defined categories.
*   Allows toggling navigation, reordering, and deletion.
*   Highlights the currently active category.

---

## Chapter 12: Category Details Screen
Displays all stations within a specific category.
*   Allows management of individual memberships (Navigation, Copy, Delete).
*   Search category memberships are usually not individually deletable (refresh would restore them).

---

## Chapter 13: Copy Station Workflow
*   Replaces the "Move" concept.
*   Creates new memberships in one or more destination categories.
*   Always preserves the source membership.

---

## Chapter 14: Category Management
*   **Standard**: Cannot be deleted; name is fixed.
*   **Search**: Created from search; can be deleted or refreshed.
*   **User-Defined**: Fully editable name and contents.

---

## Chapter 15: Navigation Indicator Design
*   **Enabled**: Item is included in sequential navigation.
*   **Disabled**: Item is visible but skipped by Next/Previous.
*   **Visuals**: Must use shape/fill differences, not just color, to ensure accessibility.
