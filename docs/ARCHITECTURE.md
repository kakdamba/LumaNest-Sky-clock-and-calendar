# LumaNest Architecture & System Design

## 1. Core Decoupling

```
┌────────────────────────────────────────────────────────┐
│                   Sky Event Engine                     │
│  - Pure deterministic event schedules & calculations   │
│  - Recurrence math (Geyser, Turtle, Grandma, Shards)   │
│  - Timezone conversion & IANA timezone resolution     │
│  - Event boundary detection & countdown math           │
│  - Zero UI logic, strictly testable                    │
└───────────────────────────┬────────────────────────────┘
                            │ Queries / State
┌───────────────────────────▼────────────────────────────┐
│                    Android App & UI                    │
│  - Jetpack Compose UI (Cozy Dark / Night Theme)        │
│  - Android Glance App Widgets (Small, Medium, Friends) │
│  - AlarmManager & WorkManager battery-friendly sync    │
│  - Notification Engine (Quiet hours, pre-event alerts) │
│  - Local Room Database (Offline first, verified cache) │
│  - Friend Timezone coordination ("Play Together")      │
│  - Export / ICS / Calendar Integration (opt-in)        │
└────────────────────────────────────────────────────────┘
```

## 2. Data Sourcing & Verification Pipeline

1. **Source Tiers:**
   - Tier 1: Official TGC announcements & patch notes (Primary authority)
   - Tier 2: Official social announcements
   - Tier 3: Community sources (Structured fallback & cross-check)
2. **Safety Pipeline:**
   - Fetch -> Parse -> Normalize -> Schema Validation -> Conflict Detection -> Local Room DB.
   - If update validation fails, reject safely and preserve verified local database.
3. **Decoupled Versions:**
   - App Version: `1.0.0`
   - Event Database Version: `2026.10.06` (updates independently without requiring new APK).
