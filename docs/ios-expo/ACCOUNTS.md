# Accounts and identifiers — the real ones

Nothing secret is written here: no password, no key file, no API secret. Those
live in `~/secure-credentials/revati/` and in EAS environment variables.
Confirmed by the owner on 20 Sep 2026 from the consoles themselves.

## Expo

| | Value |
|---|---|
| Account (username) | `astroveda1` |
| Owner / team | `astroveda1s-team-01` |
| Project name | `ASTROVEDA01` |
| Slug | `astroveda01` |
| Project ID | `1614c31b-65d5-447c-bacd-6788d4802842` |
| Builds so far | none |

This is a **separate Expo account**, created for Revati only, so the monthly
build quota is not shared with the owner's other app. Both `owner` and
`projectId` go in `app.config.js`; the slug appears in EAS Update URLs and is
awkward to change once builds exist.

> **Open question:** the slug says `astroveda01` while the product is called
> Revati. Changing it is free today and annoying later. Asked of the owner.
> (The `AstroVeda` name is banned for a **GitHub repo** by `CLAUDE.md`; this is
> Expo, so it is a tidiness question, not that rule.)

## Apple

| | Value |
|---|---|
| Apple Account | `Sunilbadzshya@gmail.com` |
| Team name | `Sunil Kumar Saini` (**Individual**) |
| Team ID | `788G662STK` |
| Role | Account Holder, Admin |
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
| iOS version | *to confirm — Liquid Glass needs iOS 26* |
| TestFlight | already installed |

An iPhone 16 can run iOS 26, so the Liquid Glass tab bar is reachable. If the
phone is on an older iOS, the app must still look right without it — the glass
components fall back to solid surfaces, exactly as they do when a user turns on
Reduce Transparency.

## Still needed

| | |
|---|---|
| Bundle ID | proposal `app.revati.jyotish` — **permanent once used**, owner's confirmation pending |
| App Store Connect app record | after the bundle ID |
| App Store Connect API key (`.p8`) | for `eas submit` |
| Firebase iOS apps | after the bundle ID |
| AdMob iOS app + 4 units | after the bundle ID |
