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
