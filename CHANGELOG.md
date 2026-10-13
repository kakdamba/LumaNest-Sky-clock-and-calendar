# Changelog — LumaNest

All notable changes to this project will be documented in this file.

## [V.5] — 2026-10-12

### 🌟 Highlights & Features
- **New In-App Logo ("Mystical Candle Shrine in Midnight Blue")**:
  - Fully replaced the old green dragon placeholder with custom high-resolution artwork (`ic_app_logo.png`).
  - Seamlessly bound to the Home Screen top header (42dp circle) and Settings Screen "About LumaNest" card (28dp circle).
- **"Emoji Timers Only" In-Game Mode**:
  - Minimalist floating display at top center with zero progress bars or arcs.
  - Active event reverse countdown: tracks remaining stay in reverse (e.g. `👵 10:00 -> 00:00`).
  - Multi-event overlap: displays active events and upcoming events side-by-side (e.g. `👵 04:30 left  ·  🐢 in 05:00`).
  - Strict 5-minute cap: upcoming events will never display earlier than 5 minutes before start.
  - Auto-dismissing badges: badges vanish cleanly the second an event ends.
- **3-Segment On-Screen Style Selector in Settings**:
  - Compact single-row segmented control: `[ 🕊️ Wing Arc ]  [ ⏱️ Emoji Timers ]  [ 📏 Bottom ]`.
  - Contextual preview actions: "Wing Arc" opens the live alignment tool; "Emoji Timers" and "Bottom" provide dedicated 10-second test previews.
- **DEC-005 Golden Rule Implementation**:
  - Enforced dynamic version resolution from Gradle (`AppUpdater.getInstalledVersionName(context)`).
  - Completely eliminated hardcoded version numbers in UI and updater checks, fixing false "New Update Available" alerts on updated devices.
- **Official App Version Bump**:
  - Upgraded codebase and configs uniformly to **V.5** (`versionCode = 5`).
- **💎 Custom Crystal Shard PNG Emojis**:
  - Replaced flat colored circle emojis (`🔴` / `⚫`) with custom high-resolution PNG artworks:
    - **Red Shards**: `Glowing Ruby Crystal Shard Cluster.png` (`emoji_shard_red.png`)
    - **Black Shards**: `Obsidian Crystal Trio in Shadow.png` (`emoji_shard_black.png`)
  - Integrated with the **Home Screen** Hero card title, Next Event preview, and Today's Schedule rows.
  - Implemented inline `ImageSpan` rendering for in-game floating overlay countdowns and `Emoji Timers` so crystal clusters display smoothly right on screen while playing Sky.
- **🕯️ Sanctuary Ambiance ("✨ Starlight Drift" & "Sky Paper Lanterns")**:
  - Renamed from "Aero Shards" to **"✨ Starlight Drift"** for a cozy, peaceful aesthetic.
  - **Multi-Screen Stream**: Starlight Drift ambient luminous crystals now flow smoothly across **Home**, **Calendar**, and **Settings** tabs.
  - **Friends Tab "Sky Paper Lanterns"**: Authentic floating Sky Wish Lanterns (9 serene lanterns across the screen) with translucent glowing parchment and warm candle flames flickering inside, gently ascending and swaying in the night breeze.
  - **3-Layer Performance & Battery Shield**: Automatically freezes to 0% CPU and 0% battery when minimized or screen locked. Controlled via master toggle in Settings.
- **📳 Tactile Haptic Vibration Feedback**:
  - Added subtle, satisfying haptic clicks (`CLOCK_TICK`) across all toggles, switches, and style selectors in Settings (12h/24h toggle, Event Reminders master switch, individual event notifications, Quiet Hours switch, Live Event Progress Bar switch, On-Screen style selector, countdown duration pills, and Starlight Drift toggle).
  - Added gentle haptic feedback during friend card dragging and swapping.
- **🕒 Chronological Friends Ordering (Option 1)**:
  - Friend timezone cards are now sorted chronologically by current local time (00:00 to 23:59 earliest to latest).
  - **"You" in Sequence**: The user's own local time card is now placed naturally in its chronological spot rather than pinned to the top.
  - **Drag-to-Reorder Preserved**: Users can still hold and drag cards to custom positions at any time, with a convenient "🕒 Sort Time" pill to revert back to chronological order whenever desired.
- **🍎 macOS-Style Interactive Dock Navigation**:
  - Replaced standard bottom navigation with a floating frosted glass capsule dock.
  - **Launch Bounce / Hop**: Icons joyfully hop and rebound with macOS spring physics upon being tapped.
  - **Smooth Magnification & Indicator Dot**: Active tab magnifies smoothly with a glowing amber indicator dot underneath.
  - Full badge support preserving the in-app GitHub update badge ("1") on Settings.
- **🐛 Bug Fix — Double Treasure Candles Event**:
  - Removed unverified `double_candles_autumn_2026` event from the seed database (`events_seed.json`), resolving false "RUNNING" badges on the Calendar and Upcoming cards on Home Screen.

## [V.4] — 2026-10-09

### 🌟 Highlights & Features
- **100% Accurate Shard Eruption Engine**:
  - Fully integrated Plutoy's verified `sky-shards.pages.dev` rotation algorithm into `SkyEventEngine.kt`.
  - Fixed realm rotation drift using exact `(dayOfMonth - 1) % 5` indexing.
  - Implemented exact Red vs. Black pattern determination:
    - Red Shard cycles: 07:40, 02:20, and 03:30 Pacific Time (6h intervals).
    - Black Shard cycles: 01:50 and 02:10 Pacific Time (8h intervals).
  - Exact 3-window daily recurrence with +8m 40s gate landing offsets and 4-hour active windows.
  - Calibrated exact realm and sub-area locations (e.g. *Crab Field*, *Ice Rink*, *Starlight Desert*, *Sanctuary Island*, *Forgotten Ark*).
  - Accurate Ascended Candle rewards (✦ ~2.0 to ~3.5 AC) for Red Shards and standard wax for Black Shards.
  - Strict "No Shard Day" weekday rules.
- **Next Event & Live Countdown Home Screen Widget**:
  - Built with Jetpack Compose Glance, adapting across all launcher grid sizes (compact 2x1 pill to large 4x2+ banners).
  - Displays high-resolution 4:5 community watercolor PNG artworks (**Geyser**, **Grandma**, **Turtle**, **Shards**) in medium and large widget sizes.
  - Strict real-time focus: Tracks live wax events (Geyser, Grandma, Turtle) and Shard Eruptions; excludes Daily Reset and Traveling Spirit.
  - Live countdown timers (`Active now XX:XX left` / `Starts in XXm XXs`) with one-tap quick launch into LumaNest.
- **Top Header Time Pill Polish (Option A)**:
  - Replaced crowded side-by-side time and city text with a clean, centered two-line stack:
    - Bold amber local time on top.
    - Clean city/region beneath with single-line constraint to prevent text wrapping.
- **Settings Update Badge ("1")**:
  - Embedded a glowing amber notification badge on the bottom navigation Settings icon when an app update is available on GitHub.
- **Official App Version Bump**:
  - Upgraded codebase and configs uniformly to **V.4** (`versionCode = 4`).

## [V.3] — 2026-10-09

### 🌟 Highlights & Polish
- **Event Reminders & Smart Notification System**:
  - Full Android `AlarmManager` exact background scheduling for Sky daily events (**Polluted Geyser**, **Grandma's Dinner**, **Sanctuary Turtle**, **Shard Eruptions**, and **Daily Reset**).
  - Configurable reminder window (**5 min**, **10 min**, or **15 min before**) with individual event toggles in Settings.
  - **Smart In-Game Silence Rule**: Notifications are automatically silenced while playing Sky with the Floating Countdown active to prevent disruptive popups.
  - **Quiet Hours Protected**: Reminders strictly respect user-defined Quiet Hours (e.g. night sleep window).
  - Runtime `POST_NOTIFICATIONS` permission prompt for Android 13+ (API 33+).
- **Official Production App Icon Everywhere**:
  - Replaced default Android Studio robot placeholders with custom high-resolution adaptive vector/webp icons across all screen densities (`mdpi` through `xxxhdpi`) and `anydpi-v26`.
  - Icon now displays properly across system App Info, Installed App List, and Home launchers.
- **Home Screen Top Card Gradient Fix**:
  - Replaced the harsh 85dp bottom black box with a smooth, seamless multi-stop vertical vignette gradient (`0% -> 35% -> 60% -> 85%`).
  - Light watercolor paintings (**Polluted Geyser**, **Grandma's Dinner**, and **Sanctuary Turtle**) now blend cleanly into the card border with zero abrupt cut lines.
- **Floating Progress Bar (Sky Running Mode) Stability**:
  - Stabilized `UsageStats` foreground detection with session state persistence and a 15-second grace window, preventing the progress bar from randomly disappearing during gameplay.
  - Excluded long 4-hour / 50-minute Shard Eruptions from the floating overlay, focusing strictly on fast recurring social wax events (**Geyser 15m**, **Grandma 10m**, **Turtle 10m**) and eliminating 99+ minute timer overflows.
- **New Artwork Integration**:
  - Converted and bound 3 brand-new community watercolor illustrations to their respective events:
    - 🌊 **Season of Pearl** (`art_season_pearl.jpg`)
    - 🎃 **Days of Mischief** (`art_days_mischief.jpg`)
    - 🕯️ **Double Treasure Candles** (`art_double_candles.jpg`)
- **Friends Privacy & UI Polish**:
  - Completely removed mock/demo friend entries (`Orange`, `Janet`, `Haru`). New users start with 100% private, clean lists with only their device time ("You").
  - Fixed button text wrapping on the Friends screen (**`+ Add`**) and Calendar screen (**`.ics`**).
- **Official App Version Bump**:
  - Upgraded codebase and configs uniformly to **V.3** (`versionCode = 3`).

### Milestone Release Highlights
- **In-Game Bottom Progress Bar (Sky Running Mode)**:
  - Full-width, glowing warm-orange-to-red depleting progress bar pinned directly to the absolute bottom screen edge (`y = 0`) across all device aspect ratios.
  - Smooth rounded corner cap on the depleting head to eliminate harsh rectangular cuts.
  - Live digital countdown clock (`07:46 left`) with bold contrast and drop shadow.
  - Pre-event early travel warning countdown selector (**5 min, 2 min, 1 min, 30s**) so players have plenty of time to travel to areas before wax burns.
  - Background overlay strictly bound to foreground gameplay (`com.tgc.sky.android`), completely invisible on home screen and other apps.
  - 10-second in-game test preview button in Settings.
- **Official Launcher & In-App Branding**:
  - Integrated custom LumaNest app icon (`App icon.ico`) across all Android production launcher resolutions (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`).
  - Embedded circular LumaNest app logo inside Home and Settings headers, replacing previous placeholder mascot.
- **Accurate Traveling Spirit Integration**:
  - Confirmed and updated bi-weekly visiting spirit to **Talented Builder** (Season of Flight) with official cosmetic details.
  - Calibrated Tuesday announcement time to 11:30 AM PT (12:00 AM IST midnight) with a strict 5-minute reveal window before transitioning to upcoming calendar status.
  - Kept *Today's Schedule* clean by only presenting TS Reveal if happening within the current 24-hour cycle.
- **Friend Timezone Persistence & Management**:
  - Full-featured SQLite persistence for all cards including local user (`You`), preserving dragged custom ordering across screen navigation.
  - Double-tap gesture on any friend card to edit name, timezone, and country flag.
- **Heartfelt Community & Settings Polish**:
  - Clean "About LumaNest" story card without dropdowns.
  - Integrated `[ ☕ Buy me a Kheer ]` supporter action, external links, TGC fan-made disclaimer, and balanced footer (`Made with 🍊 and 🌻 Built by KAKDAMBA`).

## [V.1-Beta] — 2026-10-06

### Added
- **In-Game Slim Bottom Progress Bar (Sky Running Mode)**: Full-width, glowing orange-to-red loading bar pinned to the bottom of the screen during gameplay, with a digital countdown clock (`07:46 left`) on the bottom-right corner. Strictly respects Home filters and excludes Daily Reset & TS Reveal. Includes a 5-second test preview button and permission prompt in Settings.
- **Friends Persistent Ordering & Double-Tap Edit**: "You" is now a true persistent card with your country flag. Cards sort chronologically by GMT offset by default, and double-tapping any friend card opens the editor dialog to rename or update timezone.
- **Heartfelt About & Support Section**: Expandable "About LumaNest" story card, `[ ☕ Buy me a Kheer ]` supporter button, clickable links, disclaimer, and creator footer (`Made with 🍊 and 🌻 Built by KAKDAMBA`).
- **Next TS Reveal Collision Fix**: Resolved text wrapping and timer mashup in `NextEventPreviewCard` and assigned the 🎭 mask icon.
- **Sky Running Mode Live Floating Overlay**: Built an Android Foreground Service (`FloatingCountdownService`) with `SYSTEM_ALERT_WINDOW` permission that displays a sleek, draggable floating pill over other apps (including the Sky game) counting down before and during events.
- **Glowing Depleting Event Progress Bar**: Added a live gradient progress bar (bright orange to ruby red) with a time-remaining clock at the bottom of active event cards on the Home screen that empties in real time as the event progresses.
- **Silent In-App GitHub Updater**: Integrated background release checking against GitHub releases with a bright orange one-tap update button (`📥 Download & Update Now`) that downloads and prompts package installation directly without leaving the app.
- **Unified Calendar Category Tags on Home**: Styled all category tags under *Upcoming Calendar Events* with crisp, high-contrast white text on solid vibrant backgrounds.
- **Traveling Spirit Reveal Schedule**: Added a recurring Tuesday schedule item (`🎭 Traveling Spirit Reveal`) occurring at 12:00 PM Pacific Time (12 hours after daily reset) when TGC officially unveils the visiting spirit's identity and cosmetics, fully integrated with device timezone conversion and the Home filter system.
- **Sky Calendar Category Tags**: High-contrast, solid vibrant badges with bold white text across all event cards:
  - Season: Solid Orange (`#EA580C`)
  - Traveling Spirit: Solid Blue (`#0284C7`)
  - Major Events: Solid Purple (`#9333EA`)
  - Double Currency: Solid Ruby Red (`#DC2626`)
  - Shard Eruptions: Solid Crimson (`#E11D48`)
  - Maintenance: Solid Amber (`#D97706`)
- **Friends Drag & Drop Overhaul**:
  - Removed pinned "Me" header card and King 👑 icon.
  - Added "You" as a reorderable peer item in the draggable list so you can drag yourself up and down anywhere among your friends.
  - Added live 1-second wall-clock ticker ensuring zero minute lag across all friend clocks and local device time.
- **Sky Event Engine** (`:core:eventengine`): Pure, deterministic calculations for daily recurring cycles (Polluted Geyser, Grandma's Dinner, Sanctuary Turtle, Daily Reset) and multi-day calendar events. Fully verified via automated JVM unit tests.
- **Timezone System** (`:core:timezone`): IANA timezone handling (`America/Los_Angeles` Sky reset anchor, device default detection, and manual override).
- **Core Models** (`:core:models`): Normalized `SkyEvent`, `EventOccurrence`, `Friend`, and `EventDatabaseMeta`.
- **Local Offline Database** (`:app:data:local`): Room SQLite entities and DAOs for events and friend storage with pre-seeded verified JSON (`2026.10.06`).
- **Android Jetpack Compose UI**:
  - `HomeScreen`: Top timezone indicator, featured next/active card with live ticking countdown, daily events list, and friend time conversion previews.
  - `CalendarScreen`: Category filters (Seasons, Traveling Spirits, Major Events), verification badges, and privacy-first `.ics` calendar export.
  - `FriendsScreen`: Local on-device friend timezone coordination for "Play Together".
  - `SettingsScreen`: LumaNest status verification, quiet hours toggle, and Sky running mode settings.
- **Home Screen Widget**: Android Glance AppWidget (`LumaNestGlanceWidget`) showing live Sky event countdowns.
- **Build & Android Studio Integration**: Full Gradle modular setup with Kotlin 2.0 Compose compiler, JDK 21 toolchain, and successful `app-debug.apk` compilation.
- **Time Format Toggle**: Added 12-Hour (AM/PM) vs. 24-Hour mode selector in Settings, updating all clocks across Home, Schedule, and Friends screens.
- **Global Country & Timezone Search**: Implemented a comprehensive world timezone registry with live search for 40+ countries/regions including Japan (Tokyo), Russia (Moscow, Vladivostok, Yekaterinburg, Novosibirsk), USA, India, UK, etc., with flag emojis and IANA IDs.
- **Friend Long-Press Drag & Drop**: Replaced arrow buttons with modern touch drag-and-drop gesture (`detectDragGesturesAfterLongPress`) featuring live scale & elevation animations, smooth vertical translation, and persistent database sort order.
- **UI Polish**: Re-designed Home layout inspired by modern companion dashboards with glassmorphic cards, gradient night backgrounds, clear status badges, and "Today's Schedule".
- **Red & Black Shard Eruption Engine (PlutoyDev Spec)**:
  - Exact 5-pattern rotation matrix implemented matching PlutoyDev's `shard.ts` & `ShardPredictionRule.md`.
  - Calculates Gate appear time, Landing offset (+8m 40s), 4-hour active windows, and intervals (8h for Black Shards, 6h for Red Shards).
  - Exact realm and map location detection (e.g., *Daylight Prairie • Sanctuary Island*, *Hidden Forest • Elevated Clearing*, *Golden Wasteland • Crabfield*, *Vault • Starlight Desert*).
  - Displays Ascended Candle (✦ AC) rewards (~2.0 - 3.5 AC) for Red Shards and regular candle wax for Black Shards.
  - Respects "No Shard" days (Sat/Sun, Sun/Mon, Mon/Tue, Tue/Wed, Wed/Thu depending on the active pattern) and seamlessly predicts the next active shard date.

- **Calendar Season Sync**: Corrected active season data from placeholder to **Season of Pearl** (October 16, 2026 – December 31, 2026) featuring the Old Angler, alongside concluded **Dear Van Gogh** (concluded October 1, 2026).
- **Customizable Quiet Hours**: Added step controls (`+` / `−`) to adjust quiet hours window start and end times with dynamic 12h/24h time formatting.
- **Sky Running Mode Duration Selector**: Fixed switch visibility contrast and added duration selection chips (**30 Seconds**, **20 Seconds**, and **10 Seconds**).

- **Calendar Screen Polished**:
  - Filtered out all ended events so only active/running and upcoming events appear.
  - Dynamically projects upcoming **Red and Black Shard Eruptions** for the next 7 days with exact realms, areas, and Ascended Candle rewards.
  - Removed "Daily recurring" from the Calendar filter chips (kept cleanly on Home/Today schedule).
  - Removed "Community" chip; kept "Maintenance" ready for TGC service updates.
  - Added clean empty-state presentation when a selected filter has no upcoming events.

- **Calendar Cleaned & Refined**:
  - Excluded Shard Eruptions from "All Events"; they now live exclusively under their dedicated "Shard Eruption" tab to avoid cluttering key seasonal dates.
  - Removed "✓ CONFIRMED TGC" and "Source: Thatgamecompany" text labels for a clean, distraction-free aesthetic.
- **Settings Screen Polished**:
  - Removed "LumaNest Status" diagnostic card completely.
  - Upgraded Quiet Hours with an interactive alarm clock `TimePickerDialog` triggered upon tapping the start or end time.
  - Replaced uneven chips in Sky Running Mode with a uniform, three-segment selector for **30s**, **20s**, and **10s**.

- **Home Screen Customization & Top 3 Calendar Events**:
  - Added a **⚙️ Filter** button to *Today's Schedule* allowing users to toggle visible recurring events (Geyser, Grandma, Turtle, Shards, Reset).
  - Replaced the Home friend preview with a **Top 3 Upcoming Calendar Events** preview cards. Friends remain organized on the **Friends** tab.
- **Calendar Color Palette & Status Badges**:
  - Season titles styled in **Warm Vibrant Orange** (`#FB923C`).
  - Double Currency tags styled in **Ruby Red** (`#EF4444`).
  - Traveling Spirit tags in **Starlight Blue** (`#38BDF8`).
  - Major Events styled in **Royalty Purple** (`#A855F7`).
  - Added status badges: `🟢 RUNNING` with glowing green badge and dot, and `⏳ UPCOMING` for scheduled events.

- **Persistent Settings (PreferencesManager)**:
  - Sky Running Mode toggle and countdown duration (30s, 20s, 10s) are now permanently saved to Android `SharedPreferences`. Switching apps or reopening LumaNest preserves state.
  - Quiet Hours state and custom alarm clock times are permanently saved.
  - Home Schedule event visibility filter selections are saved to disk.
- **High-Contrast Tag Typography**:
  - Replaced low-contrast red-on-dark-red badges with crisp, pure white bold text on solid Ruby Red (`#DC2626`) for Double Currency and Solid Crimson (`#E11D48`) for Shard Eruptions.

### Fixed
- Fixed activity package declaration mismatch (`com.lumanest.ui` -> `com.lumanest.app.ui`) that caused `ClassNotFoundException` on initial launch.
- Upgraded Room database schema from version 1 to 2 (`fallbackToDestructiveMigration`) to support `sortOrder` for friends without integrity crash.
- Fixed database seed sync to ensure newly updated official calendar seasons always refresh into the local Room database.
