# Ultrablock

An Android app that blocks distracting apps using customisable friction levels, tracks the time and money cost of distraction, and lets you share progress with accountability partners.

---

## Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Setup](#setup)
- [Permissions](#permissions)
- [Running the App](#running-the-app)
- [Running Tests](#running-tests)
- [Social Features](#social-features)
- [Configuration](#configuration)
- [Known Limitations](#known-limitations)

---

## Overview

Ultrablock works by running an Accessibility Service in the background. When a blocked app is opened, the service intercepts the launch and displays a full-screen overlay. The user must then pass a friction challenge before the app becomes accessible. The difficulty of that challenge is configurable per-app or globally.

The app is built entirely in Kotlin with Jetpack Compose for UI, Room for persistence, and DataStore for user preferences. Payments are handled via Stripe (running in demo/simulated mode by default).

---

## Features

### App Blocking

- Select any installed app to block from a searchable list
- Blocking is toggled globally from the home screen
- Time-based schedules define when blocking is active (start time, end time, days of week)
- Apps can be temporarily unblocked for a fixed duration

### Friction Levels

Four levels control how difficult it is to bypass a block. The level can be set globally in Settings or overridden per app.

| Level | Behaviour |
|---|---|
| Gentle | A 5-second countdown with a mindfulness prompt. Free to continue after waiting. |
| Moderate | A 30-second timer plus a written reason (minimum 10 characters). Free to continue once both are satisfied. |
| Strict | Payment required via Stripe before the app is unblocked. Cost is calculated from your configured hourly rate and the duration selected. |
| Extreme | No bypass. The app cannot be unblocked until you change the friction level in Settings. |

### Time and Cost Tracking

Every block attempt is recorded as a session. The home screen shows:

- Today's focus score: the percentage of block attempts that were held without bypassing
- Blocks held vs bypassed counts for today and this week
- Estimated time saved (successful blocks x 15 minutes each)
- Estimated value saved, calculated from your hourly rate
- Total money spent on paid unblocks (Strict level) across all time

### Social Accountability

Each installation generates a unique code in the format `UB-XXXX-XXXX`. You can:

- Copy your code to the clipboard and share it with friends
- Share a formatted stats summary via the Android share sheet
- Add friends as accountability partners by entering their code and a nickname
- View the last-known stats for each partner

Note: partner stats are stored locally. There is currently no backend syncing between devices. See [Social Features](#social-features) for details.

### Payment (Stripe)

The app is configured in demo mode by default. Payments are simulated and no real charges are made. To enable live payments:

1. Set up a Stripe account and obtain your publishable and secret keys
2. Configure the keys in `StripePaymentService`
3. Remove the demo mode flag

---

## Architecture

The project follows MVVM with a unidirectional data flow. Each screen owns a ViewModel that exposes a single `StateFlow<UiState>` built by combining multiple source flows.

```
UI (Compose screens)
    |
ViewModels (StateFlow<UiState> via combine())
    |
Repositories (business logic, aggregate data from multiple sources)
    |
DAOs (Room) + DataStore (UserPreferences) + Services (Stripe, Accessibility)
```

**Dependency injection** is handled by Hilt. All repositories and DAOs are `@Singleton`.

**Background monitoring** uses an Accessibility Service (`AppMonitorAccessibilityService`) that listens for `TYPE_WINDOW_STATE_CHANGED` events. When a blocked app comes to the foreground, it starts `BlockerOverlayActivity` as a system overlay.

---

## Project Structure

```
app/src/main/java/com/ultrablock/
|
+-- data/
|   +-- local/
|   |   +-- AppDatabase.kt             Room database, version 2
|   |   +-- dao/
|   |   |   +-- BlockedAppDao.kt
|   |   |   +-- ScheduleDao.kt
|   |   |   +-- UnblockHistoryDao.kt
|   |   |   +-- AppUsageDao.kt         Block attempt sessions
|   |   |   +-- AccountabilityPartnerDao.kt
|   |   +-- entity/
|   |       +-- BlockedApp.kt          Package name, blocked flag, friction level override, temp unblock expiry
|   |       +-- Schedule.kt            Start/end time in minutes from midnight, days of week
|   |       +-- UnblockHistory.kt      Paid unblock records with Stripe payment intent ID
|   |       +-- AppUsageSession.kt     One row per block attempt; marked unblocked if bypassed
|   |       +-- AccountabilityPartner.kt  Partner code, display name, last-known stats
|   +-- preferences/
|   |   +-- UserPreferences.kt         DataStore: hourly rate, friction level, user code, Stripe details
|   +-- repository/
|       +-- AppRepository.kt           App selection, blocking state, temp unblock
|       +-- ScheduleRepository.kt      Schedule CRUD and current-time check
|       +-- PaymentRepository.kt       Unblock history aggregates
|       +-- UsageRepository.kt         Session recording, today/week stats flows
|       +-- SocialRepository.kt        Partner CRUD with validation
|
+-- domain/
|   +-- model/
|       +-- FrictionLevel.kt           Enum: GENTLE, MODERATE, STRICT, EXTREME
|       +-- InstalledApp.kt
|       +-- BlockSchedule.kt
|       +-- UnblockRequest.kt
|
+-- service/
|   +-- AppMonitorAccessibilityService.kt   Foreground app detection
|   +-- AppBlockerService.kt                Foreground notification service
|   +-- BootReceiver.kt
|   +-- StripePaymentService.kt
|
+-- ui/
|   +-- navigation/
|   |   +-- NavGraph.kt                Five-tab bottom nav: Home, Apps, Schedule, Social, Settings
|   +-- screens/
|   |   +-- home/                      HomeScreen, HomeViewModel
|   |   +-- appselection/              AppSelectionScreen, AppSelectionViewModel
|   |   +-- schedule/                  ScheduleScreen, ScheduleViewModel
|   |   +-- blocker/                   BlockerOverlayActivity, BlockerViewModel
|   |   +-- social/                    SocialScreen, SocialViewModel
|   |   +-- settings/                  SettingsScreen, SettingsViewModel
|   +-- theme/                         Color, Type, Theme
|
+-- di/
    +-- AppModule.kt                   Hilt module: Room database, DAOs, migration
```

### Database

The Room database is named `ultrablock_database` and is currently at version 2.

| Table | Purpose |
|---|---|
| `blocked_apps` | Apps selected for blocking; stores per-app friction level override |
| `schedules` | Time-based block schedules |
| `unblock_history` | Record of every paid unblock |
| `app_usage_sessions` | One row per block attempt; updated if the user bypasses |
| `accountability_partners` | Locally stored partner codes and display names |

Migration 1 to 2 adds the `frictionLevel` column to `blocked_apps` (nullable, existing rows get NULL meaning "use global setting") and creates the two new tables.

---

## Setup

### Requirements

- Android Studio Hedgehog or later
- Android SDK 34
- Kotlin 1.9
- A physical Android device is strongly recommended for testing (see [Permissions](#permissions))

### Steps

1. Clone the repository
2. Open the project in Android Studio
3. Let Gradle sync complete
4. Connect a physical device or start an emulator (API 26 minimum)
5. Run the app via the Run button or `./gradlew installDebug`

No API keys are required to run in demo mode.

---

## Permissions

The following permissions must be granted manually after installation. The Settings screen provides buttons to open the relevant system screens.

| Permission | Why it is needed | How to grant |
|---|---|---|
| Accessibility Service | Detects which app is in the foreground | Settings > Accessibility > Ultrablock |
| Display Over Other Apps | Shows the blocker overlay on top of other apps | Settings > Apps > Ultrablock > Display over other apps |
| Usage Access | (Optional) Provides app usage statistics | Settings > Digital Wellbeing / Usage Access |

The Accessibility Service and Display Over Apps permissions are both required before blocking can be enabled. The main toggle on the Home screen is disabled until both are granted.

---

## Running the App

### First launch

1. Open the app
2. Go to the Settings tab and grant both permissions listed above
3. Go to the Apps tab and select one or more apps to block
4. Go to the Schedule tab and create a schedule covering the current time
5. Return to the Home tab and enable the Blocking toggle
6. Open one of the blocked apps to see the blocker overlay

### Friction levels

The global friction level defaults to Strict (payment required). To change it:

- Settings tab > Friction Level > select a level

To override per app, a per-app friction level is stored in the `BlockedApp` entity. This can be set programmatically; a UI for per-app selection is a planned addition.

### Demo payments

In demo mode, tapping "Unblock for $X.XX" simulates a successful payment without contacting Stripe. Add a demo card via Settings > Payment Method > Add Card.

---

## Running Tests

### Unit tests

These run on the JVM and do not require a device.

```
./gradlew test
```

Covers: `FrictionLevel` enum, `UsageRepository`, `SocialRepository`, `BlockerViewModel`, `SettingsViewModel`.

### Instrumented tests

These run on a connected device or emulator.

```
./gradlew connectedAndroidTest
```

Covers:

- `AppUsageDaoTest` - session insert, update, count queries, time-window filtering
- `AccountabilityPartnerDaoTest` - partner CRUD, ordering, stats update
- `BlockedAppDaoTest` - friction level persistence, temporary unblock logic
- `DatabaseMigrationTest` - verifies migration 1 to 2 preserves data and creates new tables correctly
- `BlockerOverlayScreenTest` - Compose UI test for all four friction level screens
- `SocialScreenTest` - Compose UI test for the social tab

### Test dependencies

| Library | Version | Purpose |
|---|---|---|
| MockK | 1.13.8 | Mocking for Kotlin |
| Turbine | 1.0.0 | Flow assertion helpers |
| kotlinx-coroutines-test | 1.7.3 | `runTest`, `advanceTimeBy` |
| androidx.room:room-testing | 2.6.1 | `MigrationTestHelper` |

---

## Social Features

### How it works today

Each device generates a unique code on first launch (`UB-XXXX-XXXX`). Users share this code out of band (message, chat, share sheet). A partner adds the code in the Social tab. Stats shared via the share sheet are plain text snapshots.

Partner data is stored entirely on-device. There is no backend. The `lastKnownBlockCount` and `lastKnownSuccessRate` fields on a partner record are never automatically populated from the partner's device.

### Adding a real backend

The recommended path is Firebase:

1. Create a Firebase project at console.firebase.google.com
2. Add an Android app and download `google-services.json` into the `app/` directory
3. Add to `build.gradle.kts`:
   ```kotlin
   implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
   implementation("com.google.firebase:firebase-firestore-ktx")
   implementation("com.google.firebase:firebase-auth-ktx")
   ```
4. Add the Google services plugin to both gradle files
5. Implement a `FirestoreRepository` that:
   - On each block session end, writes `{todayAttempts, todaySuccessful, weekAttempts, weekSuccessful, updatedAt}` to `users/{userCode}` in Firestore
   - On opening the Social tab, reads each partner's document and updates their local `AccountabilityPartner` record

Anonymous authentication (no sign-up required) is sufficient since the user code acts as the identity.

---

## Configuration

All user-configurable values are stored in DataStore (`user_preferences`) and exposed via `UserPreferences`.

| Preference | Default | Description |
|---|---|---|
| `hourly_rate_cents` | 2000 (= $20/hr) | Rate used to calculate the cost of a paid unblock |
| `default_unblock_duration` | 15 minutes | Pre-selected duration on the blocker screen |
| `global_friction_level` | STRICT | Fallback friction level for apps with no per-app override |
| `blocking_enabled` | false | Master on/off switch |
| `user_social_code` | generated on first access | Unique identifier for the Social tab |
| `stripe_payment_method_id` | none | Stored after adding a card |

---

## Known Limitations

- **No per-app friction UI.** The `BlockedApp` entity stores a per-app friction level, and the blocker overlay respects it, but there is no UI to set it. Currently all apps use the global level unless set programmatically.
- **Social features are local only.** Partner stats do not sync between devices. See [Social Features](#social-features).
- **Payments are simulated.** Stripe is integrated but runs in demo mode. Real charges require configuring live API keys.
- **Accessibility Service dependency.** The blocking mechanism relies on `TYPE_WINDOW_STATE_CHANGED` events. Some Android OEMs restrict this or delay events, which can allow a brief moment where a blocked app is visible before the overlay appears.
- **Emulator limitations.** The Accessibility Service and overlay permission behave differently on emulators. Physical device testing is recommended for blocking functionality.
- **No background usage tracking.** Session records are created when the blocker overlay is shown, not from continuous background monitoring. Time saved is estimated (successful blocks x 15 minutes), not measured.
