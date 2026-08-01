# Deployment Guide: Google Play Store

This document outlines every step needed to take this app from "builds and
runs on my device" to a public listing on the Google Play Store, assuming
**no existing Google Play Developer account**. It also covers moving each of
the three backup providers (Google Drive, Dropbox, OneDrive) out of their
current "developer testing only" configuration into something that works for
real end users.

"App store" here means Google Play specifically — it's the only realistic
distribution channel for a Kotlin/Compose Android app without a very
different strategy (F-Droid, Amazon Appstore, Samsung Galaxy Store, or
sideloaded APKs are alternatives, not covered here).

Where a policy detail is time-sensitive (fees, tester-count requirements,
target API deadlines), this doc states what was current as of **August
2026** and flags it as worth re-checking at submission time, since Google
changes these periodically.

## Overview: what's already done vs. what's new

Already in place from development so far:
- `android:allowBackup="false"`, no `CAMERA` permission declared (camera
  capture delegates to an external camera app via intent, not a direct
  permission), `compileSdk`/`targetSdk` 37 — already ahead of Google Play's
  **August 31, 2026** requirement that new apps/updates target API 36+.
- No analytics, ads, or crash-reporting SDKs anywhere in the app — this
  simplifies the Data Safety form considerably (see Phase 4).

Still needed, roughly in order:
1. Google Play Developer account
2. Release signing configuration (code change)
3. Backup provider production readiness (Google, Dropbox; OneDrive needs no action)
4. Privacy policy + Play Console compliance forms
5. Store listing assets
6. Closed testing (mandatory for new personal accounts)
7. Production release

---

## Phase 1: Google Play Developer account

1. Go to [play.google.com/console/signup](https://play.google.com/console/signup)
   and sign in with the Google account you want to publish under (consider
   creating a dedicated account rather than a personal one, since it becomes
   the public-facing support contact).
2. Choose **Personal** or **Organization** account type:
   - **Personal** is the normal choice for a solo developer — no D-U-N-S
     number or business registration needed.
   - **Organization** requires a D-U-N-S number and business verification,
     but is **exempt from the closed-testing requirement** in Phase 6.
     Not worth the overhead for a first release unless you already have a
     registered business.
3. Pay the **one-time $25 USD registration fee** (non-refundable, no annual
   renewal — confirmed current as of August 2026).
4. Complete identity verification — Google requires a government ID and,
   depending on account type, this can take anywhere from a few hours to
   several days. **Start this early**; it's the step most likely to
   introduce unexpected delay.

## Phase 2: Release signing

The app currently only builds unsigned/debug-signed output — there's no
`signingConfig` on the `release` build type in `app/build.gradle.kts`. This
needs a real signing key before a release build can be uploaded.

1. Generate an **upload keystore** (distinct from the debug keystore already
   used for local testing):
   ```
   keytool -genkeypair -v -keystore upload-keystore.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```
   Store this file and its passwords somewhere durable and *outside the
   repo* (a password manager, not `local.properties` committed by accident —
   it's already gitignored, but double-check before this step).
2. Enable **Play App Signing** (Google's recommended default for new apps):
   you upload signed with your *upload key*, and Google re-signs with an
   *app signing key* it manages and protects — if you ever lose the upload
   key, Google can help you reset it. Managing your own signing key end-to-
   end is the alternative but means losing the keystore permanently blocks
   future updates, with no recovery path.
3. Add a release `signingConfig` to `app/build.gradle.kts`, reading the
   keystore path/passwords from Gradle properties (not hardcoded, not
   committed) — e.g. via `~/.gradle/gradle.properties` or environment
   variables:
   ```kotlin
   signingConfigs {
       create("release") {
           storeFile = file(providers.gradleProperty("UPLOAD_STORE_FILE").get())
           storePassword = providers.gradleProperty("UPLOAD_STORE_PASSWORD").get()
           keyAlias = providers.gradleProperty("UPLOAD_KEY_ALIAS").get()
           keyPassword = providers.gradleProperty("UPLOAD_KEY_PASSWORD").get()
       }
   }
   buildTypes {
       release {
           signingConfig = signingConfigs.getByName("release")
           // ...existing isMinifyEnabled/proguardFiles...
       }
   }
   ```
4. Build the release artifact as an **Android App Bundle**, not an APK —
   Play Store requires `.aab` for new app submissions:
   ```
   ./gradlew bundleRelease
   ```
5. Optional but recommended: enable `isMinifyEnabled = true` for the release
   build type once the above is working, to shrink/obfuscate the release
   binary. This will need testing afterward — R8 can occasionally strip
   classes that Room/Hilt/kotlinx.serialization rely on via reflection, so
   verify a signed release build still runs correctly (add `proguard-rules.pro`
   keep rules if something breaks, don't just leave minification off to
   avoid the risk).

## Phase 3: Backup provider production readiness

Each provider's current configuration works for the *developer's own
testing* (per `STORAGE.md`, `STORAGE_DROPBOX.md`, `STORAGE_ONEDRIVE.md`) but
not yet for arbitrary public users installing from the Play Store.

### Google Drive

The OAuth consent screen is currently in **Testing** status, capped at 100
manually-added test-user emails — this does not work for a public app where
anyone can install and sign in.
1. In Google Cloud Console → **Google Auth Platform** → **Branding**, fill
   in all required fields: app name, user support email, app logo (optional
   but recommended), and a **privacy policy URL** (see Phase 4 — this is a
   hard blocker, the form won't submit without one).
2. Click **Publish App** to move the consent screen from Testing to **In
   production**.
3. Because `drive.appdata` is classified as a **non-sensitive scope**, this
   app likely does **not** need Google's full security assessment (CASA)
   that broader Drive scopes require — but Google may still run a basic
   brand-verification check if a custom logo/domain is used. Re-confirm this
   against Google's current OAuth verification requirements at submission
   time, since scope classifications and review thresholds do shift.
4. No code changes needed — same `GOOGLE_WEB_CLIENT_ID` continues to work
   once the consent screen is published.

### Dropbox

The app is currently in **Development** status (works for up to 500 linked
accounts, no review). Once past 50 linked accounts, Dropbox gives a 2-week
window to apply before blocking new logins, so apply well before a public
launch rather than waiting for that trigger:
1. Dropbox App Console → your app → **Apply for production** (or similar,
   the exact label may have changed — look under the app's status/settings).
2. This is a review form, not just a toggle — expect to describe the app's
   use case (personal journal backup), confirm the `files.content.write`/
   `files.content.read` scopes are still the minimum needed (App Folder
   access, not Full Dropbox), and link a privacy policy.
3. No code changes needed — same `DROPBOX_APP_KEY` continues to work once
   approved.

### OneDrive (Microsoft Graph)

**No production-readiness step is required here.** Unlike Google/Dropbox,
a Microsoft Entra app registration using a non-admin-consent scope like
`Files.ReadWrite.AppFolder` has no Testing/Development gate at all — any
Microsoft account (personal or work/school, per the earlier widening to the
`/common/` authority) can already consent immediately. The only optional
polish item:
- **Microsoft Partner Network (MPN) publisher verification** — without it,
  the consent screen shows "unverified" next to the app name. Purely
  cosmetic/trust-signal, not a functional blocker; worth doing for a public
  release but not required to publish.

## Phase 4: Privacy policy & Play Console compliance

### Privacy policy — required before Phase 1 submission or Phase 3's Google step

Google Play requires a privacy policy URL for any app handling personal
data (this one does — journal text and photos, plus optional cloud sync).
This repo doesn't include one; a page needs to be written and hosted
somewhere public (GitHub Pages from this repo is a natural fit — e.g. a
`docs/privacy.html` file with GitHub Pages enabled, or any static host).

Based on what the app actually does, the policy needs to accurately cover:
- **What's collected**: gratitude journal entries (text) and optional
  photos, entered directly by the user. No account registration, no email
  collection, no analytics or advertising SDKs anywhere in the app.
- **Where it's stored**: entirely on-device (Room database + app-private
  photo storage) by default.
- **Optional cloud sync**: only when the user explicitly taps "Back up now"
  or "Restore latest" — at that point, a zip of the database + photos is
  sent to *whichever single provider the user has selected* (Google Drive,
  Dropbox, or OneDrive), stored in that provider's app-private/hidden
  storage area (not the user's regular Drive/Dropbox/OneDrive file listing
  in Drive's/Dropbox's case). This is standard OAuth the user grants
  directly to Google/Dropbox/Microsoft — the app itself never sees the
  user's cloud account password, only a short-lived access token (and, for
  Dropbox/OneDrive, a refresh token stored locally on-device, never
  transmitted anywhere else).
- **PIN/biometric security**: the PIN is stored only as a salted
  PBKDF2 hash, never in plaintext, never transmitted anywhere. Biometric
  unlock is handled entirely by the Android `BiometricPrompt` system API —
  the app never receives or stores raw fingerprint/face data itself.
- **Data deletion**: uninstalling the app removes all local data; deleting
  a cloud backup is done directly through the user's own Google
  Drive/Dropbox/OneDrive account (or by revoking the app's access via that
  provider's own account-permissions page).
- **No data selling, no third-party sharing** beyond the user's own
  explicitly-chosen backup provider.

### Data Safety form (Play Console → App content)

Fill this out consistent with the policy above. Roughly:
- **Data types collected**: "Files and docs" (journal entries/photos) —
  collected, not required for basic app function beyond local use, shared
  with a third party (the user's chosen cloud provider) only at explicit
  user action, encrypted in transit (standard HTTPS/TLS for every network
  call the app makes).
- **Personal info**: none collected by the app itself.
- No advertising ID, no analytics identifiers — there's no SDK in this app
  that would collect either.

### Other required Play Console declarations

- **Exact alarm permission** (App content → Permissions declarations): the
  app requests `SCHEDULE_EXACT_ALARM` for the daily reminder's exact-time
  notification. Play Console requires a short justification form for this
  permission on apps targeting API 31+ — describe the daily-reminder-at-a-
  user-chosen-time use case.
- **Content rating questionnaire**: answer honestly: no violence, no user-
  generated content shared publicly (entries are private and local/cloud-
  backed only, never shared between users), no ads. Should land in the
  lowest/most permissive rating tier.
- **Target audience & content**: this app isn't designed for or targeted at
  children — declare the target age group accordingly (avoids the stricter
  Families Policy requirements, which don't fit a personal journaling app
  anyway).
- **Ads declaration**: declare "No ads" — there are none.
- **Government apps / Financial features / Health declarations**: not
  applicable, decline/skip.

## Phase 5: Store listing assets

None of these exist yet and all need to be created before submission:
- **App icon**: a 512×512 hi-res PNG for the store listing (separate from
  the in-app adaptive launcher icon already in `res/mipmap-anydpi-v26/`).
- **Feature graphic**: 1024×500 PNG/JPG, shown at the top of the store
  listing.
- **Phone screenshots**: at least 2 required (Google recommends more,
  showing the calendar/feed, an entry with a photo, Settings, and the
  Backup & Restore screen would cover the app's main value well).
- **Short description** (80 characters max) and **full description** (4000
  characters max) — marketing copy, not covered by this doc.
- **App category**: likely "Lifestyle" or "Productivity."
- **Contact details**: a support email is required; a website is optional.

## Phase 6: Closed testing (mandatory for new personal accounts)

**Confirmed current as of August 2026**: personal Play Developer accounts
created after November 13, 2023 must complete closed testing before
production access is granted — organization accounts are exempt.

1. Create a **closed testing** track in Play Console, upload the signed
   release bundle from Phase 2.
2. Add **at least 12 testers** (reduced from 20 as of December 2024) — via
   email list or a Google Group — who each opt in via the testing link and
   actually install/open the app.
3. Testers must stay opted in continuously for **at least 14 days** after
   the release is approved and the 12-tester threshold is met — the clock
   only starts once both conditions hold, and dropping below 12 testers at
   any point during the window resets it. Recruit a couple more than the
   bare minimum as a buffer against someone opting out.
4. Only after this 14-day window can the app be promoted to production.

Plan for this window early — it's calendar time, not engineering time, and
is often the longest single step in the whole process for a first release.

## Phase 7: Production release

1. Promote the tested build from the closed testing track to **Production**
   (or create a new production release from the same signed bundle).
2. Complete the final rollout — Play Console supports a staged rollout
   (e.g. 20% of users first) if you want to de-risk the first public
   release; not required, but a reasonable default for a first submission.
3. Review typically takes anywhere from a few hours to a few days for a
   first submission.

## Phase 8: After launch (not blockers, worth planning for)

- **Crash reporting**: nothing is currently wired up (no Firebase
  Crashlytics or equivalent). Play Console's own "Android vitals" gives
  basic crash/ANR visibility for free without adding any SDK, which may be
  sufficient for a small personal-scale app — consider before adding a new
  dependency.
- **Versioning discipline**: bump `versionCode` (always increasing integer)
  and `versionName` (user-facing string) in `app/build.gradle.kts` for every
  release going forward.
- **Keep the upload keystore safe long-term** — losing it (without Play App
  Signing enabled) permanently blocks future updates to this app listing.
