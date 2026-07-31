# Cloud Backup Setup (Google Drive)

This document covers the **one-time developer setup** required before the app's
Google Drive backup/restore feature can work. This is done **once, by the app's
developer/publisher**, in their own Google Cloud project — end users never see
Google Cloud Console or register anything themselves. They just tap "Sign in
with Google" inside the app and grant access to their own Drive, the same as
signing into any other app that supports Google Sign-In.

## Why Drive's `appDataFolder`, not regular Drive

Backups are stored in Drive's hidden **appDataFolder** — a per-app storage area
that:
- Is invisible in the user's normal Drive UI (it only shows up in Drive's
  storage-quota "manage apps" breakdown), so a user's regular Drive doesn't get
  cluttered with an app-internal backup file.
- Only the scope `drive.appdata` is needed to access it, which Google
  classifies as a **non-sensitive scope** — it does not require Google's OAuth
  API verification/security-assessment process, unlike broader Drive scopes
  (`drive`, `drive.readonly`, etc.). Keeping the OAuth consent screen in
  **Testing** status (see below) means no verification submission is needed at
  all, regardless of scope class.

## Scope of what's backed up

A single zip archive containing:
- The Room database file (`gratitude.db`, plus its `-wal`/`-shm` sidecar files
  if present)
- Everything under the app's private `files/photos/` directory

This is a manual, whole-snapshot **backup/restore** feature ("Back up now" /
"Restore latest"), not continuous multi-device sync — appropriate for a
single-user local journal. The `syncState` column on `JournalEntry` is reserved
for a possible future incremental-sync design but is unused by this feature.

## One-time setup steps (Google Cloud Console)

1. Go to [console.cloud.google.com](https://console.cloud.google.com), create
   or select a project.
2. **APIs & Services → Library**: enable the **Google Drive API**.
3. **Google Auth Platform** (the current name for what used to be called the
   "OAuth consent screen"):
   - **Audience** tab: choose **External**, and add your own Google account
     under **Test users**. Staying in **Testing** status (rather than
     publishing/submitting for verification) is sufficient for personal use
     and any number of real end users you invite as test users — verification
     is only required if you want the consent screen to work for arbitrary
     public users without being added as a test user first.
   - **Data Access** tab: add the scope
     `https://www.googleapis.com/auth/drive.appdata`.
4. **Clients** tab: create **two** OAuth client IDs (both are needed):
   - **Android** client:
     - Package name: `com.gratitudelogger`
     - SHA-1 signing certificate fingerprint — get this by running:
       ```
       keytool -list -v -keystore <path-to-keystore> -alias <key-alias> -storepass <password> -keypass <password>
       ```
       For a debug build, the keystore is at `%USERPROFILE%\.android\debug.keystore`,
       alias `androiddebugkey`, password `android` for both `-storepass` and
       `-keypass`. Every developer machine's debug keystore has a different
       fingerprint, so this must be re-run (and a new Android OAuth client
       added, or the existing one updated) on whichever machine builds the
       binary you're testing with. For a signed release build, use the release
       keystore's fingerprint instead (or the SHA-1 Google Play Console shows
       under App Signing, if using Play App Signing) — you can register both
       the debug and release SHA-1s as separate Android-type client entries
       under the same project, so debug and release builds both work.
   - **Web application** client:
     - No redirect URI is needed for this flow.
     - Its **Client ID** (looks like `xxxxxxxxxx.apps.googleusercontent.com`)
       is what the app uses as the `serverClientId` — required by both the
       Credential Manager sign-in step and the Drive authorization step, even
       though the app itself is Android, not web.
5. Take the **Web application client's Client ID** and put it in your local
   `local.properties` (already gitignored, never committed) as:
   ```
   GOOGLE_WEB_CLIENT_ID=xxxxxxxxxx.apps.googleusercontent.com
   ```
   The Gradle build reads this into a `BuildConfig` field, so the actual ID
   never touches source control.

## What end users experience

None of the above. An end user installing the published app just sees a
"Sign in with Google" button in Settings → Backup & Restore. Tapping it shows
the standard Google account picker and a one-time consent screen asking to
grant this app access to "app data" in their Drive — the same UX pattern as
"Sign in with Google" on any other app. They never interact with Google Cloud
Console, never see a client ID, and never need a Google Cloud project of their
own.

## Status

Implemented (M6). `domain/backup/BackupProvider.kt` is the provider-agnostic
interface; `data/backup/GoogleDriveBackupProvider.kt` is the Google Drive
implementation, reached via Settings → Backup & Restore
(`ui/backup/BackupScreen.kt`).

One deviation from the original sketch above: there's no separate "Sign in"
step. Tapping **Back up now** or **Restore latest** triggers the Google
account picker + `drive.appdata` consent screen the first time (via the
Drive `AuthorizationClient`'s own resolution flow) and re-authorizes silently
on later taps - no access token is stored between calls. The
Credential-Manager identity step described above (for a "signed in as
you@gmail.com" label) was dropped as unnecessary complexity for what's shown
in the UI; the backup screen just shows a generic "Google Drive" label with
the last-backup timestamp instead.
