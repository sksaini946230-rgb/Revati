# Services — what each one becomes in Expo, and the console work behind it

Library names are the current best candidates; each is re-checked against the
Expo SDK in use at phase 0, and any that needs a native module is exercised in
a development build before it is relied on (Expo Go cannot load them).

Console steps are listed so the owner can do them — **the owner clicks, not
Claude.**

---

## 1. Build and delivery (EAS)

| Item | Plan |
|---|---|
| Expo account | A **separate** Expo account for Revati, so its build quota is not shared with any other app. Owner creates it. |
| Project | `~/Revati-Expo`, repo `sksaini946230-rgb/Revati-Expo` |
| iOS builds | EAS Build (cloud Mac). No local Xcode. Each build is asked for first. |
| Android builds | Locally with `npx expo run:android` onto the test phone during development — costs no EAS quota. |
| iOS credentials | EAS creates and stores the distribution certificate and provisioning profile after the owner logs in to the Apple account once in the terminal (`eas credentials`). |
| Test installs on iPhone | TestFlight (internal testers: the owner). A development build for the owner's iPhone needs the device registered (`eas device:create`). |
| Submitting | `eas submit -p ios` uploads to App Store Connect with an App Store Connect API key. |
| OTA fixes | EAS Update for JavaScript-only fixes, channel per build profile; never used to change what a review approved in a way Apple would object to. |
| Quota | The free plan's monthly iOS build count is small. Plan: batch changes, build only at gates; if it runs out, the owner decides on a paid month. Check expo.dev → Billing before phase 0. |

## 2. App identity

| | Android (today) | iOS (new) |
|---|---|---|
| Name on store | Revati : Kundli & Panchang | Revati : Kundli & Panchang (30-char limit — fits) |
| Id | `com.aistudio.astroveda.kpvqzm` (permanent) | Bundle ID proposal: **`app.revati.jyotish`** (matches the Android namespace; permanent once used — owner confirms) |
| Seller | Msunjay Enterprises | An **Individual** Apple account shows the owner's personal legal name as the seller, not the business. Showing "Msunjay Enterprises" needs an Organization account (D-U-N-S). Owner decides. |
| Version | 2.x | starts at 1.0.0, build number auto-incremented by EAS |

## 3. Firebase (production `astroveda-7126b`, debug `revati-debug`)

| Step | Who | Notes |
|---|---|---|
| Add an **iOS app** with the bundle ID to `astroveda-7126b`; download `GoogleService-Info.plist` | Owner | The plist is not a secret; it goes in the repo like the Android file does. |
| Same in `revati-debug` for development builds (bundle ID + `.debug` suffix) | Owner | Keeps development traffic out of production, as on Android. |
| Firebase creates an **iOS API key**; restrict it to *iOS apps* with the bundle ID | Owner | The existing key is restricted to Android apps and will refuse iOS calls — that is correct, do not loosen it. |
| **App Check → App Attest** (with DeviceCheck fallback) for the iOS app; enforce for Firebase AI Logic | Owner | Needs the Apple Team ID. Debug builds register a debug token, as on Android. |
| Authentication → **Apple** provider enabled (Services ID, key ID, private key from the Apple account) | Owner, guided | Required because the app offers Google sign-in (App Store guideline 4.8). |
| Google sign-in on iOS: iOS OAuth client (Firebase creates it), reversed client ID as URL scheme | Claude (config) | In the Expo config plugin. |
| Firestore rules | unchanged | Same `users/{uid}/…` rule covers iOS. |
| Analytics / Crashlytics | Claude | React Native Firebase; Crashlytics needs its build step configured for dSYM upload on EAS. |

## 4. AI (Gemini via Firebase AI Logic)

- Same model, prompts, limits and PRO gate as the Kotlin app
  (`GeminiAstroService.kt`, `AiRateLimiter.kt`, 3 s / 20 per hour / 50 per day).
- App Check token attached to every call.
- **New for iOS:** App Store guideline 5.1.2 requires clearly disclosing that
  personal data goes to a third-party AI and getting explicit permission
  first. A one-time consent sheet before the first AI use, naming what is sent
  (name, date of birth, lagna, numerology numbers, the rashi, the period, the
  question — never time or place of birth, never location) and to whom
  (Google). Shown on Android too, for consistency.
- News with search grounding: same, with the bilingual offline fallback.

## 5. Sign-in and cloud backup

- Optional, only for backup — same as today.
- Google, **Apple (iOS)**, email/password with name, forgot password.
- Firestore `users/{uid}/kundali_profiles/{uuid}`, batched at 450,
  `ProfileMerge` rules (uuid only).
- Delete account: refresh the token first, then wipe Firestore, then delete the
  user — the Kotlin order. With Apple sign-in, also **revoke the Apple token**
  on deletion (Apple requirement).

## 6. Purchases (PRO, ₹199 a year)

Decision for the owner at phase 4:

| Option | For | Against |
|---|---|---|
| **RevenueCat** (recommended) | Server-side receipt validation for both stores without us running a server — closes the "PRO can be faked with a patched app" gap noted in `CLAUDE.md`; one entitlement across iOS and Android later; free below a revenue threshold | A third-party SDK: named in the privacy policy and store forms |
| `expo-iap` / `react-native-iap` | No third party | Validation stays client-side, as today |

Either way:

- **App Store Connect:** an auto-renewable subscription in a subscription
  group, one-year duration, price tier ₹199 (India), localized name and
  description in English and Hindi, review screenshot. Product ID proposal:
  `revati_pro_yearly`.
- **Paid Apps Agreement**, bank account and tax forms must be complete in App
  Store Connect before any purchase works — including sandbox testing.
- **Small Business Program** enrolment halves Apple's commission (15%).
- "Restore purchases" button (required on iOS).
- Subscription terms, price and renewal wording on the PRO screen, plus links to
  Terms and Privacy — Apple rejects subscription screens without them.
- Sandbox tester account for the owner's test purchase.
- PRO bought on iPhone does not unlock Android and vice versa unless tied to an
  account (RevenueCat can, via the Firebase uid). Say so in the FAQ.

## 7. Ads (AdMob)

| Step | Who |
|---|---|
| Add an **iOS app** in AdMob (same publisher `pub-5513456541171739`) | Owner |
| Create four iOS ad units: banner, interstitial, app open, rewarded | Owner |
| Link the App Store listing once it is live (app verification) | Owner |
| `app-ads.txt` — already on the website with this publisher id; the App Store listing's marketing/support URL must point at the same domain | Owner |
| Ad rating **T**, not child-directed, UMP consent before init | Claude |
| iOS: `SKAdNetworkItems` in Info.plist, **App Tracking Transparency** prompt after UMP and before the first ad request, `NSUserTrackingUsageDescription` in both languages | Claude |
| Test units in development, release refuses test ids (port `AdIds`) | Claude |
| Same frequency rules as Android (`FEATURE_PARITY.md` 8.12) | Claude |

Ads on iPhone are on unless the owner says otherwise (a question in
`OWNER_CHECKLIST.md`).

## 8. Notifications (local only)

iOS has no reliable daily background job, and **iOS keeps at most 64 pending
local notifications per app.** So the Expo app computes the text ahead and
schedules it:

- Daily Panchang: next 21 days.
- Muhurat: next 21 days.
- Festivals: next 20 festival reminders.

That stays under 64. Every app open (and a background fetch when iOS grants
one) tops the queue up, so a user who opens the app weekly never runs dry. If
the user does not open the app for three weeks, reminders stop — acceptable, and
said nowhere in the UI as a promise. Language and city changes reschedule
everything. Android uses the same scheduling code (exact alarms not needed at
this precision).

Permission is asked at onboarding, as today, with a clear reason line.

## 9. Widgets

| Platform | Plan |
|---|---|
| iOS | A WidgetKit extension written in SwiftUI, added to the Expo project as an Apple target (config plugin), compiled by EAS — no Xcode needed locally. Small and medium sizes: Panchang (sunrise, sunset, tithi, nakshatra, Rahu Kaal) and Tithi & Nakshatra. Data: the app writes the next 14 days of values into a shared App Group; the widget reads it and refreshes at midnight. Both languages. Lock-screen accessory widget optional. |
| Android | `react-native-android-widget`, same two widgets as today — only relevant if Android moves over. |

Needs an **App Group** identifier on the Apple account (EAS can create it).

## 10. Location and city

- `expo-location`: when-in-use permission only, with a reason string in both
  languages; reverse geocode on the device to name the city.
- Coordinates never leave the device — not to AI, not to Firestore, not to
  Analytics (same rule as today). The privacy policy depends on it.

## 11. Files, sharing, PDF, review

- Profile export/import: the `revati-profiles` v1 JSON format, shared via the
  share sheet, imported via the document picker.
- Kundali share image: render the chart view to PNG, share sheet.
- Guna Milan PDF: HTML template → `expo-print` → share/print.
- Rate the app: `expo-store-review`, same once-a-while rules.

## 12. Local storage and security

- `expo-sqlite` with SQLCipher enabled; 32-byte random key in
  `expo-secure-store` (Keychain, "after first unlock, this device only"; never
  in iCloud Keychain, never in backups).
- Real schema migrations from version 1, with a migration test per step.
- Settings in a small key-value store; nothing sensitive there.
- Input sanitising ported from `SecurityUtils` to every free-text entry.
- Network: HTTPS only (ATS on by default — no exceptions added).
- WebView for bundled legal pages only, JavaScript off.
