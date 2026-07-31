# Build milestones

This app was built incrementally against [SPECS.md](SPECS.md), one
self-contained, buildable milestone at a time. This doc is a readable
summary of each milestone's scope and key decisions; full detail (including
verification notes) lives in each milestone's commit message.

## M0 — Project scaffold

Buildable skeleton: Gradle wrapper + version catalog (AGP 9.3.1, Kotlin
2.3.10, Compose BOM 2026.06.01, Room 2.8.4, Hilt 2.60.1, Navigation-Compose
2.9.8, DataStore 1.2.1, Biometric 1.1.0, WorkManager 2.11.2, Coil3 3.5.0),
`compileSdk`/`targetSdk` 37, `minSdk` 26 (native `NotificationChannel` +
`java.time`, no desugaring needed). Hilt-enabled `Application` class,
`MainActivity` hosting a Compose `NavHost` with type-safe
(`kotlinx.serialization`) routes, Material3 theme, and a placeholder Home
screen.

## M1 — Room data layer + calendar view

- `JournalEntry` Room entity (`entryDate`, `text`, optional `photoPath`,
  timestamps, a `syncState` column reserved for future cloud sync but
  unused at this stage) and `JournalEntryDao` (per-day list, distinct
  entry-dates-in-range for calendar dots, by-id lookup).
- `JournalRepository` domain interface / `JournalRepositoryImpl`, so a
  future sync-aware implementation could be swapped in without touching
  ViewModels. `addEntry` always stamps `entryDate = LocalDate.now()`,
  enforcing "no backdating" at the repository layer.
- Hand-rolled monthly calendar grid (`CalendarHomeScreen` +
  `CalendarHomeViewModel`): Monday-start weeks, red dot on days with
  entries, today highlighted, forward navigation capped at the current
  month.
- Temporary `DummyDataSeeder` to validate the calendar UI before real entry
  creation existed (deleted in M2).

## M2 — Entry CRUD

- `DayEntriesScreen`: reactive list of a day's entries (via a Room `Flow`),
  with a "add" FAB shown only when viewing today — enforced at both the UI
  and repository layer.
- `AddEditEntryScreen`: single text field; creates a new entry with no
  `entryId` nav arg, loads and updates an existing one when an id is
  passed; delete only available in edit mode.
- Type-safe `DayEntriesRoute(epochDay)` / `AddEditEntryRoute(entryId)` nav
  routes replace M1's inline "selected day" panel.
- Removed `DummyDataSeeder` — a fresh install now starts genuinely empty.

## M3 — PIN security + biometric unlock

Wraps the M1/M2 screens behind an auth gate rather than modifying them
directly.

- `PinHasher`: salted PBKDF2WithHmacSHA256 (120k iterations) — the PIN is
  only ever verified, never decrypted, so this sidesteps needing
  Keystore-backed encryption for a personal-journal threat model.
- `SecurityPreferences` (DataStore): hash/salt/iterations,
  `biometricEnabled`.
- `AppLockManager`: a `ProcessLifecycleOwner` observer that locks on every
  `onStop` (background), not a grace-period timeout — cold start and any
  backgrounding both require re-auth.
- `PinSetupScreen`/`UnlockScreen` with a shared `PinPad`; PIN setup offers a
  one-time biometric opt-in when `BIOMETRIC_STRONG` is available.
- `AppRoot` sits above the nav graph (not part of its route stack) so the
  lock overlay is independent of back-stack state.
- Two bugs found on a physical device (no biometric sensor on the emulator,
  so neither was visible until real hardware testing) and fixed: the
  biometric opt-in screen never appeared because persisting the PIN hash
  immediately flipped the top-level "PIN is set" gate before the opt-in
  step could render; and the PIN pad showed stale filled dots after a
  successful unlock because the success path never cleared entered digits
  (the wrong-PIN path did).

## M4 — Photo attachments

- App-private photo storage under `filesDir/photos/` (not `MediaStore`) via
  `PhotoStorage`/`PhotoStorageImpl`: camera capture through a
  `FileProvider`-backed target, gallery picks re-encoded through the same
  pipeline. Both paths run EXIF-aware rotation + downscale to a 1600px long
  edge + JPEG re-encode at 85% quality to bound storage growth.
- `JournalRepositoryImpl.deleteEntry` now also deletes the entry's photo
  file — verified no orphaned files survive a delete.
- 56dp thumbnails in `DayEntriesScreen` via Coil.
- Bug found and fixed: launching the camera/picker backgrounds the app,
  correctly triggering M3's lock-on-background — but `AppRoot` was
  recreating `NavController` from scratch every time the lock-gate branch
  swapped, dropping the in-progress entry on unlock. Fixed by hoisting
  `rememberNavController()` above the conditional branch.

## M5 — Daily reminders + Settings screen

- `ReminderTimeCalculator` (pure `ZonedDateTime` math, so DST transitions
  resolve correctly) + `ReminderPreferences` (DataStore, default 20:00) +
  `ReminderScheduler` using exact alarms (`setExactAndAllowWhileIdle`,
  falling back to `setAndAllowWhileIdle` without the exact-alarm
  permission) — `WorkManager`'s periodic API has a 15-minute floor and
  can't hit a user-chosen exact time, so it's not used for the primary
  schedule.
- `ReminderAlarmReceiver` (fires notification + reschedules tomorrow, since
  exact alarms are one-shot) and `BootReceiver` (exact alarms are cleared
  on reboot; re-schedules if enabled).
- New `SettingsScreen`: reminder toggle + time picker, a biometric toggle
  (always visible, with a human-readable reason shown when unavailable
  rather than hiding the row), and Change PIN — routed through a
  `VerifyPinForChangeRoute` that re-proves the current PIN first, since the
  user is otherwise already inside the authenticated area.

## M6 — Google Drive backup and restore

Manual "Back up now" / "Restore latest" against Google Drive's hidden
`appDataFolder`, behind a provider-agnostic `BackupProvider` domain
interface so OneDrive/Dropbox can be added later. Full design rationale and
one-time Google Cloud Console setup: [STORAGE.md](STORAGE.md).

- Auth via Google Identity Services' `AuthorizationClient` requesting only
  the non-sensitive `drive.appdata` scope (no OAuth verification needed
  while the consent screen stays in Testing status). No access token is
  cached between calls — each backup/restore re-authorizes, which resolves
  silently once the scope has already been granted.
- Drive v3 REST (list/create/update/download in `appDataFolder`) via OkHttp
  directly rather than the full `google-api-services-drive` client, since
  only four narrow operations are needed.
- `BackupArchiver`: runs a WAL checkpoint (`PRAGMA wal_checkpoint(FULL)`)
  before zipping `gratitude.db` + `filesDir/photos/`, so a single `.db`
  file is a complete snapshot without needing `-wal`/`-shm` sidecars.
  Restore closes the Room database, replaces both, then force-restarts the
  whole process (relaunch `MainActivity` + `Runtime.getRuntime().exit(0)`)
  rather than trying to keep Hilt/Room/DataStore singletons consistent with
  files swapped out from underneath them.
- New Settings → Backup & Restore screen; app's first-ever `INTERNET`
  permission; `GOOGLE_WEB_CLIENT_ID` sourced from `local.properties`
  (gitignored) into a `BuildConfig` field.
- Bug found and fixed during on-device testing: the initial multipart
  upload set an explicit `Content-Type` header alongside a `RequestBody`
  that already carried a media type, which OkHttp rejects
  (`IllegalArgumentException: Unexpected header: Content-Type`) — fixed by
  using the plain `addPart(RequestBody)` overload.

---

*Not a milestone itself, but done alongside M6 prep:* a selectable
color-scheme theming system (Sunset Gold / Golden Hour / Terra Cotta,
user-chosen in Settings, independent of system light/dark mode) and a
re-themed launcher icon to match the default palette.
