package com.coppersmith.music1chat.docs

@Suppress("unused")
val designSpec000 = """
══════════════════════════════════════════════════════════════════════

                       MUSIC1CHAT

                   DESIGN SPECIFICATION

                       VERSION 2.0

                        JULY 2026

══════════════════════════════════════════════════════════════════════

MISSION STATEMENT

Music1Chat is designed to provide the safest, simplest, and most
enjoyable hands-free audio experience possible.

Whether walking, cycling, driving, exercising, working, or relaxing
at home, users should be able to discover, organize, and enjoy audio
with minimal interaction and maximum reliability.

Every feature in Music1Chat is evaluated according to one guiding
principle:

Can the user continue enjoying audio without needing to look at
or touch the phone?

When the answer is yes, the feature supports the central purpose of
Music1Chat.

DOCUMENT PURPOSE

This document defines the intended design and behavior of Music1Chat.

It is the primary reference for:

• User-interface behavior
• Navigation behavior
• Category and station organization
• Playback behavior
• Search behavior
• Bluetooth and external-control behavior
• Failure recovery
• Persistence
• Settings
• Casting
• Future development

The specification describes both what the application does and why
important design decisions were made.

When the implementation and this specification disagree, the
difference must be reviewed deliberately.

Either:

• The implementation must be corrected to match the specification

or:

• The specification must be revised to document an intentional
  design change

Undocumented behavioral drift is not acceptable.

DOCUMENT FORMAT

This specification is stored as valid Kotlin source code.

Its text is contained in Kotlin multiline string values so that
Android Studio displays the document using string-literal syntax
highlighting rather than low-contrast comment coloring.

The values are intentionally unused.

The Suppress annotation prevents unused-declaration warnings.

DOCUMENT FILES

Version 2.0 is divided into five Kotlin source files:

1. DesignSpecificationPart1.kt
   Mission, revision history, introduction, architecture,
   and core concepts

2. DesignSpecificationPart2.kt
   Main screen, interface structure, search, categories,
   and visual interaction

3. DesignSpecificationPart3.kt
   Stations, playback, navigation controls, Bluetooth,
   and failure recovery

4. DesignSpecificationPart4.kt
   Persistence, settings, casting, themes, startup,
   shutdown, and platform behavior

5. DesignSpecificationPart5.kt
   Implementation guidance, design rationale, future work,
   glossary, workflows, and appendices

DOCUMENT STATUS

This is a living design specification.

Version 2.0 establishes the first complete master design for the
Music1Chat application.

Future changes should be recorded in the revision history and then
incorporated into the appropriate chapters.

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec005 = """
══════════════════════════════════════════════════════════════════════

                     REVISION HISTORY

══════════════════════════════════════════════════════════════════════

VERSION 2.0
July 2026

Version 2.0 replaces the earlier collection of design notes with a
cohesive application specification.

MAJOR CHANGES

• Replaced the Favorites and heart model with the more accurate
  Navigation Enabled and Navigation Disabled model

• Defined category navigation independently from station
  navigation

• Defined station navigation as a property of a station's
  membership within a category rather than as a global station
  property

• Established Standard, Search, and User-Defined categories as
  first-class category types

• Made Search categories persistent until explicitly deleted

• Defined station copying to one or more destination categories

• Clarified that the former Move action copies a station rather
  than removing it from its current category

• Defined category and station browsing behavior

• Defined automatic station failure detection and recovery

• Defined automatic advancement when all usable stations in a
  category have failed

• Established hands-free navigation as the central design
  priority

• Defined large on-screen playback and navigation controls

• Defined Bluetooth media-button behavior

• Added support architecture for external playback and casting

• Adopted Material 3 interface conventions

• Defined light-mode and dark-mode behavior

• Added persistence requirements for categories, stations,
  navigation settings, searches, and current playback selection

• Added design rationale so important decisions remain
  understandable in future development

TERMINOLOGY CHANGES

Earlier terminology:

Favorite category
Favorite station
Hearted category
Hearted station
Included in favorites

Version 2.0 terminology:

Navigation Enabled category
Navigation Disabled category
Navigation Enabled station membership
Navigation Disabled station membership
Included in navigation

IMPORTANT DISTINCTION

A station does not have one universal Navigation Enabled setting.

The same station may be:

• Navigation Enabled in one category
• Navigation Disabled in another category
• Absent from a third category

This is intentional and fundamental to the Version 2.0 data model.

ICONOGRAPHY CHANGE

The heart icon is no longer the conceptual basis of navigation.

A dedicated navigation indicator is used to communicate whether an
item participates in normal Previous and Next navigation.

The exact artwork may evolve, but the meaning must remain:

Navigation Enabled
    The item participates in normal navigation.

Navigation Disabled
    The item remains visible and directly selectable but is
    skipped during normal navigation.

VERSIONING GUIDELINE

Minor clarifications that do not change application behavior may use
a revision such as:

Version 2.01
Version 2.02

Behavioral or architectural additions may use:

Version 2.1
Version 2.2

A major redesign may use:

Version 3.0

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec002 = """
══════════════════════════════════════════════════════════════════════

                         TABLE OF CONTENTS

══════════════════════════════════════════════════════════════════════

PART 1
    Cover Page
    Mission Statement
    Table of Contents
    Revision History

    Chapter 1  Introduction
    Chapter 2  Design Philosophy
    Chapter 3  Overall Architecture
    Chapter 4  Core Concepts

PART 2
    Chapter 5  Main Screen Design
    Chapter 6  Search System
    Chapter 7  Current Category Section
    Chapter 8  Now Playing Section
    Chapter 9  Primary Control Interactions
    Chapter 10 Search Chips
    Chapter 11 Category List Screen
    Chapter 12 Category Details Screen
    Chapter 13 Copy Station Workflow
    Chapter 14 Category Creation, Editing, and Deletion
    Chapter 15 Navigation Indicator Design

PART 3
    Chapter 16 Station Model
    Chapter 17 Playback Controller
    Chapter 18 Navigation Engine
    Chapter 19 Bluetooth & Media Buttons
    Chapter 20 Failure Recovery

PART 4
    Chapter 21 Persistence
    Chapter 22 Settings
    Chapter 23 Casting
    Chapter 24 Themes
    Chapter 25 Startup and Shutdown

PART 5
    Chapter 26 Developer Notes
    Chapter 27 Future Enhancements
    Chapter 28 Design Rationale
    Chapter 29 Glossary
    Appendices

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec010 = """
══════════════════════════════════════════════════════════════════════

                        CHAPTER 1

                        INTRODUCTION

══════════════════════════════════════════════════════════════════════

1.1 PURPOSE

Music1Chat is an audio application designed around reliable,
low-attention, and hands-free listening.

Most audio applications assume that the user can frequently look at
the screen, inspect a list, and make precise selections.

Music1Chat assumes that the phone may instead be:

• In a pocket
• Mounted on a bicycle
• Connected to Bluetooth earbuds
• Connected to a vehicle
• Connected to a television or remote speaker
• Sitting across the room
• Temporarily inaccessible during exercise
• Unsafe to handle while moving

The application must therefore make continued listening possible
without repeated visual interaction.

1.2 PRIMARY USE CASE

The defining use case is a person listening while bicycling.

The user may need to:

• Start or stop playback
• Move to the next station
• Return to the previous station
• Move to the next category
• Return to the previous category
• Recover from an unavailable stream

These actions must be possible with minimal attention.

The bicycling use case is not the only supported use case, but it is
the most demanding one and therefore serves as the principal design
test.

An interface that works safely and reliably while bicycling should
also work well while:

• Walking
• Driving
• Exercising
• Cooking
• Working
• Relaxing
• Using the application from across a room

1.3 PRODUCT IDENTITY

Music1Chat is not intended to be merely another Internet radio
directory.

Its distinguishing characteristic is:

Hands-free organization and navigation of audio content.

The application combines:

• Search
• Persistent categories
• User-defined organization
• Category-specific navigation
• Reliable playback
• Automatic failure recovery
• Bluetooth control
• Large direct-manipulation controls

1.4 DESIGN PRIORITIES

The application follows these priorities, in order:

1. Safety
2. Reliable continued playback
3. Simple navigation
4. Clear current-state feedback
5. Easy organization
6. Visual polish
7. Advanced secondary features

Visual attractiveness is important, but it must never make primary
controls difficult to recognize or operate.

1.5 SIMPLICITY

The main listening screen should be understandable within a few
seconds.

The user should be able to identify:

• What category is active
• What station is active
• Whether audio is playing
• How to play or stop
• How to change stations
• How to change categories

Frequently used actions should require one action rather than a
sequence of menus.

1.6 RELIABILITY

Playback interruption should not immediately require user
intervention.

When a stream fails, the application should:

• Detect the failure
• Mark that station as failed for the current session
• Attempt the next eligible station
• Continue through the category when possible
• Advance to another eligible category when necessary
• Explain what happened without blocking playback

The application should recover quietly whenever recovery is possible.

1.7 PERSISTENCE

The application should remember the user's organizational choices.

Persistent information includes:

• Standard category customizations
• Search categories
• User-defined categories
• Station memberships
• Category ordering
• Station ordering within categories
• Category Navigation Enabled states
• Station-membership Navigation Enabled states
• Most recently selected category
• Most recently selected station
• Relevant playback preferences
• Settings
• Recent searches and search chips

Temporary session failure status is not persisted unless a future
revision explicitly changes that rule.

1.8 ORGANIZATION

Users commonly think in terms such as:

Play Classical.
Play Jazz.
Play something for my bike ride.
Play my morning stations.

They do not naturally think in terms such as:

Play station 247 from a directory of ten thousand stations.

Music1Chat therefore organizes playback around categories.

1.9 DIRECT CONTROL

Primary actions must remain directly available.

The user should not need to open a menu to:

• Play
• Stop
• Select the next station
• Select the previous station
• Select the next category
• Select the previous category

Menus are appropriate for configuration and organization, not for
routine listening controls.

1.10 ACCESSIBILITY AND LEGIBILITY

The interface should favor:

• Large touch targets
• High contrast
• Clear state indicators
• Concise labels
• Predictable icon placement
• Screen-reader-compatible descriptions
• Light and dark theme support
• Minimal dependence on color alone

Navigation Enabled and Navigation Disabled must not be distinguished
solely by color.

The icon shape, fill, outline, label, or content description must also
communicate the state.

1.11 INITIAL PLATFORM

The initial implementation targets Android phones and tablets using:

• Kotlin
• Jetpack Compose
• Material 3
• Media3 and ExoPlayer
• Android media-session support
• Persistent local storage

The architecture should avoid unnecessary assumptions that would
prevent later support for other platforms.

1.12 POSSIBLE FUTURE PLATFORMS

Future implementations may include:

• iOS
• Android Auto
• Apple CarPlay
• Wear OS
• Android TV
• Google TV
• Fire TV
• Desktop companion applications
• Web-based remote control

These are future directions rather than Version 2.0 release
requirements.

1.13 CONTENT SOURCES

The initial application emphasizes Internet radio streams.

The architecture may later support:

• Local audio files
• Local folders
• Local playlists
• Podcasts
• Audiobooks
• Cloud-based music
• Licensed commercial services
• Network audio sources

All future sources should enter the same organizational and
navigation model whenever practical.

1.14 NON-GOALS

Music1Chat is not initially intended to be:

• A full music-production tool
• A social network
• A general-purpose file manager
• A video streaming application
• A replacement for every commercial music service
• A complex station-directory administration system

Features outside the primary listening mission should be added only
when they do not compromise simplicity.

1.15 SUCCESS CRITERIA

The design is successful when a user can:

• Find audio easily
• Organize it meaningfully
• Begin playback quickly
• Navigate without looking at the screen
• Recover automatically from failed streams
• Understand what is playing at a glance
• Return later and find the application in a familiar state

DESIGN NOTE

Music1Chat begins with the user experience rather than with the data
model or playback library.

The technical architecture exists to support the listening
experience, not to dictate it.

END OF CHAPTER 1

Related sections:

Chapter 2 — Design Philosophy
Chapter 3 — Overall Architecture
Chapter 4 — Core Concepts

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec020 = """
══════════════════════════════════════════════════════════════════════

                        CHAPTER 2

                    DESIGN PHILOSOPHY

══════════════════════════════════════════════════════════════════════

2.1 USER-FIRST DESIGN

Music1Chat is designed from the user's point of view.

The first question for a feature is not:

How can this be coded?

The first question is:

What should happen for the person using the application?

After the intended behavior is understood, the implementation should
be selected to support it cleanly.

2.2 BEHAVIOR BEFORE SCREEN LAYOUT

A screen is not considered fully designed merely because its controls
have been positioned.

The behavior behind each control must also be defined.

For every action, the design should answer:

• What happens immediately?
• What state changes?
• Does playback begin or stop?
• What happens if the requested item is unavailable?
• What feedback does the user receive?
• What is remembered after the application closes?
• Can the same action be performed hands-free?

2.3 HANDS-FREE FIRST

Hands-free behavior is not an optional accessory to the visual
interface.

It is a primary interface.

Bluetooth controls, media-session commands, and large on-screen
buttons should operate through the same navigation and playback
logic.

This prevents different control methods from producing inconsistent
results.

2.4 ONE SOURCE OF TRUTH

The application should maintain one authoritative state for:

• Current category
• Current station
• Playback state
• Category order
• Station order
• Navigation eligibility
• Failure status
• Search state

The visual interface observes this state.

Bluetooth commands modify this same state.

Casting and future remote controls should also use this same state.

2.5 PREDICTABLE NAVIGATION

Previous and Next must behave consistently.

A user should be able to develop confidence that:

• Next Station advances within the current category
• Previous Station moves backward within the current category
• Next Category advances among eligible categories
• Previous Category moves backward among eligible categories
• Disabled items are skipped
• Selecting a new station while playback is active immediately
  begins the newly selected station
• Selecting a new category while playback is active immediately
  begins an eligible station in that category

2.6 MINIMAL INTERRUPTION

Informational feedback should avoid interrupting playback.

Suitable feedback includes:

• Snackbar
• Toast
• Compact banner
• Status line
• Temporary message near the affected control

Modal dialogs should not be used for routine stream failures.

2.7 SAFE DEFAULTS

Default behavior should favor continued playback.

Examples:

• Advancing to a new station while playing should play it
  automatically

• Advancing to a new category while playing should play an
  eligible station automatically

• A failed stream should be skipped automatically

• A category containing no usable stations should not trap the
  user in silence

• Search results should remain available after the search has
  been completed

2.8 USER CONTROL

Automatic recovery should not remove user control.

The user may still:

• Directly select a disabled station
• Revisit a failed station during a later session
• Stop playback
• Change categories manually
• Enable or disable navigation for any editable item
• Reorder categories
• Reorder stations
• Delete permitted categories or station memberships

2.9 REVERSIBILITY

Destructive operations should be clearly identified.

When practical, deletion should support:

• Confirmation
• Undo
• Recovery from accidental activation

Copying a station into another category is preferred over silently
removing it from the source category.

2.10 CONSISTENT TERMINOLOGY

The application and specification must consistently use:

Category
Station
Navigation Enabled
Navigation Disabled
Search Category
Standard Category
User-Defined Category
Copy to Category
Current Category
Current Station

The words Favorite, hearted, and liked should not be used to describe
navigation eligibility.

2.11 STATE SHOULD BE VISIBLE

Important state should be visible without opening another screen.

The main screen should communicate:

• Current category
• Current station
• Playing or stopped state
• Navigation state where relevant
• Playback errors or recovery activity
• External-output state where relevant

2.12 LARGE CONTROLS

The primary playback and navigation controls should be large enough
for reliable use:

• With one hand
• While moving
• With reduced visual attention
• On a bicycle mount
• On smaller phones
• By users with limited dexterity

Decorative density should never reduce primary touch-target size.

2.13 PROGRESSIVE COMPLEXITY

The main screen presents routine listening controls.

Less frequent actions belong in secondary screens or dialogs.

Examples of secondary actions include:

• Reordering
• Copying stations
• Deleting categories
• Editing category names
• Changing timeouts
• Configuring casting
• Importing or exporting data

2.14 GRACEFUL DEGRADATION

Not every station provides:

• Artwork
• Song title
• Artist name
• Genre metadata
• Reliable stream metadata
• Secure HTTPS transport

The interface must remain usable when optional metadata is absent.

Fallbacks should be clean and intentional.

2.15 DESIGN FOR CHANGE

The architecture should expect:

• New content sources
• New playback destinations
• New navigation devices
• New category types
• Additional settings
• Platform-specific implementations

The core user model should remain stable even as technical details
change.

DESIGN NOTE

The strongest design decisions in Music1Chat describe behavior rather
than appearance.

A button can be restyled later.

Unclear navigation behavior is far more expensive to correct after it
has spread throughout the application.

END OF CHAPTER 2

Related sections:

Chapter 1 — Introduction
Chapter 3 — Overall Architecture
Chapter 4 — Core Concepts
Part 3 — Playback and Navigation

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec030 = """
══════════════════════════════════════════════════════════════════════

                        CHAPTER 3

                   OVERALL ARCHITECTURE

══════════════════════════════════════════════════════════════════════

3.1 ARCHITECTURAL GOAL

Music1Chat separates user-interface concerns, application state,
navigation rules, persistence, search, and audio playback.

The purpose of this separation is not complexity for its own sake.

It allows each part of the application to change without forcing
unrelated parts to be rewritten.

3.2 CONCEPTUAL LAYERS

The application can be viewed as the following layers:

User Interface
    ↓
Application State and View Models
    ↓
Navigation and Playback Coordination
    ↓
Repositories and Services
    ↓
Platform Playback and Storage

3.3 USER-INTERFACE LAYER

The user-interface layer is responsible for:

• Drawing screens
• Displaying state
• Receiving touch input
• Providing accessibility descriptions
• Showing dialogs, banners, and messages
• Sending user intentions to the application layer

The user-interface layer should not independently invent navigation
rules.

3.4 APPLICATION-STATE LAYER

The application-state layer maintains observable state such as:

• Current screen
• Current category
• Current station
• Search text
• Search suggestions
• Playback status
• Error messages
• Cast state
• Settings
• Lists of categories and stations

Jetpack Compose observes this state and redraws affected interface
elements.

3.5 NAVIGATION CONTROLLER

The Navigation Controller is responsible for:

• Next station
• Previous station
• Next category
• Previous category
• Skipping Navigation Disabled items
• Respecting category-specific station membership
• Wrapping at list boundaries
• Selecting an eligible starting station
• Coordinating navigation with playback
• Avoiding session-failed stations when possible

All control sources should use this same logic:

• On-screen buttons
• Bluetooth buttons
• Media notifications
• Lock-screen controls
• Future remote controls
• Future voice controls

3.6 PLAYBACK CONTROLLER

The Playback Controller is responsible for:

• Loading a selected source
• Starting playback
• Stopping playback
• Reporting buffering
• Reporting active playback
• Reporting errors
• Applying audio attributes
• Managing retries and timeouts
• Cooperating with the media session
• Cooperating with external-output systems

The Playback Controller should not decide category order.

3.7 FAILURE RECOVERY MANAGER

The Failure Recovery Manager is responsible for:

• Detecting startup timeout
• Receiving playback errors
• Marking stations failed for the current session
• Requesting the next eligible station
• Determining when the current category is exhausted
• Requesting another eligible category
• Preventing infinite failure loops
• Reporting recovery activity to the user interface

3.8 CATEGORY REPOSITORY

The Category Repository stores and retrieves:

• Category identity
• Category name
• Category type
• Category ordering
• Category Navigation Enabled state
• Category refresh information
• Category station memberships

It provides an authoritative category list to the application.

3.9 STATION REPOSITORY

The Station Repository stores and retrieves global station
information such as:

• Station identity
• Station name
• Stream URL
• Source type
• Genre
• Location
• Artwork URL
• Tags
• Optional metadata

Navigation state is not stored as a universal station property.

It belongs to the category-station membership record.

3.10 CATEGORY-STATION MEMBERSHIP

A membership record represents one station appearing in one category.

The membership record contains information such as:

• Category identifier
• Station identifier
• Position within the category
• Navigation Enabled state
• Membership-specific user notes, if later supported
• Membership-specific display overrides, if later supported

This relationship allows the same station to behave differently in
different categories.

3.11 SEARCH SERVICE

The Search Service is responsible for:

• Accepting a search term
• Searching supported station directories
• Normalizing results
• Removing exact duplicates when appropriate
• Producing Station records
• Updating or creating a Search Category
• Reporting search errors
• Supporting refresh behavior

3.12 PERSISTENCE MANAGER

The Persistence Manager is responsible for durable storage.

Possible implementation technologies include:

• Room database
• DataStore
• Serialized local files
• A combination of these

The exact storage technology is an implementation choice.

The required behavior is defined by the persistence chapters.

3.13 SETTINGS MANAGER

The Settings Manager stores and exposes:

• Theme preference
• Dynamic-color preference
• Playback timeout
• Failure-recovery settings
• Cast preferences
• Search behavior
• Optional startup behavior
• Future accessibility preferences

3.14 MEDIA-SESSION CONTROLLER

The media-session subsystem integrates Music1Chat with Android.

It supports:

• Bluetooth media controls
• Lock-screen controls
• Notification controls
• Vehicle media controls
• External media-button events
• System playback state reporting

Media-session commands must be translated into the same application
commands used by the on-screen controls.

3.15 CAST MANAGER

The Cast Manager is responsible for external playback destinations.

Its responsibilities may include:

• Discovering compatible devices
• Connecting and disconnecting
• Sending playable media information
• Reporting destination state
• Transferring playback
• Recovering from connection loss

Casting should not create a second independent category-navigation
system.

3.16 DATA FLOW EXAMPLE

A Next Station command follows this conceptual path:

1. The user presses an on-screen or Bluetooth control.

2. The command is translated into NEXT_STATION.

3. The Navigation Controller locates the next eligible membership
   in the current category.

4. Navigation Disabled memberships are skipped.

5. Session-failed stations are skipped when another eligible
   station is available.

6. Application state updates the current station.

7. When playback is active, the Playback Controller immediately
   loads and plays the new station.

8. The interface updates to display the new station.

9. The current selection is persisted.

3.17 UNIDIRECTIONAL STATE FLOW

The preferred state pattern is:

Event
    ↓
Application action
    ↓
State update
    ↓
Interface redraw

The interface should display application state rather than attempting
to maintain separate hidden copies of that state.

3.18 CONCURRENCY

Search, playback, persistence, and device discovery may operate
asynchronously.

The architecture must prevent:

• Stale search results replacing newer results
• A late playback error skipping a newly selected station
• Multiple recovery loops running simultaneously
• Duplicate persistence writes producing inconsistent order
• Device callbacks modifying abandoned screens incorrectly

Coroutine scopes and cancellation should be tied to appropriate
lifecycles.

3.19 ERROR BOUNDARIES

Subsystem failures should be isolated when practical.

Examples:

• Search failure should not stop current playback.
• Artwork failure should not stop audio.
• Cast discovery failure should not disable local playback.
• One corrupt saved category should not destroy all user data.
• Metadata failure should not mark an otherwise playable station
  as failed.

3.20 TESTABILITY

Core navigation and eligibility rules should be testable without a
real Android device.

Pure or mostly pure logic should be used for:

• Next and previous selection
• Wrapping behavior
• Filtering Navigation Disabled items
• Selecting an initial station
• Detecting category exhaustion
• Avoiding infinite recovery loops
• Copying memberships
• Reordering

Playback integration requires platform tests, but navigation logic
should not depend unnecessarily on ExoPlayer.

3.21 PLATFORM ABSTRACTION

Where future multiplatform support is likely, interfaces should
separate:

• Application concepts
• Android-specific playback
• Android-specific persistence
• Android media-session integration
• Device discovery

Premature abstraction should be avoided, but core concepts should not
be unnecessarily tied to one screen or framework.

DESIGN NOTE

The category-station membership is one of the most important records
in the architecture.

A global station record describes the playable source.

A membership record describes how that station behaves in a
particular category.

END OF CHAPTER 3

Related sections:

Chapter 4 — Core Concepts
Part 2 — Category and Search Interface
Part 3 — Playback and Navigation
Part 4 — Persistence

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec040 = """
══════════════════════════════════════════════════════════════════════

                        CHAPTER 4

                       CORE CONCEPTS

══════════════════════════════════════════════════════════════════════

4.1 CATEGORY

A category is an ordered collection of station memberships.

A category represents a meaningful listening context.

Examples:

Classical
Jazz
Hawaiian
News
Bike Ride
Morning Coffee
Relaxing Piano
Search: Swing

A category contains references to stations rather than requiring
separate duplicate station records.

4.2 CATEGORY IDENTITY

Every category has a stable identifier.

The category name is editable where permitted, but the identifier
does not change when the name changes.

Stable identity prevents renamed categories from losing:

• Station memberships
• Navigation settings
• Ordering
• Playback history
• User preferences

4.3 CATEGORY TYPE

Each category has one of three primary types:

STANDARD
SEARCH
USER_DEFINED

The type affects creation, refresh, and deletion behavior.

The type does not make a category less important in normal browsing.

4.4 STANDARD CATEGORY

A Standard Category is supplied by the application.

Examples:

Classical
Jazz
Rock
Country
Blues
Hawaiian
News

Standard Categories provide familiar organizational destinations.

Users may:

• Open them
• Play stations from them
• Change their Navigation Enabled state
• Copy stations into them
• Reorder stations where allowed

Whether Standard Categories may be deleted or renamed is controlled
by the detailed category-management rules in Part 2.

4.5 SEARCH CATEGORY

A Search Category is created from a station search.

Example:

Search term:
    Classical

Category name:
    Search: Classical

The Search Category stores the result set associated with that
search.

Search Categories persist after the search interface closes.

They remain part of the category list until:

• The user deletes them
• A future retention rule explicitly removes them
• The user resets application data

4.6 SEARCH CATEGORY REFRESH

Search results may change over time.

A Search Category therefore retains enough information to repeat its
search.

A refresh may occur:

• At application startup
• On first use during a session
• On direct user request
• After an age threshold
• According to a future setting

The selected strategy should avoid delaying startup unnecessarily.

Refreshing a Search Category should preserve the user's current
experience where practical.

4.7 USER-DEFINED CATEGORY

A User-Defined Category is created manually.

Examples:

Bike Ride
Morning Coffee
Driving Home
Favorites for Mom
Sunday Morning
Sleep

The user controls:

• Name
• Station membership
• Station order
• Category order
• Navigation Enabled state
• Deletion

4.8 CATEGORY ORDER

Categories have a persistent user-visible order.

Category Previous and Next operations use that order after filtering
for eligibility.

Reordering categories changes future navigation order.

4.9 CATEGORY NAVIGATION STATE

Every category has one of two navigation states:

Navigation Enabled
Navigation Disabled

Navigation Enabled categories participate in:

• Next Category
• Previous Category
• Bluetooth category navigation
• Automatic advancement after category exhaustion

Navigation Disabled categories:

• Remain visible
• Remain directly selectable
• May still be played manually
• Are skipped during normal category navigation

4.10 STATION

A station represents one playable audio source.

A station may initially represent an Internet radio stream.

Possible fields include:

• Stable identifier
• Display name
• Stream URL
• Source type
• Genre
• Location
• Country or state
• Artwork URL
• Directory identifier
• Tags
• Homepage
• Codec or bitrate, when known
• Metadata capability

4.11 STATION IDENTITY

Two category memberships may refer to the same station record.

This avoids unnecessary duplication of:

• Stream URL
• Station name
• Artwork
• Directory metadata

Station identity should be stable when a directory provides a stable
identifier.

When no reliable external identifier exists, the application may use
a normalized URL or generated identifier.

4.12 STATION MEMBERSHIP

A station becomes part of a category through a membership record.

A membership includes:

• Category identifier
• Station identifier
• Position
• Navigation Enabled state

A membership may later include:

• Custom label
• Notes
• Added date
• Last played date
• Play count
• Category-specific volume adjustment

4.13 MEMBERSHIP NAVIGATION STATE

Each station membership is independently:

Navigation Enabled

or:

Navigation Disabled

This state affects navigation only within that category.

4.14 EXAMPLE OF CATEGORY-SPECIFIC NAVIGATION

The station KUSC Classical may appear in three categories.

Classical
    Navigation Enabled

Bike Ride
    Navigation Enabled

Morning Coffee
    Navigation Disabled

The station has not changed.

Only its behavior within each category differs.

4.15 DIRECT SELECTION OF DISABLED ITEMS

Navigation Disabled does not mean unavailable.

A user may directly tap:

• A Navigation Disabled category
• A Navigation Disabled station membership

The application should select and play the item normally.

The disabled state controls sequential navigation, not manual access.

4.16 CURRENT CATEGORY

The Current Category is the category in which station navigation is
taking place.

It determines:

• Which station list is active
• Which membership order is used
• Which membership navigation states apply
• Which category title is displayed
• Where Previous Station and Next Station operate

4.17 CURRENT STATION

The Current Station is the selected station within the Current
Category.

The current station may be:

• Playing
• Buffering
• Stopped
• Failed
• Waiting to play

The Current Station should remain identifiable even when playback is
stopped.

4.18 PLAYBACK STATE

The application should distinguish at least:

STOPPED
CONNECTING
BUFFERING
PLAYING
PAUSED, if supported
ERROR
RECOVERING

The user interface may present simplified language while the internal
state remains more precise.

4.19 SESSION-FAILED STATION

A station may be marked failed for the current application session
when:

• The stream throws an error
• The stream does not begin within the configured timeout
• The source is unreachable
• The media format cannot be played

The failure mark prevents repeated automatic selection of the same
broken stream during that session.

It should normally be cleared when the application starts a new
session.

4.20 ELIGIBLE STATION

A station membership is eligible for ordinary navigation when:

• It belongs to the Current Category
• Its membership is Navigation Enabled
• It has a playable source
• It is not excluded by an active failure-recovery rule

Direct user selection may override some automatic eligibility rules.

4.21 ELIGIBLE CATEGORY

A category is eligible for ordinary category navigation when:

• It is Navigation Enabled
• It contains at least one potentially playable station
• It is not temporarily excluded by failure recovery

A category with no Navigation Enabled station memberships may still
be opened manually.

4.22 SEARCH

A search is a request to discover stations matching user-supplied or
predefined text.

Search may match:

• Genre
• Station name
• Location
• Tags
• Directory metadata

A completed search normally creates or updates a Search Category.

4.23 SEARCH CHIP

A Search Chip is a compact shortcut representing a recent or saved
search.

Search chips:

• Appear below the search field
• Are horizontally scrollable
• Place the newest search at the left
• Can restore or activate the related search
• Persist according to the detailed search rules

4.24 COPY TO CATEGORY

The station-management action historically called Move is logically
a copy operation.

The source membership remains in its original category.

The station may be copied into:

• One existing category
• Several existing categories
• A newly created category

Copying creates new membership records.

It does not duplicate the underlying station unnecessarily.

4.25 DELETE STATION MEMBERSHIP

Deleting a station from a category removes that membership.

It does not necessarily delete the global station record.

If no category references the station afterward, the repository may
remove the unused global record as an internal cleanup operation.

4.26 SEARCH-RESULT MEMBERSHIP

Stations in a Search Category represent directory search results.

Deleting individual result memberships is normally disabled because
the next refresh could recreate them.

The user may instead:

• Copy useful stations elsewhere
• Disable their navigation within that Search Category
• Delete the entire Search Category

4.27 PLAYBACK DESTINATION

The Playback Destination identifies where audio is rendered.

Examples:

• Local device
• Bluetooth earbuds
• Bluetooth speaker
• Cast device
• Fire TV receiver
• Future remote player

Changing destination should not change category organization.

4.28 COMMAND

A command is a user or system intention.

Core commands include:

PLAY
STOP
TOGGLE_PLAYBACK
NEXT_STATION
PREVIOUS_STATION
NEXT_CATEGORY
PREVIOUS_CATEGORY
SELECT_STATION
SELECT_CATEGORY

Commands should be independent of the control that generated them.

4.29 WRAPAROUND

Sequential navigation normally wraps.

Examples:

• Next Station from the last eligible station returns to the first
  eligible station in that category.

• Previous Station from the first eligible station returns to the
  last eligible station.

• Next Category from the last eligible category returns to the
  first eligible category.

Wraparound must avoid infinite loops when no eligible destination
exists.

4.30 NO ELIGIBLE ITEM STATE

When no eligible station exists in the Current Category, the
application should communicate that state clearly.

Possible behavior includes:

• Displaying a no-playable-station message
• Offering direct access to the station list
• Advancing to another eligible category when playback recovery
  is active

When no eligible category exists, the application should display a
no-playable-category state rather than repeatedly cycling.

4.31 CONTENT VERSUS BEHAVIOR

The global station record describes content.

The category membership describes behavior in context.

The category record describes organization.

The navigation controller describes movement.

The playback controller describes audio execution.

Keeping these responsibilities distinct is central to Music1Chat.

DESIGN NOTE

Navigation Enabled does not mean liked.

Navigation Disabled does not mean disliked.

These states answer only:

Should this item participate when the user navigates
sequentially?

END OF CHAPTER 4

Related sections:

Part 2 — Main Screen, Search, and Category Management
Part 3 — Station Navigation and Playback
Part 4 — Persistence
Part 5 — Glossary and Design Rationale

══════════════════════════════════════════════════════════════════════
""".trimIndent()

@Suppress("unused")
val designSpec049 = """
══════════════════════════════════════════════════════════════════════

                  END OF SPECIFICATION PART 1

══════════════════════════════════════════════════════════════════════

FILE

DesignSpecificationPart1.kt

CONTENTS

• Cover page and mission statement
• Revision history
• Chapter 1 — Introduction
• Chapter 2 — Design Philosophy
• Chapter 3 — Overall Architecture
• Chapter 4 — Core Concepts

NEXT FILE

DesignSpecificationPart2.kt

PART 2 WILL DEFINE

• Main-screen structure
• Top application bar
• Search field
• Search autocomplete
• Search chips
• Current category card
• Now Playing card
• Navigation indicator behavior
• Category-list screen
• Category-details screen
• Category creation
• Category editing
• Category deletion
• Station copying workflow
• Major visual and interaction rules

══════════════════════════════════════════════════════════════════════
""".trimIndent()