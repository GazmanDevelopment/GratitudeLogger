# Specification Document for Gratitude Journal Android App

## Overview

The **Gratitude Journal** app is designed to help users record their daily gratitude journal entries. The app will store all data locally and provide options to backup and restore the data to Google Cloud, OneDrive, or DropBox.

---

## Features

### 1. Daily Reminder
- Implement a daily reminder with a customizable time.
- Provide an option to dismiss the reminder and open the app directly from it.

### 2. Data Storage
- Store all data locally on the device.
- Allow users to backup their data to Google Cloud, OneDrive, or DropBox.
- Implement a restore feature for backed-up data.

### 3. Monthly Calendar View
- Display a monthly calendar view of the current month.
- Provide an option to navigate through previous months.
- Colour-code days with journal entries (e.g., red dot for entries).

### 4. Journal Entries
- Allow users to add new journal entries only for the current day.
- Display all journal entries for a selected day in a list view.
- Provide an option to attach a photo to each entry.

### 5. Security
- Request a user PIN on first login.
- Provide an option to use biometrics (e.g., face, fingerprint) if available.

---

## Design Requirements

1. The app should have a clean and minimalistic design.
2. The monthly calendar view should be easily navigable through previous months.
3. Journal entries should be displayed in a list view with an option to add new entries.
4. Photo attachment should be optional for each entry.

---

## Technical Requirements

1. Develop the app using Android SDK (Java or Kotlin).
2. Implement data storage and backup/restore features using local database and cloud services.
3. Use a calendar library to display the monthly calendar view.
4. Integrate biometric authentication if available on the device.