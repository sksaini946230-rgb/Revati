# Revati — Account & Data Deletion Policy

**Developer / Organisation:** Msunjay Enterprises
**Application:** Revati : Kundli & Panchang
**Contact Email:** supportrevati@gmail.com

---

## How to request account and data deletion

You can delete your account and everything stored with it at any time, by either
method below.

---

### Method 1 — Delete inside the Revati app

1. Open **Revati**.
2. Go to **Settings** (the gear icon at the top right).
3. Scroll to **Account & data controls**.
4. Tap **Delete account & all data**.
5. Confirm.

For your own protection, Android and Firebase require a recent sign-in before an
account can be deleted. If you signed in some time ago, the app will ask you to
sign in again first. Nothing is deleted until that check passes — so if you see
that message, your data is still intact.

---

### Method 2 — Request deletion by email

If you have uninstalled the app or cannot reach your device:

1. Email **supportrevati@gmail.com** with the subject **"Account Deletion
   Request - Revati"**.
2. Include the email address of the account you used to sign in to Revati.
3. We verify the request and delete the data within **48 to 72 hours**, and email
   you once it is done.

---

## What is deleted

* **Account credentials** — your Firebase sign-in UID, email address and display
  name.
* **Saved birth profiles** — every Janam Kundali profile held in your cloud
  backup: names, dates of birth, times of birth, birth places and coordinates.
* **Everything on the device** — saved profiles, saved reports and recent
  searches in the app's local database.
* **The account record** — the name, email address, creation date and last-sync
  time kept so the operator can see who has an account.

## What is kept

* **A restriction, if an admin ever placed one,** and its entry in the admin log
  (account identifier, email address, reason, date, and which admin did it).
  They are the record of what was done and are not deleted with the account.

## What the app does not hold, and so cannot delete

Listed for accuracy, because an earlier version of this document promised to
delete two things the app has never collected:

* **Notification tokens.** Announcements are sent through Firebase Cloud
  Messaging, which gives each installation a token. Revati never stores it or
  links it to an account; uninstalling the app ends it.
* **AI conversation history.** Questions asked of the AI astrologer are sent to
  Google's model to be answered and are not stored by Revati — there is no chat
  history to delete.

## Retention

* **Deleted immediately:** your account, your cloud-backed birth profiles, and
  everything the app stored on your device.
* **Usage statistics and crash reports:** Firebase Analytics usage data and
  Firebase Crashlytics crash reports are kept by Google under its own retention
  periods. Neither contains your birth details or is linked to your name or
  email address. A crash report sent while you were signed in carries your
  account identifier, which points to nothing once the account is deleted.
* **Advertising data** collected by Google AdMob is governed by Google's own
  policies; you can reset your advertising identifier in your phone's Google
  settings.
