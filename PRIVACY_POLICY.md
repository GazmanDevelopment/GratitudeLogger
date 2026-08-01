# Privacy Policy for Gratitude Logger

**Last updated:** 2026-08-01

This policy describes what information Gratitude Logger ("the app")
collects, how it's stored, and when it's ever shared with anyone else. It's
written to describe exactly what the app actually does — nothing here is
boilerplate copied from another app's policy.

## 1. Who this policy is for

Gratitude Logger is developed by Gareth. This policy applies to
everyone who installs and uses the app, and is provided to comply with the
requirements of the Google Play Store and of the third-party services
(Google Drive, Dropbox, Microsoft OneDrive) the app can optionally connect
to.

## 2. Information the app collects

The app collects only what you type or attach yourself:

- **Journal entries** — the gratitude text you write.
- **Photos** — an optional photo you attach to an entry, either captured
  with your camera or chosen from your gallery.

The app does **not** require you to create an account, does not collect
your name or email address, and contains no advertising or analytics
software of any kind. Nothing about your usage is tracked or transmitted
anywhere unless you explicitly use the backup feature described below.

## 3. Where your information is stored

By default, everything is stored **only on your device** — in a local
database and a private app folder that other apps cannot access. None of it
leaves your device unless you take the action described in the next
section.

## 4. Optional cloud backup

The app includes an optional, manual backup feature ("Back up now" /
"Restore latest"), which does nothing until you choose to use it. If you
do:

- You pick **one** destination: Google Drive, Dropbox, or Microsoft
  OneDrive.
- You sign in and grant access **directly to that provider** — the app
  never sees or stores your Google, Dropbox, or Microsoft password. It only
  receives a limited access token from that provider, scoped to a single
  private storage area:
  - **Google Drive**: a hidden app-data folder, invisible in your normal
    Drive file listing.
  - **Dropbox** / **Microsoft OneDrive**: a dedicated app folder (shown
    under an "Apps" folder in your account), which only this app can read
    or write — it cannot see or touch the rest of your Dropbox or OneDrive.
- A backup is a single file containing your journal entries and photos.
  Nothing else in your cloud account is read, modified, or accessed.
- For Dropbox and OneDrive, a "refresh token" (used to reconnect without
  asking you to sign in every time) is stored locally on your device only —
  it is never sent anywhere except back to that same provider to renew
  access.

If you never use the backup feature, none of this applies to you — your
data stays local for as long as the app is installed.

## 5. Who your information is shared with

Nobody, except the single cloud provider you personally choose and
authorize, and only when you actively trigger a backup or restore. The
developer does not have access to your journal entries, your photos, or
your cloud storage. Your information is never sold, rented, or shared for
advertising, marketing, or analytics purposes.

## 6. Security

- Your app PIN is never stored as readable text. It's converted into a
  salted cryptographic hash (PBKDF2), a one-way process that cannot be
  reversed back into your PIN.
- If you enable biometric unlock (fingerprint/face), your biometric data
  never reaches the app — it's handled entirely by your device's own
  operating system, which only tells the app "yes" or "no."
- All network communication with backup providers uses standard encrypted
  HTTPS connections.

## 7. Data retention and deletion

- **Local data** stays on your device until you delete individual entries
  yourself or uninstall the app, which removes everything immediately.
- **Cloud backups** persist in your chosen provider's storage until you
  delete them yourself, or revoke the app's access from that provider's own
  account permissions page (for example, Google Account → Security → Third-
  party access; Dropbox → Settings → Connected apps; Microsoft Account →
  Privacy → Apps and services). Uninstalling the app does **not**
  automatically delete a cloud backup — it remains in your account until
  you remove it there.

## 8. Children's privacy

Gratitude Logger is not directed at children and is not knowingly used to
collect information from children under 13 (or the relevant minimum age in
your region).

## 9. Your choices

- You can use the app entirely offline and never connect a backup provider.
- You can switch backup providers, or stop using backup entirely, at any
  time in Settings.
- You can disable the daily reminder and the backup reminder independently
  in Settings.
- You can revoke the app's cloud access at any time directly through your
  Google, Dropbox, or Microsoft account settings.

## 10. Changes to this policy

If this policy changes, the "Last updated" date at the top will change to
reflect it. Material changes affecting how your data is handled will be
noted here.

## 11. Contact

Questions about this policy or how the app handles your data can be sent to
gazman.development@gmail.com.
