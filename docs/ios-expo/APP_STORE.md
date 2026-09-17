# App Store — review risks, listing, submission

## Review risks, highest first

| Risk | Guideline | What we do |
|---|---|---|
| **Astrology apps are a saturated category** — Apple rejects ones that add nothing | 4.3(b) (spam; fortune telling is named) | Lead the listing and the first screens with what is actually different: an on-device ephemeris (Meeus + Lahiri) that works offline, a real Hindu Panchang for the user's own city, Choghadiya/Rahu Kaal/Muhurat, festival dates computed from tithi rules, full Hindi. Put the calculation in front, not generic "your future" copy. Review notes explain this in two sentences. If rejected, reply in Resolution Center with the same points before changing anything. |
| Sharing personal data with a third-party AI without consent | 5.1.2(i) | One-time consent sheet before the first AI call (see `SERVICES.md` §4). |
| Offers Google sign-in without Sign in with Apple | 4.8 | Sign in with Apple on iOS. |
| No in-app account deletion | 5.1.1(v) | Exists; with Apple token revocation. |
| Subscription screen missing terms, price or restore | 3.1.2 | Price, period, auto-renew wording, Terms + Privacy links, Restore button. |
| Asking for tracking before explaining | 5.1.2 / ATT | ATT prompt only after UMP, with a plain reason line; app works fully if declined. |
| Location or notification permission without a reason | 5.1.1 | Reason strings in both languages; asked in context (onboarding), never at cold launch before any UI. |
| Crashes or dead ends on iPad compatibility mode | 2.1 / 2.4.1 | Tested at iPad size even though `supportsTablet` is false. |
| Claims the app cannot back up | 2.3.1 | Same claims list as the Play listing: no invented ratings, user counts or "accurate predictions". |
| Implying a human astrologer answers | 1.1.6 / 2.3 | Keep the "answers are generated automatically" line. |
| Placeholder or debug content | 2.1 | Sample data only in screenshots; no test ads in release (the `AdIds` port refuses them). |

## App Store Connect — the record

| Field | Value / source |
|---|---|
| Name | Revati : Kundli & Panchang |
| Subtitle (30) | e.g. "Panchang, Rashifal & Kundli" — owner approves |
| Primary / secondary category | Lifestyle / Reference (owner approves) |
| Languages | English (primary), Hindi |
| Keywords (100 chars each language) | drafted from `docs/ASO_AND_MARKETING_PLAYBOOK.md` |
| Description, promotional text | adapted from the Play listing and the goldie store copy, both languages |
| Support URL | `https://sksaini946230-rgb.github.io/` (support section) |
| Marketing URL | `https://sksaini946230-rgb.github.io/` |
| Privacy Policy URL | the published policy, **updated for iOS first** |
| Age rating | new questionnaire; no mature content; ads rated T |
| Copyright | "2026 <seller legal name>" |
| Review contact | owner's name, phone, email |
| Review notes | what the app is, that sign-in is optional, how to reach PRO features in sandbox, that AI needs the consent sheet |
| Export compliance | the app uses encryption (HTTPS, SQLCipher for local data). Answered in App Store Connect; `ITSAppUsesNonExemptEncryption` set to match the answer. Owner reads and confirms the question before it is answered. |

## Privacy "nutrition" labels

Filled from the code, the same way the Play Data safety form was:

| Data type | Linked to user | Tracking | Why |
|---|---|---|---|
| Name, email (if signed in) | yes | no | account, backup |
| User content: saved profiles (name, DOB, birth time/place) | yes (if backed up) | no | app functionality |
| User content: AI question | no | no | app functionality (Google as processor) |
| Purchase history | yes | no | PRO |
| Coarse location (IP-derived by Analytics/AdMob) | no | per ATT answer | analytics, ads |
| Identifiers: device ID / IDFA (if ATT allowed) | — | yes if allowed | ads |
| Usage data, diagnostics, crash data | per SDK disclosures | ads-related only if allowed | analytics, ads, crashes |

Precise location is **not** collected: GPS coordinates stay on the device.
Re-check against the Firebase and Google Mobile Ads iOS privacy manifests at
submission time; every SDK must ship its privacy manifest (`PrivacyInfo.xcprivacy`).

## Screenshots

- Required sizes: **6.9"** (1320 × 2868) and, if still asked, 6.5"; Apple scales
  down for smaller phones. English and Hindi sets.
- Built the same way as the Play tiles (goldie), from TestFlight captures with
  sample data ("Aarav Sharma", "Rahul & Priya", Jaipur), no personal data, no
  status-bar clutter.
- Optional App Preview video later.

## Submission steps

1. TestFlight build approved for internal testing; owner's full test pass done.
2. Listing, labels, age rating, pricing (free with IAP), availability (India
   first; owner decides other countries) filled in.
3. Subscription submitted **with** the first app version (Apple reviews the
   first IAP together with a build).
4. `eas submit`, attach the build, "Submit for Review".
5. After approval: owner chooses manual or automatic release.
6. After release: link the listing in AdMob, add the App Store badge to the
   website, update `CLAUDE.md` and memory.
