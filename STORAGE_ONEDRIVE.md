# Cloud Backup Setup (OneDrive)

This document covers the **one-time developer setup** required before the app's
OneDrive backup/restore feature can work. This is done **once, by the app's
developer/publisher**, in their own Microsoft Entra tenant — end users never
see the Entra admin center or register anything themselves. They just tap
"Back up now" inside the app, approve access in a browser tab, and it backs up
to their own OneDrive, the same as any other app that supports "Sign in with
Microsoft".

This is planning/requirements documentation only — see the Status section at
the bottom. The actual implementation approach is tracked separately in
`STORAGE_OD_BUILD.md` (not committed to this repo — see `.gitignore`).

## Why an "App folder", not full OneDrive access

Backups are stored in OneDrive's special **App folder** (`special/approot`,
which appears as `Apps/Gratitude Logger/` in the user's OneDrive) rather than
requesting access to their whole OneDrive:
- The app can only see and write inside its own folder — never the rest of
  the user's files.
- This maps to a single delegated Microsoft Graph permission,
  `Files.ReadWrite.AppFolder`, rather than the broader `Files.ReadWrite` or
  `Files.ReadWrite.All` scopes full-OneDrive access would need.

This is Microsoft Graph's direct equivalent of Google Drive's hidden
`appDataFolder` and Dropbox's App folder (see `STORAGE.md` /
`STORAGE_DROPBOX.md`) — the same "sandboxed, per-app storage" idea.

## Scope of what's backed up

Identical to the other two providers — the same `gratitude-backup.zip`
(Room database + everything under `files/photos/`) produced by the existing
provider-agnostic `BackupArchiver`.

## One-time setup steps (Microsoft Entra admin center)

1. Go to [entra.microsoft.com](https://entra.microsoft.com) (or
   [portal.azure.com](https://portal.azure.com) → Microsoft Entra ID) and
   create a new **App registration**.
2. **Supported account types**: choose **Accounts in any organizational
   directory and personal Microsoft accounts** — this app supports backing up
   to either an individual's personal OneDrive or a work/school account's
   OneDrive for Business, not just one or the other. (If an existing
   registration was created with "Personal accounts only," this can be
   widened afterwards from the app registration's **Authentication** page —
   the same "Supported account types" section reappears there — no need to
   create a new registration.)
3. **Authentication** → **Add a platform**:
   - Choose **Mobile and desktop applications**, *not* "Android". The Android
     platform option computes a `msauth://<package>/<signature-hash>`
     redirect URI requiring the same `keytool`-derived SHA fingerprint dance
     Google's setup already needs (see `STORAGE.md`) — re-registered per
     debug/release keystore, per developer machine. "Mobile and desktop
     applications" instead lets you specify your own custom redirect URI with
     **no package name or signing-certificate fingerprint required at all**,
     matching Dropbox's platform-agnostic setup (`STORAGE_DROPBOX.md`).
   - Add the custom redirect URI the app already uses for Dropbox:
     `gratitudelogger://oauth2redirect`. Reusing the same scheme is
     deliberate — see `STORAGE_OD_BUILD.md` for why one shared redirect
     works fine across providers.
   - Under **Advanced settings**, set **Allow public client flows** to **Yes**
     (required for a PKCE/no-secret installed app).
4. **API permissions** → add **Microsoft Graph** → **Delegated permissions** →
   `Files.ReadWrite.AppFolder`. This does not require admin consent.
5. Note the **Application (client) ID** shown on the app registration's
   Overview page, and put it in your local `local.properties` (already
   gitignored, never committed) as:
   ```
   ONEDRIVE_CLIENT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
   ```
   The Gradle build reads this into a `BuildConfig` field, so the actual
   client ID never touches source control — same pattern already used for
   `GOOGLE_WEB_CLIENT_ID` and `DROPBOX_APP_KEY`.

## No client secret, no Development/Production gate

Like Dropbox, this app authenticates via OAuth2 PKCE — there is no client
secret to protect. Unlike Google (Testing/test-user allowlist) and Dropbox
(Development status, 500-account cap), a Microsoft Entra app registration
with a non-admin-consent scope like `Files.ReadWrite.AppFolder` can be
consented to by any Microsoft account immediately, with no review step and
no per-user allowlisting needed.

## What end users experience

None of the above. An end user just sees **Back up now**/**Restore latest**
in Settings → Backup & Restore (after selecting OneDrive as the provider).
Tapping either opens a browser tab to Microsoft's own login/consent screen
the first time, asking to grant this app access to its own app folder — the
same "Sign in with Microsoft" pattern used by other apps. Approving redirects
straight back into the app, which then completes the backup/restore. They
never interact with the Entra admin center, never see a client ID, and never
need a Microsoft Entra tenant of their own.

## A UX nuance worth knowing about (not a setup step)

Microsoft Graph refresh tokens are rolling/sliding-window (commonly ~90 days,
resetting on each use) rather than permanent like Dropbox's. Since this is a
manual, occasional-use backup feature, a user who goes long enough between
backups could find their stored refresh token has quietly expired, requiring
a fresh browser-based re-auth next time — the app should treat this the same
way it already treats "no refresh token stored" (fall back to
`NeedsBrowserAuth`), so this doesn't need special handling, just awareness
that it's a somewhat more likely path here than with Dropbox.

## Status

**Planned, not yet implemented.** This document and `STORAGE_OD_BUILD.md`
(implementation notes, not committed to GitHub) scope out what's needed
before work begins. See those files / the commit log for the current state
of the OneDrive backup feature once implementation starts.
