# Revati on iPhone — the Expo rebuild plan

Written 17 Sep 2026, on the owner's decision: **Revati is rebuilt in Expo
(React Native) so it can ship on the App Store without a Mac or Xcode**, the
same way the owner's other app was built. Nothing is started until the owner
approves this plan and the design (phase 1).

This folder is the whole plan. Read in this order:

| File | What it answers |
|---|---|
| `README.md` (this) | the decision, the rules, the phases, the gates |
| `ACCOUNTS.md` | the real Expo and Apple identifiers, and what is still missing |
| `OWNER_CHECKLIST.md` | everything the owner has to do or hand over, and when (Hinglish) |
| `FEATURE_PARITY.md` | every screen, setting, data path and behaviour of the Kotlin app — the rebuild is not done until every line is ticked |
| `ENGINE_PORT.md` | how the calculation engine moves to TypeScript without changing a single answer |
| `PLATFORM_DESIGN.md` | how iOS and Android look different on purpose, and how they are kept apart |
| `SERVICES.md` | Firebase, sign-in, App Check, AI, ads, purchases, notifications, widgets — the Expo equivalent of each, and the console work behind it |
| `APP_STORE.md` | Apple review risks, privacy labels, screenshots, submission |
| `ANDROID_HANDOVER.md` | whether and how the Play app ever moves to the Expo build without losing anyone's saved profiles |
| `CODE_STANDARDS.md` | the owner's seven standards, turned into commands that refuse a build |
| `BRAND_COLORS.md` | the only place hex codes are written down |
| `LESSONS_CARRIED_OVER.md` | every bug the Kotlin app already shipped once, restated as a rule for the new code |

---

## The decision, and why

Revati today is ~30,000 lines of Kotlin/Compose (5,075 of them the calculation
engine, 231 unit tests). Kotlin/Compose does not run on iPhone. The choices
were:

| Option | Verdict |
|---|---|
| Kotlin Multiplatform engine + SwiftUI screens | Needs a Mac with Xcode 26 on hand for every change. Rejected by the owner. |
| Compose Multiplatform everywhere | No real Liquid Glass on iOS; the app would look like an Android app. |
| **Expo / React Native, built in the cloud with EAS** | **Chosen.** No Mac needed, real iOS system tab bar (Liquid Glass), one codebase with deliberately separate iOS and Android looks, and a workflow the owner has already seen work end to end. |

The price of the choice is that the engine is rewritten in TypeScript. That is
the single biggest risk in the whole project, and `ENGINE_PORT.md` is how it is
contained: the Kotlin app produces the answers, the TypeScript code must
reproduce every one of them, and nothing moves on until it does.

---

## Non-negotiable rules

1. **The Kotlin app keeps shipping.** `~/Revati` stays the Play app, untouched
   by this work except for one test-only file that dumps golden answers
   (phase 2). Play releases carry on as before.
2. **New code lives in a new folder and a new repo**: `~/Revati-Expo`,
   GitHub `sksaini946230-rgb/Revati-Expo` (never a repo named `AstroVeda` — see
   `CLAUDE.md`).
3. **Nothing from the owner's other project is copied** — no code, config,
   account, key or decision. Only the *approach* (Expo + EAS) is shared. If a
   pattern from there looks useful, ask first.
4. **iOS and Android look different on purpose and never leak into each
   other.** See `PLATFORM_DESIGN.md`. A test loads the UI modules as each
   platform and fails if the Android output changes because of iOS work.
5. **Every answer the engine gives must match the Kotlin engine.** Golden
   fixtures, not eyeballing.
6. **Both languages, always.** No bare user-facing string, same as the Kotlin
   app.
7. **No build without asking.** EAS builds have a monthly quota; ask before
   every one. Push to GitHub without asking.
8. **Console and browser changes are the owner's.** Say exactly what to click;
   do not click it.
9. **Phone and browser checks are the owner's.** Report what to look at; the
   owner looks.
10. **The seven standards in `CODE_STANDARDS.md` are build gates**, not
    intentions: clean structure, no duplicate or dead code, every check green
    before a build, hack-proof by the table in §4, nothing that reads as
    vibe-coded, and the owner's colours. `npm run preflight` is all of them and
    the build script cannot be reached around it.
11. **Every build, on both platforms, is made on expo.dev with EAS.** Never a
    local `expo run:android`, never a local archive. One build path means one
    set of native settings, one signing story, one build log — and a record on
    expo.dev of every artifact that ever existed. Saving quota is not a reason
    to have two ways of building.
12. **Colours come from `BRAND_COLORS.md` only** — green `#2E8B4E`, copper
    `#E8934A` (replacing gold), cream `#F0D09A`, background `#0B0E1A`. A
    literal hex in `src/` outside `theme/` fails lint.

---

## Phases and gates

Each phase ends with something the owner can see, and the next phase does not
start without a yes.

| # | Phase | Output | Gate (owner) | Rough time |
|---|---|---|---|---|
| 0 | Accounts and skeleton ✅ *20 Sep 2026* | Expo account, empty app with both platforms' tab bars, first TestFlight build on the owner's iPhone, Android dev build on the test phone | "Mere iPhone par khul gaya" | 3–5 days |
| 1 | Design 🟡 *mockups v2, 25 Sep 2026, waiting on the owner* | Mockups of every screen, iOS and Android side by side, both languages, both themes, small and large phone | Design approved | ~1 week |
| 2 | Engine ✅ *20 Sep 2026* | TypeScript engine + golden fixtures from Kotlin, 100% match | Report showing 0 differences | 1–2 weeks |
| 3 | Screens | All screens from `FEATURE_PARITY.md`, offline features complete | TestFlight walk-through | 2–4 weeks |
| 4 | Services | Sign-in, cloud backup, AI (PRO), ads, purchases, notifications, widgets | Each service checked on the iPhone | 1–2 weeks |
| 5 | Hardening | Parity checklist 100%, accessibility, font scaling, SE-to-Pro-Max layouts, crash-free test round | Owner's full test pass | ~1 week |
| 6 | App Store | Listing, screenshots, privacy labels, review | Approved and live | 1–2 weeks |
| 7 | Android decision | Only after iOS is live: whether Play moves to the Expo build | Separate decision, see `ANDROID_HANDOVER.md` | later |

**Phase 1 status (25 Sep 2026).** Mockups v2 of 16 screens are an artifact:
https://claude.ai/artifact/G5dZchv9sAAKEFwwfHa9vs — iPhone and Android side by
side, Hindi/English, dark/light, small/large, every number from the TypeScript
engine (Jaipur, 25 Sep 2026, 7:00 AM). Each screen was rendered in headless
Chrome and looked at before v2 went out; that pass caught selected pills with
invisible labels, chart lines too pale on the light theme, and an overflowing
South Indian house. Phase 2 ran ahead of phase 1 because the engine does not
depend on the design. **No screen is built until the owner approves these.**
Open decisions the page lists: the fifth tab's name (More vs Settings), Apple
+ Google sign-in on iPhone, English in brackets inside Hindi engine strings,
muhurat windows that run sunrise to sunset whatever the nakshatra does (and a
description naming two of the four nakshatras a category accepts — the Kotlin
behaviour, ported as-is), and copper for the new Android build.

**Total to App Store: about 6–10 weeks** of work, plus Apple's review time and
whatever the console steps take on the owner's side.

---

## Tech stack (to be confirmed against the Expo SDK current at phase 0)

| Need | Choice |
|---|---|
| Framework | Expo (latest stable SDK at start), TypeScript strict, expo-router |
| iOS tab bar | `expo-router` native tabs (system bar, Liquid Glass on iOS 26) |
| Glass surfaces | `expo-glass-effect`, iOS only, with a solid fallback |
| Android tab bar | custom floating capsule, matching today's design |
| Charts | `react-native-svg` (North/South Indian chart, transit wheel, dasha timeline) |
| Fonts | `expo-font` — Noto Sans Devanagari, Outfit (same files as today) |
| Local database | `expo-sqlite` with SQLCipher enabled; key in `expo-secure-store` |
| Settings | small key-value store (MMKV or expo-sqlite kv) |
| Location | `expo-location` (permission + reverse geocode for the city name) |
| Firebase | React Native Firebase: app, auth, firestore, analytics, crashlytics, app-check; Firebase AI Logic for Gemini |
| Google sign-in | `@react-native-google-signin/google-signin` |
| Apple sign-in | `expo-apple-authentication` (required on iOS when Google sign-in exists) |
| Ads | `react-native-google-mobile-ads` (+ UMP consent), `expo-tracking-transparency` on iOS |
| Purchases | decision in `SERVICES.md`: RevenueCat (server-side receipt checks) or `expo-iap` |
| Notifications | `expo-notifications`, local only, scheduled ahead |
| Widgets | iOS WidgetKit extension (Swift, compiled by EAS); Android `react-native-android-widget` |
| PDF / share | `expo-print`, `expo-sharing`, `react-native-view-shot` |
| In-app review | `expo-store-review` |
| Legal pages | bundled HTML in `react-native-webview`, JavaScript off |
| Tests | Jest (engine golden tests, logic), a render smoke test per screen |
| Builds | **EAS Build, both platforms, always** — the owner's rule of 20 Sep 2026. No local `expo run:android`. |
| Updates | EAS Update for JS-only fixes, used carefully |

---

## Definition of done (iOS launch)

- Every row in `FEATURE_PARITY.md` ticked on the owner's iPhone.
- Engine golden report: 0 differences.
- Both languages, both themes, iPhone SE (375pt) to Pro Max (440pt), text size
  up to the app's cap — checked.
- Sign-in (Google, Apple, email), backup/restore, delete account, PRO purchase
  and restore, ads with ATT, notifications, widget — each checked on the device.
- Privacy policy updated for iOS and published; App Store privacy labels match
  it; Data safety on Play unchanged unless the Android app changed.
- Owner has approved the listing text and screenshots.
