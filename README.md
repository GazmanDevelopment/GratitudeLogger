# Gratitude Journal

A private, local-first Android app for recording daily gratitude entries,
with optional manual backup to Google Drive or Dropbox. Built with Kotlin
and Jetpack Compose.

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
  either Google Drive's hidden `appDataFolder` or a Dropbox app folder (a
  picker in the Backup & Restore screen selects which), so backups never
  clutter or appear in the user's normal Drive/Dropbox. Built behind a
  provider-agnostic interface. See [STORAGE.md](STORAGE.md) /
  [STORAGE_DROPBOX.md](STORAGE_DROPBOX.md) for setup and design details.

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
  Google Drive REST integration; OAuth2 PKCE via **AndroidX Browser**
  (Custom Tabs) + OkHttp for Dropbox

Minimum SDK 26, target/compile SDK 37.

## Project structure

```
app/src/main/java/com/gratitudelogger/
├── data/            # Room DAOs/entities, DataStore preference stores, backup archiving
│   └── backup/      # BackupArchiver, BackupPreferences, GoogleDriveBackupProvider,
│                     # DropboxBackupProvider, OAuthRedirectRelay
├── domain/          # Repository interfaces, backup provider abstraction
│   └── backup/      # BackupProvider (provider-agnostic interface)
├── di/              # Hilt modules
├── reminder/        # WorkManager-based daily/backup reminder scheduling
├── security/        # PIN + biometric gating
├── theme/           # Color scheme + entry-order preference definitions
└── ui/              # Compose screens, ViewModels, navigation
    ├── auth/        # PIN entry/setup, unlock
    ├── backup/      # Backup & Restore screen (provider picker, backup/restore)
    ├── calendar/    # Calendar + scrollable entries feed (the home screen)
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

### Optional: cloud backup (Google Drive and/or Dropbox)

Each backup provider needs one developer-side secret added to your own
`local.properties` (never committed — see `.gitignore`):

```
GOOGLE_WEB_CLIENT_ID=xxxxxxxxxx.apps.googleusercontent.com
DROPBOX_APP_KEY=xxxxxxxxxxxxxxx
```

Without either, the app still builds and runs fine — that provider's entry
in the Backup & Restore picker will just fail to authorize until its key is
set. Full one-time setup instructions: [STORAGE.md](STORAGE.md) (Google
Drive), [STORAGE_DROPBOX.md](STORAGE_DROPBOX.md) (Dropbox).

## Status

Actively developed in milestones (M0–M6 so far): project scaffold, local
data layer + calendar, entry CRUD, PIN/biometric security, photo
attachments, reminders/settings, and Google Drive backup. See the commit
history for the detailed changelog of each milestone.
