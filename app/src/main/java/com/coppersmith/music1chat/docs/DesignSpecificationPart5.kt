package com.coppersmith.music1chat.docs


@Suppress("unused")
val designSpec290 = """
══════════════════════════════════════════════════════════════════════

        MUSIC1CHAT DESIGN SPECIFICATION — VERSION 2.0 — PART 5        

══════════════════════════════════════════════════════════════════════

PART 5 PURPOSE

This file completes Version 2.0 with:

    • Developer implementation guidance
    • Testing guidance
    • Future enhancements
    • Design rationale
    • Glossary
    • Core workflows
    • Acceptance criteria
    • Final design principles

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec300 = """
══════════════════════════════════════════════════════════════════════

             CHAPTER 26 — DEVELOPER IMPLEMENTATION NOTES              

══════════════════════════════════════════════════════════════════════

26.1 SPECIFICATION AUTHORITY

Code should implement this specification deliberately.

When behavior changes, update both code and specification.

26.2 RECOMMENDED PACKAGE AREAS

Possible packages:

    ui
    ui.components
    ui.screens
    model
    data
    data.local
    data.search
    playback
    navigation
    media
    cast
    settings
    util

26.3 COMPOSABLE RESPONSIBILITY

Composable functions should:

    • Render state
    • Emit user intents
    • Avoid owning business rules
    • Avoid direct repository writes
    • Remain previewable where practical

26.4 VIEW MODEL

The Main ViewModel or coordinated view models should expose immutable
UI state and receive commands.

26.5 UI STATE

A MainUiState may include:

    categories
    currentCategory
    currentMembership
    currentStation
    playbackState
    searchText
    suggestions
    chips
    statusMessage
    outputDestination
    dialogs

26.6 EVENTS

Use explicit events such as:

    SearchTextChanged
    SearchSubmitted
    CategorySelected
    CategoryNavigationToggled
    StationSelected
    StationNavigationToggled
    CopyStationRequested
    PlaybackToggled
    NextStationRequested
    NextCategoryRequested

26.7 AVOID DUPLICATE LOGIC

Do not implement separate navigation rules in:

    • Buttons
    • Bluetooth callback
    • Notification
    • Category screen

All should call shared commands.

26.8 IMMUTABILITY

Prefer immutable public state.

26.9 COROUTINES

Use structured concurrency.

Cancel obsolete search and playback jobs.

26.10 ERROR MODEL

Use typed application errors where useful.

Do not expose raw exception text directly to users.

26.11 LOGGING

Development logs should be meaningful and searchable.

A consistent tag such as KenCheck may be used during development.

26.12 DEPENDENCY INJECTION

Constructor injection is preferred.

A formal dependency-injection framework may be added when complexity
justifies it.

26.13 TESTABLE NAVIGATION

Keep navigation selection logic independent from Compose and ExoPlayer.

26.14 DATABASE TRANSACTIONS

Use transactions for multi-record organization operations.

26.15 ICONS

Use Material icons where they clearly match the intended meaning.

Custom navigation artwork may be introduced when Material icons are
insufficient.

26.16 CONTENT DESCRIPTIONS

Every actionable icon requires a useful description.

26.17 PREVIEWS

Compose previews should cover:

    • Playing
    • Stopped
    • Empty category
    • Search dropdown
    • Light theme
    • Dark theme

26.18 BUILD CONFIGURATION

Development data, verbose logging, and experimental features should be
controlled through build configuration or clear source boundaries.

26.19 VERSION CONTROL

Commit cohesive changes.

Specification changes should accompany behavior changes.

26.20 COMMENTS

Comments should explain why, not repeat obvious code.

26.21 NAMING

Prefer complete behavioral names:

    includedInNavigation
    currentCategory
    copyStationToCategories
    advanceToNextPlayableStation

Avoid ambiguous names such as:

    selected
    active
    flag

when a more precise name exists.

26.22 PERFORMANCE

Avoid unnecessary recomposition and repeated database loads.

Playback reliability has priority over decorative animation.

26.23 PRIVACY

Do not transmit user-created categories or listening history unless a
feature explicitly requires it and the user understands the behavior.

26.24 SECURITY

Validate imported data, remote commands, URLs, and receiver messages.

26.25 DOCUMENT FILES

The five DesignSpecificationPart files are documentation containers.

They do not need to be referenced by production code.

A future project structure may place them under a dedicated
documentation source set.

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec310 = """
══════════════════════════════════════════════════════════════════════

                 CHAPTER 27 — TESTING AND ACCEPTANCE                  

══════════════════════════════════════════════════════════════════════

27.1 TESTING LEVELS

Testing should include:

    • Unit tests
    • Repository tests
    • ViewModel tests
    • Compose UI tests
    • Player integration tests
    • Device tests
    • Bluetooth hardware tests
    • Cast tests

27.2 NAVIGATION UNIT TESTS

Cover:

    • Enabled and disabled categories
    • Enabled and disabled memberships
    • Wraparound
    • Empty lists
    • One-item lists
    • Session failures
    • Manual override
    • Reordering

27.3 SEARCH TESTS

Cover:

    • Blank query
    • Exact suggestion
    • Prefix suggestion
    • Duplicate normalized query
    • Empty result
    • Provider failure
    • Stale result cancellation
    • Refresh preserving navigation state

27.4 COPY TESTS

Cover:

    • One destination
    • Several destinations
    • Existing membership
    • New category
    • Partial failure
    • Source preservation

27.5 PERSISTENCE TESTS

Cover fresh install, restore, migration, deletion, reorder, and process
recreation.

27.6 PLAYBACK TESTS

Cover:

    • Valid stream
    • Invalid URL
    • Immediate error
    • Timeout
    • Stop during connect
    • Next during connect
    • Stale callback
    • Background playback

27.7 BLUETOOTH TESTS

Use real devices.

Verify screen-off behavior.

27.8 THEME TESTS

Verify actual response to device light and dark modes.

27.9 ACCESSIBILITY TESTS

Check:

    • Touch target size
    • Screen-reader labels
    • Font scaling
    • Color independence
    • Focus order

27.10 ACCEPTANCE CRITERIA — MAIN SCREEN

A user can identify category, station, playback state, and five primary
controls without opening a menu.

27.11 ACCEPTANCE CRITERIA — SEARCH

A user can search a genre, obtain a persistent Search Category, and
reuse it from a chip.

27.12 ACCEPTANCE CRITERIA — NAVIGATION

Sequential navigation skips disabled items and respects per-category
membership state.

27.13 ACCEPTANCE CRITERIA — PLAYBACK

Next Station and Next Category begin playback automatically when
playback was already active.

27.14 ACCEPTANCE CRITERIA — FAILURE RECOVERY

A failed station is skipped automatically and does not trap the user
in silence.

27.15 ACCEPTANCE CRITERIA — PERSISTENCE

After restart, organization and current selection are restored.

27.16 ACCEPTANCE CRITERIA — THEME

Changing the system between light and dark changes the application when
System Default is selected.

27.17 ACCEPTANCE CRITERIA — COPY

Copying a station creates destination memberships without removing the
source membership.

27.18 ACCEPTANCE CRITERIA — EXIT

Explicit Exit stops playback and releases application media resources.

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec320 = """
══════════════════════════════════════════════════════════════════════

                   CHAPTER 28 — FUTURE ENHANCEMENTS                   

══════════════════════════════════════════════════════════════════════

28.1 PRINCIPLE

Future features should support the mission without overwhelming the
Main Screen.

28.2 POSSIBLE CONTENT SOURCES

    • Local MP3 folders
    • Local playlists
    • Podcasts
    • Audiobooks
    • Apple Music, where permitted
    • Spotify, where permitted
    • YouTube Music, where permitted
    • SiriusXM, where permitted

28.3 AUTOMOTIVE SUPPORT

    • Android Auto
    • Apple CarPlay
    • Voice-first category selection
    • Simplified driving interface

28.4 WEARABLE SUPPORT

    • Wear OS remote
    • Playback toggle
    • Station navigation
    • Category navigation
    • Current station display

28.5 VOICE COMMANDS

Examples:

    Play Classical
    Next station
    Next category
    Stop music
    Play Bike Ride

28.6 LOCAL LIBRARY

Local folders and playlists may enter the same category model.

28.7 RELIABILITY HISTORY

A future reliability score may prioritize stable stations.

28.8 SLEEP TIMER

A sleep timer may stop playback after:

    • Fixed duration
    • End of program
    • User-selected clock time

28.9 RANDOM MODE

A secondary random or shuffle action may choose:

    • Random eligible station
    • Random eligible category
    • Weighted station based on history

It should not replace predictable Previous and Next.

28.10 IMPORT AND SHARING

Users may export or share categories.

28.11 CLOUD SYNC

Optional sync may keep categories consistent across devices.

28.12 DESKTOP COMPANION

A desktop application may organize categories and control playback.

28.13 TV INTERFACE

A TV interface may show large artwork, current metadata, and remote
navigation.

28.14 MULTIROOM

Future playback may target multiple synchronized receivers.

28.15 USER METADATA

Possible additions:

    • Notes
    • Ratings
    • Custom artwork
    • Tags
    • Per-category volume

28.16 ADVANCED SEARCH

Possible filters:

    • Country
    • Language
    • Codec
    • Bitrate
    • Popularity
    • Reliability

28.17 DISCOVERY PROVIDERS

Search architecture may support multiple directories.

28.18 OFFLINE CONTENT

Local content can remain available without network access.

28.19 ANALYTICS

Any analytics must be privacy-respecting and optional where required.

28.20 FEATURE TEST

Every future feature should answer:

    Does this improve safe, reliable, low-attention listening?

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec330 = """
══════════════════════════════════════════════════════════════════════

                    CHAPTER 29 — DESIGN RATIONALE                     

══════════════════════════════════════════════════════════════════════

29.1 WHY HANDS-FREE FIRST

The application originated from a real need to control listening while
bicycling.

Designing for the hardest safe-use case improves ordinary use.

29.2 WHY CATEGORIES

Categories match how people think about listening contexts.

29.3 WHY SEARCH CATEGORIES PERSIST

A repeated search often represents a stable listening interest.

Temporary search would force unnecessary repetition.

29.4 WHY NAVIGATION REPLACED FAVORITES

Favorites express affection.

Navigation Enabled expresses operational participation.

The application needed the second concept.

29.5 WHY STATION NAVIGATION IS PER CATEGORY

The same station may be appropriate in one context and inappropriate in
another.

29.6 WHY DISABLED ITEMS REMAIN SELECTABLE

Navigation Disabled should reduce sequential clutter without hiding
content.

29.7 WHY COPY INSTEAD OF MOVE

Copy avoids surprising removal and supports one station in several
listening contexts.

29.8 WHY AUTOMATIC FAILURE RECOVERY

A cyclist should not need to retrieve a phone because a remote stream
failed.

29.9 WHY FAILURE IS SESSION-BASED

Internet radio failures are often temporary.

One failure should not permanently condemn a station.

29.10 WHY LARGE CONTROLS

Large controls reduce visual demand and accidental presses.

29.11 WHY SINGLE AND DOUBLE CHEVRONS

The visual relationship communicates station versus category movement
without long labels.

29.12 WHY STOP RATHER THAN PAUSE

Live radio cannot normally resume from the same moment.

Stop is more honest.

29.13 WHY SEARCH DOES NOT STOP PLAYBACK

Discovery and organization should not interrupt listening.

29.14 WHY THE SPECIFICATION IS KOTLIN

Multiline strings are displayed clearly in Android Studio and remain
valid Kotlin source.

29.15 WHY FIVE FILES

Five files are manageable to review, edit, and transfer while still
forming one master specification.

29.16 WHY THE SPECIFICATION IS AUTHORITATIVE

A behavioral product needs durable decisions that survive beyond the
current code and conversation.

29.17 WHY SETTINGS ARE LIMITED

Strong defaults keep the app understandable.

29.18 WHY CASTING SHARES NAVIGATION STATE

Changing speakers should not create a second product model.

29.19 WHY NO AUTOPLAY BY DEFAULT

Opening the app and beginning audio are distinct intentions.

29.20 WHY USER EXPERIENCE PRECEDES IMPLEMENTATION

Technology changes.

The intended listening behavior should remain understandable and
stable.

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec340 = """
══════════════════════════════════════════════════════════════════════

                               GLOSSARY                               

══════════════════════════════════════════════════════════════════════

Application Command
    A platform-independent instruction such as NEXT_STATION.

Category
    An ordered collection of station memberships.

CategoryStationMembership
    The relationship between one Station and one Category, including
    order and Navigation Enabled state.

Current Category
    The category that defines station-navigation context.

Current Station
    The selected station within Current Category.

Eligible Category
    A category available for sequential navigation or recovery.

Eligible Station
    A station membership available for sequential navigation or
    recovery.

Navigation Disabled
    Visible and directly selectable, but skipped during sequential
    navigation.

Navigation Enabled
    Included in sequential navigation.

Playback Destination
    The device or receiver rendering audio.

Search Category
    A persistent category produced from a station search.

Search Chip
    A shortcut to a recent or saved search.

Session Failure
    A temporary record that a station failed during the current
    application session.

Standard Category
    An application-supplied category.

Station
    A global playable audio-source record.

User-Defined Category
    A category created and controlled by the user.

Wraparound
    Returning from the end of an eligible sequence to its beginning.

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec350 = """
══════════════════════════════════════════════════════════════════════

                     APPENDIX A — CORE WORKFLOWS                      

══════════════════════════════════════════════════════════════════════

A.1 SEARCH AND PLAY

    1. User enters Classical.
    2. Search executes.
    3. Search: Classical is created or refreshed.
    4. Search chip moves to the left.
    5. Search Category becomes Current Category.
    6. An eligible station becomes Current Station.
    7. If playback was active, the station plays immediately.

A.2 NEXT STATION WHILE PLAYING

    1. User presses Next Station or headset Next.
    2. Navigation Engine finds the next eligible membership.
    3. Current Station changes.
    4. Playback Controller replaces the stream.
    5. Main Screen updates.
    6. Selection persists.

A.3 NEXT CATEGORY WHILE PLAYING

    1. User presses Next Category.
    2. Navigation Engine skips disabled categories.
    3. It selects the next eligible category.
    4. It selects an eligible station.
    5. Playback begins immediately.

A.4 COPY FROM SEARCH

    1. User opens Copy for a search result.
    2. Matching Standard Category is suggested.
    3. User selects Classical and Bike Ride.
    4. Two memberships are created.
    5. Search membership remains unchanged.

A.5 STATION FAILURE

    1. Play request begins.
    2. Stream errors or times out.
    3. Station is marked failed for session.
    4. Next eligible membership is selected.
    5. Playback continues.
    6. Compact feedback is shown.

A.6 CATEGORY EXHAUSTION

    1. Every eligible station in Current Category fails.
    2. Category is exhausted for the recovery pass.
    3. Next eligible category is selected.
    4. Playback continues there.
    5. User receives a compact message.

A.7 EXPLICIT STOP

    1. User presses Stop.
    2. Playback and recovery stop.
    3. Current selection remains.
    4. Later Play retries the selected context.

A.8 EXPLICIT EXIT

    1. User presses Exit.
    2. Playback stops.
    3. Remote playback is ended according to policy.
    4. State persists.
    5. Media service and resources release.
    6. UI closes.

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec360 = """
══════════════════════════════════════════════════════════════════════

                 APPENDIX B — FINAL DESIGN PRINCIPLES                 

══════════════════════════════════════════════════════════════════════

The final Version 2.0 principles are:

    • Hands-free use is primary.
    • Safety outranks visual density.
    • Playback should recover automatically when possible.
    • Categories define listening context.
    • Navigation state describes behavior, not affection.
    • Station navigation state belongs to category membership.
    • Disabled items remain visible and directly selectable.
    • Search results become persistent categories.
    • Copying preserves the source.
    • All control surfaces share one command path.
    • User organization persists.
    • Opening the app does not imply autoplay.
    • Casting changes destination, not organization.
    • The interface must work in light and dark themes.
    • Primary controls remain large and direct.
    • Automatic behavior must be bounded and understandable.
    • The specification and implementation must remain synchronized.

MISSION TEST

For every proposed feature, ask:

    Can the user continue enjoying audio without needing to look at or
    touch the phone?

When the answer is yes, the feature supports Music1Chat's mission.

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec369 = """
══════════════════════════════════════════════════════════════════════

          END OF MUSIC1CHAT DESIGN SPECIFICATION VERSION 2.0          

══════════════════════════════════════════════════════════════════════

FILES

    DesignSpecificationPart1.kt
    DesignSpecificationPart2.kt
    DesignSpecificationPart3.kt
    DesignSpecificationPart4.kt
    DesignSpecificationPart5.kt

STATUS

    Version 2.0 complete.

Future changes should be entered in the Revision History and reflected
in the affected chapters.

══════════════════════════════════════════════════════════════════════
""".trimIndent()