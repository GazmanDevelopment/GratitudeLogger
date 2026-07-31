# Gratitude Journal

A private, local-first Android app for recording daily gratitude entries,
with optional manual backup to Google Drive. Built with Kotlin and Jetpack
Compose.

See [SPECS.md](SPECS.md) for the original feature spec this app was built
against.

## Features

- **Monthly calendar view** — browse any month, with days that have entries
  marked. Entries can only be *added* for the current day, but past days can
  be viewed.
- **Journal entries** — multiple entries per day, each with text and an
  optional photo (camera capture or gallery picker).
- **Daily reminder** — a customizable local notification that opens the app
  directly to add today's entry.
- **Security** — PIN required on launch, with optional biometric unlock
  (fingerprint/face) on supported devices.
- **Selectable themes** — three built-in color schemes (Sunset Gold, Golden
  Hour, Terra Cotta), independent of system light/dark mode.
- **Backup & restore** — manual "Back up now" / "Restore latest" against
  Google Drive's hidden `appDataFolder`, so backups never clutter or appear
  in the user's normal Drive. Built behind a provider-agnostic interface so
  OneDrive/Dropbox can be added later. See [STORAGE.md](STORAGE.md) for setup
  and design details.

All data (database + photos) lives entirely on-device unless the user
explicitly triggers a backup.

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3) for UI
- **Hilt** for dependency injection
- **Room** for local persistence, **DataStore Preferences** for settings
- **Navigation Compose** (type-safe routes via `kotlinx.serialization`)
- **Coil 3** for image loading
- **WorkManager** for scheduled reminders
- **AndroidX Biometric** for fingerprint/face unlock
- **Google Identity Services** (`play-services-auth`) + **OkHttp** for the
  Google Drive REST integration

Minimum SDK 26, target/compile SDK 37.

## Project structure

```
app/src/main/java/com/gratitudelogger/
├── data/            # Room DAOs/entities, DataStore preference stores, backup archiving
│   └── backup/      # BackupArchiver, BackupPreferences, GoogleDriveBackupProvider
├── domain/          # Repository interfaces, backup provider abstraction
│   └── backup/      # BackupProvider (provider-agnostic interface)
├── di/              # Hilt modules
├── reminder/        # WorkManager-based daily reminder scheduling
├── security/        # PIN + biometric gating
├── theme/           # Color scheme definitions
└── ui/              # Compose screens, ViewModels, navigation
    ├── auth/        # PIN entry/setup, unlock
    ├── backup/      # Backup & Restore screen
    ├── calendar/    # Monthly calendar home screen
    ├── dayentries/  # Per-day entry list
    ├── entry/       # Add/edit entry screen (text + photo)
    ├── navigation/  # NavGraph, type-safe routes
    ├── settings/    # Settings screen
    └── theme/       # Compose theme wiring (CompositionLocals, Material3 scheme)
```

## Building

Requires JDK 17+ and the Android SDK (`sdk.dir` is picked up automatically
by Android Studio, or set it in `local.properties`).

```sh
./gradlew assembleDebug
```

### Optional: Google Drive backup

The backup feature needs one developer-side secret,
`GOOGLE_WEB_CLIENT_ID`, added to your own `local.properties` (never
committed — see `.gitignore`):

```
GOOGLE_WEB_CLIENT_ID=xxxxxxxxxx.apps.googleusercontent.com
```

Without it, the app still builds and runs fine — the backup screen will
just fail to authorize. Full one-time Google Cloud Console setup
instructions are in [STORAGE.md](STORAGE.md).

## Status

Actively developed in milestones (M0–M6 so far): project scaffold, local
data layer + calendar, entry CRUD, PIN/biometric security, photo
attachments, reminders/settings, and Google Drive backup. See the commit
history for the detailed changelog of each milestone.
