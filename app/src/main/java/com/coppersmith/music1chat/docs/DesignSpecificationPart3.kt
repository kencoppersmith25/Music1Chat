package com.coppersmith.music1chat.docs

@Suppress("unused")
val designSpec170 = """
══════════════════════════════════════════════════════════════════════

        MUSIC1CHAT DESIGN SPECIFICATION — VERSION 2.0 — PART 3        

══════════════════════════════════════════════════════════════════════

PART 3 PURPOSE

This file defines:

    • Station data and category-membership behavior
    • Playback-controller responsibilities
    • Play, Stop, and Toggle semantics
    • Station navigation
    • Category navigation
    • Bluetooth and media-button mappings
    • Playback startup timing
    • Failure detection
    • Session failure tracking
    • Category exhaustion
    • Automatic recovery
    • User-visible playback status

The persistence, settings, cast, theme, startup, and shutdown rules are
defined in Part 4.

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec180 = """
══════════════════════════════════════════════════════════════════════

                      CHAPTER 16 — STATION MODEL                      

══════════════════════════════════════════════════════════════════════

16.1 PURPOSE

A Station represents one playable audio source. A Station is global
application data. Its use within a particular category is represented
by a separate CategoryStationMembership record.

16.2 REQUIRED STATION FIELDS

A Station should contain at least:

    id
    name
    sourceType
    streamUrl or sourceLocation
    genre
    location
    artworkUrl
    tags
    directoryProvider
    directoryStationId

Only fields required by the selected source type must be populated.

16.3 SOURCE TYPES

Initial and anticipated source types include:

    STREAM
    FOLDER
    PLAYLIST
    FILE
    PODCAST
    AUDIOBOOK
    REMOTE_SERVICE

Version 2.0 primarily implements STREAM.

16.4 STABLE IDENTITY

When an external directory supplies a stable station identifier, that
identifier should participate in identity.

When no stable directory identifier exists, identity may be derived
from a normalized stream URL.

A generated local identifier remains the application's primary key.

16.5 URL NORMALIZATION

Before comparing station URLs, the application may normalize:

    • Leading and trailing spaces
    • Scheme and host case
    • Harmless trailing slash differences
    • Known directory redirect wrappers

Normalization must not combine two genuinely different streams.

16.6 DUPLICATES

Two search results that clearly identify the same station should
normally share one Station record.

Different stream URLs may still be preserved when they represent:

    • Different bitrates
    • Different codecs
    • Regional feeds
    • Backup feeds

16.7 DISPLAY NAME

The station name should be trimmed and readable.

Blank or unusable names should fall back to:

    Unknown Station

A future editing screen may permit a user-defined display name.

16.8 OPTIONAL METADATA

Optional station data may include:

    homepage
    country
    stateOrRegion
    language
    codec
    bitrate
    votesOrPopularity
    lastDirectoryCheck
    faviconUrl
    metadataSupport

Missing optional metadata must not prevent playback.

16.9 CATEGORY-STATION MEMBERSHIP

A membership record contains at least:

    categoryId
    stationId
    sortPosition
    navigationEnabled

The membership may later contain:

    addedAt
    lastPlayedAt
    playCount
    customLabel
    notes
    categorySpecificVolume

16.10 MEMBERSHIP IDENTITY

A category should not contain duplicate memberships for the same
Station unless a later design explicitly supports alternate feeds as
distinct entries.

16.11 DEFAULT MEMBERSHIP STATE

A newly copied or user-added station membership defaults to:

    Navigation Enabled

Search-result membership defaults may also be Navigation Enabled.

16.12 MEMBERSHIP ORDER

Station navigation follows membership order after filtering for
eligibility.

Reordering memberships changes Previous Station and Next Station
behavior immediately.

16.13 DIRECT SELECTION

A Navigation Disabled membership remains directly selectable.

Direct selection temporarily makes that station Current Station but
does not silently enable it for sequential navigation.

16.14 DELETION

Deleting a station from a category deletes the membership.

The global Station record may be removed later only when:

    • No memberships reference it
    • It is not otherwise retained
    • Cleanup is safe

16.15 SEARCH MEMBERSHIPS

Search-result memberships are generated from the current Search
Category result set.

Individual deletion is normally disabled because refresh may recreate
the result.

16.16 SESSION FAILURE FLAG

Failure for the current session should not normally be stored on the
permanent Station record.

A separate runtime structure should track failed station identifiers
and failure reasons.

16.17 STREAM FALLBACKS

A future Station may retain multiple playable endpoints in priority
order.

Version 2.0 may use one primary stream URL.

16.18 VALIDATION

A playable STREAM station requires:

    • Nonblank name or fallback name
    • Nonblank URL
    • Supported URL scheme
    • Valid source type

Validation errors should be visible during development and handled
gracefully in production.

DESIGN NOTE

The Station describes the source.

The membership describes how that source participates in one category.

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec190 = """
══════════════════════════════════════════════════════════════════════

                   CHAPTER 17 — PLAYBACK CONTROLLER                   

══════════════════════════════════════════════════════════════════════

17.1 PURPOSE

The Playback Controller owns execution of audio playback.

It receives a selected Station and attempts to render it through the
active Playback Destination.

17.2 INITIAL IMPLEMENTATION

The Android implementation uses Media3 ExoPlayer with appropriate
audio attributes for music or radio playback.

17.3 CORE RESPONSIBILITIES

The Playback Controller must:

    • Load the selected source
    • Start playback
    • Stop playback
    • Release resources
    • Report state changes
    • Report playback errors
    • Support media-session integration
    • Prevent stale callbacks from controlling newer selections
    • Cooperate with failure recovery

17.4 PUBLIC OPERATIONS

Recommended operations include:

    play(station)
    stop()
    togglePlayback()
    release()

A future implementation may add:

    pause()
    seek()
    changeDestination()
    preload()

17.5 PLAY SEMANTICS

Play attempts to start Current Station.

When Current Station is invalid or absent, the application selects an
eligible station using the Navigation Controller before playback.

17.6 STOP SEMANTICS

Stop ends audio playback and cancels the active startup timeout.

Stop preserves:

    • Current Category
    • Current Station
    • Navigation position
    • Organization state

17.7 PAUSE SEMANTICS

Live radio does not normally support meaningful resume from the same
point.

The initial application may map Pause to Stop.

The interface should avoid promising DVR-like behavior that does not
exist.

17.8 TOGGLE SEMANTICS

Toggle Playback means:

    If playing, connecting, buffering, or recovering:
        Stop.

    If stopped or in nonrecovering error:
        Play the selected or next eligible station.

17.9 PLAYBACK STATES

Internal playback state should distinguish:

    STOPPED
    PREPARING
    CONNECTING
    BUFFERING
    PLAYING
    STOPPING
    ERROR
    RECOVERING
    RELEASED

17.10 STATE OWNERSHIP

Only the playback subsystem should interpret low-level ExoPlayer
callbacks.

It translates them into application-level playback state.

17.11 AUDIO ATTRIBUTES

Playback should use audio attributes appropriate for media.

The system should integrate correctly with:

    • Bluetooth routing
    • Audio focus
    • Volume controls
    • Media notifications
    • Vehicle controls

17.12 AUDIO FOCUS

The application should respond appropriately to audio-focus changes.

Recommended initial behavior:

    • Permanent loss: stop
    • Transient loss: pause or stop
    • Duck request: lower volume when supported
    • Focus regain: resume only when consistent with user expectations

Automatic resume after a phone call should be configurable if later
implemented.

17.13 PLAY REQUEST GENERATION

Each explicit play attempt should receive a generation or request ID.

Callbacks from an older request must not skip or stop a newer station.

17.14 REPLACING A STATION

When the user navigates while playback is active:

    • Cancel pending startup timeout
    • Stop or replace the old media item
    • Load the new station
    • Start immediately
    • Treat subsequent callbacks as belonging to the new request

17.15 PLAYBACK DESTINATION

The controller may delegate playback to:

    • Local ExoPlayer
    • Cast controller
    • Remote receiver

Application-level state should remain consistent across destinations.

17.16 RESOURCE RELEASE

Release must:

    • Cancel pending jobs
    • Remove listeners
    • Release ExoPlayer
    • Release media-session resources
    • Clear transient callbacks
    • Avoid retaining an Activity or Context improperly

17.17 BACKGROUND PLAYBACK

When background playback is supported, audio should continue through a
proper media service rather than relying on an Activity remaining
alive.

17.18 NOTIFICATION

The playback notification should display:

    • Station name
    • Available artwork
    • Playing or stopped state
    • Previous, toggle, and next controls as supported

17.19 METADATA

Stream metadata may update:

    • Song title
    • Artist
    • Program
    • Artwork

Metadata errors must not stop the station.

17.20 NETWORK CHANGES

A temporary network loss may be allowed a brief reconnect period.

The recovery policy must still prevent endless buffering.

DESIGN NOTE

Navigation selects what should play.

The Playback Controller determines whether and how it can play.

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec200 = """
══════════════════════════════════════════════════════════════════════

                    CHAPTER 18 — NAVIGATION ENGINE                    

══════════════════════════════════════════════════════════════════════

18.1 PURPOSE

The Navigation Engine selects categories and station memberships in a
predictable order.

18.2 COMMANDS

Core commands are:

    PREVIOUS_STATION
    NEXT_STATION
    PREVIOUS_CATEGORY
    NEXT_CATEGORY
    SELECT_STATION
    SELECT_CATEGORY

18.3 STATION ELIGIBILITY

For ordinary sequential navigation, a membership is eligible when:

    • It belongs to Current Category
    • Navigation Enabled is true
    • Its Station has a playable source
    • It is not excluded by session failure handling

18.4 CATEGORY ELIGIBILITY

A category is eligible when:

    • Category Navigation Enabled is true
    • It is not temporarily exhausted for the active recovery pass
    • It contains at least one potentially playable station

18.5 NEXT STATION

NEXT_STATION searches forward from the current membership.

It:

    • Skips ineligible memberships
    • Wraps after the last item
    • Stops after examining each membership at most once
    • Reports no eligible station when none exists

18.6 PREVIOUS STATION

PREVIOUS_STATION mirrors NEXT_STATION in reverse order.

18.7 NEXT CATEGORY

NEXT_CATEGORY searches forward through category order.

For each candidate category, it selects an eligible station.

18.8 PREVIOUS CATEGORY

PREVIOUS_CATEGORY mirrors NEXT_CATEGORY in reverse order.

18.9 INITIAL STATION IN NEW CATEGORY

Preferred selection order:

    1. Last selected usable station in that category
    2. First Navigation Enabled, nonfailed membership
    3. First directly playable membership when manually opening the
       category and no enabled membership exists
    4. No-station state

18.10 MANUAL CATEGORY SELECTION

A directly selected Navigation Disabled category becomes Current
Category.

Manual selection is allowed even though sequential navigation skips it.

18.11 MANUAL STATION SELECTION

A directly selected Navigation Disabled station becomes Current
Station and may be played.

18.12 PLAYING VERSUS STOPPED

When playback is active:

    • Navigation immediately loads and plays the new selection.

When playback is stopped:

    • Navigation changes selection only.

18.13 WRAPAROUND

Station and category sequential navigation normally wrap.

The engine must never loop indefinitely.

18.14 SINGLE ELIGIBLE ITEM

When only one eligible station exists:

    • Previous and Next may resolve to the same station.
    • The engine should avoid unnecessary reload when the command
      produces no meaningful change unless recovery requires retry.

18.15 NO ENABLED STATIONS

A category with stations but no Navigation Enabled memberships:

    • Remains browsable
    • May be manually played
    • Is not useful for ordinary station navigation
    • Should show a clear state

18.16 NO ENABLED CATEGORIES

When no category is Navigation Enabled:

    • Manual selection remains available
    • Category Previous and Next report no eligible category
    • Automatic recovery stops instead of looping

18.17 CATEGORY ORDER

Category order is persistent and user-controlled.

18.18 STATION ORDER

Membership order is persistent per category.

18.19 COMMAND SERIALIZATION

Navigation commands should be serialized or otherwise coordinated so
rapid input does not corrupt current selection.

18.20 LATEST USER INTENT

A newer explicit user command takes priority over an older automatic
recovery action.

18.21 RECOVERY EXCLUSIONS

During one recovery sequence, the engine tracks stations and categories
already attempted.

18.22 TEST CASES

Automated tests should cover:

    • Empty category list
    • One category
    • All categories disabled
    • One station
    • All stations disabled
    • Failed first station
    • Failed last station
    • Wraparound
    • Category exhaustion
    • Manual selection of disabled items
    • Rapid repeated commands

DESIGN NOTE

Eligibility filtering and wraparound belong in one central engine.
They must not be reimplemented separately by each button.

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec210 = """
══════════════════════════════════════════════════════════════════════

               CHAPTER 19 — BLUETOOTH AND MEDIA BUTTONS               

══════════════════════════════════════════════════════════════════════

19.1 PURPOSE

Bluetooth and media controls are first-class Music1Chat controls.

19.2 ANDROID MEDIA KEY MAPPINGS

Required logical mappings include:

    KEYCODE_MEDIA_NEXT
        NEXT_STATION

    KEYCODE_MEDIA_PREVIOUS
        PREVIOUS_STATION

    KEYCODE_MEDIA_PLAY_PAUSE
        TOGGLE_PLAYBACK

    KEYCODE_MEDIA_PLAY
        PLAY

    KEYCODE_MEDIA_PAUSE
        STOP or PAUSE according to live-stream policy

    KEYCODE_MEDIA_STOP
        STOP

19.3 OBSERVED HEADSET KEY CODES

Known tested key codes include:

    87
        NEXT_STATION

    88
        PREVIOUS_STATION

    126
        PLAY or TOGGLE_PLAYBACK

    127
        PAUSE or TOGGLE_PLAYBACK

Platform constants should be used rather than raw numbers where
possible.

19.4 SINGLE SOURCE OF COMMAND LOGIC

Media-button events must be converted to application commands and sent
through the same command path as on-screen buttons.

19.5 MEDIA SESSION

A media session should expose:

    • Current station metadata
    • Playback state
    • Supported actions
    • Previous
    • Next
    • Play
    • Stop or Pause

19.6 KEY DOWN AND KEY UP

The implementation should avoid executing the same action on both key
down and key up.

19.7 REPEAT EVENTS

Repeated key events should be ignored unless an intentional long-press
behavior is implemented.

19.8 HEADSET VARIATION

Different earbuds may map gestures differently.

Music1Chat should log media-button events in development builds to aid
testing.

19.9 CATEGORY CONTROL FROM HEADSET

Version 2.0 guarantees station previous, station next, and playback
toggle.

Future mappings may support category navigation through:

    • Double tap
    • Long press
    • Configurable gesture
    • Voice command

These must not interfere with reliable basic controls.

19.10 STATUS FEEDBACK

The Main Screen may display:

    Bluetooth controls ready

This indicates media-session readiness, not necessarily that a
Bluetooth device is currently connected.

19.11 LOCK SCREEN

Lock-screen controls should produce the same results as Bluetooth
controls.

19.12 NOTIFICATION CONTROLS

Notification controls should also use the same commands.

19.13 AUDIO ROUTE

Bluetooth output routing is primarily managed by Android.

Music1Chat should report the active destination when reliably known.

19.14 VOICE ASSISTANT CONFLICTS

System assistants may intercept headset gestures.

The application should not claim exclusive control when Android or the
device firmware prevents it.

19.15 TESTING MATRIX

Testing should include:

    • Multiple earbud brands
    • Wired headset controls
    • Lock-screen controls
    • Notification controls
    • Screen on and off
    • App foreground and background
    • Bluetooth reconnect
    • Rapid repeated taps

DESIGN NOTE

A hands-free feature is not complete until it works with the screen
off and the application in the background.

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec220 = """
══════════════════════════════════════════════════════════════════════

                    CHAPTER 20 — FAILURE RECOVERY                     

══════════════════════════════════════════════════════════════════════

20.1 PURPOSE

Failure recovery keeps audio playing without requiring the user to
handle the phone.

20.2 FAILURE CONDITIONS

A station may fail because of:

    • Immediate player error
    • DNS failure
    • Connection refusal
    • Unsupported format
    • HTTP error
    • Startup timeout
    • Endless buffering
    • Lost network
    • Remote server failure

20.3 STARTUP TIMEOUT

If a station does not reach a playable state within approximately
three to four seconds, it may be considered failed for that attempt.

The timeout should be configurable internally and later through
Settings.

20.4 TIMEOUT START

The timer begins when the active play request starts loading the
station.

20.5 TIMEOUT CANCELLATION

The timer is canceled when:

    • Playback begins
    • The user stops
    • The user selects another station
    • The request is replaced
    • The player is released

20.6 SESSION FAILURE RECORD

On confirmed failure, record:

    • Station ID
    • Failure reason
    • Time
    • Play-request generation
    • Optional category context

20.7 SESSION SCOPE

Failed status normally lasts only for the current application session.

A new session gives the station another chance.

20.8 AUTOMATIC SKIP

When playback was requested and a station fails:

    1. Mark it failed for the session.
    2. Find the next eligible station in Current Category.
    3. Start it automatically.
    4. Show compact recovery feedback.

20.9 CATEGORY EXHAUSTION

A category is exhausted for the recovery pass when every eligible
membership has:

    • Failed
    • Already been attempted
    • Become otherwise unavailable

20.10 CATEGORY ADVANCE

When Current Category is exhausted:

    • Show a compact message
    • Select the next eligible category
    • Select an eligible station
    • Continue playback automatically

20.11 USER MESSAGE

Example:

    No playable stations remain in Classical.
    Moving to Jazz.

The message should not require dismissal.

20.12 ALL CATEGORIES EXHAUSTED

When no eligible category can provide a playable station:

    • Stop recovery
    • Stop playback attempts
    • Display a clear final message
    • Avoid infinite loops

Recommended message:

    No playable stations are currently available.

20.13 MANUAL RETRY

The user may manually select a session-failed station.

Recommended behavior:

    • Permit one explicit retry
    • Do not clear all failure history
    • If it fails again, return to recovery behavior

20.14 NEW EXPLICIT COMMAND

A new user station or category selection cancels the older automatic
recovery path.

20.15 NETWORK-WIDE FAILURE

When many stations fail because the network is offline:

    • Detect connectivity loss when practical
    • Stop rapid category cycling
    • Display a network message
    • Retry only according to a controlled policy

20.16 RECOVERY LOOP SAFETY

Each recovery sequence must track attempted items.

Maximum attempts should be bounded by the number of eligible
memberships and categories.

20.17 FAILURE VERSUS STOP

A user-requested Stop is not a failure.

It must cancel recovery.

20.18 FAILURE VERSUS METADATA ERROR

Metadata or artwork failure is not a station playback failure.

20.19 FAILURE FEEDBACK STATES

The interface may show:

    Connecting
    Buffering
    Station unavailable
    Trying next station
    Trying next category
    No playable station available

20.20 LOGGING

Development logs should include:

    • Station ID and name
    • URL
    • Request generation
    • Error class
    • Timeout
    • Recovery decision
    • Next selected item

Sensitive information should not be logged unnecessarily.

20.21 FUTURE RELIABILITY SCORE

A later version may maintain a station reliability score across
sessions.

Version 2.0 does not permanently penalize a station from one temporary
failure.

20.22 TESTING

Failure-recovery tests should include:

    • Immediate error
    • Startup timeout
    • One good station after several failures
    • Entire category failure
    • Entire navigation set failure
    • User Stop during recovery
    • User Next during recovery
    • Network loss
    • Stale error callback from prior station

DESIGN NOTE

Recovery exists to preserve the listening experience.

It must be assertive enough to continue playback but bounded enough to
avoid frantic or endless switching.

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec229 = """
══════════════════════════════════════════════════════════════════════

                     END OF SPECIFICATION PART 3                      

══════════════════════════════════════════════════════════════════════

FILE

    DesignSpecificationPart3.kt

CONTENTS

    • Chapter 16 — Station Model
    • Chapter 17 — Playback Controller
    • Chapter 18 — Navigation Engine
    • Chapter 19 — Bluetooth and Media Buttons
    • Chapter 20 — Failure Recovery

NEXT FILE

    DesignSpecificationPart4.kt

══════════════════════════════════════════════════════════════════════
""".trimIndent()