# Project Decisions Log

## DEC-001: Offline-First Architecture & Engine Decoupling
- **Status:** Approved
- **Rationale:** The Sky Event Engine must perform all timezone, boundary, and recurrence math independently of network status or UI state. The app must never fail or show empty screens when offline.

## DEC-002: Version Decoupling (App vs Database)
- **Status:** Approved
- **Rationale:** Sky event schedules change dynamically with patches. App version (e.g. 1.0.0) is decoupled from the Event Database version (e.g. 2026.10.06) so schedule updates don't necessitate full APK app store releases.

## DEC-003: Privacy & Zero-Spam Calendar Policy
- **Status:** Approved
- **Rationale:** Friend lists and timezones remain 100% on-device. Google/Android Calendar export is opt-in and restricted to major events or user-selected categories to prevent calendar spam.

## DEC-004: Information Collection & 101% Verification Rule
- **Status:** Approved
- **Rationale:** External reference sources are archived in project research records first until 101% confidence is reached:
  - `thatskyapplication.com/calendar`
  - `thatskyapplication.com/schedule`
  - Sky Clock screenshots (Sky Mean Time & event schedule table)
  - Sky Wiki Timing System: `https://sky-children-of-the-light.fandom.com/wiki/Timing_System`
  - Sky Dreamers Notion Calendar: `https://skydreamers.notion.site/Sky-Event-Calendar-FM-ec6e6134924048859b2a8410b0a8b20d`
  - SkyClock Web Tracker: `https://skyclock.app/`
  Features and event schedules will not be implemented prematurely until full specifications and 101% verification are achieved with user approval.

## DEC-005: Golden Rule — Dynamic Version Resolution & Zero Hardcoding
- **Status:** Approved
- **Rationale:** Never hardcode application version strings (`"V.3"`, `"V.4"`, etc.) in UI composables, settings screens, or in-app updater check functions. The Single Source of Truth for the app version is Gradle (`build.gradle.kts` `versionName`). The runtime application and all updater queries must dynamically resolve the installed version via `AppUpdater.getInstalledVersionName(context)` (`packageManager.getPackageInfo()`). This guarantees that after updating the APK, the settings screen and update indicators never falsely claim the app is outdated or trigger redundant update alerts.
