# Privacy Policy for Revati

**Effective:** July 24, 2026 · **Last updated:** September 26, 2026

Revati ("we", "our", "us") is published by Msunjay Enterprises. This policy
explains what the **Revati : Kundli & Panchang** Android app collects, where it
goes, and how you can delete it. The iPhone app, which is newer and does less,
is covered in section 11.

*This app was previously published as "AstroVeda". It is the same app under a
new name.*

---

## 1. The Short Version

- **You do not need an account.** Everything except cloud backup works without
  one.
- Panchang, Kundali, Dasha, Guna Milan, Muhurat and numerology are
  **calculated on your phone**. Your birth details are not sent anywhere to
  produce them.
- Your birth details leave your phone only if you back them up to your
  account, export them yourself, or ask the AI a question while a Kundali is
  open.
- The app shows ads from Google AdMob, and uses Google's Firebase services for
  sign-in, backup, AI answers, usage statistics and crash reports.
- We do not sell, rent or trade your personal information.

---

## 2. Accounts Are Optional

You can use Revati without creating an account. An account adds one thing: a
cloud backup of your saved profiles, so they survive a new phone or a
reinstall.

If you create one, you can sign in with Google Sign-In or with an email address
and password, both through Firebase Authentication. We receive your name, your
email address and an account identifier. Your password goes to Firebase
Authentication, which stores it in hashed form; we cannot read it. Google
Sign-In may also pass the address of your profile photo to Firebase; the app
does not use it.

---

## 3. Information the App Handles

- **Birth details.** The name, date, time and place of birth you enter for a
  Kundali, a Guna Milan match or a saved profile. They are stored on your phone
  and leave it only as described in section 4.
- **Location, if you allow it.** The app reads your phone's precise location to
  calculate sunrise, sunset, Rahu Kaal, Choghadiya and the Panchang for where
  you are. The calculation happens on your phone. To show your city's name, the
  coordinates are passed to Android's built-in geocoder, which on most phones
  asks Google's location service. The app keeps only the last city it used, on
  your phone. It never builds a location history, never reads your location in
  the background, and never sends it to us. You can refuse and choose a city by
  hand instead. Separately from this, Google's analytics and advertising
  services estimate an approximate location, such as your city, from your
  device's IP address — see the next item and section 5.
- **Usage statistics.** Firebase Analytics records how the app is used: app
  opens, the screens you visit and the features you use, with coarse results —
  the rashi whose horoscope you opened, a Guna Milan score, whether a match has
  a Manglik mismatch, the single-digit numerology numbers a calculation
  produced, how you signed in, whether you shared something, and whether a
  purchase was started, completed or failed. It records that you asked the AI a
  question and how long the question was, but not what it said. It also
  receives basic device information, an app-instance identifier, and your
  language, theme and whether you have PRO, and Google derives a coarse
  location from your device's IP address. It never receives names, dates, times
  or places of birth, and it is not linked to your name or email address.
- **Crash reports.** Firebase Crashlytics collects a report when the app
  crashes or records an error: the device model, the Android version and a
  technical trace of what went wrong. Reports do not contain your birth
  details. If you are signed in, a report carries your account identifier so
  that a problem you tell us about can be matched to it.
- **Advertising identifier.** See section 5.
- **Notifications.** Daily Panchang, festival and Muhurat reminders are
  scheduled and written on your phone. No push messaging service is used, and
  no notification token exists.

---

## 4. What Leaves Your Phone

Calculations happen on your phone and work without an internet connection. Your
personal information is sent elsewhere only in these cases:

- **Cloud backup — only if you sign in.** Your saved profiles are stored in
  Google's Cloud Firestore under your account. The database's security rules
  let only your own signed-in account read or change them. They are encrypted
  in transit (TLS) and encrypted at rest by Google.
- **Asking the AI — only when you send a question.** Your question is sent to
  Google's Firebase AI Logic, which uses Gemini models, to compose a reply. If
  you have calculated numerology, the name, date of birth and the numbers worked
  out from them are sent with it; if a Kundali is open on screen, that person's
  name, date of birth and ascendant (lagna) are sent too — so the answer can
  refer to them. The time and place of birth are not sent. Google processes the question under its own terms for
  Firebase AI Logic and the Gemini API. Revati does not keep a history of your
  questions, but do not type anything into the question box that you would not
  want a third-party service to process.
- **Rashifal insights.** When you tap for an AI insight on a Rashifal, the name
  of the rashi, the period and that period's reading from the app are sent —
  nothing about you.
- **Astro news.** The highlights on the More tab are requested from the same
  service and carry no personal information.
- **Exporting your profiles.** When you export saved profiles, the app creates a
  file and hands it to your phone's share sheet. Where it goes is your choice.

AI answers are generated automatically; no human astrologer is involved. They
are traditional interpretation, not prediction or professional advice, and they
can be wrong. Revati does not give medical, legal or financial advice. If you
are in distress, Tele-MANAS (14416) offers free, confidential support in Indian
languages, 24x7.

---

## 5. Ads

Revati shows ads through Google AdMob: banners, full-screen ads between
screens, an ad when you return to the app, and an optional video you can choose
to watch before a PDF report is made — the report is free either way. To serve
and measure ads, AdMob may collect your device's advertising identifier, your IP
address — which can be used to estimate your general location — and
information about your activity in the app, and may share it with its
advertising partners.

In the EEA, the UK and other regions where the law requires consent, the app
asks for it through Google's consent form, the User Messaging Platform, before
any ad loads. You can change or withdraw that choice at any time from Settings
→ Legal & Privacy. You can also reset or delete your advertising identifier in
your phone's Google settings.

Ads are requested with a general-audience content rating, and the app is not
directed at children.

---

## 6. Purchases and Ratings

If you buy a subscription, payment is handled entirely by Google Play Billing.
We receive confirmation of the purchase, never your card or bank details.

If you choose to rate the app from inside it, Google Play's In-App Review
handles it. The rating goes to Google Play, and the app is not told whether you
left one.

---

## 7. Storage and Security

- Saved profiles, reports and recent searches are kept in the app's private
  storage on your phone, which other apps cannot read. They are excluded from
  Android's cloud backup and from device-to-device transfer, so they are not
  copied to Google Drive or to a new phone without you. To move them, sign in
  and use cloud backup, or export them.
- The cloud backup is protected as described in section 4.
- To stop other software from using our services in Revati's name, the app
  proves it is genuine through Firebase App Check, which uses Google Play
  Integrity. That check concerns the app and the device, not your birth details
  or your account.

---

## 8. Keeping and Deleting Your Data

- You can delete any saved profile inside the app at any time.
- **If you are signed in,** Settings → Account & data controls → Delete account
  & all data removes your cloud profiles, the profiles, reports and recent
  searches on your phone, and your sign-in account. It cannot be undone. For
  your protection you may be asked to sign in again first; nothing is deleted
  until that check passes.
- **If you are not signed in,** the same place offers Delete all saved data,
  which clears everything the app has stored on your phone. Uninstalling the
  app does the same.
- Or email us at the address below and we will delete your account and cloud
  data for you. Step-by-step instructions:
  https://github.com/sksaini946230-rgb/Revati/blob/main/docs/ACCOUNT_DELETION.md
- Usage statistics, crash reports and advertising data are kept by Google under
  its own retention periods. They are not linked to your name or email address.

---

## 9. Children

Revati is not directed at children and is not intended for anyone under 13. We
do not knowingly collect information from children under 13. If you believe a
child has given us their information, email us and we will delete it.

---

## 10. Services We Use

All of these are provided by Google, and each is covered by Google's privacy
policy at https://policies.google.com/privacy

- **Firebase Authentication** and **Google Sign-In** — optional sign-in
- **Cloud Firestore** — optional cloud backup
- **Firebase AI Logic** — AI answers, Rashifal insights and astro news
- **Firebase Analytics** — usage statistics
- **Firebase Crashlytics** — crash reports
- **Firebase App Check** with Google Play Integrity — confirms that requests
  come from the genuine app
- **Google AdMob** and the **User Messaging Platform** — ads and ad consent
- **Google Play Billing** and **In-App Review** — purchases and ratings
- **Google Play services** — your precise location, if you allow it

---

## 11. The iPhone App

The iPhone version of Revati does less than the Android app today, so it
handles less:

- **No sign-in, cloud backup, AI answers, ads, purchases, usage statistics or
  crash reports.** Neither Firebase nor AdMob is in it. The parts of sections
  2, 4, 5, 6 and 10 about those features apply to the iPhone only once they
  arrive there, and this section will be updated before they do.
- **Your settings and saved profiles stay on your iPhone.** Profiles are kept
  in a database encrypted with a key held in the iPhone's Keychain for this
  device only: never in iCloud Keychain and never in a backup.
- **Location, only when you ask for it.** When you tap "Use my location", the
  app asks for permission while it is in use. To name the town, the
  coordinates (or the name you type in a place search) are sent to Apple's
  geocoding service, covered by Apple's privacy policy at
  <https://www.apple.com/legal/privacy/>. Revati keeps only the town and its
  coordinates, on the phone.
- **Reminders are scheduled on the phone.** There is no push service and no
  notification token.
- **App updates.** When it opens, the app asks Expo's update service
  (expo.dev) whether a fix to its own code is available. That request carries
  the app's version, the platform and a random identifier Expo makes for this
  installation, and none of your data.
- **Sharing is yours to start.** A chart picture, a Guna Milan PDF or a
  profile file leaves the phone only when you share it through the iPhone's
  share sheet, to the app you pick.

---

## 12. Changes

If this policy changes materially, we will update the date at the top and,
where the change is significant, tell you in the app.

---

## 13. Contact

- **Developer / Organisation:** Msunjay Enterprises
- **Email:** supportrevati@gmail.com
- **Source and support:** https://github.com/sksaini946230-rgb/Revati
