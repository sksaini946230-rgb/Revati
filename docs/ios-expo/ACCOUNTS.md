# Accounts and identifiers — the real ones

Nothing secret is written here: no password, no key file, no API secret. Those
live in `~/secure-credentials/revati/` and in EAS environment variables.
Confirmed by the owner on 20 Sep 2026 from the consoles themselves.

## Expo

| | Value |
|---|---|
| Account (username) | `astroveda1` (email `sksaini012026@gmail.com`) |
| Owner / team | `astroveda1s-team-01` |
| Display name | `Revati` (renamed 20 Sep 2026) |
| Slug | `astroveda01` — **deliberately left alone** |
| Project ID | `1614c31b-65d5-447c-bacd-6788d4802842` |
| Builds so far | none |

This is a **separate Expo account**, created for Revati only, so the monthly
build quota is not shared with the owner's other app. Both `owner` and
`projectId` go in `app.config.js`; the slug appears in EAS Update URLs and is
awkward to change once builds exist.

The project was created as `ASTROVEDA01`. The display name is now `Revati`;
**the slug stays `astroveda01`** and that is a decision, not an oversight.
Expo offers no way to rename a slug — only deleting the project and making a
new one — and the owner decided on 20 Sep 2026 that it is not worth it. The
slug is never shown to a user: it appears in expo.dev URLs and as `slug` in
`app.config.js`, and updates are addressed by project id, not by slug. So
`app.config.js` must say `slug: 'astroveda01'` while `name` says Revati, and
nobody should "fix" that later.

## Apple

| | Value |
|---|---|
| Apple Account | `Sunilbadzshya@gmail.com` |
| Team name | `Sunil Kumar Saini` (**Individual**) |
| Team ID | `788G662STK` |
| Role | Account Holder, Admin |
| App Store Connect API key | name `Revati EAS Submit`, **Key ID `5M6CRWLM5B`**, role App Manager |
| Issuer ID | `abf00db0-c3b8-4d4c-a10a-448af12971bd` (one per team) |
| Key file | `~/secure-credentials/revati/AuthKey_5M6CRWLM5B.p8`, mode 600, never in git |
| Membership | active, renews 15 Sep 2027 |
| Preferred currency | USD — check this before the Paid Apps agreement; payouts for an Indian seller are normally INR |

**The same Apple team already holds the owner's other app** (`Tezzo: Carpool &
Ride Share`, iOS 2.0.1 waiting for review). One team can hold many apps and
that is fine. **Nothing about that app is ever opened, edited or submitted from
this project** — not its record, not its builds, not its credentials, not its
API keys. When a step below says "App Store Connect", it means the Revati app
record only.

Because the account is **Individual**, the App Store will show the seller as
the owner's personal legal name, not a business name. Showing a business name
needs an Organization account with a D-U-N-S number — a separate decision.

## Test device

| | Value |
|---|---|
| Phone | iPhone 16 |
| iOS version | **26** (iPhone reports iOS 27 era build) — Liquid Glass available |
| TestFlight | already installed |

The phone is current, so the Liquid Glass tab bar is reachable on the owner's
own device. The app must still look right without it — on an older iPhone, and
whenever a user turns on Reduce Transparency, every glass surface falls back to
a solid one. That fallback is tested, not assumed.

## Done on 20 Sep 2026

| | |
|---|---|
| Apple identifier | `app.revati.jyotish`, name "Revati Kundli Panchang", Sign in with Apple enabled |
| App Store Connect record | **Revati : Kundli & Panchang**, iOS 1.0, "Prepare for Submission" |
| App Store Connect API key | `Revati EAS Submit` / `5M6CRWLM5B` |

The Apple team now lists two identifiers and two apps — the other pair belongs
to the owner's other product and is never touched from here.

## Bundle ID — decided

**`app.revati.jyotish`**, chosen by the owner on 20 Sep 2026. Permanent.
Everything hangs off it: the App Store record, both Firebase iOS apps (the
debug one adds `.debug`), the AdMob iOS app, the subscription, Sign in with
Apple, the App Group for the widget. It is **not** the Android id
(`com.aistudio.astroveda.kpvqzm`, which stays as it is) — the two stores hold
separate identifiers and there is no requirement that they match.

## Still needed

| | |
|---|---|
| Firebase iOS apps | production `app.revati.jyotish`, debug `app.revati.jyotish.debug` |
| AdMob iOS app + 4 units | banner, interstitial, app open, rewarded; banner refresh 45 s |
| App Group (widget) | `group.app.revati.jyotish` — only when widgets are built |

## A second API key exists, and it is not ours

App Store Connect lists two active team keys: `EAS` (`M45742P682`) belongs to
the owner's other app, and `Revati EAS Submit` (`5M6CRWLM5B`) is this one. Keys
are per team, not per app, so both are visible from either project. **Never
use, edit or revoke the other one.**

## The code

`~/Revati-Expo`, GitHub `sksaini946230-rgb/Revati-Expo` (private), pushed over
the `github-revati` SSH alias — the same account as the Kotlin repo.

Created 20 Sep 2026 on Expo SDK 57.0.24. Expo Doctor 21/21; `npm run preflight`
passes all eleven checks. No EAS build has been made.
