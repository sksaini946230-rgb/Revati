# Standards the Expo app is held to

The owner set these on 20 Sep 2026, when the rebuild was approved:

> 1. Structure bilkul clean chahiye jisse dekh ke koi developer gaali na de
> 2. Code ekdum clean chahiye — kuch duplicate nahi chahiye, koi extra dead code nahi hona chahiye
> 3. Jab bhi build karna ho usse pehle prebuild, Expo Doctor wagaira sab check kar lena, kuch bhi fail nahi hona chahiye
> 4. App hack-proof banni chahiye
> 5. Vibe coding nahi lagni chahiye app
> 6. Colour jo maine diye hai waise use hone chahiye
> 7. Har build se pehle upar waale saare kaam check karna hai

These are not aspirations. Each one below is a command that either passes or
refuses the build. `npm run preflight` runs all of them and **exits non-zero on
the first failure**; `eas build` is only ever reached through a script that runs
preflight first, so none of this depends on anyone remembering.

---

## 1. Structure — a stranger can find anything in a minute

```
src/
  app/            expo-router routes only. A route file wires things together;
                  it holds no business logic and no stylesheet of its own.
  components/
    common/       shared UI, one copy of each (AppText, Card, Sheet, Empty…)
    <feature>/    components belonging to one feature
  features/       one folder per feature: panchang, rashifal, kundali,
                  muhurat, profiles, settings. Screen logic + hooks live here.
  engine/         the calculation engine. Pure TypeScript, no React, no I/O,
                  no platform API. Testable on its own.
  services/       Firebase, ads, purchases, notifications, storage, AI
  i18n/           strings, both languages, one key per string
  theme/          tokens only (colour, spacing, radius, type)
  ui/platform/    the iOS vs Android split (see PLATFORM_DESIGN.md)
  lib/            small helpers. No `utils/` folder — that name collects junk.
```

Rules that are checked, not hoped for:

- **No file over 400 lines.** A screen that grows past it is split into
  sections in its feature folder. (`scripts/check-file-size.mjs`)
- **No logic in a route file.** A file under `src/app/` may not import
  `engine/` directly; it goes through its feature's hook.
  (eslint `no-restricted-imports`)
- **No circular imports.** (`eslint-plugin-import` `no-cycle`)
- **One folder, one job.** A feature folder may not import another feature's
  internals — only `components/common`, `lib`, `engine`, `services`, `theme`.
- **Every folder that is not obvious gets a `README.md` of five lines.**

## 2. Clean code — no duplicates, no dead code

| Check | Command | Refuses at |
|---|---|---|
| Unused variables, imports, parameters | `tsc --noEmit` with `noUnusedLocals` + `noUnusedParameters` | any |
| Unreachable / unused exports | `knip` | any |
| Duplicate code | `jscpd src --min-lines 10` | above **3%**, and the number may only ever go down |
| Stale `eslint-disable` that disables nothing | eslint `reportUnusedDisableDirectives` | any |
| Dead dependencies in `package.json` | `knip` | any |

A second copy of anything is folded into `components/common/` or `lib/` in the
same change that would have created it. "I will clean it later" is how the
Kotlin app ended up with three Panchang formatters.

## 3. Before every build — nothing may be failing

`npm run preflight`, in this order, stopping at the first failure:

1. `npx expo install --check` — every Expo package level (a package one patch
   behind marks the EAS build page red)
2. `npx expo-doctor` — must be all green
3. `npx expo prebuild --clean --no-install` into a temp dir — proves the native
   projects actually generate, before EAS is paid for the same discovery
4. `npx tsc --noEmit`
5. `npx eslint . --max-warnings 0`
6. `npx jest` — the whole suite, including the engine golden fixtures
7. `npm run check:duplicates` (jscpd)
8. `npx knip`
9. `npm audit --omit=dev` — a high or critical advisory in shipped code stops
   the build (build-time tooling advisories are listed and ignored, with the
   list written down)
10. secrets scan (`gitleaks`) over the working tree
11. the version/build number is above the highest ever submitted

Only after all eleven pass does the script call EAS — **and it still asks the
owner first.**

## 4. Hack-proof — what that means concretely

| Surface | What is done |
|---|---|
| Local database | SQLCipher, 32-byte random key in the Keychain / Android Keystore (`expo-secure-store`, "after first unlock, this device only"). Never in iCloud, never in a device backup. |
| Secrets | No API key, password or token in the repo, in `app.config.js` output, or in the JS bundle. Anything secret lives in EAS environment variables. A build with a missing secret **refuses to build** rather than shipping a disabled feature. |
| Firebase | App Check (App Attest on iOS, Play Integrity on Android) enforced on AI and on every callable. A side-loaded or repackaged build is refused by the server, not by the client. |
| Firestore | Rules deny by default; a user reaches only `users/{own uid}/…`. Rules are tested (`@firebase/rules-unit-testing`) in the same suite. |
| PRO | Receipts validated **server-side** (RevenueCat). A patched client cannot grant itself PRO. This is the one real hole in the Kotlin app today. |
| Network | HTTPS only, ATS with no exceptions, no cleartext on Android. |
| Input | Every free-text field sanitised (the `SecurityUtils` port); nothing user-typed is ever concatenated into a query or into HTML. |
| WebView | Bundled legal pages only, JavaScript off, no remote URL loading. |
| Deep links | Every scheme URL has a route; no link may carry a token or a session. |
| Logging | No personal data, no birth details, no tokens in logs in a release build; `console.*` stripped from production bundles. |
| Dependencies | `npm audit` in preflight; a new dependency needs a reason written in the commit. |
| Code | R8/minify on for Android release; `expo-updates` signed. |

A security review runs at phase 5 against this table, and the table is the
checklist — not a fresh opinion each time.

## 5. It must not look vibe-coded

Concretely, what "vibe-coded" looks like and what is done instead:

| Tell | What is required |
|---|---|
| Magic numbers in styles | every spacing, radius, font size and colour comes from `theme/` — a raw number or hex in a component is a lint **error**, as in the owner's other app |
| Inconsistent spacing between screens | one spacing scale, and a screen layout component both platforms use |
| Copy that changes meaning between screens | one `i18n` key per string; the same idea is never worded twice |
| A control that changes nothing | every input is followed to the value it changes; a test covers it |
| `any`, `@ts-ignore`, `catch {}` | `any` is banned by eslint; an empty catch is banned; every error either shows the user something or is reported |
| Comments that restate the code | comments explain *why*, and only where the reason is not obvious |
| Screens that work only with data present | every screen has a loading, empty and error state, and `everyScreenMounts` renders each one with every read failing |
| Placeholder text, lorem, TODO in shipped code | preflight greps for them and fails |
| No tests on the thing that matters | the engine is fixture-tested; every screen mounts in a test; every service is mocked at its boundary |

## 6. Colours

`BRAND_COLORS.md` is the only place hex codes are written down. Everything else
reads a token. A literal hex anywhere in `src/` outside `theme/` fails lint.

## 7. Every build

Rules 1–6 are exactly what `npm run preflight` checks, and the build script
calls it. There is no path to a build that skips them.
