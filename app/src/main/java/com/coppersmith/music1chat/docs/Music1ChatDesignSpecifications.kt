package com.coppersmith.music1chat.docs

object Music1ChatDesignSpecifications {

    const val LAST_UPDATED = "2026-07-10"

    val INTRODUCTION = """
        MUSIC1CHAT DESIGN BIBLE

        This is the source of truth for Music1Chat behavior and design.

        If the code and this document disagree, this document is considered
        correct until we deliberately change the design.

        Important behavior changes must also be recorded here.

        DOCUMENT STATUS

        Approved:
        • Design philosophy
        • Category model
        • Search-category persistence and refresh behavior
        • Category-list controls
        • Multiple-category station membership
        • Basic Move and Copy behavior
        • Station-heart behavior

        Partially approved:
        • Main screen
        • Now Playing area
        • Detailed Move and Copy dialogs
    """.trimIndent()

val Glossary = """
    Category
    A navigable collection of stations.

    Station
    One unique internet radio station.

    Membership
    A relationship between one category and one station.

    Heart
    Determines participation in automatic navigation.

    Search Category
    A persistent category whose contents are refreshed from a stored search.

    Eligible
    Playable and hearted.
    
    """.trimIndent()


    val DESIGN_PHILOSOPHY = """
        DESIGN PHILOSOPHY
        Status: APPROVED

        • Music1Chat is designed for people whose attention is occupied
          elsewhere.

        • Important use cases include:
            - Bicycle riding
            - Walking
            - Driving
            - Working
            - Exercising
            - Navigating
            - Other activities where the user should not study the screen

        • Common actions must require very little interaction.

        • Primary controls should be large, obvious, and easy to operate.

        • The user should be able to control music through:
            - Large on-screen buttons
            - Bluetooth controls
            - Other supported external media controls

        • The app should provide substantial control without requiring the
          user to repeatedly remove the phone from a pocket.

        • Stations should be organized according to how the user actually
          listens, not only by musical genre.

        Examples:
            - Classical
            - Jazz
            - Bike Ride
            - Bike Ride 1
            - Relaxing
            - Road Trip
            - Work
            - News

        • Frequent actions should be immediately available.

        • Less common actions should remain available without cluttering the
          main listening screen.

        Rationale:

        Music1Chat began largely because existing music applications did not
        provide the simple category and station navigation needed while
        bicycle riding.
    """.trimIndent()

    val CATEGORY_MODEL = """
        CATEGORY MODEL
        Status: APPROVED

        • Every navigable collection of stations is called a Category.

        • The word Category should be used consistently throughout the app.

        • Do not introduce a separate user-facing term such as Folder or
          Collection unless a later design decision changes the terminology.

        • Categories may originate in different ways:
            - Standard categories
            - User-created categories
            - Search categories

        • Category origin may affect internal behavior, but all are presented
          to the user as categories.

        • A standard category such as Classical is not fundamentally
          different from a user-created category such as Ken's Bike Ride.

        • All categories are persistent.

        • A category remains available until the user deliberately deletes it.

        • Every category may be hearted or unhearted.

        • A hearted category participates in Previous Category and
          Next Category navigation.

        • An unhearted category remains in the app but is skipped during
          category navigation.

        • Category navigation may be initiated through:
            - Main-screen controls
            - Bluetooth controls
            - Other supported media controls

        Rationale:

        The user should not need to remember different navigation rules for
        different kinds of categories. All categories should feel consistent.
    """.trimIndent()

    val STATION_IDENTITY = """
    STATION IDENTITY
    Status: APPROVED

    • A station has one application-wide identity.

    • The preferred identity is the stable station ID or UUID supplied by
      the radio-station directory.

    • If no stable directory ID is available, the normalized stream URL may
      be used as a fallback identity.

    • Station name alone must not be used as identity because:
        - Different stations may have the same name.
        - A station may change its displayed name.
        - Search results may format the same name differently.

    • When a newly discovered result matches an existing station:
        - Reuse the existing station record.
        - Refresh directory-controlled information when appropriate.
        - Do not create a duplicate station.

    • Directory-controlled information may include:
        - Station name
        - Stream URL
        - Artwork URL
        - Homepage
        - Country
        - Language
        - Codec
        - Bitrate

    • User-controlled information must be preserved during refresh:
        - Station heart state
        - Category memberships
        - Position within each category
        - Any user-supplied name or note added later

    • If a directory changes a station's ID unexpectedly but the stream URL
      still matches, the app should attempt to reconcile the records rather
      than immediately creating a duplicate.

    Rationale:

    Search results may rediscover the same station many times. Stable identity
    prevents duplicates while allowing station information to stay current.
""".trimIndent()


    val STATION_MEMBERSHIP = """
        STATION MEMBERSHIP
        Status: APPROVED

        • A station may belong to more than one permanent category.

        Example:

        A station may belong to:
            - Classical
            - Bike Ride
            - Relaxing Evening

        • The station itself should not be duplicated internally merely
          because it belongs to multiple categories.

        • Category membership is a relationship between a station and a
          category.

        • Adding a station to another category does not interrupt playback.

        • Adding a station to another category does not automatically change
          the current category.

        • A station saved from a search category remains in the current
          search results.

        Rationale:

        People organize stations according to both genre and activity. The
        same station may reasonably belong to several categories.
    """.trimIndent()


    val SEARCH_CATEGORIES = """
        SEARCH CATEGORIES
        Status: APPROVED

        • Search categories are persistent.

        • A search category does not disappear when the app closes.

        • The user may delete a search category from the category list.

        • A search category permanently stores its search term.

        Examples:
            - Search: Classical
            - Search: Jazz
            - Search: Swing

        • The results of a search category are refreshed during every app
          session.

        • Search categories should be loaded lazily.

        • The app does not perform every saved search during startup.

        • The first time the user enters a search category during an app
          session:
            - Run the stored search again.
            - Replace the previous session's search results.
            - Mark the category as refreshed for the current session.

        • Returning to the category during the same app session reuses the
          current results.

        • The next app session causes another refresh the first time the
          category is entered.

        • Stations may appear, disappear, or change because internet-radio
          stations change over time.

        • Saving a station from a search category does not remove it from the
          search results.

        • The user remains in the search category after saving a station
          unless the user deliberately changes categories.

        Rationale:

        The category and its search term should persist, while its results
        remain fresh. Lazy loading keeps startup quick and avoids performing
        searches the user may never visit during that session.
    """.trimIndent()

    val Navigation = ""

    val CATEGORY_NAVIGATION = """
    CATEGORY NAVIGATION
    Status: APPROVED

    • Next Category moves forward through the ordered category list.

    • Previous Category moves backward through the ordered category list.

    • Normal category navigation includes only hearted categories.

    • An unhearted category:
        - Remains visible in the category list.
        - Retains all of its stations and settings.
        - Is skipped by category-navigation controls.

    • A category is eligible for playback navigation when:
        - The category is hearted.
        - It contains at least one eligible station.

    • If navigation reaches a saved search category that has not yet been
      refreshed during the current app session:
        - Perform that category's saved search.
        - Store the refreshed results for the current session.
        - Then determine whether it contains an eligible station.

    • If a hearted category contains no eligible stations:
        - Do not stop on that category during low-attention navigation.
        - Continue automatically to the next eligible category.

    • A category with no eligible stations remains available for:
        - Direct selection
        - Viewing its station list
        - Editing
        - Adding stations
        - Retrying or refreshing a saved search

    • Category navigation wraps.

    • Next Category after the final eligible category moves to the first
      eligible category.

    • Previous Category before the first eligible category moves to the final
      eligible category.

    • Changing categories selects an eligible station within the destination
      category.

    • The preferred station-selection behavior is:
        - Resume the last successfully played station in that category when
          it is still eligible.
        - Otherwise select the first eligible station in that category's
          stored order.

    • If no hearted category contains an eligible station:
        - Stop automatic navigation.
        - Stop playback.
        - Display a clear message that no playable stations are currently
          available.

    • An empty or temporarily unusable category is not automatically:
        - Unhearted
        - Deleted
        - Renamed
        - Permanently marked invalid

    Rationale:

    Category navigation is intended for low-attention use. It should move
    smoothly past categories that cannot currently play while preserving those
    categories for later management or recovery.
""".trimIndent()


    val CATEGORY_PLAYBACK_MEMORY = """
    CATEGORY PLAYBACK MEMORY
    Status: APPROVED

    • Each category remembers the last station successfully played within it.

    • When the user returns to a category:
        - Resume that station when it is still present and eligible.
        - Otherwise choose the first eligible station in category order.

    • The remembered station belongs to the category membership, not merely
      to the global station record.

    • The same station may therefore be remembered independently in several
      categories.

    • Removing the remembered station from a category clears or replaces that
      category's remembered playback position.

    • Reordering stations does not erase the remembered station.

    • A failed station should not become the remembered successful station.

    Rationale:

    Returning to a category should feel like returning to where the listener
    left off, while still recovering gracefully when that station is no
    longer available.
""".trimIndent()

    val STATION_NAVIGATION = """
    STATION NAVIGATION
    Status: APPROVED

    • Every category has its own ordered list of station memberships.

    • Next Station moves forward through the current category's membership
      order.

    • Previous Station moves backward through the current category's
      membership order.

    • Normal station navigation includes only stations that are currently
      eligible for navigation.

    • A station is eligible when:
        - It is hearted.
        - It is not currently marked unavailable or failed.
        - It has a usable stream location.

    • An unhearted station remains in the category.

    • An unhearted station remains visible in the station list.

    • An unhearted station is skipped by:
        - Next Station
        - Previous Station
        - Bluetooth station-navigation controls
        - Other equivalent external media controls

    • The user may still select and play an unhearted station directly from
      the station list.

    • Navigation wraps within the current category.

    • Next Station from the final eligible station moves to the first eligible
      station.

    • Previous Station from the first eligible station moves to the final
      eligible station.

    • Station navigation does not automatically change categories.

    • Station order is stored in CategoryMembership rather than in the Station
      itself.

    • Reordering a station in one category does not alter its position in any
      other category.

    Rationale:

    Hearts define which stations participate in low-attention navigation while
    allowing every station to remain available for direct selection and
    management.
""".trimIndent()

    val STATION_FAILURE_RECOVERY = """
    STATION FAILURE RECOVERY
    Status: APPROVED

    • A playback failure does not automatically unheart or delete a station.

    • When the current station fails:
        - Record the failed attempt for the current session.
        - Attempt the next eligible station in the current category.
        - Keep the user in the same category.

    • During one recovery sequence, the app should not repeatedly retry a
      station that has already failed in that sequence.

    • A temporary failure is session state and does not permanently change the
      station unless a later feature explicitly confirms that the station is
      unavailable.

    • If another station plays successfully, normal navigation resumes from
      that station.

    • If every eligible station in the category fails:
        - Stop automatic recovery.
        - Stop playback.
        - Display a clear message that no playable stations are currently
          available in the category.

    • The user may manually retry a failed station.

    Rationale:

    Internet-radio streams often fail temporarily. Recovery should keep the
    music moving without destroying the user's saved organization.
""".trimIndent()

val User_Interaction = ""

    val CATEGORY_LIST = """
        CATEGORY LIST
        Status: APPROVED

        • Tapping the current category title opens the category list.

        • The list displays every saved category.

        • Each category row contains:
            - Category name
            - Heart button
            - Station-list button
            - Trash button

        • Heart button:
            - Filled heart means the category participates in navigation.
            - Outline heart means the category is skipped.
            - Changing the heart does not alter or delete the category.

        • Station-list button:
            - Opens the stations contained in that category.
            - For a search category, it shows the current session's results.

        • Trash button:
            - Deletes the category after appropriate confirmation.
            - Search categories may be deleted.
            - User-created categories may be deleted.
            - Final rules for standard-category deletion are not yet settled.

        • The user may open category details from this list.

        • Category details may eventually include:
            - Rename
            - Station count
            - Category origin
            - Search term
            - Ordering options
            - Other advanced settings
    """.trimIndent()

    val DIRECT_CATEGORY_SELECTION = """
    DIRECT CATEGORY SELECTION
    Status: APPROVED

    • Tapping a category in the category list immediately makes it the
      current category.

    • Direct selection is allowed whether the category is hearted or
      unhearted.

    • A category heart controls whether the category participates in
      Previous Category and Next Category navigation.

    • A category heart does not prevent the user from selecting that category
      directly.

    • After a category is selected:
        - Close the category list.
        - Display the selected category on the main screen.
        - Select the category's remembered station when it is still eligible.
        - Otherwise select the first eligible station in category order.

    • If the selected category is a saved search category that has not yet
      been refreshed during the current session:
        - Run its saved search.
        - Replace the prior session's results.
        - Then select a station from the refreshed results.

    • Directly selecting a category does not automatically change its heart
      state.

    • Directly selecting a category with no eligible station is permitted.

    • If the selected category has no eligible station:
        - Keep that category selected.
        - Do not automatically jump to another category.
        - Stop playback if necessary.
        - Show a clear empty or unavailable state.
        - Allow the user to view, edit, refresh, or add stations.

    • This differs intentionally from low-attention category navigation.

    • Previous Category and Next Category automatically skip categories that
      cannot currently play.

    • Direct selection respects the user's explicit choice and remains in the
      selected category even when it cannot currently play.

    Rationale:

    Automatic navigation should keep music playing, but a direct tap is an
    explicit request to open a particular category. The app should honor that
    request rather than silently moving somewhere else.
    
""".trimIndent()

    val DIRECT_STATION_SELECTION = """
    DIRECT STATION SELECTION
    Status: APPROVED

    • Tapping a station in a station list immediately selects that station.

    • Direct selection is allowed whether the station is hearted or
      unhearted.

    • A station heart controls participation in Previous Station and
      Next Station navigation.

    • A station heart does not prevent direct playback.

    • Selecting a station:
        - Makes its category the current category.
        - Makes that station the current station.
        - Attempts playback immediately.
        - Closes the station list and returns to the main screen.

    • A successful direct selection becomes the category's remembered
      station.

    • If the selected station fails:
        - Show the failure clearly.
        - Keep the selected category and station visible.
        - Do not automatically delete or unheart the station.

    • Because the user explicitly selected the station, the app should not
      immediately leave it without giving visible feedback.

    • After reporting the failure, the app may offer or perform recovery
      according to the approved playback-recovery rules.

    • Exact timing of automatic recovery after a direct selection remains an
      implementation detail to be tested for usability.

    Rationale:

    Hearts define hands-free navigation, not permission. The user must always
    be able to directly choose any saved station.
""".trimIndent()

    val MOVE_AND_COPY = """
        MOVE AND COPY
        Status: APPROVED IN PRINCIPLE

        • Move and Copy remain separate user operations.

        • When a station is found through a search, Move normally saves it
          into the corresponding permanent category.

        Example:

        Current category:
            Search: Classical

        Likely Move destination:
            Classical

        • The app should intelligently preselect or highlight the most likely
          destination category.

        • The search term provides the strongest clue for the default
          destination.

        • Moving a station from a search category does not remove it from the
          search results.

        • In this situation, Move means saving the station into its expected
          primary permanent category. It does not literally remove it from the
          search category.

        • Copy adds the station to an additional category.

        Example:

        A station found in Search: Classical may be:
            - Moved or saved to Classical
            - Copied to Bike Ride

        • The destination list includes Create New Category at the top.

        • The user may create any number of categories.

        Examples:
            - Bike Ride
            - Bike Ride 1
            - Bike Ride 2

        • A newly created category immediately becomes available as a Move or
          Copy destination.

        • Move and Copy do not interrupt playback.

        Still to decide:
            - Exact dialog layout
            - Confirmation behavior
            - Whether Copy permits multiple destinations at once
    """.trimIndent()

    val Hearting = ""

    val CATEGORY_HEART = """
    CATEGORY HEART
    Status: APPROVED

    • A category may be hearted or unhearted.

    • Tapping the category heart toggles its state.

    • A hearted category participates in Previous Category and
      Next Category navigation.

    • An unhearted category remains in the application but is skipped during
      category navigation.

    • Hearting or unhearting a category does not affect:
        - The stations it contains.
        - Playback.
        - Category membership.
        - Search behavior.

    • A category may be selected directly whether it is hearted or
      unhearted.

    • The category heart affects automatic navigation only.

    Rationale:

    Hearts allow users to customize low-attention navigation while preserving
    every category for direct access and management.
""".trimIndent()

    val STATION_HEART = """
        STATION HEART
        Status: APPROVED

        • A station may be hearted or unhearted.

        • Tapping the station heart toggles its state.

        • Hearting or unhearting a station does not interrupt playback.

        • Hearting or unhearting a station does not automatically change
          categories.

        • The station-heart behavior used during navigation must remain
          consistent with the approved playback rules.

        • Additional edge cases will be documented during implementation.
    """.trimIndent()

    val STATION_DELETION = """
    STATION DELETION
    Status: APPROVED

    • Deleting a station from a category normally removes only that station's
      membership in the current category.

    • Removing a station from one category does not remove it from any other
      category that contains it.

    • Removing a station from a category does not automatically delete the
      shared station record.

    Example:

    If BBC Radio 3 belongs to:
        - Classical
        - Bike Ride
        - Relaxing

    deleting it from Bike Ride leaves it in:
        - Classical
        - Relaxing

    • The station record may be removed from application storage only when:
        - It no longer belongs to any permanent category.
        - It is not required by any saved user data.
        - It is not currently needed by an active search result or playback
          session.

    • Search-category results are different from permanent memberships.

    • Removing a station from a search category should not normally be
      interpreted as deleting the station from the directory or permanently
      blocking it from future searches.

    • A future Block or Hide feature may allow users to suppress unwanted
      stations from recurring search results.

    • Removing the currently playing station from its current category:
        - Must not abruptly interrupt playback merely because membership was
          removed.
        - Keeps the stream playing until the user changes station, playback
          ends, or another approved rule requires a change.
        - Removes that station from future navigation within that category.

    • If the removed station was the category's remembered station:
        - Replace the remembered station after another station plays
          successfully.
        - Clear it if no replacement exists.

    • Deletion should require confirmation when the action may be difficult
      to reverse or may affect several memberships.

    Rationale:

    A station is shared across categories. Most station deletion actions are
    therefore membership changes, not destruction of the underlying station.
""".trimIndent()


    val CATEGORY_DELETION = """
    CATEGORY DELETION
    Status: APPROVED IN PRINCIPLE

    • Deleting a category removes the category and its station-membership
      records.

    • Deleting a category does not delete shared station records that are
      still used by other categories.

    • The app must clearly identify the category being deleted.

    • Category deletion requires confirmation.

    • The confirmation should explain that:
        - The category itself will be removed.
        - Stations shared with other categories will remain there.
        - The operation does not delete internet-radio stations themselves.

    • If the deleted category is the current category:
        - Select the next eligible hearted category when one exists.
        - Otherwise select the previous eligible category.
        - If no eligible category exists, show "Please search for a genre or station to begin listening"

    • Deleting a search category removes:
        - The saved search category
        - Its stored search term
        - Its current session results
        - Its category-specific playback memory

    • Deleting a user-created category removes:
        - The category
        - Its memberships
        - Its category-specific station order
        - Its category-specific playback memory

    • Standard-category deletion rules may be restricted later if standard
      categories are required by the application.

    Rationale:

    Category deletion should remove the user's organizational container without
    destroying station information that remains useful elsewhere.
""".trimIndent()


    val MAIN_SCREEN = """
        MAIN SCREEN
        Status: PARTIALLY APPROVED

        Known main-screen elements include:

        • App title area

        • Search field

        • Search suggestions or selectable search items

        • Current category display

        • Now Playing area

        • Station artwork when available

        • Station name

        • Song title, artist, or stream metadata when available

        • Station heart

        • Large playback controls:
            - Previous category
            - Previous station
            - Play or Stop
            - Next station
            - Next category

        • The central Play or Stop button should be especially large and
          visually obvious.

        • Single-arrow controls move between stations.

        • Double-arrow controls move between categories.

        • The screen must remain useful when a station provides:
            - Full artwork and song metadata
            - Only its station name
            - Little or no metadata

        • Long station or song text should scroll or otherwise remain
          readable.

        • Exact Now Playing behavior still needs to be defined.
    """.trimIndent()

    val IMPLEMENTATION_PRINCIPLES = """
        IMPLEMENTATION PRINCIPLES
        Status: APPROVED

        • Important design decisions should be recorded before or alongside
          implementation.

        • Reusable Compose components should be used where appropriate.

        Likely reusable components:
            - HeartButton
            - CategoryRow
            - StationRow
            - NowPlayingCard
            - PlaybackControls
            - SearchField
            - StatusFooter

        • UI code should avoid unnecessary copies of the same behavior.

        • Persistent data and temporary session data must be clearly
          separated.

        Persistent examples:
            - Category name
            - Category heart state
            - Stored search term
            - User-created station memberships

        Session-only examples:
            - Whether a search category was refreshed this session
            - Current search results
            - Temporary playback state unless deliberately persisted
    """.trimIndent()

    val OPEN_QUESTIONS = """
        OPEN DESIGN QUESTIONS
        Status: UNDER DISCUSSION

        • Exact Now Playing card behavior

        • Station metadata priority:
            - Song title
            - Artist
            - Station name
            - Other stream-provided text

        • Artwork fallback behavior

        • Exact Move dialog design

        • Exact Copy dialog design

        • Whether Copy supports one or several destinations at once

        • Rules for deleting standard categories

        • Category rename rules

        • Category ordering:
            - Alphabetical
            - Manual
            - Hearted first
            - Another method

        • Station ordering within permanent categories

        • Station ordering within search categories

        • Confirmation rules for deleting categories or stations

        • Detailed Bluetooth-button mapping

        • Behavior when a category contains no playable stations
    """.trimIndent()


    val DESIGN__CHANGE_LOG ="""
    ===============================================================================

    2026-07-10

    Version 0.10

    • Initial Design Bible created.
    • Category architecture defined.
    • Search-category model approved.
    • Navigation rules approved.
    • Membership architecture approved.
    • Playback recovery approved.
    """.trimIndent()

}