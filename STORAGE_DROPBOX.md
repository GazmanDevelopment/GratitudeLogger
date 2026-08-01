# Cloud Backup Setup (Dropbox)

This document covers the **one-time developer setup** required before the app's
Dropbox backup/restore feature can work. This is done **once, by the app's
developer/publisher**, in their own Dropbox account — end users never see the
Dropbox App Console or register anything themselves. They just tap "Back up
now" inside the app, approve access in a browser tab, and it backs up to their
own Dropbox, the same as any other app that supports "Connect to Dropbox".

## Why an "App folder", not full Dropbox access

Backups are stored in a dedicated **App folder** (`Apps/Gratitude Logger/` in
the user's Dropbox) rather than requesting access to their whole Dropbox:
- The app can only see and write inside its own folder — never the rest of
  the user's files.
- This maps to the `files.content.write`/`files.content.read` scopes only,
  not the broader `files.content.write`/`sharing` scopes full-Dropbox access
  would need.

This is Dropbox's direct equivalent of Google Drive's hidden `appDataFolder`
(see `STORAGE.md`) — the same "sandboxed, per-app storage" idea, just visible
under `Apps/` in the user's own Dropbox rather than fully hidden.

## Scope of what's backed up

Identical to the Google Drive provider — a single zip archive (`gratitude-
backup.zip`) containing the Room database and everything under the app's
private `files/photos/` directory. See `STORAGE.md` for the full rationale;
both providers share the same `BackupArchiver`.

## One-time setup steps (Dropbox App Console)

1. Go to [dropbox.com/developers/apps](https://www.dropbox.com/developers/apps)
   and create a new app.
2. Choose **Scoped access**, and access type **App folder** (not "Full
   Dropbox").
3. **Permissions** tab: enable `files.content.write` and `files.content.read`.
4. **Settings** tab:
   - Note the **App key** — this is the only credential the app needs. Unlike
     Google's setup, there's **no client secret to protect**: the app
     authenticates using OAuth2 PKCE, which is designed for exactly this kind
     of installed app that can't keep a secret confidential.
   - Under **OAuth 2** → **Redirect URIs**, add:
     ```
     gratitudelogger://oauth2redirect
     ```
     A non-`https` custom URI scheme is explicitly supported here for PKCE
     flows (unlike the general "must be https" rule Dropbox applies to its
     older, non-PKCE auth code flow).
   - Unlike Google/Microsoft, there's **no package name or signing-certificate
     fingerprint to register at all** — Dropbox's app registration is entirely
     platform-agnostic, so nothing here needs to change between debug/release
     builds or between developer machines.
5. Take the **App key** and put it in your local `local.properties` (already
   gitignored, never committed) as:
   ```
   DROPBOX_APP_KEY=xxxxxxxxxxxxxxx
   ```
   The Gradle build reads this into a `BuildConfig` field, so the actual key
   never touches source control.

## Development vs Production status

New apps start in **Development** status, usable by up to 500 linked Dropbox
accounts without any review — more than enough for personal use or a small
group of testers. If you ever exceed 50 linked accounts, Dropbox gives you a
2-week window to apply for Production status before new logins are blocked;
this app is nowhere near that scale, so no action is needed here for now.

## What end users experience

None of the above. An end user just sees **Back up now**/**Restore latest**
in Settings → Backup & Restore (after selecting Dropbox as the provider).
Tapping either opens a browser tab to Dropbox's own login/consent screen the
first time, asking to grant this app access to its own app folder — the same
"Connect to Dropbox" pattern used by other apps. Approving redirects straight
back into the app, which then completes the backup/restore. Later taps don't
need the browser again (a refresh token is stored on-device, and is
re-exchanged for a fresh access token on each use rather than being used
directly). They never interact with the Dropbox App Console, never see an
App key, and never need a Dropbox developer account of their own.

## Status

Implemented. `domain/backup/BackupProvider.kt` is the provider-agnostic
interface (shared with Google Drive — see `STORAGE.md`);
`data/backup/DropboxBackupProvider.kt` is the Dropbox implementation, selected
via a provider picker in Settings → Backup & Restore (`ui/backup/BackupScreen.kt`).

Unlike the Google Drive provider, Dropbox's OAuth flow requires a browser hop
(a Custom Tab, not an in-process consent dialog) and returns a **refresh
token**, which is the one piece of standing credential this app persists to
disk (in the same plain DataStore used for everything else — see the
`BackupPreferences` reasoning in code, consistent with this app's existing
"OS sandbox + PIN/biometric lock is enough for a personal journal" threat
model rather than adding a separate encryption-at-rest dependency).
