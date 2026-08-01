# Gratitude Journal

A private, local-first Android app for recording daily gratitude entries,
with optional manual backup to Google Drive, Dropbox, or OneDrive. Built
with Kotlin and Jetpack Compose.

See [SPECS.md](SPECS.md) for the original feature spec this app was built
against.

## Features

- **Calendar + scrollable entry feed** — a month grid stays fixed at the top
  of the home screen, above a single continuous feed of every day since your
  first entry. Scrolling the feed updates which month the calendar shows;
  tapping a calendar day scrolls the feed to it instead of navigating away.
  Days with entries show a dot that scales with how many entries that day
  has (1/2/3+). Entries can only be *added* for the current day, but any
  past day can be viewed, edited, or deleted.
- **Configurable entry order** — Newest First (default) or Oldest First,
  in Settings → Appearance.
- **Journal entries** — multiple per day, each with text and an optional
  photo (camera capture or gallery picker). Swipe an entry left to delete it
  (with confirmation) or right to open it for editing.
- **Daily reminder** — on by default from first install, with a customizable
  time in Settings; a local notification that opens the app directly to add
  today's entry.
- **Backup reminder** — a separate, also-on-by-default notification if it's
  been more than a configurable number of days (14 by default) since your
  last backup, so backups don't get forgotten. Can be disabled or
  retuned in Settings.
- **Security** — PIN required on launch, with optional biometric unlock
  (fingerprint/face) on supported devices.
- **Selectable themes** — three built-in color schemes (Sunset Gold, Golden
  Hour, Terra Cotta), independent of system light/dark mode.
- **Backup & restore** — manual "Back up now" / "Restore latest" against a
  choice of three providers — Google Drive, Dropbox, or OneDrive (a picker
  in the Backup & Restore screen selects the active one). Each stores the
  backup in that provider's hidden/app-private storage area (Drive's
  `appDataFolder`, Dropbox's App folder, OneDrive's App folder), so backups
  never clutter or appear in the user's normal cloud storage. Built behind a
  provider-agnostic interface. See [STORAGE.md](STORAGE.md) /
  [STORAGE_DROPBOX.md](STORAGE_DROPBOX.md) / [STORAGE_ONEDRIVE.md](STORAGE_ONEDRIVE.md)
  for setup and design details.

All data (database + photos) lives entirely on-device unless the user
explicitly triggers a backup.

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3) for UI
- **Hilt** for dependency injection
- **Room** for local persistence, **DataStore Preferences** for settings
- **Navigation Compose** (type-safe routes via `kotlinx.serialization`)
- **Coil 3** for image loading
- **AlarmManager** for the exact-time daily reminder; **WorkManager** for the
  periodic (not exact-time) backup-reminder check
- **AndroidX Biometric** for fingerprint/face unlock
- **Google Identity Services** (`play-services-auth`) + **OkHttp** for the
  Google Drive REST integration; OAuth2 PKCE via **AndroidX Browser**
  (Custom Tabs) + OkHttp for Dropbox and OneDrive (Microsoft Graph)

Minimum SDK 26, target/compile SDK 37.

## Project structure

```
app/src/main/java/com/gratitudelogger/
├── data/            # Room DAOs/entities, DataStore preference stores, backup archiving
│   └── backup/      # BackupArchiver, BackupPreferences, GoogleDriveBackupProvider,
│                     # DropboxBackupProvider, OneDriveBackupProvider, Pkce (shared
│                     # OAuth2 PKCE helper), OAuthRedirectRelay
├── domain/          # Repository interfaces, backup provider abstraction
│   └── backup/      # BackupProvider (provider-agnostic interface), BackupProviderType
├── di/              # Hilt modules
├── reminder/        # Daily reminder (AlarmManager) + backup reminder (WorkManager)
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

### Optional: cloud backup (Google Drive, Dropbox, and/or OneDrive)

Each backup provider needs one developer-side secret added to your own
`local.properties` (never committed — see `.gitignore`):

```
GOOGLE_WEB_CLIENT_ID=xxxxxxxxxx.apps.googleusercontent.com
DROPBOX_APP_KEY=xxxxxxxxxxxxxxx
ONEDRIVE_CLIENT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

Without any of these, the app still builds and runs fine — that provider's
entry in the Backup & Restore picker will just fail to authorize until its
key is set. Full one-time setup instructions:
[STORAGE.md](STORAGE.md) (Google Drive),
[STORAGE_DROPBOX.md](STORAGE_DROPBOX.md) (Dropbox),
[STORAGE_ONEDRIVE.md](STORAGE_ONEDRIVE.md) (OneDrive).

## Status

Actively developed. The initial build (M0–M6: project scaffold, local data
layer + calendar, entry CRUD, PIN/biometric security, photo attachments,
reminders/settings, and Google Drive backup) is detailed milestone-by-
milestone in [MILESTONES.md](MILESTONES.md). Since M6, development has
continued feature-by-feature rather than by milestone number — merging the
calendar and day-entry views into one scrollable feed with swipe-to-edit/
delete, a configurable entry display order, a periodic backup reminder, and
Dropbox/OneDrive as additional backup providers alongside Google Drive. See
the commit history for the detailed changelog of each change.
