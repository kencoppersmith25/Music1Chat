package com.coppersmith.music1chat.docs

@Suppress("unused")
val designSpec230 = """
══════════════════════════════════════════════════════════════════════

        MUSIC1CHAT DESIGN SPECIFICATION — VERSION 2.0 — PART 4        

══════════════════════════════════════════════════════════════════════

PART 4 PURPOSE

This file defines:

    • Persistent storage
    • Data restoration
    • Settings
    • Casting and external playback
    • Light, dark, and dynamic themes
    • Startup behavior
    • Shutdown and Exit behavior
    • Background playback
    • Import, export, and recovery considerations

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec240 = """
══════════════════════════════════════════════════════════════════════

                       CHAPTER 21 — PERSISTENCE                       

══════════════════════════════════════════════════════════════════════

21.1 PURPOSE

Persistence makes Music1Chat return in a familiar state.

21.2 PERSISTENT DATA

The application should persist:

    • Categories
    • Category type
    • Category order
    • Category Navigation Enabled state
    • Stations
    • Memberships
    • Membership order
    • Membership Navigation Enabled state
    • Search Categories
    • Search query information
    • Search-chip order
    • Current Category
    • Current Station
    • Settings
    • User-defined names
    • Relevant cast preferences

21.3 NONPERSISTENT DATA

The following are normally session-only:

    • Failed-station set
    • Active recovery pass
    • Temporary snackbar messages
    • Current search dropdown state
    • Transient loading flags

21.4 STORAGE TECHNOLOGY

Recommended Android storage:

    • Room for relational category, station, and membership data
    • DataStore for settings and compact preferences

21.5 DATABASE ENTITIES

Recommended entities include:

    CategoryEntity
    StationEntity
    CategoryStationEntity
    SearchChipEntity
    SearchMetadataEntity

21.6 CATEGORY ENTITY

Fields may include:

    id
    name
    normalizedName
    type
    sortPosition
    navigationEnabled
    createdAt
    updatedAt

21.7 STATION ENTITY

Fields may include:

    id
    name
    sourceType
    sourceLocation
    genre
    location
    artworkUrl
    tags
    provider
    providerStationId
    createdAt
    updatedAt

21.8 MEMBERSHIP ENTITY

Fields include:

    categoryId
    stationId
    sortPosition
    navigationEnabled
    addedAt

A composite unique constraint should prevent duplicate category and
station membership.

21.9 SEARCH METADATA

Search Categories should retain:

    categoryId
    originalQuery
    normalizedQuery
    provider
    lastRefreshAt
    refreshStatus

21.10 CURRENT SELECTION

Persist:

    currentCategoryId
    currentStationId

If the station no longer belongs to the category at restore time,
select another valid station.

21.11 PLAYBACK STATE RESTORATION

Recommended default:

    Restore selection, but do not automatically begin playback after a
    normal app launch.

A future setting may allow optional automatic resume.

21.12 TRANSACTIONAL UPDATES

Operations affecting several tables should use transactions.

Examples:

    • Copy station to several categories
    • Delete category
    • Refresh Search Category
    • Reorder many memberships

21.13 SEARCH REFRESH TRANSACTION

A refresh should avoid exposing a partially replaced result list.

Preferred approach:

    • Fetch and normalize results
    • Apply changes transactionally
    • Preserve category identity
    • Update memberships
    • Update refresh timestamp

21.14 USER CUSTOMIZATION DURING REFRESH

Search-category refresh should preserve membership Navigation Enabled
state for stations that remain in the result set.

21.15 ORPHAN CLEANUP

Unused Station records may be removed through safe cleanup.

Cleanup must not delete a Station still referenced elsewhere.

21.16 MIGRATIONS

Every schema change after release requires a migration strategy.

Destructive migration should not be used for ordinary upgrades.

21.17 CORRUPTION RECOVERY

If persistent data cannot be read:

    • Preserve a backup when possible
    • Report the problem clearly
    • Offer reset only as a last resort
    • Restore built-in defaults without pretending user data survived

21.18 BACKUP

User-created data should participate in platform backup where
appropriate and privacy-safe.

21.19 EXPORT

A future export format may include:

    • Categories
    • Membership order
    • Navigation state
    • Station source information
    • Settings

JSON is a suitable portable format.

21.20 IMPORT

Import should validate all data and avoid duplicate categories and
stations.

21.21 WRITE FREQUENCY

Frequent transient player events should not trigger excessive database
writes.

Persist meaningful selection changes and organization changes.

21.22 TESTING

Persistence tests should cover:

    • Fresh install
    • Upgrade migration
    • Deleted current category
    • Deleted current station
    • Search refresh
    • Reorder
    • Copy to multiple categories
    • Corrupt or missing preference IDs

DESIGN NOTE

The application should remember user intent, not every transient event.

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec250 = """
══════════════════════════════════════════════════════════════════════

                        CHAPTER 22 — SETTINGS                         

══════════════════════════════════════════════════════════════════════

22.1 PURPOSE

Settings provide control over behavior that should not clutter the Main
Screen.

22.2 SETTINGS CATEGORIES

Recommended groups:

    Appearance
    Playback
    Navigation
    Search
    Devices and Casting
    Data
    About

22.3 APPEARANCE SETTINGS

Possible settings:

    Theme
        System Default
        Light
        Dark

    Dynamic Color
        On
        Off

    Reduced Motion
        On
        Off

22.4 PLAYBACK SETTINGS

Possible settings:

    Station startup timeout
    Automatic failure recovery
    Resume after interruption
    Background playback
    Audio-focus behavior

22.5 STARTUP TIMEOUT

A reasonable default is approximately:

    3 to 4 seconds

The UI may offer a restrained range rather than arbitrary values.

22.6 FAILURE RECOVERY TOGGLE

Automatic recovery should default to On.

When Off:

    • Playback stops on failure
    • The error remains visible
    • The user chooses the next action

22.7 NAVIGATION SETTINGS

Possible settings:

    Wrap station navigation
    Wrap category navigation
    Default state for new categories
    Default state for copied memberships
    Future headset gesture mappings

22.8 SEARCH SETTINGS

Possible settings:

    Search provider
    Refresh on first use
    Search chip limit
    Content filters
    Include low-quality results

22.9 DEVICE SETTINGS

Possible settings:

    Preferred playback destination
    Reconnect to prior cast device
    Show unavailable remembered devices
    Local receiver discovery

22.10 DATA SETTINGS

Actions may include:

    Export data
    Import data
    Reset Standard Categories
    Clear Search History
    Delete all Search Categories
    Reset application data

22.11 RESET APPLICATION DATA

This action is destructive and requires explicit confirmation.

22.12 ABOUT

The About screen should include:

    Application name
    Version
    Privacy information
    Open-source licenses
    Support information
    Design mission statement

22.13 IMMEDIATE APPLICATION

Settings should take effect immediately when safe.

A restart should be required only when technically necessary.

22.14 PERSISTENCE

Settings are persisted through DataStore or equivalent storage.

22.15 DEFAULTS

Defaults should favor:

    • System theme
    • Automatic recovery On
    • Navigation wrap On
    • No automatic playback at launch
    • Search chips enabled
    • Local playback destination

22.16 ACCESSIBILITY

Settings rows must support screen readers and large text.

DESIGN NOTE

A setting is justified when users may reasonably prefer different
behaviors.

Settings should not be used to avoid making a clear default decision.

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec260 = """
══════════════════════════════════════════════════════════════════════

              CHAPTER 23 — CASTING AND EXTERNAL PLAYBACK              

══════════════════════════════════════════════════════════════════════

23.1 PURPOSE

External playback allows Music1Chat to send audio to a compatible
receiver while preserving the same category and navigation model.

23.2 DESTINATIONS

Potential destinations include:

    • Local device
    • Bluetooth device
    • Google Cast device
    • Fire TV receiver
    • Android TV receiver
    • Custom network receiver

23.3 CAST ICON

The top application bar uses the familiar Cast-style icon.

23.4 DEVICE SELECTION

The device interface should display:

    • This device
    • Available receivers
    • Connected receiver
    • Connection state
    • Disconnect action

23.5 CONNECTION STATES

Recommended states:

    DISCONNECTED
    DISCOVERING
    CONNECTING
    CONNECTED
    TRANSFERRING
    ERROR

23.6 NAVIGATION WHILE CASTING

Previous, Next, category navigation, and playback toggle continue to use
the same application commands.

The active playback controller routes them to the receiver.

23.7 STATE AUTHORITY

Music1Chat remains the authority for:

    • Current Category
    • Current Station
    • Navigation order

The remote player reports playback execution state.

23.8 TRANSFER TO RECEIVER

When connecting during active playback:

    • Stop or fade local playback
    • Send the current Station
    • Begin remote playback
    • Avoid prolonged simultaneous playback

23.9 TRANSFER BACK

When disconnecting:

    • Preserve current selection
    • Stop remote playback when possible
    • Return to local destination
    • Resume locally only according to explicit policy

23.10 FIRE TV RECEIVER

A custom Fire TV receiver may:

    • Advertise itself on the local network
    • Respond to discovery
    • Accept station URL and metadata
    • Play through ExoPlayer
    • Report state to Music1Chat

23.11 DISCOVERY

Custom discovery may use local-network UDP or another supported
protocol.

Discovery must:

    • Time out
    • Avoid blocking the UI
    • Handle duplicate responses
    • Handle changing IP addresses

23.12 SECURITY

A custom receiver should not accept unrestricted commands from any
network client.

Future production design should include:

    • Pairing
    • Authentication token
    • Local-network restrictions
    • Command validation

23.13 FAILURE

Cast connection failure must not destroy current category or station
state.

The user should be offered local playback.

23.14 RECEIVER LOSS

When a connected receiver disappears:

    • Report the loss
    • Preserve current selection
    • Offer or perform local fallback according to policy

23.15 METADATA

Send:

    • Station name
    • Artwork
    • Genre
    • Current stream URL
    • Available live metadata

23.16 BLUETOOTH DISTINCTION

Bluetooth is generally an audio route selected by the operating system.

Casting is an application-controlled remote playback session.

23.17 BACKGROUND CONTROL

Media controls should remain functional while casting.

23.18 TESTING

Test:

    • Connect before playback
    • Connect during playback
    • Disconnect
    • Receiver crash
    • Network change
    • App backgrounding
    • Station failure on receiver
    • Rapid station changes

DESIGN NOTE

Casting changes where audio plays.

It does not change how the user organizes or navigates content.

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec270 = """
══════════════════════════════════════════════════════════════════════

                CHAPTER 24 — THEMES AND VISUAL SYSTEM                 

══════════════════════════════════════════════════════════════════════

24.1 PURPOSE

Music1Chat supports comfortable use in varied lighting conditions.

24.2 REQUIRED THEMES

The application must support:

    • Light theme
    • Dark theme
    • System-default theme

24.3 DYNAMIC COLOR

On supported Android versions, Material dynamic color may be offered.

Dynamic color must not reduce legibility or obscure navigation state.

24.4 THEME SELECTION

The selected theme should respond immediately.

System Default follows the current device appearance.

24.5 MATERIAL 3

The interface uses Material 3 conventions for:

    • Typography
    • Surfaces
    • Icons
    • Dialogs
    • Touch feedback
    • Color roles

24.6 COLOR SEMANTICS

Color may reinforce state but must not be the only indicator.

24.7 STOP CONTROL

The active Stop control may use a strong red treatment.

It must remain legible in both themes.

24.8 NAVIGATION INDICATOR

Enabled and disabled states require distinct shape or fill treatment in
addition to color.

24.9 CURRENT SELECTION

Current Category and Current Station highlights must work in both
themes.

24.10 CONTRAST

Text and icons should meet reasonable accessibility contrast.

24.11 TYPOGRAPHY

The station title may be large and bold.

Secondary metadata should remain readable and not become excessively
small.

24.12 LARGE TEXT

The layout should tolerate increased system font scale.

Text should wrap, marquee, or reflow without hiding primary controls.

24.13 MOTION

Marquee and VU animation should be restrained.

Reduced Motion should disable or simplify nonessential animation.

24.14 STATUS BAR

Status-bar icon appearance should match the active theme.

24.15 THEME IMPLEMENTATION

The root Activity should wrap content in Music1ChatTheme.

Theme selection must not be hard-coded to dark mode.

24.16 DYNAMIC COLOR FALLBACK

When dynamic color is unavailable or disabled, use defined light and
dark color schemes.

24.17 SCREENSHOTS AND TESTING

Visual testing should include:

    • Light phone theme
    • Dark phone theme
    • Dynamic color on
    • Dynamic color off
    • Large text
    • Small screen
    • Landscape
    • High-contrast wallpaper-generated palettes

DESIGN NOTE

Theme support is functional behavior, not decoration.

The application must actually respond when the device changes between
light and dark appearance.

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec280 = """
══════════════════════════════════════════════════════════════════════

            CHAPTER 25 — STARTUP, BACKGROUND, AND SHUTDOWN            

══════════════════════════════════════════════════════════════════════

25.1 STARTUP GOAL

Startup should restore the user's listening context quickly without
unexpected audio.

25.2 INITIALIZATION ORDER

Recommended order:

    1. Initialize storage.
    2. Load settings.
    3. Load categories, stations, and memberships.
    4. Restore Current Category and Current Station.
    5. Initialize playback and media-session services.
    6. Begin optional background refresh.
    7. Display the Main Screen.

25.3 FIRST RUN

On first run:

    • Create Standard Categories
    • Insert selected development or starter stations
    • Apply default Navigation states
    • Select a reasonable initial category and station
    • Do not autoplay unless explicitly required

25.4 SEEDING

Default seeding must be idempotent.

Running startup repeatedly must not create duplicates.

25.5 DEVELOPMENT DATA

Development-only data should be clearly separated from production
defaults.

25.6 RESTORE VALIDATION

If saved Current Category is missing:

    • Select the first available category.

If saved Current Station is invalid for that category:

    • Select an eligible membership.

25.7 SEARCH REFRESH AT STARTUP

Search Categories may refresh:

    • In the background
    • On first encounter
    • According to age

Startup should not be unnecessarily delayed by many network searches.

25.8 AUTOPLAY

Default Version 2.0 behavior:

    Do not automatically begin playback merely because the app opens.

25.9 BACKGROUND PLAYBACK

When audio is active, the application should use the appropriate
foreground media service and notification.

25.10 ACTIVITY CLOSURE

Closing the Main Activity should not unintentionally kill active
playback when background playback is supported.

25.11 EXIT ACTION

The explicit Exit action means:

    • Stop playback
    • Cancel recovery
    • Disconnect or stop remote playback according to policy
    • Release media resources
    • Stop foreground service
    • Persist current selection
    • Close application UI

25.12 BACK BUTTON

Back should follow normal Android navigation.

On Main Screen, Back may leave the UI while playback continues when
background playback is active.

Back is not identical to Exit.

25.13 PROCESS DEATH

After process recreation:

    • Restore organization and selection
    • Recreate state safely
    • Avoid pretending playback is active when no player exists

25.14 APP UPDATE

An update should preserve user data through migrations.

25.15 SESSION DEFINITION

A new session begins when the playback/application process is freshly
initialized after prior session state has ended.

Session-failed flags clear at this point.

25.16 SHUTDOWN PERSISTENCE

Meaningful current selection should be saved before controlled exit.

25.17 CRASH RECOVERY

Following an unclean shutdown:

    • Restore the last committed data
    • Clear transient recovery flags
    • Do not autoplay
    • Preserve enough logs for diagnosis in development

25.18 LOW MEMORY

The application should release artwork and other nonessential caches
before risking playback stability.

25.19 CONNECTIVITY AT STARTUP

Lack of network should not prevent browsing saved categories.

Playback and search should report network unavailability clearly.

DESIGN NOTE

Opening the app restores context.

Pressing Play begins listening.

Those are separate user intentions.

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec289 = """
══════════════════════════════════════════════════════════════════════

                     END OF SPECIFICATION PART 4                      

══════════════════════════════════════════════════════════════════════

FILE

    DesignSpecificationPart4.kt

CONTENTS

    • Chapter 21 — Persistence
    • Chapter 22 — Settings
    • Chapter 23 — Casting and External Playback
    • Chapter 24 — Themes and Visual System
    • Chapter 25 — Startup, Background, and Shutdown

NEXT FILE

    DesignSpecificationPart5.kt

══════════════════════════════════════════════════════════════════════
""".trimIndent()