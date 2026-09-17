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
