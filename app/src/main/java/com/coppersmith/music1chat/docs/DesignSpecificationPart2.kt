package com.coppersmith.music1chat.docs

@Suppress("unused")
val designSpec050 = """
══════════════════════════════════════════════════════════════════════

                       MUSIC1CHAT

                   DESIGN SPECIFICATION

                       VERSION 2.0

                        PART 2

          MAIN SCREEN, SEARCH, AND CATEGORIES

══════════════════════════════════════════════════════════════════════

PART 2 PURPOSE

This file defines the visible structure and interaction behavior of:

• The Main Screen
• The top application bar
• Search
• Autocomplete
• Search chips
• The Current Category section
• The Now Playing section
• Primary playback and navigation controls
• The category-list screen
• The category-details screen
• Category creation
• Category editing
• Category deletion
• Station-copying workflow
• Navigation indicators
• Core visual behavior

The detailed playback, Bluetooth, and failure-recovery rules are
defined in Part 3.

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec060 = """
══════════════════════════════════════════════════════════════════════

                        CHAPTER 5

                     MAIN SCREEN DESIGN

══════════════════════════════════════════════════════════════════════

5.1 PURPOSE OF THE MAIN SCREEN

The Main Screen is the primary listening interface.

It should allow the user to:

• See the current category
• See the current station
• Start or stop playback
• Move to the previous or next station
• Move to the previous or next category
• Perform a search
• Reuse a recent search
• Open category management
• Open station management
• Open settings
• Open external-output controls
• Exit the application

The Main Screen should remain visually simple despite supporting all
of these functions.

5.2 PRIMARY DESIGN PRINCIPLE

The Main Screen is optimized for quick recognition and low-attention
control.

The most important controls are:

• Play or Stop
• Previous Station
• Next Station
• Previous Category
• Next Category

These controls must be large, visually distinct, and separated enough
to avoid accidental presses.

5.3 SCREEN ORDER

The Main Screen is arranged vertically in the following order:

1. Top application bar
2. Search field
3. Search chips
4. Current Category card
5. Now Playing card
6. Primary navigation and playback controls
7. Playback activity indicator
8. Status footer

The exact spacing may vary by screen size, but the logical order must
remain consistent.

5.4 SAFE AREA

The Main Screen must respect:

• Status-bar insets
• Display cutouts
• Navigation-bar insets
• Gesture-navigation regions

Content must not overlap the status bar.

The top-level layout should use status-bar padding or scaffold insets
rather than hard-coded placement.

5.5 SCROLLING

The preferred Main Screen layout keeps all primary controls visible
without vertical scrolling on a typical phone in portrait orientation.

On smaller displays:

• Secondary content may compress
• Artwork may shrink
• Spacing may reduce
• Search chips may remain horizontally scrollable

Primary playback controls should not be pushed off-screen if avoidable.

5.6 TOP APPLICATION BAR

The top application bar displays:

• Application title on the left
• External-output icon
• Settings icon
• Exit icon

The application title is:

Music1Chat

A future product rename may change this title without altering the
layout.

5.7 EXTERNAL-OUTPUT ICON

The external-output icon uses a familiar Cast-style symbol.

It opens the output-selection or cast interface.

Its purpose is broader than Bluetooth alone.

The icon may eventually represent:

• Local device output
• Bluetooth route
• Cast device
• Fire TV receiver
• Other remote destinations

The icon should use a content description such as:

Select playback device

5.8 SETTINGS ICON

The Settings icon opens the Settings screen.

It should use the standard Material Settings symbol.

Its content description should be:

Open settings

5.9 EXIT ICON

The Exit icon uses a power-style symbol.

It requests controlled application exit behavior.

Exit behavior is defined in Part 4.

The icon must not be placed so close to Settings that accidental
activation is likely.

Its content description should be:

Exit Music1Chat

5.10 SEARCH SECTION

The search field appears directly below the top application bar.

Search is visually prominent but should not dominate the playback
controls.

The search field should use a single line.

5.11 CURRENT CATEGORY CARD

The Current Category card appears below the search area.

It identifies the active category.

Example:

Search: Classical

or:

Bike Ride

The card also displays the category Navigation indicator.

Tapping the card opens the Category List screen.

5.12 NOW PLAYING CARD

The Now Playing card appears below the Current Category card.

It displays:

• Station artwork or placeholder
• Station name
• Available metadata
• Station membership Navigation indicator
• Copy action
• Optional playback-status information

The Now Playing card represents the current station in the context of
the Current Category.

5.13 PRIMARY CONTROL AREA

The primary control area contains five major controls:

• Previous Category
• Previous Station
• Play or Stop
• Next Station
• Next Category

The Play or Stop control is centered.

Previous controls appear to the left.

Next controls appear to the right.

5.14 CONTROL SYMBOLS

Recommended symbols:

Previous Category
    Double left chevron

Previous Station
    Single left chevron

Play
    Play triangle

Stop
    Stop square

Next Station
    Single right chevron

Next Category
    Double right chevron

The difference between station and category navigation must be obvious.

5.15 PLAY AND STOP CONTROL

The center control is the largest control on the Main Screen.

When playback is stopped, it displays Play.

When playback is active, connecting, buffering, or recovering, it may
display Stop.

The active Stop state should be visually prominent.

A red circular Stop control is preferred because it is easy to identify
at a glance.

5.16 CONTROL SPACING

Station and category navigation controls must be separated enough to
reduce accidental activation.

Single and double chevrons should not be placed so closely that they
appear to be one compound control.

5.17 PLAYBACK ACTIVITY INDICATOR

A small animated level-style or VU-style indicator may appear near the
Now Playing section or primary controls.

Its purpose is to show that playback is active.

The indicator:

• Is decorative but informative
• Must not be the only indication of playback state
• May animate while audio is playing
• Stops or becomes inactive when playback stops
• Uses a restrained visual style

5.18 STATUS FOOTER

The Main Screen includes a compact status footer.

Examples:

Bluetooth controls ready

Connecting to station

Playing on this device

Recovering from station failure

No playable station in this category

The footer should provide useful state without becoming distracting.

5.19 ORIENTATION

Portrait orientation is the primary design target.

Landscape should remain functional.

In landscape:

• The layout may use columns
• Artwork and controls may appear side by side
• Primary controls must remain large
• Search must remain accessible

5.20 TABLET LAYOUT

On larger screens, the application may display:

• Main playback controls on one side
• Category or station list on the other side

This is an enhancement rather than a requirement for the first
implementation.

DESIGN NOTE

The Main Screen is not intended to expose every organizational feature
at once.

It should present the smallest useful set of controls for listening,
while making deeper organization available through obvious secondary
screens.

END OF CHAPTER 5

Related sections:

Chapter 6 — Search
Chapter 7 — Current Category
Chapter 8 — Now Playing
Chapter 9 — Primary Controls

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec070 = """
══════════════════════════════════════════════════════════════════════

                        CHAPTER 6

                     SEARCH SYSTEM

══════════════════════════════════════════════════════════════════════

6.1 PURPOSE

Search allows the user to discover stations and create persistent
Search Categories.

Search is not treated as a temporary screen that disappears after a
station is selected.

A completed search becomes part of the user's category system.

6.2 SEARCH FIELD

The search field:

• Uses one line
• Accepts free-form text
• Displays a genre-oriented placeholder
• Provides autocomplete
• Provides a dropdown control
• Provides a Search action
• Supports the keyboard Search action

Recommended placeholder:

Search genres

6.3 SEARCH TEXT

Search text is trimmed before execution.

A blank search must not create a category.

Search matching should normally ignore case.

6.4 SEARCH TRAILING CONTROLS

The search field contains two trailing controls:

• Dropdown control
• Search control

The dropdown control opens or closes the predefined suggestion list.

The Search control executes the current search.

6.5 PREDEFINED GENRE LIST

The application includes a predefined list of common genres.

The list should include at least:

60s
70s
80s
90s
Alternative
Ambient
Americana
Big Band
Bluegrass
Blues
Brazilian
Celtic
Chill
Chillout
Christian
Classical
Classic Rock
College
Comedy
Country
Dance
Disco
Easy Listening
Electronic
Folk
Funk
Gospel
Hawaiian
Hip-Hop
House
Indie
Instrumental
Jazz
K-Pop
Latin
Lounge
Meditation
Metal
Motown
New Age
News
Oldies
Opera
Piano
Pop
Public
Punk
R&B
Reggae
Relaxation
Rock
Salsa
Show Tunes
Smooth Jazz
Soul
Soundtracks
Swing
Talk
Techno
Trance
World

The exact list may expand without changing the search architecture.

6.6 SUGGESTION FILTERING

When the user types text, suggestions are filtered.

Preferred matching order:

1. Prefix matches
2. Word-prefix matches
3. General contains matches

Prefix matches should appear before looser matches.

6.7 DROPDOWN HEIGHT

The suggestion dropdown should be tall enough to reduce unnecessary
scrolling.

It may use a substantial portion of the available screen height while
leaving enough context to recognize the search field.

6.8 DROPDOWN BEHAVIOR

The dropdown opens when:

• The dropdown icon is tapped
• The search field gains focus and suggestions are available
• The user types text, according to interface preference

The dropdown closes when:

• A suggestion is selected
• Search is executed
• The user taps outside the dropdown
• The field loses focus
• The dropdown icon is tapped while open

6.9 SUGGESTION SELECTION

Selecting a suggestion:

• Places the suggestion into the search field
• Executes the search immediately or prepares it for execution,
  according to the chosen interaction

The preferred behavior is immediate execution because genre
suggestions are explicit choices.

6.10 SEARCH BUTTON BEHAVIOR

When the Search button is pressed:

• Trim the current text
• Reject blank text
• Prefer an exact suggestion match when available
• Otherwise use the user's text
• Execute the search
• Create or refresh the Search Category
• Add or update the search chip
• Clear or retain the field according to the final interaction rule

The preferred final behavior is to clear the field after a successful
search so that the active category remains the primary confirmation.

6.11 KEYBOARD SEARCH ACTION

The software keyboard Search action performs the same operation as the
search icon.

There must not be separate logic for these two actions.

6.12 SEARCH CATEGORY NAME

The default category name is:

Search: <search term>

Example:

Search: Classical

Capitalization may be normalized for known genres.

Free-form searches should preserve a sensible display form.

6.13 DUPLICATE SEARCHES

Running a search that already has a Search Category should normally
refresh and activate that category rather than create a duplicate.

Matching should ignore insignificant differences such as:

• Capitalization
• Leading spaces
• Trailing spaces
• Repeated internal spaces

6.14 SEARCH EXECUTION FEEDBACK

While searching, the interface should show a compact busy state.

Examples:

Searching for Classical

Refreshing Search: Jazz

The current station may continue playing during the search.

6.15 SEARCH FAILURE

Search failure should not interrupt current playback.

The interface should:

• Preserve the previous active category
• Display an error message
• Allow retry
• Avoid creating an empty accidental category unless empty Search
  Categories are intentionally supported

6.16 EMPTY RESULTS

When a valid search returns no stations:

• The application may create an empty Search Category
• The category should show a clear empty state
• The user should be able to refresh or delete it

Recommended empty-state message:

No stations were found for this search.

6.17 SEARCH RESULT NORMALIZATION

Search results should be normalized before display.

Normalization may include:

• Trimming station names
• Removing exact duplicate URLs
• Normalizing genre text
• Normalizing location
• Selecting a preferred artwork URL
• Assigning stable identifiers

6.18 SEARCH RESULT ORDER

Initial search result order may follow:

• Directory relevance
• Popularity
• Reliability
• Bitrate
• Name

The user may later reorder copied stations in Standard or User-Defined
categories.

6.19 SEARCH CATEGORY REFRESH

A Search Category should retain:

• Original search text
• Normalized search key
• Last refresh date
• Search provider
• Optional provider-specific continuation information

Refresh behavior is defined further in Part 4.

6.20 SEARCH FIELD ACCESSIBILITY

The search field and controls require clear content descriptions.

Examples:

Search genres
Show genre suggestions
Search for stations

DESIGN NOTE

A repeated search is a listening habit.

By preserving searches as categories, Music1Chat allows discovery to
become part of routine hands-free navigation.

END OF CHAPTER 6

Related sections:

Chapter 10 — Search Chips
Chapter 11 — Category List
Part 4 — Search Persistence and Refresh

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec080 = """
══════════════════════════════════════════════════════════════════════

                        CHAPTER 7

                 CURRENT CATEGORY SECTION

══════════════════════════════════════════════════════════════════════

7.1 PURPOSE

The Current Category section identifies the category that controls
station navigation.

It provides an immediate answer to:

What group of stations am I listening within?

7.2 DISPLAY CONTENT

The Current Category card displays:

• Category name
• Category type when helpful
• Category Navigation indicator
• Optional station count
• Optional refresh state for Search Categories

7.3 CATEGORY NAME

Examples:

Classical
Bike Ride
Search: Swing

The category name should use strong typography.

7.4 CATEGORY TYPE PRESENTATION

The category type does not always need a separate label because Search
Categories are already identified by the Search prefix.

A subtle type label may be used on management screens.

7.5 NAVIGATION INDICATOR

The Current Category card displays whether the category is:

Navigation Enabled

or:

Navigation Disabled

The indicator may be interactive.

7.6 INDICATOR SIZE

The navigation indicator should be smaller and less visually dominant
than the category name.

It must remain large enough to tap reliably.

7.7 INDICATOR BEHAVIOR

Tapping the category navigation indicator toggles the category between:

Navigation Enabled
Navigation Disabled

Tapping the rest of the card opens the Category List screen.

These touch targets must not overlap ambiguously.

7.8 DIRECTLY PLAYING A DISABLED CATEGORY

The Current Category may be Navigation Disabled if the user selected it
directly.

This is valid.

The category remains skipped during Previous Category and Next
Category navigation.

7.9 STATION COUNT

An optional count may be shown.

Examples:

18 stations

5 of 18 available

The count must not clutter the primary category name.

7.10 SEARCH CATEGORY REFRESH STATE

A Search Category may display subtle states such as:

Refreshing
Updated today
Refresh available

These are secondary details.

7.11 OPENING CATEGORY LIST

Tapping the Current Category card opens the full Category List screen.

The selected category should be visually highlighted in that list.

7.12 EMPTY CURRENT CATEGORY

When the current category has no stations, the card remains visible.

The Now Playing area should display a no-station state rather than
showing stale station information.

7.13 NO CATEGORY STATE

When no category exists, the Current Category section should display a
clear call to action.

Recommended message:

Please search for a genre or station to begin listening.

The application may also provide a button to create a User-Defined
Category.

DESIGN NOTE

The Current Category is not merely a heading.

It defines the context in which Previous Station and Next Station
operate.

END OF CHAPTER 7

Related sections:

Chapter 11 — Category List
Chapter 12 — Category Details
Part 3 — Category Navigation

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec090 = """
══════════════════════════════════════════════════════════════════════

                        CHAPTER 8

                   NOW PLAYING SECTION

══════════════════════════════════════════════════════════════════════

8.1 PURPOSE

The Now Playing section displays the current station and its available
metadata.

It also provides direct access to station-level organization.

8.2 REQUIRED CONTENT

The section displays:

• Station name
• Artwork or placeholder
• Station membership Navigation indicator
• Copy action
• Playback or connection status when useful

8.3 STATION TITLE

The station title should receive the strongest emphasis in the card.

Long titles should use horizontal marquee behavior when the card is
visible and active.

The text must remain on one line when marquee is used.

8.4 MARQUEE TEXT

The marquee may combine useful information.

Example:

KUSC Classical • Classical radio • Live now • KUSC Classical

Repeated station text may be used to provide a smooth continuous
marquee loop.

The text should not become excessively long or distracting.

8.5 LIVE METADATA

When the stream provides metadata, the section may also display:

• Current song
• Artist
• Program
• Show title

Metadata should not replace the station identity entirely.

The user must still be able to tell which station is playing.

8.6 ARTWORK

Artwork may come from:

• Station directory
• Stream metadata
• Current-track metadata
• User-selected image
• Default placeholder

Artwork failure must not affect playback.

8.7 ARTWORK PLACEHOLDER

The placeholder should be visually intentional.

Possible placeholder content:

• Music note
• Radio symbol
• App logo
• Genre symbol

It should not appear as a broken-image error.

8.8 STATION NAVIGATION INDICATOR

The indicator represents the station membership in the Current
Category.

Toggling it changes navigation only for that membership.

The same station in another category is unaffected.

8.9 STATION COPY ACTION

The card includes a Copy action.

The visible label may initially remain Move if that wording is more
familiar, but the operation must behave as a copy.

Preferred label:

Copy

Acceptable transitional label:

Move / Copy

The specification uses Copy to Category.

8.10 OPENING STATION DETAILS

Tapping the station card may open:

• Station details
• Current category station list
• A compact action menu

The chosen behavior must not conflict with the Copy control or
navigation indicator.

8.11 PLAYBACK STATUS

Useful status text may include:

Connecting
Buffering
Playing
Stopped
Recovering
Stream unavailable

Status should be concise.

8.12 NO CURRENT STATION

When the Current Category contains no selected station, display:

No station selected

or:

No playable station in this category

The Play control should be disabled if no valid station can be chosen.

8.13 FAILED CURRENT STATION

A failed station may appear briefly with:

Stream unavailable

The recovery system should then advance automatically when playback was
requested.

8.14 CARD HEIGHT

The card should be compact enough to preserve room for primary
controls.

Artwork and title should not consume most of the screen.

8.15 ACCESSIBILITY

Controls require descriptions such as:

Enable this station for navigation
Disable this station from navigation
Copy station to categories
Open station details

DESIGN NOTE

The navigation indicator in the Now Playing card refers to the current
category membership, not to a universal property of the station.

END OF CHAPTER 8

Related sections:

Chapter 13 — Copy Station Workflow
Part 3 — Station Playback and Failure Recovery

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec100 = """
══════════════════════════════════════════════════════════════════════

                        CHAPTER 9

                PRIMARY CONTROL INTERACTIONS

══════════════════════════════════════════════════════════════════════

9.1 CONTROL SET

The primary control set contains:

• Previous Category
• Previous Station
• Play or Stop
• Next Station
• Next Category

9.2 PREVIOUS STATION

Previous Station selects the previous eligible station membership in
the Current Category.

It skips:

• Navigation Disabled memberships
• Session-failed stations when recovery rules require skipping

If playback is active, the selected station begins playing
immediately.

9.3 NEXT STATION

Next Station selects the next eligible station membership in the
Current Category.

If playback is active, the selected station begins playing
immediately.

9.4 PREVIOUS CATEGORY

Previous Category selects the previous eligible category.

After selecting the category, the application chooses an eligible
station within it.

If playback is active, playback begins immediately.

9.5 NEXT CATEGORY

Next Category selects the next eligible category.

If playback is active, playback begins immediately with an eligible
station from the newly selected category.

9.6 PLAY

When stopped, Play attempts to play:

• The Current Station, when valid

or:

• The first eligible station in the Current Category

or:

• An eligible station in another eligible category, when the
  current category cannot provide one and automatic recovery is
  appropriate

9.7 STOP

Stop ends active playback.

Stop does not clear:

• Current Category
• Current Station
• Category position
• Station position

Pressing Play later resumes by reloading the selected station.

9.8 TOGGLE

Bluetooth Play or Pause commands may map to a playback toggle.

The initial implementation may treat Pause as Stop for live radio
streams.

This avoids implying that a live stream can resume from the exact
paused position.

9.9 ACTIVE NAVIGATION

When playback is active:

• Station navigation selects and immediately plays
• Category navigation selects and immediately plays

The user should not need to press Play again after each navigation
action.

9.10 INACTIVE NAVIGATION

When playback is stopped:

• Navigation changes the selection
• Playback remains stopped
• The user can inspect the new selection
• Pressing Play starts the selected station

9.11 NO ELIGIBLE STATION

When a station-navigation command finds no eligible station:

• Do not loop indefinitely
• Preserve a stable selection when practical
• Show a clear message
• Offer direct access to category details

9.12 NO ELIGIBLE CATEGORY

When category navigation finds no eligible category:

• Do not continue cycling
• Show a no-category state
• Leave playback stopped if no valid source exists

9.13 TOUCH FEEDBACK

All primary controls should provide:

• Visual press indication
• Haptic feedback when appropriate
• Accessible labels
• Disabled styling when unavailable

9.14 REPEATED PRESSES

Rapid repeated navigation should not produce overlapping playback
loads.

A newer explicit user command should supersede an older pending load.

9.15 LONG PRESS

Long-press behavior is reserved for future use.

Possible future mappings include:

• Rapid station scanning
• Category navigation from headset controls
• Opening station details

No long-press action should be implemented accidentally or
inconsistently.

9.16 BUTTON SIZE

Each primary control should meet or exceed normal accessibility touch
target guidance.

The center Play or Stop control should be substantially larger than the
four navigation controls.

9.17 BUTTON SHAPE

Recommended shapes:

• Circular Play or Stop
• Rounded or circular navigation controls

The category controls may use a subtly different container shape to
distinguish them from station controls.

9.18 BUTTON LABELING

Icons are preferred to full text on the Main Screen.

Content descriptions must identify the complete action:

Previous category
Previous station
Play
Stop
Next station
Next category

DESIGN NOTE

The difference between single and double chevrons must remain obvious
even when the user glances at the screen briefly.

Spacing is as important as icon design.

END OF CHAPTER 9

Related sections:

Part 3 — Detailed Navigation Rules
Part 3 — Bluetooth Commands
Part 3 — Failure Recovery

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec110 = """
══════════════════════════════════════════════════════════════════════

                       CHAPTER 10

                      SEARCH CHIPS

══════════════════════════════════════════════════════════════════════

10.1 PURPOSE

Search chips provide fast access to recent searches.

They reduce repeated typing and allow recurring searches to become part
of the user's routine.

10.2 PLACEMENT

Search chips appear directly below the search field.

They use a horizontally scrollable row.

10.3 ORDER

The newest search appears at the left.

Older searches extend to the right.

10.4 CHIP LABEL

The chip may display:

Classical
Jazz
Hawaiian

The Search prefix is not required inside the chip because the chip's
location already implies search.

10.5 CHIP TAP

Tapping a chip:

• Activates the corresponding Search Category
• Refreshes it first when required by refresh policy
• Updates the Current Category
• Preserves stopped state when playback is stopped
• Begins playback when playback is active and a valid station can
  be selected

10.6 DUPLICATE CHIPS

A repeated search should move the existing chip to the left rather than
create a duplicate.

10.7 CHIP LIMIT

A configurable or fixed maximum may be used.

A reasonable initial limit is:

10 to 20 recent searches

Older chips may be removed from the chip row without deleting the
associated Search Category.

10.8 CHIP DELETION

Chip deletion and Search Category deletion are separate operations.

Removing a chip may remove only the shortcut.

Deleting a Search Category removes the category itself.

10.9 LONG PRESS

A long press may open chip actions such as:

• Remove shortcut
• Refresh search
• Open category
• Delete Search Category

This is optional for the initial version.

10.10 EMPTY CHIP ROW

When no search history exists, the chip row may be hidden.

It should not reserve unnecessary vertical space.

10.11 PERSISTENCE

Search-chip order and membership should persist across application
restarts.

10.12 ACCESSIBILITY

Each chip should have a description such as:

Open Classical search category

DESIGN NOTE

Search chips are shortcuts.

Search Categories are persistent content containers.

They are related but not identical concepts.

END OF CHAPTER 10

Related sections:

Chapter 6 — Search System
Chapter 11 — Category List
Part 4 — Persistence

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec120 = """
══════════════════════════════════════════════════════════════════════

                       CHAPTER 11

                  CATEGORY LIST SCREEN

══════════════════════════════════════════════════════════════════════

11.1 PURPOSE

The Category List screen displays all categories and provides
category-level management.

It is opened by tapping the Current Category card.

11.2 INCLUDED CATEGORY TYPES

The list includes:

• Standard Categories
• Search Categories
• User-Defined Categories

All three types participate in the same browsing model.

11.3 ROW CONTENT

Each category row displays:

• Category name
• Category type or type icon
• Navigation indicator
• Open-details action
• Delete action when allowed
• Optional station count

11.4 CURRENT CATEGORY HIGHLIGHT

The Current Category should be visually highlighted.

The highlight must remain visible in both light and dark themes.

11.5 ROW TAP

Tapping the main area of a row:

• Selects that category
• Returns to the Main Screen or opens details according to the
  chosen interaction model

Preferred behavior:

• Tap row to select and return
• Tap list or details icon to open Category Details

11.6 NAVIGATION TOGGLE

Tapping the Navigation indicator toggles the category's eligibility for
normal category navigation.

This action does not automatically select the category.

11.7 DETAILS ACTION

The details action opens the Category Details screen.

A list-style icon is appropriate.

11.8 DELETE ACTION

A delete icon appears only when category deletion is permitted.

Deleting a category removes:

• The category record
• Its membership records
• Related chip when appropriate

It does not automatically delete global station records still used by
other categories.

11.9 STANDARD CATEGORY DELETION

Recommended Version 2.0 rule:

Standard Categories cannot be permanently deleted.

They may be:

• Navigation Disabled
• Hidden through a future setting
• Restored to defaults

This preserves known organizational destinations.

11.10 SEARCH CATEGORY DELETION

Search Categories may be deleted.

Deletion should remove:

• Search Category
• Search result memberships
• Related search chip

Copied stations in other categories remain unaffected.

11.11 USER-DEFINED CATEGORY DELETION

User-Defined Categories may be deleted.

A confirmation or Undo action should be provided.

11.12 CREATE CATEGORY ACTION

The screen includes a Create Category action.

This may appear as:

• Floating action button
• Top-bar action
• Add row

It opens the Create Category dialog.

11.13 CATEGORY ORDER

The Category List reflects the persistent category order used for
navigation.

The user should eventually be able to reorder categories by drag and
drop.

11.14 DRAG HANDLE

A drag handle may appear for editable ordering.

Long-pressing and dragging a row is also acceptable.

11.15 SEARCH AND FILTER

A category filter may be added when the list becomes large.

This is optional for the initial version.

11.16 EMPTY LIST

When no categories exist, display:

Please search for a genre or station to begin listening.

Also provide:

• Search action
• Create Category action

11.17 RETURN BEHAVIOR

Returning from the Category List preserves:

• Current playback
• Current station
• Scroll position when practical

DESIGN NOTE

Category management should not stop playback.

The user should be able to organize content while continuing to listen.

END OF CHAPTER 11

Related sections:

Chapter 12 — Category Details
Chapter 14 — Category Creation and Editing
Part 4 — Persistence

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec130 = """
══════════════════════════════════════════════════════════════════════

                       CHAPTER 12

                CATEGORY DETAILS SCREEN

══════════════════════════════════════════════════════════════════════

12.1 PURPOSE

The Category Details screen displays all stations in one category and
provides membership-level management.

12.2 HEADER

The header displays:

• Category name
• Category type
• Category Navigation state
• Station count
• Refresh action for Search Categories
• Edit action where allowed

12.3 STATION ROW CONTENT

Each station row displays:

• Station name
• Optional artwork
• Optional genre or location
• Station membership Navigation indicator
• Copy action
• Delete action when allowed
• Reorder handle when allowed

12.4 STATION TAP

Tapping a station row:

• Sets the category as Current Category
• Sets the station as Current Station
• Begins playback immediately when playback is already active
• Otherwise updates the selection without starting playback

The screen may return to Main after selection.

12.5 MEMBERSHIP NAVIGATION TOGGLE

Tapping the station Navigation indicator changes only that membership.

The same station in other categories is unaffected.

12.6 COPY ACTION

The Copy action opens the Copy Station dialog.

The source membership remains unchanged.

12.7 DELETE ACTION

Deleting a station from a category removes only that membership.

The action is allowed in:

• Standard Categories, if user-added or removable entries are
  supported
• User-Defined Categories

The action is normally disabled in Search Categories.

12.8 SEARCH CATEGORY DELETE RESTRICTION

Individual search-result memberships should not normally be deleted.

The reason is that refresh may recreate them.

The user may:

• Disable navigation for the result
• Copy it elsewhere
• Delete the entire Search Category

12.9 REORDERING

Station order may be changed in:

• Standard Categories, where customization is allowed
• User-Defined Categories

Search Category order normally follows search result order and is not
manually rearranged.

12.10 DRAG BEHAVIOR

The preferred reordering interaction is:

• Long press or press drag handle
• Drag station to new position
• Persist the new order immediately

Playback should continue during reordering.

12.11 CURRENT STATION HIGHLIGHT

The Current Station should be highlighted when viewing its Current
Category.

The highlight may include:

• Background emphasis
• Playing icon
• Animated indicator
• Text emphasis

12.12 FAILED STATION DISPLAY

A session-failed station may show a temporary indicator.

Examples:

Failed this session
Stream unavailable

This status should clear on a new session.

12.13 EMPTY CATEGORY

An empty User-Defined or Standard Category should show:

No stations in this category.

Suggested actions:

• Search for stations
• Copy stations from another category
• Delete category when permitted

12.14 EMPTY SEARCH CATEGORY

An empty Search Category should show:

No stations were found for this search.

Suggested actions:

• Refresh
• Edit or rerun search
• Delete Search Category

12.15 CATEGORY EDITING

The header edit action may allow:

• Rename
• Change Navigation state
• Reorder category
• Reset Standard Category
• Delete where permitted

12.16 BACK NAVIGATION

Leaving Category Details preserves playback and current selection.

DESIGN NOTE

The Category Details screen manages station memberships.

It is not a separate station database editor.

END OF CHAPTER 12

Related sections:

Chapter 13 — Copy Station Workflow
Chapter 14 — Category Creation and Editing
Part 3 — Station Navigation

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec140 = """
══════════════════════════════════════════════════════════════════════

                       CHAPTER 13

                COPY STATION WORKFLOW

══════════════════════════════════════════════════════════════════════

13.1 PURPOSE

The Copy Station workflow adds one station to one or more destination
categories.

The source category remains unchanged.

13.2 TERMINOLOGY

Earlier designs used the word Move.

The actual operation is Copy.

The preferred user-facing label is:

Copy to Categories

A shorter Main Screen label may be:

Copy

13.3 DIALOG CONTENT

The dialog displays:

• Station name
• Create New Category option
• Existing category list
• Selection control for each category
• Suggested destination
• Cancel action
• Copy action

13.4 MULTIPLE DESTINATIONS

The user may select more than one destination.

Example:

Classical
Bike Ride
Morning Coffee

One membership is created in each selected category.

13.5 DEFAULT DESTINATION

When copying from a Search Category, the application should suggest the
matching Standard Category when one exists.

Example:

Source:
    Search: Classical

Suggested destination:
    Classical

13.6 CREATE NEW CATEGORY

The first option in the destination list should be:

Create New Category...

Selecting it opens the Create Category dialog.

After creation:

• The new category is selected as a destination
• The user returns to the Copy dialog or completes the copy

13.7 EXISTING MEMBERSHIP

If the station already belongs to a selected destination category:

• Do not create a duplicate membership
• Show that it is already included
• Leave the existing order and navigation state unchanged unless
  the user explicitly changes them

13.8 DEFAULT MEMBERSHIP NAVIGATION STATE

A newly copied membership should default to:

Navigation Enabled

A future setting may allow another default.

13.9 SOURCE MEMBERSHIP

The source membership remains:

• In the source category
• In its original order
• With its original navigation state

13.10 SUCCESS FEEDBACK

After copying, show concise feedback.

Example:

KUSC Classical copied to 2 categories.

13.11 NO DESTINATION

The Copy action is disabled until at least one valid destination is
selected.

13.12 COPYING FROM USER-DEFINED CATEGORIES

Copying is allowed from all category types.

It always preserves the source membership.

13.13 COPYING GLOBAL STATION DATA

The station record should be reused.

Only new membership records are created.

13.14 ERROR HANDLING

If one destination fails while others succeed:

• Preserve successful copies
• Report the partial failure
• Avoid duplicating successful copies during retry

DESIGN NOTE

Copying is safer and more understandable than a true move because it
does not unexpectedly remove a station from the place where the user
found it.

END OF CHAPTER 13

Related sections:

Chapter 12 — Category Details
Chapter 14 — Category Creation
Part 4 — Persistence

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec150 = """
══════════════════════════════════════════════════════════════════════

                       CHAPTER 14

           CATEGORY CREATION, EDITING, AND DELETION

══════════════════════════════════════════════════════════════════════

14.1 CREATE CATEGORY DIALOG

The Create Category dialog contains:

• Category name field
• Navigation Enabled toggle
• Create action
• Cancel action

14.2 CATEGORY NAME VALIDATION

A valid category name:

• Is not blank
• Is trimmed
• Does not duplicate another category name under normalized
  comparison
• Does not use reserved internal identifiers

Display names may include spaces and normal punctuation.

14.3 DEFAULT NAVIGATION STATE

A new User-Defined Category should default to:

Navigation Enabled

14.4 DUPLICATE NAME

When a duplicate name is entered, the dialog should explain:

A category with this name already exists.

14.5 CASE COMPARISON

Category-name duplicate checking should ignore case.

Example:

Bike Ride
bike ride

These should be treated as the same name.

14.6 CREATE FROM COPY WORKFLOW

When a category is created from the Copy Station dialog:

• Create the category
• Add it to the category list
• Copy the station into it
• Preserve the original source membership

14.7 EDIT CATEGORY

Editable properties may include:

• Name
• Navigation state
• Order
• Search refresh preference, for Search Categories
• Future artwork or color

14.8 STANDARD CATEGORY EDITING

Recommended rules:

• Navigation state may be changed
• Order may be changed
• Station membership may be customized where allowed
• Name is normally fixed
• Permanent deletion is disabled

14.9 SEARCH CATEGORY EDITING

Recommended rules:

• Navigation state may be changed
• Category may be deleted
• Search may be refreshed
• Search term may be rerun
• Manual rename is normally disabled or limited

14.10 USER-DEFINED CATEGORY EDITING

The user may:

• Rename
• Reorder
• Change Navigation state
• Add stations
• Remove stations
• Delete category

14.11 DELETE CONFIRMATION

Deletion confirmation should identify the category.

Example:

Delete "Bike Ride"?

Stations copied to other categories will not be removed.

14.12 UNDO

When practical, category deletion should provide an Undo action.

The underlying data may be retained temporarily until the Undo period
expires.

14.13 DELETING CURRENT CATEGORY

If the Current Category is deleted:

• Stop playback if the current station no longer has an active
  context

or:

• Select the next eligible category and continue when playback is
  active

The preferred behavior is:

• Select the next eligible category
• Continue playback when possible
• Stop only when no eligible destination exists

14.14 RESET STANDARD CATEGORY

A Standard Category may offer:

Reset to defaults

Reset may restore:

• Default station memberships
• Default order
• Default Navigation state

The action requires confirmation.

14.15 CATEGORY COLOR OR ARTWORK

Custom category color or artwork may be added later.

It is not required for Version 2.0.

DESIGN NOTE

Category deletion removes organization.

It should not destroy station records that remain useful elsewhere.

END OF CHAPTER 14

Related sections:

Chapter 11 — Category List
Chapter 12 — Category Details
Part 4 — Persistence

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec160 = """
══════════════════════════════════════════════════════════════════════

                       CHAPTER 15

             NAVIGATION INDICATOR DESIGN

══════════════════════════════════════════════════════════════════════

15.1 PURPOSE

The Navigation indicator communicates whether an item participates in
ordinary sequential navigation.

15.2 SUPPORTED ITEMS

The indicator is used for:

• Categories
• Station memberships

15.3 ENABLED STATE

Navigation Enabled means:

• Category is included in Previous and Next Category
• Station membership is included in Previous and Next Station
• Item may participate in automatic recovery selection

15.4 DISABLED STATE

Navigation Disabled means:

• Item remains visible
• Item remains directly selectable
• Item is skipped during sequential navigation

15.5 ICON REQUIREMENTS

The icon should:

• Be understandable after brief use
• Have distinct enabled and disabled appearances
• Avoid relying only on color
• Work in light and dark themes
• Remain legible at small sizes
• Support accessibility descriptions

15.6 POSSIBLE VISUAL STATES

Enabled may use:

• Filled navigation symbol
• Strong outline
• Check or route marker
• Higher visual emphasis

Disabled may use:

• Outline-only symbol
• Slash or exclusion mark
• Lower emphasis
• Empty interior

15.7 HEART ICON

The heart icon should not be used as the primary navigation control.

It implies preference rather than navigation behavior.

15.8 CONTENT DESCRIPTIONS

Examples:

Category included in navigation
Category excluded from navigation
Station included in navigation for this category
Station excluded from navigation for this category

15.9 TOGGLE FEEDBACK

Toggling should provide immediate visual feedback.

Optional feedback:

Included in navigation

Excluded from navigation

15.10 SIZE

The indicator should be smaller than primary playback controls.

It should still meet minimum touch-target guidance through padding.

15.11 STATION CONTEXT

Station indicator descriptions should emphasize that the state applies
to the Current Category.

15.12 CATEGORY CONTEXT

Category indicator descriptions should emphasize category-level
navigation.

DESIGN NOTE

Navigation state describes participation in a route through content.

It does not express emotional preference.

END OF CHAPTER 15

Related sections:

Chapter 7 — Current Category
Chapter 8 — Now Playing
Chapter 11 — Category List
Chapter 12 — Category Details

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec169 = """
══════════════════════════════════════════════════════════════════════

                  END OF SPECIFICATION PART 2

══════════════════════════════════════════════════════════════════════

FILE

DesignSpecificationPart2.kt

CONTENTS

• Chapter 5 — Main Screen Design
• Chapter 6 — Search System
• Chapter 7 — Current Category Section
• Chapter 8 — Now Playing Section
• Chapter 9 — Primary Control Interactions
• Chapter 10 — Search Chips
• Chapter 11 — Category List Screen
• Chapter 12 — Category Details Screen
• Chapter 13 — Copy Station Workflow
• Chapter 14 — Category Creation, Editing, and Deletion
• Chapter 15 — Navigation Indicator Design

NEXT FILE

DesignSpecificationPart3.kt

PART 3 WILL DEFINE

• Station data and membership behavior
• Playback controller behavior
• Play and stop semantics
• Station navigation
• Category navigation
• Bluetooth and media-button commands
• Automatic station failure detection
• Session failure tracking
• Category exhaustion
• Recovery loops
• Playback status and user feedback

══════════════════════════════════════════════════════════════════════
""".trimIndent()