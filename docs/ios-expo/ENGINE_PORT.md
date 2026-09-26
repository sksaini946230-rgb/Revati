# Porting the calculation engine to TypeScript

The engine is the part of Revati where a wrong answer is invisible until a user
notices. An earlier Kotlin version put **92 of 365 days on the wrong Tithi and
71 on the wrong Nakshatra**, and it was caught only by diffing a year of output.
A rewrite is the easiest way to bring that back. This document is how that is
prevented.

## What moves

`app/src/main/java/com/example/astro/` — pure Kotlin, no Android imports,
5,075 lines:

| Kotlin file | Lines | Holds |
|---|---|---|
| `AstroMath.kt` | 287 | Meeus ch. 25 (Sun), ch. 47 + tables 47.A/B (Moon), ch. 22 (nutation, obliquity), Lahiri ayanamsa, mean obliquity for the ascendant |
| `MoonPosition.kt` | 146 | lunar longitude helpers |
| `AstroTime.kt` | 142 | **the only UT conversion** — every entry point takes UT |
| `RiseSetCalculator.kt` | 146 | sunrise/sunset/moonrise/moonset |
| `PanchangCalculator.kt` | 330 | the day's Panchang; the 30 built-in cities |
| `PanchangElements.kt` | 244 | tithi, nakshatra, yoga, karana tables; `isAdhikaMasa`; samvat rollover |
| `ChoghadiyaCalculator.kt` | 155 | day (step 1) and night (step 5) sequences |
| `MuhuratCalculator.kt` | 184 | upcoming muhurats for a given city |
| `KundaliCalculator.kt` | 231 | lagna, houses, planet placement |
| `VimshottariDashaCalculator.kt` | 255 | mahadasha and antardasha |
| `TransitCalculator.kt` | 85 | gochar |
| `KundaliMatchingCalculator.kt` | 559 | the eight koots, three Bhakoot doshas, Manglik, Nadi |
| `NumerologyCalculator.kt` | 177 | Moolank, Bhagyank, Chaldean name number, friendly/enemy |
| `NumerologyValidator.kt` | 100 | date rules that follow today |
| `FestivalCalculator.kt` | 272 | festival dates from tithi rules |
| `FestivalProvider.kt` | 253 | festival texts, regions |
| `RashifalProvider.kt` | 1,017 | readings, ratings (gochar phala), lucky values from the rashi lord |
| `AstroNames.kt` | 220 | names in both languages, chart glyph tokens |
| `BirthData.kt` | 157 | parsing and sanitising birth input |
| `DevanagariTransliterator.kt` | 115 | Latin → Devanagari for names |

Destination: `~/Revati-Expo/src/engine/`, one TypeScript module per Kotlin
file, same names, **no React or Expo imports** — the same rule that keeps the
Kotlin engine testable.

## The method

### Step 1 — golden answers from the Kotlin app (test-only change in `~/Revati`)

Add one JVM test, `GoldenFixtureExportTest`, that writes JSON to
`app/build/golden/` and asserts nothing. It is the only change this project
makes to the Kotlin repo, it ships in no APK, and it runs with
`./gradlew testDebugUnitTest --tests '*GoldenFixtureExportTest*'`.

It dumps, at minimum:

| Fixture | Coverage |
|---|---|
| `panchang.json` | every day of 2024–2031 × 6 cities spread across India (Jaipur, Guwahati, Thiruvananthapuram, Mumbai, Delhi, Kolkata) — every field of the Panchang row, times as minutes-since-midnight UT and as displayed strings in both languages and both clock formats |
| `planets.json` | longitudes of all nine grahas at 00:00 and 12:00 UT, every 5 days, 1950–2050 |
| `choghadiya.json` | day and night for 60 dates × 3 cities, all 7 weekdays covered |
| `kundali.json` | 500 birth charts: random but seeded dates 1940–2025, times, 30 cities — lagna, houses, planets, retrograde, dasha periods with antardasha |
| `matching.json` | every rashi × nakshatra pairing that matters: all 144 rashi pairs, 27 × 27 nakshatra pairs, plus 200 full couples with times and places |
| `numerology.json` | 300 names (Latin and Devanagari) × dates |
| `festivals.json` | every festival date 2024–2031, with Adhika years |
| `rashifal.json` | 12 signs × day/week/month × 400 dates, every field, both languages |
| `muhurat.json` | upcoming muhurats for 3 cities on 20 start dates |
| `transit.json` | transit table for 50 charts on 10 dates |
| `validator.json` | accepted/refused dates around today, leap years, the year boundary |

Fixture size is fine; they are generated, compressed and kept out of the app
bundle (`__fixtures__/`, test-only).

### Step 2 — port, file by file, in dependency order

`AstroTime` → `AstroMath` → `MoonPosition` → `RiseSetCalculator` →
`PanchangElements` → `PanchangCalculator` → `ChoghadiyaCalculator` →
`KundaliCalculator` → `VimshottariDashaCalculator` → `TransitCalculator` →
`KundaliMatchingCalculator` → `NumerologyValidator` → `NumerologyCalculator` →
`FestivalCalculator` → `FestivalProvider` → `MuhuratCalculator` →
`AstroNames` → `DevanagariTransliterator` → `BirthData` → `RashifalProvider`.

Each file is done when its fixtures pass. The next one does not start before.

### Step 3 — the comparison rules

- Longitudes: equal within **0.0001°** (the two runtimes both use IEEE doubles;
  anything larger is a porting mistake, not rounding).
- Times: equal to the **minute** as displayed, in both clock formats.
- Names, labels, readings, counts, scores, booleans: **byte-for-byte equal**.
- A report is printed per fixture: rows checked, rows different, the first 20
  differences. **The phase-2 gate is a report with 0 differences**, shown to
  the owner.

### Step 4 — port the existing tests too

The 231 Kotlin tests encode facts checked against the shastra and against
published panchangs. Port every one that lives under `astro/` — at least
`EphemerisAccuracyTest` (8 golden values to 0.02°), `ClassicalTablesTest`,
`ChoghadiyaSequenceTest` (Drik Panchang, Jaipur, 13–19 Sep 2026, day and
night), `ChoghadiyaSunriseTest`, `BhakootDoshaTest` (all 144 pairs),
`PanchangDayDivisionsTest`, `MuhuratTithiTest`, `FestivalCalculatorTest`,
`RashifalVariesByRashiTest`, `NakshatraProgressTest`, `TransitCalculatorTest`,
`NumerologyValidatorDateTest`, `CalculatorsTest`, `DataIntegrityAndAccuracyTest`,
`AstroVedaComprehensiveQATest`, `NoHardcodedYearTest`.

Golden fixtures prove "same as Kotlin"; these tests prove "Kotlin was right".
Both are needed.

## JavaScript traps to design out from the start

| Trap | Rule |
|---|---|
| `Date` is local time by default | The engine takes and returns **UT numbers** (Julian day / epoch ms). No `new Date(y, m, d)` inside `engine/`. A lint rule forbids it. |
| `%` keeps the sign of the dividend | One `mod(a, n)` helper, used everywhere an angle or index wraps. |
| Integer division | `Math.floor`, never `\|0` (breaks past 2³¹) and never `Math.trunc` for negatives. |
| Kotlin `Int` overflow semantics | Check any place that relied on it (hash-like expressions in `RashifalProvider`). |
| `toFixed` / locale formatting | Numbers are formatted by one function with fixed Latin digits; never `toLocaleString` (Devanagari-digit devices broke the festival notification once). |
| Hermes vs Node differences | Run the fixture suite both under Node (Jest) and once on-device in a debug screen that runs a sample and reports the checksum. |
| Floating-point order of operations | Keep the Kotlin expression order; do not "simplify" formulas while porting. |
| String sorting / comparison | Use explicit code-point comparison, never `localeCompare`, in anything that decides a result. |

## Performance

Sixty full Panchang computations once ran on the UI thread during start-up. In
the Expo app the upcoming-muhurat list and any multi-day calculation run
off the render path (deferred with `InteractionManager` or a worklet) and are
cached per city + date + language + clock format.

---

## Status: done, 20 September 2026

All twenty files are ported and all twenty are held to a golden fixture at
**zero differences**. `~/Revati-Expo/src/engine/README.md` has the per-file
counts; 128 tests, and `npm run preflight` passes all eleven checks.

Three things the plan above did not anticipate, each found by the fixtures
rather than by reading:

**India's clock is not a constant.** 400 birth charts matched except for
births in 1941–45, every one of them out by exactly one day. India ran on
**UTC+6:30 through the war**, and on Madras mean time (+5:21:10) before
1906. Kotlin's `Calendar` reads the tz database; a fixed +5:30 does not, and
a dasha period beginning near midnight in 1943 then prints the wrong date.
`istZone.ts` holds the transitions, measured to the minute and written out
rather than looked up — the engine may not import a platform API, and
`Intl` with a named zone is not something to rely on identically across Node
and Hermes.

**Java's calendar settings are inputs, not details.** `Calendar.getInstance()`
reads the device's time zone and the locale's first day of the week, and two
files depended on both: the mid-week Wednesday a weekly reading is fixed to,
and `WEEK_OF_YEAR`, which the horoscope rotates on. Every Indian locale
starts the week on Sunday, so the app has always been right by luck. Both
are written down in the port.

**Anything that reads the clock cannot be tested.** Five entry points took
"now" from inside — the dasha, the numerology validator, the muhurat scan,
the festival list and the horoscope. All five now take the instant as an
argument. Two of them had a bug that only appeared on certain days.

### The one kind of mistake numbers cannot catch

Four files are **generated** from the Kotlin fixtures by `npm run names`:
`astroNames.ts`, `cities.ts`, `festivalRules.ts` and `rashifalTemplates.ts`
— together some seven hundred Devanagari and English strings the screen
shows word for word. A retyped matra looks right in a diff and wrong on a
phone. One slip did get through, in a hand-written English string: a curly
apostrophe where Kotlin has a straight one. Only a character-for-character
comparison found it.

### What the Kotlin repo gained

One file, `app/src/test/java/com/example/astro/GoldenFixtureExportTest.kt`.
Test-only, asserts nothing, ships in no APK. Three of its exports pin the
platform before reading it — the transit export fixes the JVM to
Asia/Kolkata and a Sunday-start week, and the muhurat and rashifal exports
refuse to write a fixture that straddles midnight, because both read the
clock.

## The profile file, 25 September 2026

`ProfileTransfer.kt` is ported to `Revati-Expo/src/features/profiles/profileTransfer.ts`
and held to Kotlin the same way: a second export class in the same test file,
`ProfileTransferFixtureExportTest`, hands the Play app's reader 70 files —
damaged, hand-edited and odd, one oddity each, plus a file each app wrote —
and records what it kept, what it refused and the words it refused with.
**0 differences.** It runs under Robolectric, because it is Android's
org.json whose behaviour has to be matched, not json.org's. A deliberate
mutation (reading JSON `null` as empty) fails three cases, so the check
bites.

`sanitizeTextInput` moved to `src/lib/textInput.ts` with Kotlin's own
whitespace (U+001C–U+001F trimmed, U+FEFF kept), and the birth-data fixture
still passes on it.

What the Play app did with odd files was ported as-is and listed for the
owner, who decided on 25 Sep 2026 (D6, D7) to fix it — in both apps at once,
so the fixture stays the one truth:

- A JSON `null` field is now absent: a null `name` skips the profile, a null
  `uuid` mints a fresh one, null `notes` or place are empty. (It used to import
  a profile named "null", and every null-uuid profile shared the uuid "null".)
- Dates and times are stored as `YYYY-MM-DD` / `HH:MM` in ASCII digits,
  zero-padded, so `" 1994-08-25"`, `"+1994-8-5"` and `१९९४-०८-२५` all become a
  date the chart screen accepts. (Devanagari digits used to pass the import and
  then be refused at the chart.)
- Still as before: a uuid repeated inside one file is counted twice, and
  Room's REPLACE keeps the last copy.

**Later owner decisions, both apps (25 Sep 2026).** D3: Hindi strings carry
no English in brackets — choghadiya rulers, numerology planets, karanas, every
Guna Milan name, koot and verdict, festival names; the numerology form's
refusals are a Hindi/English pair. D4: a muhurat window ends at sunset, the
tithi's end or the nakshatra's end, whichever is first; a day with less than
one muhurta (a fifteenth of the day) left is not offered; the description names
the day's own nakshatra. Both regenerated every fixture, at 0 differences.

## Speed, 26 September 2026

Building the calendar found the festival list taking **52 seconds** in Node for
two years (it would be longer on a phone). Four changes, none of which moves an
answer — every golden suite still passes at 0 differences:

- **Moonrise only where it is read.** It was worked out for every day of the
  year (35 s of the 52) and only Karwa Chauth's rule reads it.
- **New moons found once per lunation.** `lastNewMoonJd` walked back and
  bisected on every call; masa and adhika checks asked about the same dozen new
  moons thousands of times. Lunation *n* is now searched once from three days
  past its mean date; the result differs from the old walk by about 10⁻¹⁴ day.
- **Days that cannot match are skipped early.** Sunrise at Ujjain is always
  between 05:38 and 07:10 IST and the tithi only moves forward, so the tithi at
  04:30 and 08:00 (cached per day) settles most days without a sunrise; a day in
  the wrong masa at both ends of its 24 hours is skipped before any sampling.
- **Pervasion counted by bisection.** The tithi (and its karanas) form unbroken
  runs through a window, so the minute count is found in a few dozen readings
  instead of two thousand.

Result: 52 s → 1.3 s. Muhurat's sixty-day scan likewise reads only the sunrise
limbs (125 s → 12 s for its golden suite). The Kotlin app is unchanged; these are
the same answers, reached faster.
