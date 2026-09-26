# Revati — Kundli & Panchang

An Android Vedic astrology app: daily Panchang, Rashifal, birth charts, Guna
Milan, Muhurat, numerology. Hindi and English throughout. Live on Google Play.

**Read this whole file before changing anything.** Several of the notes below
record bugs that shipped once already.

---

## Identity — read carefully, these disagree on purpose

| | Value |
|---|---|
| App name (users see) | **Revati** |
| Play listing | Revati : Kundli & Panchang |
| `applicationId` | `com.aistudio.astroveda.kpvqzm` |
| `namespace` (R / BuildConfig) | `app.revati.jyotish` |
| Kotlin source package | `com.example.*` |
| Firebase project | `astroveda-7126b` |
| Play developer | Msunjay Enterprises |
| Local checkout | `~/Revati` — renamed from `~/AstroVeda` on 8 Sep 2026 |
| GitHub repo | `sksaini946230-rgb/Revati` — renamed from `AstroVeda`; the old URL redirects |

The app was called **AstroVeda** until Aug 2026. That name had to go: five apps
on Play already use it, two with six-figure installs. The rename reached the UI
and the namespace, but **not** the `applicationId` — the first production
release went out under the old one, and an applicationId is permanent once
published.

So: `namespace ≠ source package`. Two consequences that have already bitten:

- `R` and `BuildConfig` live at **`app.revati.jyotish`**, not `com.example`.
  Import them explicitly.
- Manifest components must be **fully qualified** (`com.example.MainActivity`),
  never `.MainActivity` — a leading dot resolves against the namespace, which is
  no longer where the classes are.

The checkout and the GitHub repo both say **Revati** now. The folder was
renamed from `~/AstroVeda` on 8 Sep 2026 and the repo from
`sksaini946230-rgb/AstroVeda` shortly after, by the owner from GitHub's settings
page. Nothing in the build refers to either name: paths are relative, and the two
absolute ones that existed — `goldie.config.ts` and `.env`'s `KEYSTORE_PATH` —
were rewritten with the folder.

**Never create a repository called `AstroVeda` on this account.** The old URL
still works only because GitHub redirects it, and it has to keep working: every
copy of the privacy policy handed out before the rename points at
`github.com/sksaini946230-rgb/AstroVeda`. That redirect lasts until the name is
taken again, and a new repo under it breaks every old link at once. The ads site,
`sksaini946230-rgb.github.io`, is a separate repository and was not affected;
this repo has no Pages site of its own, which matters because Pages URLs are the
one kind a rename does not redirect.

Do not "tidy" any of this into agreement. Each half is pinned by something
external.

---

## Layout

```
app/src/main/java/com/example/
  astro/      the ephemeris and all Vedic calculation — no Android imports
  data/       Room entities/DAOs, models, Firebase AI service
  service/    billing, auth, ad consent, PDF report
  ui/         Compose screens, components, theme, MainViewModel
  util/       LanguageManager, analytics, feature flags, share, image export
  widget/     home-screen widgets
  worker/     WorkManager notification jobs
```

`astro/` is pure Kotlin and unit-tested. Keep it that way — it is the one part
of this app where a wrong answer is invisible until a user notices.

---

## iPhone — the Expo rebuild (approved 20 Sep 2026)

On 17 Sep 2026 the owner decided Revati goes to the App Store as an **Expo /
React Native rebuild**, built in the cloud with EAS (no Mac, no Xcode), in a new
folder `~/Revati-Expo`. The whole plan is `docs/ios-expo/` — start at its
`README.md`; what the owner must do is `OWNER_CHECKLIST.md`. Nothing is built
until the owner approves the plan and then the design.

What that means for this repo: **this Kotlin app stays the Play app** and keeps
shipping. The only planned change here is one test-only file that exports golden
answers for the TypeScript engine (`docs/ios-expo/ENGINE_PORT.md`). iOS and
Android looks are kept apart on purpose (`PLATFORM_DESIGN.md`), and nothing is
taken from the owner's other project except the Expo + EAS approach.

**Android moves to Expo too** (owner, 20 Sep 2026) — but last, after the Expo
build does everything this one does. Until then **this Kotlin app is the Play
app and keeps shipping**. `docs/ios-expo/ANDROID_HANDOVER.md` has the three
irreversible steps: the Play upload key into EAS, the package name, and the
users' SQLCipher data.

**The owner approved the plan on 20 Sep 2026 and set seven standards**
(`docs/ios-expo/CODE_STANDARDS.md`): clean structure, no duplicate or dead
code, every check green before a build, hack-proof, nothing that reads as
vibe-coded, and the colours they gave — green `#2E8B4E`, copper-orange
`#E8934A` **in place of gold**, cream `#F0D09A`, background `#0B0E1A`
(`docs/ios-expo/BRAND_COLORS.md`). Those are build gates in the Expo repo, not
intentions. **This Kotlin app keeps its gold `#D4A84B`** until the owner asks
otherwise — do not repaint it as a tidy-up.

---

## Things that will bite you

**The ephemeris is real and was wrong once.** `AstroMath.kt` implements Meeus
*Astronomical Algorithms* 2nd ed. — ch. 25 (solar), ch. 47 + tables 47.A/47.B
(lunar), ch. 22 (nutation/obliquity), with Lahiri ayanamsa. An earlier version
was off by enough to put **92 of 365 days on the wrong Tithi and 71 on the wrong
Nakshatra**. It was caught by dumping a year of output and diffing against an
independent implementation, not by reading the code. `EphemerisAccuracyTest`
pins eight golden values to 0.02°. If you touch `astro/`, run it, and if you
change anything about how longitudes are produced, re-do the year-long diff.

**The ascendant uses mean obliquity, and that is a decision.** It used to read
`meanObliquity(t) + nutationInLongitude(t) * 0.0` under a comment saying
apparent obliquity was being used: nutation in *longitude* is the wrong quantity
for an obliquity, and it was multiplied by zero besides, so nothing was being
added. Nothing should be — nutation in obliquity peaks near 9 arcseconds, which
moves the Lagna by well under an arcminute, far inside what a whole-sign chart
can show. The dead term is gone and the comment says what is true.

**Every ephemeris entry point takes Universal Time, not IST.** `AstroTime.kt`
does the conversion, once. Passing local time silently moves the Moon ~3°.

**Room will not wipe user data any more — keep it that way.** The database was
on `fallbackToDestructiveMigration(dropAllTables = true)`, so every schema
change silently dropped saved profiles, reports and searches. It now takes real
migrations, registered in `MIGRATIONS` in `AppDatabase.kt`, and fails loudly in
development instead. Schemas are exported to `app/schemas/`. Bumping the version
without writing a migration is now a crash, which is the intended pressure.
`MigrationTest` runs every migration against a real database of the previous
version — write one alongside the migration, not after.

**A saved profile's identity is its `uuid`, never its Room `id`.** `id` is
`autoGenerate`, so it restarts at 1 on every device. The cloud backup used to key
its Firestore document on it, and the sync used to treat a matching `id` as "we
already have this" — so two phones on one account each skipped restoring the
other's first profile and then overwrote it on the next upload. One user's saved
birth chart, gone, unrecoverably. The merge also fell back to matching on name +
date of birth, which silently dropped one of a pair of twins.

Firestore documents are keyed on `uuid`, and `ProfileMerge.profilesToRestore`
matches on `uuid` alone. `ProfileMergeTest` covers both failure modes. Do not
reintroduce an id comparison or a name-based one.

**Both languages, always.** Nothing user-facing may be a bare string.
`LanguageManager.getString(hi, en)` or a `*Local` extension
(`PanchangLocalized.kt`, `FestivalLocalized.kt`). A sweep in Aug 2026 fixed 170
hardcoded Hindi strings that English users were seeing — notifications, the
widget, share text, the PDF report, chart glyphs, error messages. Do not
reintroduce them. Chart planet glyphs in particular are stored as
language-neutral tokens (`"Sun"`, `"Mars|R"`) and localised at draw time by
`AstroNames.houseGlyph()`; baking the Hindi in froze charts into whatever
language drew them.

That sweep missed two whole categories, both found in the Sep 2026 audit and both
fixed:

- **`LanguageManager.init` has to run for every process entry, not just
  MainActivity.** It was called only from `MainActivity.onCreate`, but the process
  also starts from two `AppWidgetProvider`s and three WorkManager workers, which
  never touch an Activity. In those processes `currentLanguage` sat at its default
  of Hindi, so an English user's 6:30 AM notification and both widgets came out in
  Hindi after every reboot. **`RevatiApp`** (`android:name` on `<application>`)
  does it now, along with App Check. `LanguageInitTest` fails if that manifest
  registration is ever removed.
- **XML layouts count as user-facing.** Five label `TextView`s in
  `panchang_widget.xml` and two in `tithi_nakshatra_widget.xml` had no `android:id`
  and were never set at runtime, so they sat at their layout defaults —
  `"SUNRISE / सूर्योदय"`, `"TITHI / तिथि"` — showing both languages at once
  forever. They have ids and are set from `LanguageManager` now. `lintDebug`
  reports these as `HardcodedText`; a widget label with a hardcoded string is a
  real bug, not noise.

**A value derived from both the rashi and the transit house may not vary at
all.** `RashifalProvider` set the rating, the lucky colour and the lucky stone
from `(rashiIdx + house) % n`. But

    house = ((planetRashiIdx - rashiIdx + 12) % 12) + 1

so `rashiIdx` cancels out of that sum and what is left is the transiting
planet's own sign — the same for all twelve readers. Every rashi was shown five
stars out of five on the daily view, four on the weekly, and the lucky colour
took two distinct values across the whole zodiac. It survived because the
generated *text* was correctly per-rashi, so the screen looked varied, and
because any single rashi looks perfectly right on its own — only comparing all
twelve exposes it. The rating now comes from classical gochar phala for the
driver planet, and colour and stone from the rashi lord.
`RashifalVariesByRashiTest` compares all twelve. Any new per-rashi quantity
wants the same treatment: check it across the zodiac, not on one sign.

**Narrow phones are 320dp, and Hindi is longer than English.** Checked with
`ScreenSizeScreenshotTest`, which renders the riskiest screens at 320dp and
360dp (plus dark and 1.3x text) into `app/src/test/screenshots/`. It asserts
nothing on purpose — an automated overflow walker was built first, over the
semantics tree, and it could not be trusted: `TextLayoutResult` reached that way
returns whichever layout pass ran last, and anything measured speculatively
(Rows with weights, `IntrinsicSize.Min`) leaves a result describing a width
nobody ever saw. It reported the sub-tab headers and the PRO upgrade banner as
broken when rendering proved both fit. Look at the pictures instead. Three real
bugs it did find, now fixed: the dashboard shortcut tiles clipped every label at
320dp (the grid drops to one column below 300dp of *content* width — the
threshold is on the width the grid is handed, not the screen), the kundali
date/time fields clipped their labels (shortened to "तिथि"/"समय"), and the PRO
badge in Settings was laid out zero pixels wide and simply never drawn, because
the Row beside it had no `weight(1f)`.

**"Everyone sees the same data" has had four separate causes.** They are worth
listing together because each looked different and none was visible from one
user's screen:

1. `RashifalProvider` derived values from `(rashiIdx + house)`, in which rashiIdx
   cancels — see the note above.
2. The horoscope **cache** kept serving those values for seven days after the
   fix. `MIGRATION_6_7` clears it.
3. The Rashifal **AI insight** was one shared `String` for all twelve signs, so
   fetching for one sign left its text under every other sign's heading. It is
   keyed by sign now.
4. When the model cannot be reached, `getOfflineVedicResponse` picks text by
   keyword — and the insight question carries none of them, so all twelve signs
   got one identical paragraph under a heading promising a personalised reading.
   That is treated as a failure now, with a retry, rather than shown as an
   insight. `AiInsightFallbackTest` pins the premise.

Also in that family: **Numerology shipped pre-filled with the developer's own
name and date of birth**, already calculated, so every first-time visitor was
shown a stranger's Moolank and reading. The fields start empty and the results
stay hidden until the user asks.

The general lesson: a per-user or per-item derived value cannot be judged from
one instance. Dump the whole set and count the distinct values.

**AI calls are rate limited, because nothing was limiting them.** `AiRateLimiter`
— a 3 second minimum gap and a rolling 20 per hour — sits in front of both
`askAiAstrologer` and `fetchPersonalizedInsight`. There is no server between this
app and the bill, so an unbounded question box is a direct cost and a shared
quota one user can exhaust for everyone.

**Analytics were almost entirely unwired.** `AstroAnalytics` had eighteen
logging methods; `init` and `logAppOpen` were the only two ever called, so the
live app recorded app opens and nothing else. Sixteen of the seventeen that
remain are wired — screen views, onboarding and its three steps, horoscope
views, kundali, matching, numerology, AI queries, login on all four paths, the
three purchase outcomes, and sharing.

`logOnboardingStep` fires from `OnboardingScreen` for each page reached. Skip
and "Enter Revati" both call `onComplete()`, so `onboarding_complete` alone
cannot tell a user who read all three pages from one who left on the first; the
step can. Its names live in `ONBOARDING_STEPS` and are **language-neutral
tokens**, for the same reason the chart glyphs are: a step logged as "भाषा" in
Hindi and "language" in English splits one funnel into two halves that never add
up. `totalPages` is that list's size, and `OnboardingStepsTest` pins both the
list and the call — a method nobody calls being exactly what was wrong here.

`logFirstOpen` is **deleted**. Firebase logs `first_open` automatically, once
per install; a hand-rolled second one keyed on SharedPreferences fires again
after "clear data" and not at all if the write is lost, so the two would
disagree and neither would be the one to trust.

`logPanchangView` is the one still unwired, and it is not a free wiring job. The
city it sends is usually the one derived from the phone's GPS, which would make
it the only place the app itself sends a user's location off the device —
against what the privacy policy says. Its doc comment says what has to change
with it.

`recordNonFatal` in particular now sits on every data path that can lose or fail
to save a profile: background backup, cloud backup, sync, local wipe, export,
import, account deletion. They were calling `Log.e` and stopping, which means
nobody ever learned it happened — the test device records nothing below `E`, and
no one reads a stranger's logcat.

`AstroFeatureFlags` was the same dead pattern and is **deleted**. Its own doc
called it a "Remote Kill-Switch & Soft Launch Feature Flag Engine"; it had zero
call sites, no remote source at all (every flag a hardcoded default), and a
support address, `support@astroveda.app`, that is not the one the app or the Play
listing uses. A real kill switch needs a real remote source and is its own piece
of work.

Not every silent `catch` is a bug, and the count is misleading. The ones in
`AstroAnalytics` are correct — analytics must never crash the app, and an
analytics failure cannot be reported through analytics. The ones in `onCleared`,
in ads/consent init, and around an external browser that may not exist are
correct too. The user-facing data paths already reported failures in both
languages before this pass; what they lacked was telling *the developer*.

**R8 was keeping three quarters of the app.** `proguard-rules.pro` carried
blanket rules — `-keep class com.google.firebase.** { *; }` and
`-keep class com.google.android.gms.** { *; }` chief among them — which held the
two largest dependency trees completely immune to the optimiser. Play measured
it and said so on the release dashboard for versionCode 7: **optimization 27%,
obfuscation 28%, shrinking 28%**. Firebase and Play Services ship their own
consumer ProGuard rules inside their AARs and R8 applies those automatically; a
blanket keep on top only defeats it. Removing them took the APK from 10.4 MB to
8.8 MB and obfuscated all 15,482 classes instead of a quarter of them.

What must stay is this app's own classes that are reached by reflection —
`com.example.data.model.**`, `com.example.data.local.**`, `com.example.worker.**`,
the Room `@Entity` keep and the Firestore `PropertyName` members — plus the
Firebase AI serializer rules. Those are kept by name.

`com.android.billingclient.api.**` also keeps its blanket rule, deliberately and
alone: PRO is not purchasable until the merchant account exists, so a purchase
cannot be walked through a minified build to see it work. The library is small,
and a purchase that silently fails in production is not worth the bytes.
Revisit when a real purchase can be tested.

Verified on a device against the minified build: no crash on any screen, Google
Sign-In's account picker opens (that is `androidx.credentials` and
`com.google.android.libraries.identity`, both of which lost their blanket keeps),
and real banner and interstitial ads serve. The Firebase AI call was not
re-exercised live — the device was stuck in landscape, where the content area is
too short to reach the question box — but its serializer keeps were left intact
and 710 Firebase AI classes with 124 `serializer()` methods survive in the
mapping.

**Landscape is cramped and nobody has designed it.** On a 720x1600 phone held
sideways the sub-tab header and bottom nav leave roughly 250dp of content, the
banner lands mid-screen, and some screens cannot be scrolled to their end. It is
usable but not good, and it has never been a design pass of its own.

**The Dependabot count is about the build, not the app.** GitHub reports 52
vulnerabilities on the default branch and the number is alarming until you look
at where they are. Checking every dependency by name against OSV — re-run on
12 Sep 2026, and the shape has not moved:

    277 libraries that ship inside the APK   ->  0 vulnerable
    141 build-time libraries (Gradle plugins) ->  6 vulnerable, 7 advisories

The six are bcprov-jdk18on, bcpkix-jdk18on, commons-lang3, jose4j, jdom2 and the
Kotlin Gradle plugin — all of them pulled in by Gradle plugins, all of them
running on the build machine and on the CI runner, none of them present in the
APK. The threat they describe is someone compromising a build, not a user's
phone.

The count does not reconcile: this scan finds 7 advisories where GitHub counts
52. The likely reason is that Automatic dependency submission reports a graph
per build variant, so one advisory is counted several times — but that is a
guess and has not been confirmed against GitHub's own list. What *is* confirmed
is the half that matters: nothing that reaches a user is vulnerable.

Keeping AGP, Kotlin and KSP current pulls newer versions of all six in time,
and there is nothing to pull yet: AGP is 9.1.1 and Kotlin 2.2.10, and the one
advisory that names a direct dependency — GHSA-r937-wjx7-w2jp, unsafe
deserialization in the Kotlin build cache — is first fixed in `2.4.20-Beta1`.
Moving the whole build onto a Kotlin beta to close a moderate local-attack
advisory on the build machine is the worse trade. Forcing new bouncycastle,
jose4j or jdom2 versions into AGP's own classpath is available and was also left
alone: it would put untested crypto and XML libraries underneath signing and
bundletool to quiet a warning about a machine, not a user.
Reproduce with `./gradlew :app:dependencies --configuration releaseRuntimeClasspath`
and `./gradlew buildEnvironment`, then post the resolved coordinates to
`https://api.osv.dev/v1/querybatch`.

**The Rashifal cache answered a wider question than it was asked.**
`getAllValidHoroscopes` matched on the period and a seven-day window, and the
rows are written with the day, week or month they were computed for baked into
the key — which nothing read back. So yesterday's twelve "TODAY" rows were still
inside the window this morning and came back as today's: **the daily Rashifal
actually changed once a week.** Nothing looked wrong, because a stale horoscope
and a fresh one are the same shape.

The second half is worse. A forced refresh — which is what a language switch
does — writes a fresh set without displacing the old one, so the table then held
twenty-four valid rows and the next ordinary load returned all of them. The
Rashifal list is `items(horoscopes, key = { it.rashiId })`, and a duplicate key
in a lazy list throws. The read now asks for the twelve exact keys of the
current period, built by the same function the write uses.
`AstroCacheRepositoryTest` was confirmed to fail against the old query, once
with `expected:<12> but was:<24>`.

**The Panchang cache key had the clock format in it and not the language.** Some
of what the row holds is already localised text — the Tithi and Nakshatra end
times read "09:57 AM तक" or "until 09:57 AM", and the Sun and Moon signs are
stored in one language only. A language switch does force *today* to be
recomputed, and the comment on `onLanguageChanged` says exactly why; what it
could not do is reach every other date the user had already looked at, which
came straight back out of the cache in the old language on the next tap of the
date picker. The language is part of the key now, for the same reason
`use24Hour` already was.

**Adhika Jyeshtha 2026 and the Nija Jyeshtha behind it were both just
"ज्येष्ठ".** A lunar month is Adhika precisely because the Sun changes no sign
during it, so the intercalary month and the real one begin with the Sun in the
same sign and `masaIndex` gives them the same name — there is no way to tell
them apart from the index alone. The Panchang screen said ज्येष्ठ for the
fifty-nine days from 17 May to 14 July 2026 and never said which one.
`FestivalCalculator` already knew the difference, because festivals are not kept
in an Adhika month; the test it used is `PanchangElements.isAdhikaMasa` now and
the Panchang prefixes the name with अधिक / Adhika. The next one is Chaitra 2029.

**Every Muhurat was Jaipur's.** `getUpcomingMuhurats()` took no place and
declared `val defaultCity = CityLocation("Jaipur", …)` inside itself — the same
mistake Guna Milan's Manglik reading made with the literal string "Default".
Each window is that city's sunrise to its sunset, and Guwahati's sunrise is the
better part of an hour before Jaipur's, so a reader in Assam was told to begin
at a time that had already gone and one in Kerala at one that had not come. The
tithi and nakshatra are read at sunrise too, so near a boundary the *day* could
be wrong. It takes the selected city now, and the 12/24-hour setting with it.

Two more things were wrong about the same line. `upcomingMuhurats` was a plain
`val` on `MainViewModel`, so **sixty full Panchang computations ran
synchronously while the ViewModel was being constructed** — every launch, for a
screen most sessions never open. And being a `val`, the list never changed
afterwards, so switching city or clock format left it as it was. It is a
`StateFlow` filled from `Dispatchers.IO`, refreshed when the Muhurat screen is
opened and whenever the city changes.

**A muhurta is a fifteenth of the day, not forty-eight minutes.** Abhijit was
`midday ± 24` and Brahma `sunrise - 96 … sunrise - 48`, which is the equinox
case and only the equinox case. In Jaipur the daylight runs from about 10h20m to
13h40m, so a day muhurta is 41 to 55 minutes and Abhijit was up to three minutes
out at each end — on the one window in the day people read to decide when to
begin something. Both are computed from the real day and night lengths now.
`PanchangDayDivisionsTest` also pins the Rahu Kaal, Gulika and Yamaganda weekday
tables against the eighth of the daylight each belongs to, over a full week, and
checks that no two of the three ever claim the same eighth.

**A hardcoded year is a defect with a date on it.** `NumerologyValidator`
capped the birth year at the literal **2026**: on 1 January 2027 a baby born
that morning could not have been entered, and the message would have told the
parent the year was out of range. It follows today now, and a date that has not
happened yet is refused whatever year it falls in. Four more literal 2026s were
user-visible text — the calendar subtitle ("2026 Hindu Panchang calendar", which
was also wrong for any other year the user scrolled to), the tagline in the top
bar and on the onboarding page, and the About dialog's title. The taglines
simply do not carry a year any more; the calendar shows the year it is actually
displaying. The onboarding one was Hindi-only besides, on a page that is
otherwise deliberately bilingual because the user has not chosen yet.

**Guna Milan reads the Moon back out of the chart by its Hindi name.**
`KundaliMatchingCalculator` does `RASHI_SHORT_HI.indexOf(chart.moonRashiHi)` and
`NAKSHATRAS.indexOf(chart.moonNakshatraHi)`, each `.coerceAtLeast(0)`. Both
tables are the ones the chart was drawn from, so the round trip holds today —
but a miss does not fail, it returns Mesha and Ashwini, and every match in the
app would then be computed for the wrong couple with nothing on screen to say
so. `ClassicalTablesTest` pins the round trip for all twelve rashis and all
twenty-seven nakshatras. If you ever localise those fields, fix this first.

**The classical tables are checked against the shastra, not against
themselves.** `ClassicalTablesTest` restates Varna by element, the Vashya
grouping, Tara's third/fifth/seventh, all twenty-seven Yoni animals and the
seven sworn-enemy pairs, the Gana of every nakshatra and the asymmetric 6/5/1
scoring table, all three Bhakoot doshas, every nakshatra's Nadi, the twelve
rashi lords through Graha Maitri, the Vimshottari order summing to 120, each
nakshatra's dasha lord, and the sixty-slot Karana cycle. A September 2026 audit
found every one of them already correct — Varna, Vashya, Tara, Yoni, Graha
Maitri (lords, the natural friendships, and the 5/4/3/1/0.5/0 scoring), Gana,
Bhakoot, Nadi, Vimshottari, the Chaldean letter values and their planets, the
Rahu/Gulika/Yamaganda weekday slots, the twenty-seven Yogas in order, the
Karana sequence, Amavasya versus Purnima, the Amanta month naming and the
Vikram/Saka rollover. They are pinned now so that stays a fact rather than a
memory.

**Sunday's Choghadiya was wrong, and a test that counted slots let it
through.** Each Choghadiya belongs to a graha — Udveg/Sun, Char/Venus,
Labh/Mercury, Amrit/Moon, Kaal/Saturn, Shubh/Jupiter, Rog/Mars — and the daytime
sequence for a weekday is that Chaldean cycle rotated to begin with the
weekday lord's own Choghadiya. Six of the seven rows in `ChoghadiyaCalculator`
obeyed that. Sunday stepped through the cycle three at a time, so on a Sunday
07:34-09:09 was labelled **Amrit**, the most auspicious slot there is, when it
is Char and merely neutral; and 09:09-10:43 was labelled **Rog** when it is
**Labh**. That is the screen people read to choose when to begin something.

The only test touching Choghadiya asserted that eight slots came back. Eight
always came back. `ChoghadiyaSequenceTest` checks the sequence itself now, and
was confirmed to fail against the old table before the fix went in.

**The night table steps five at a time, and that is right.** All seven night
rows are rotations of one list that walks the Chaldean cycle five places per slot,
where the day rows walk it one. It looked like the same class of slip as Sunday's
and sat here as an open question until it was checked against a published
panchang: Drik Panchang's Choghadiya for Jaipur, 13-19 Sep 2026, matches all
fourteen rows, day and night, slot for slot. `ChoghadiyaSequenceTest` pins that
published table, so making the night step by one now fails a test.

**The night Choghadiya could not be reached at all, which is why nobody had
looked hard at the night table above.** Tapping "रात का चौघड़िया" moved the pill and changed nothing
else — all eight tiles kept the daytime sequence and the daytime hours under a
control that said night. `choghadiyaSlots` was a plain `get()` reading four
MutableStateFlows through `.value`, which is not a snapshot read, so Compose
never learned the list depended on them; the only scope invalidated was the one
drawing the pill. The same silence covered the city, the date and the 12/24-hour
setting. It is a `combine` of all four now. Half a screen had never worked, and
nobody had noticed because the screen looked like it responded.

**Bhakoot had two of the three doshas.** The classical set is 2/12
Dwirdwadasha, 5/9 Nav-Pancham and 6/8 Shadashtaka; `calculateBhakoot` listed 5/9
among the *scoring* distances instead, so one couple in six was handed the full
seven points and told "भकूट दोष नहीं है" underneath. Seven of thirty-six is
enough to carry a match across the 18-point line the conclusion text treats as
the verdict. The dosha label had no branch for Nav-Pancham either, so a fix to
the score alone would have called it Shadashtaka. The three distances now live
in one named list so the score and the label cannot disagree again, and
`BhakootDoshaTest` asserts all 144 rashi pairs against the rule rather than
restating the table.

**Guna Milan never asked for a birth time.** The calculator has always taken
one and `MainViewModel` has always held the field, defaulted to "12:00" — but
nothing on screen set it, so every match was computed for noon. The Moon changes
nakshatra about once a day and Tara, Yoni, Gana and Nadi are all read from the
nakshatra: 21 of the 36 points. The field is there now, still optional, and the
card under the form says what leaving it blank costs.

**The lucky time reached three of its six values.** `(rashiIdx * 5 + house +
dayOfMonth) % 6`, and six divides twelve, so the modulo inside `house` survived
the outer one and the expression reduced to `4 * rashiIdx` plus terms every
rashi shares — and 4 is not coprime with 6. Half the list was unreachable on any
given day and four rashis shared each of the rest. The fix is a shape that
cannot cancel rather than a better multiplier: the lucky time no longer involves
the transit house at all. This is the fifth separate cause in the "everyone sees
the same data" family, and the first one found by *counting a field nobody had
counted* — `RashifalVariesByRashiTest` covered the rating, the colour, the stone
and the six readings, and this was not among them.

**The recent-search chip on Guna Milan had never worked.** It assigned the four
local `remember` variables, which is what the fields render, while
`calculateGunaMatching()` reads the ViewModel — so the form filled in and the
calculation then refused it with "कृपया नाम दर्ज करें" over a form that plainly
had a name in it. No test could have caught it: the calculator was right, the
screen was right, only the wiring between them was wrong.

**Generated text does not follow a language switch on its own.**
`onLanguageChanged` recomputes the Panchang and the horoscopes for exactly this
reason, and the astro news was missed: switching to English left the Hindi
bulletins on the More tab verbatim. It was the only Devanagari string a sweep of
the whole app in English still found. The offline copy is bilingual so re-reading
it is free; model-fetched news costs one call. The AI answer and the per-rashi
insights are cleared rather than re-asked — re-asking spends a call to say the
same thing, and leaving them puts a Hindi paragraph under an English heading.

**A clipped word at the top of a scrolling list is not a font bug.** Devanagari
loses its matras first, so a half-scrolled heading reads "भकूट दाष ावचार" and
looks like broken shaping. It is the scroll viewport. The tell is that the
warning icon beside it is clipped at exactly the same y — a font problem cannot
crop a vector. Check that before reaching for `includeFontPadding`.

**What the security review found, and what it did not.** Checked before the
public launch, all of it against the release build rather than the source alone:

    manifest        not debuggable; no app component exported without a
                    launcher intent, a widget, or a permission behind it
    backup          Room database and both preference files excluded from
                    cloud backup and device transfer, on both API levels
    network         cleartext blocked, system trust anchors only
    WebView         JavaScript off, DOM storage off, file-URL access off, and
                    only the two bundled legal pages load in it
    APK strings     nothing extractable that is not public by design — the
                    AdMob unit ids and the Firebase key, both of which ship
                    in every APK on Play
    input           sanitised at all four entry points: both profile-save
                    paths, BirthData.parse, and the profile import
    Firestore       users/{uid}/** behind request.auth.uid == userId
    code            no world-readable modes, no addJavascriptInterface, no
                    custom TrustManager or hostname verifier, no Runtime.exec,
                    no PII in any log

**App Check is the control that matters, and it is provably working.** Play
Integrity is installed in `RevatiApp.onCreate`, so worker and widget processes
are attested too, and the debug provider lives in a variant source set that
never reaches release. A release APK signed with the upload key and side-loaded
was refused by the server:

    Firebase AI Logic call failed: Firebase App Check token is invalid

So extracting the API key from the APK does not buy an attacker Firebase AI —
which is the thing that costs money. The API key restriction in Cloud Console
is still worth adding as defence in depth; it is not the only thing standing
there.

**The two AI features are PRO-only, since 13 Sep 2026, and so is their cost
ceiling.** Rashifal's AI insight and Numerology's question box open the PRO
dialog for a free user and never reach the model; the buttons stay visible and
say PRO, because that is how anyone finds out they exist. Until the merchant
account and the subscription exist, **nobody can use either** — that was the
owner's call, made knowing it. The subscription is ₹199 a YEAR, base plan `yearly`
(the owner's figure, 13 Sep 2026; it replaced a ₹99-a-month plan never created); the price is set in Play Console and read from Play, and
no string in the app carries it.

AiRateLimiter's daily cap (50) is persisted in SharedPreferences, unlike the gap
and hourly cap, because a paid call with no server in front needs a ceiling that
swiping the app away does not reset. PremiumDialog states the number.

The insight is keyed by sign, period and date. It was the sign alone, with a
prompt that always said "daily", so the weekly and monthly tabs showed the day's
insight. The question box sends the calculated numerology — it used to send only
a Kundali, or the words "General Vedic Chart", on the numerology screen — and
the answer follows the app language rather than always Hindi.

**PRO cannot be defended against a patched APK, and does not need to be yet.**
Entitlement is a boolean in SharedPreferences, so anyone with root can set it.
What stops that mattering is `queryPurchases()`: Play is asked on every launch
and an absent subscription revokes Pro, so a hand-set flag survives until the
next start. A patched binary that skips the revocation cannot be stopped
without a server, and there is no server. The financial exposure today is zero
because PRO is not purchasable.

Left as they are, with reasons: `USE_BIOMETRIC` and `USE_FINGERPRINT` arrive
from `androidx.credentials`, which Google Sign-In needs. Nothing in this app
uses biometrics, but both are normal-level permissions with no prompt and no
Play declaration, and removing a permission a sign-in library asks for is not
a change to make the day before a launch — Google Sign-In has broken in
production here once already.

**minSdk is 24 and there is no core library desugaring.** `java.time` is off
limits in `app/`. Lint catches it; it once got as far as a crash-on-Android-7
before that. Use `java.util.Calendar` or parse strings.

**Devanagari clips without help.** Compose defaults `includeFontPadding = false`,
which eats i-matras ("पिछला दिन" → "ापछला ादन"). `Type.kt` sets
`PlatformTextStyle(includeFontPadding = true)` plus `LineHeightStyle(trim =
None)` and 1.35× line heights. Leave it alone.

**Themes come from `LocalAstroColors`.** Light and dark both real, following the
system. Never hardcode a colour that only works in one.

**The bottom navigation bar is a floating capsule and every tab carries its
label under its icon**, with the selected one inside a tinted pill. It follows a
design the owner supplied. An earlier version showed the label on the selected
tab only, on the reasoning that five labels cannot share a 320dp screen — which
was true of the way it was measuring, and is not true of the way it measures
now. Nothing in this bar is a constant that was chosen by eye; four separate
things are computed, and each of them was got wrong first:

- **Measure every label, not the longest one.** "कुण्डली" has more characters
  than "राशिफल" and is 5px narrower, so picking by `length` measures the wrong
  string.
- **Measure in the style that is drawn.** The labels render SemiBold and were
  being measured at the default weight, which is narrower — an under-measurement
  in the one direction that clips.
- **Measure against the pill, not against the slot.** Only the selected tab
  draws a pill, so only its label is bounded by one — and near the bottom of a
  capsule there is far less width than the shape's own width. A capsule 60dp
  wide and 47dp tall has 11dp of straight side; the label sits at the bottom,
  deep in the curve, and "Panchang" ran out past both sides of its own pill on
  the device. `cornerInsetAt` computes that loss and the size ladder is checked
  against what is left.
- **The fix for that is a shorter pill, not a squarer one.** The first attempt
  dropped the corner radius to 32% of the height, which is a rounded box and was
  rejected on sight — the design being followed has a capsule. The radius is
  half the height again, and what changed instead is everything that makes the
  pill tall: an 18dp icon, a 2dp gap under it, 4dp of ring around the bar. The
  bar went from 61dp to 51dp and the label fits. The arithmetic is worth
  keeping: the width lost to the curve is `R - sqrt(R² - (C/2)²)` for a stack of
  height C, so a *deeper* vertical padding makes the end cap flatter and buys
  width back — 5dp to 6dp there is the difference between a label and no label
  at 320dp in English.
- **The pill has to stay wider than it is tall, and that is a constraint on the
  label.** `MIN_PILL_RATIO` is checked before the width, with a second pass that
  drops the requirement rather than the label. The design's own ratio is 1.6 and
  is not reachable on a phone: five tabs on a 320dp screen give a 57dp pill, so
  1.6 would need a 36dp pill around a 32dp stack. The limit is the word
  "Horoscope" — the reference's longest label is "Publish".
- **Solve for both languages at once, not for the one on screen.** The bar used
  to change height when the language was switched — 53.5dp in Hindi against
  51dp in English, measured on the device, growing upward from a fixed bottom
  edge. Devanagari's line box is about 1dp taller than the Latin one at the same
  size, and, the larger part, the two were choosing different sizes: "राशिफल" is
  33px at 11sp and fits, "Horoscope" is 53px at 11sp and does not. Solving them
  separately and taking the taller pill fixed the height and broke the shape —
  at 320dp Hindi has no size that satisfies the ratio, so it fell through to the
  pass that gives up the ratio, took the largest size that merely fits, and
  handed English a 53x49 disc. One measurement over both label sets is what
  holds: identical in either language by construction, and sized by "Horoscope",
  which is the widest string in the app in either script anyway.
- **`BAR_MAX_WIDTH` is 420dp and no portrait phone reaches it.** The widest
  common phone is 412dp, which leaves a 388dp bar, so this changes nothing where
  the app is used. It is there for everything wider: the same phone turned
  sideways is 800dp, and five tabs sharing that gave a pill 155dp wide against
  47dp tall — 3.3, against 1.4 in portrait — a stretched band with the labels
  marooned. Capped and centred it comes out at 1.7.
- **A font scale that does not fit is not a reason to drop the label.** The size
  ladder runs 12 down to 8 twice: first in `sp`, which honours the user's font
  setting, then — only for a user who enlarged the text — in `dp`, which ignores
  it. So a 320dp phone at 1.6x shows the label at the size a 320dp phone always
  shows it, instead of showing no label at all. Icons alone remain the last
  resort and no configuration in the screenshot set now reaches it.

Two standing traps: **the English labels are the long ones**, so a bar checked
only in Hindi proves nothing; and **the shot must have the widest label
selected**. `NavBarOnly` used to select "Kundali", the shortest of the five, so
every screenshot showed a comfortable fit while the device did not — it selects
"Horoscope" now. The bar's height follows the measured label, so it grows a
little with the font scale instead of cropping the Devanagari matras.
`navbar_*` and `navbar_en_*` screenshots cover 320/360/412/600/800, dark, 1.3x
and 1.6x; confirmed on the device in both languages, both themes, portrait and
landscape, and at 1.0x/1.3x/1.6x.

**An AdView reserves its height whether or not it has an ad.** Roughly 50dp of
empty strip, and it used to hide itself by accident: the banner gave up after
three failures and rendered nothing. Making it retry indefinitely left that
strip sitting above the tab bar for the whole session on a phone getting no
fill, which reads as a rendering fault. It is `height(0.dp)` until `loaded`.

**Compact facts use `BentoTile`** (`ui/components/BentoPanchangGrid.kt`). Rows of
tiles need `Modifier.height(IntrinsicSize.Min)` on the Row and `.fillMaxHeight()`
on each tile, or a two-line neighbour leaves the other short. Saved Profiles is
deliberately *not* bento — those are action cards, not facts.

---

## Secrets

`.env` (gitignored, mirrored by `.env.example`) feeds BuildConfig via the secrets
plugin. **The plugin cannot emit an empty string** — an unset value must be a
sentinel, which is why `PLAY_LICENSE_KEY=NOT_CONFIGURED` rather than blank.

Keys: `ADMOB_APP_ID_ANDROID`, `ADMOB_BANNER_ID`, `ADMOB_INTERSTITIAL_ID`,
`ADMOB_APP_OPEN_ID`, `ADMOB_REWARDED_ID`, `GOOGLE_WEB_CLIENT_ID`,
`PLAY_LICENSE_KEY`, `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD`.

All five AdMob keys hold real `ca-app-pub-…` ids (checked 11 Sep 2026;
`ADMOB_APP_OPEN_ID` and `ADMOB_REWARDED_ID` were the last two filled in). A key
left at `NOT_CONFIGURED` makes its placement absent — no crash, no test ad,
nothing on screen. See `AdIds.resolve`. `PLAY_LICENSE_KEY` is the only sentinel
still in `.env`.

Release signing uses `upload-keystore.jks` (alias `upload`). The older
`astroveda-upload-key.jks` is dead — Play no longer accepts it.

---

## Signing and Firebase — the part that broke in production

Play re-signs every upload with **its own** app signing key. A Play-installed
app therefore presents a different certificate than the one you signed with, and
Google Sign-In matches on that certificate. Getting this wrong took Google
Sign-In down for every real user of the first release, while it worked fine on
every device tested locally.

Every certificate that can sign this app, copied out of Play Console → *App
signing* and the keystores on 11 Sep 2026:

| Key | SHA-1 | In Firebase? |
|---|---|---|
| Play app signing, current, **classical** | `B7:D1:DE:10:DD:F3:5E:FD:56:27:99:FA:F6:C4:D1:D0:38:B4:5C:C5` | yes |
| Play app signing, current, **post-quantum** | `02:33:B3:63:13:E2:4E:92:9D:6B:5E:B4:F9:73:96:EA:CF:74:85:46` | yes, added 12 Sep 2026 |
| Play app signing, **previous** (first used 25 Aug 2026) | `FB:3F:D2:6B:05:B8:71:4E:8B:2D:DB:E5:DB:67:AF:FE:B5:96:52:2F` | yes |
| Upload key (`upload-keystore.jks`) | `77:D9:C2:35:B4:EB:2D:47:8E:89:DB:27:41:A8:D7:E2:06:9A:40:81` | yes |
| Debug | `D3:46:B8:A4:61:5B:85:6A:4F:AF:5B:F2:64:D2:01:47:2B:F2:31:E1` | yes |

The previous key's SHA-256 is
`9A:42:6C:10:AD:96:C7:69:0E:A3:FC:4B:62:DC:99:34:BE:E5:DF:C6:D0:95:8D:93:B4:55:E7:EF:7D:9F:49:B9`.

**An earlier version of this file had the two Play keys the wrong way round.**
It called `B7:D1` the old key and `FB:3F` the current one, because `FB:3F` is
what reading the certificate of the APK Play serves gave. That APK does carry the
previous key: the Play signing key was upgraded on 25 Aug 2026, and an upgraded
app keeps its original signer and proves the rotation to the new one. The test
phone presented `FB:3F` on 4 Sep 2026 — Sign-In and App Check failed on it until
`FB:3F` was registered — and devices that apply the rotation (Android 13 and
later) can present `B7:D1`. Both are live; neither is stale. The console shows
the fingerprints only behind copy buttons, which makes them easy to misread.

Firebase also lists a SHA-1 `BE:60:FC:75:A5:25:D0:DF:41:2D:B2:19:29:CB:C0:83:42:49:BB:7B`
that matches none of the above — most likely the dead
`astroveda-upload-key.jks`. It is harmless and was left.

The post-quantum SHA-1 was registered on 12 Sep 2026, ahead of the API-key pass
below rather than inside it. The two were bundled together because both are
about the same list, but they carry opposite risks: adding a fingerprint is
purely additive — no existing one is removed, no device that works today can
stop working, and it takes effect server-side without a release — while an
application restriction is a whitelist, so a missing entry takes down AI and
sign-in. No device presents the post-quantum certificate yet (install base
0.0%), so this changes nothing today and removes one way for the API-key pass to
go wrong. Firebase now lists six SHA-1s: the five in the table plus `BE:60`.

`app/google-services.json` was not re-downloaded, and does not need to be. Its
`certificate_hash` entries are not what the server matches on — that is the
console's list — and Google Sign-In here uses `GOOGLE_WEB_CLIENT_ID` from `.env`
rather than the file's Android OAuth clients. The local copy has been a
fingerprint or two behind since 4 Sep with nothing broken by it.

**The Firebase API key is restricted to this app, since 12 Sep 2026.** GitHub
secret scanning flags `app/google-services.json`, and the honest reading is that
the key in it is not a password — it ships inside every APK and anyone can read
it out of a Play download. What matters is what the key is allowed to do, and it
used to say

    Action recommended: This key can currently be used with any application.
    Application restrictions: None

with API restrictions set to 25 APIs, among them Firebase AI Logic and Identity
Toolkit, so anyone holding the key could call those from anywhere. The project
is on the **Spark plan** — no billing account — so that could not run up a bill;
what it could do is spend the free Gemini quota, and then the AI box stops
working for every real user.

It is now *Application restrictions → Android apps* with package
`com.aistudio.astroveda.kpvqzm` against **all five** SHA-1s in the table above.
`BE:60` was deliberately left out: it belongs to the dead keystore and nothing
signs with it. Saving needs the word UPDATE typed into a "Potential breakage due
to active usage" dialog, which lists Maps and `generativelanguage.googleapis.com`
— that warning is about the *API* restrictions, which were not touched, and it
appears whether or not you change anything else.

**How this was verified, and why a device could not have done it.** Google
Sign-In was checked on the phone — the account picker opens, which is the exact
thing that broke in production when a certificate was wrong. But a phone can
only ever exercise the *one* certificate the installed build carries, and the
two that matter most are the Play app signing keys, which no side-loaded build
presents. The restriction is enforced by Google's API key service against two
request headers, `X-Android-Package` and `X-Android-Cert` (the SHA-1, uppercase,
no colons), so every certificate can be tested directly:

    curl -X POST -H 'X-Android-Package: com.aistudio.astroveda.kpvqzm' \
         -H 'X-Android-Cert: <SHA-1 with the colons removed>' \
         -H 'Content-Type: application/json' \
         -d '{"identifier":"nobody@example.invalid","continueUri":"http://localhost"}' \
         'https://identitytoolkit.googleapis.com/v1/accounts:createAuthUri?key=<key>'

Run for all five on 12 Sep 2026, plus two controls:

    Play current classical  B7:D1…   200
    Play post-quantum       02:33…   200
    Play previous           FB:3F…   200
    Upload key              77:D9…   200
    Debug                   D3:46…   200
    BE:60 (dead keystore)            403  API_KEY_ANDROID_APP_BLOCKED
    all zeroes                       403  API_KEY_ANDROID_APP_BLOCKED

**And the AI, which is the thing being protected.** The same two calls against
`firebasevertexai.googleapis.com/v1beta/projects/astroveda-7126b/models/gemini-3.6-flash:generateContent`:

    no Android headers   403  API_KEY_ANDROID_APP_BLOCKED
    package + 77:D9      401  Firebase App Check token is invalid

The 403 is the restriction doing its job — the free Gemini quota is no longer
reachable by anyone holding the key out of an APK, which was the whole point.
The 401 is the key being *accepted* and App Check refusing an unattested caller,
so the restriction is not what stops the AI. The side-loaded build fails at the
same 401, which is why the AI box cannot be exercised end to end without either
a Play install or a registered App Check debug token — and why that has nothing
to do with this change.

Find the fingerprints under Play Console → Test and release → **App signing**.
Adding one takes effect server-side — no new release needed.

**The project's public-facing name said `project-330378380471`** — the
placeholder Firebase generates — which is what Firebase Authentication puts in a
password-reset email and what a consent screen can show beside the account
picker. A user asked to sign in to "project-330378380471" has no way to tell
that is Revati. Set to **Revati** on 12 Sep 2026. The project's own display name
in the console is still "AstroVeda" and was deliberately left: it is not shown
to users, and the project ID `astroveda-7126b` is permanent anyway, so renaming
only the label would make the console harder to search, not easier.

A second Firebase app, `app.revati.jyotish`, exists from the abandoned package
rename. It is unused. Leaving it costs nothing; deleting it is fine too.

**Firestore rules live in the console; `firebase/firestore.rules` is a copy.**
The client only ever touches `users/{uid}/kundali_profiles/{id}`, but that
scoping is worth nothing unless the server enforces it. Checked in the console
on 3 Sep 2026: the deployed rules require `request.auth != null &&
request.auth.uid == userId` over `users/{userId}/{document=**}`, and everything
else is denied by default. That is correct. Nothing here deploys the file, so
the two can drift — if you change the data model, change both.

Firebase Storage is not used by the app, so its rules do not matter.

---

## Build and verify

```bash
./gradlew testDebugUnitTest lintDebug   # must both pass before any commit
./gradlew installDebug                  # onto a connected device
```

The layout screenshots are Roborazzi over Robolectric, and **`testDebugUnitTest`
does not write them** — it runs `ScreenSizeScreenshotTest` and every capture in
it is a no-op without the record flag. The task that produces PNGs is:

```bash
./gradlew recordRoborazziDebug          # writes app/src/test/screenshots/
```

They assert nothing on purpose; the point is to open them. And a screenshot
proves something only when the worst case is the one inside it — see the
narrow-phone and navigation-bar notes above.

`.github/workflows/ci.yml` runs those same two on every push and pull request.
`gradle.properties` no longer pins `org.gradle.java.home` — it used to hold an
absolute path to one Mac's Temurin 17, so the repo could not build anywhere else,
CI included. The toolchain is provisioned by the foojay resolver in
`settings.gradle.kts`. Do not put that line back.

Screens are best checked on a real device in **both languages and both themes** —
that is how most of the UI bugs in this app were found, not by reading code.

---

## The Play listing

The store screenshots are generated, not taken by hand. `goldie/goldie.config.ts`
holds all eight scenes, their headlines, the background and the typeface, and
`goldie frame` renders them into `goldie/out/screenshots/pixel-10-pro/en-US/` as
eight 1080x1920 tiles. The config is the source; `goldie/out/` is gitignored.
`goldie/README.md` has the commands.

The raw screens behind the tiles were captured from the real device rather than
an emulator — there is no AVD on this machine — and four things about that
capture show up in the finished tiles when they are got wrong:

- **Render bigger than the phone.** The device is 720x1600 and goldie draws into
  1280x2856, so a straight capture comes out soft. `wm size 1080x2400` with
  `wm density 420` gives 411dp, an ordinary modern phone. Reset both after.
- **Cut the network.** `AdBanner` is `height(0.dp)` until an ad loads, so with
  wifi and data off there is no banner in any capture and no interstitial
  landing mid-walk.
- **Put SystemUI in demo mode**, or a notification badge turns up in a tile.
- **Sample data only.** The device's saved profiles carry the owner's real name
  and birth time, and the Kundali form's Recent Searches chips display them.
  The tiles use "Aarav Sharma", "Rahul & Priya" and Jaipur.

**The Hindi set exists since 13 Sep 2026**, and it has its own config:
`goldie/hi/goldie.config.ts`, rendering to `goldie/hi/out/screenshots/pixel-10-pro/hi-IN/`.
goldie renders every locale from ONE set of raw captures, so the Hindi tiles
cannot be a second locale in the English config — they need screens captured
with the app in Hindi, and a config in its own directory gets its own `out/raw`.

Two things about it that are not obvious. **The font:** Montserrat has no
Devanagari and goldie's canvas never falls back to system fonts on its own, so
`--font system` renders every headline as tofu boxes. The Hindi config's stack
is `Montserrat, "Kohinoor Devanagari"` — macOS's own Devanagari face — and the
canvas falls through per glyph. **The captures** were taken from the debug build
with the network off, so the header carries the "ऑफलाइन" chip; the English set
predates that chip. Kundali uses a saved sample profile rather than the form,
because the geocoder needs the network that is switched off.

**The privacy policy is one text in two places.** `docs/PRIVACY_POLICY.md` is
the URL on the Play listing and is what a reviewer reads — Play Console →
*Policy and programs → App content → Actioned → Privacy policy → Manage*, and a
save there waits in *Publishing overview* until it is submitted for review; `app/src/main/assets/privacy_policy.html` is what Settings
opens. They used to be written separately, and by Sep 2026 they disagreed on
almost everything. The published one opened with "An Account Is Required",
after the sign-in gate had been removed and against the Data safety form, which
marks name and email optional, and it called a precise location "approximate".
The in-app one described push-notification tokens for an app with no push
messaging, and said the AI receives the time and place of birth, which it does
not. Neither mentioned Firebase Analytics.

Edit the markdown, then run `python3 docs/render_privacy_policy.py`.
`PrivacyPolicyTest` fails if the two copies differ by a word, if a Google
library ships in the app without being named in the policy (or the policy names
one that does not ship), if `ACCESS_FINE_LOCATION` and "precise location"
disagree, or if the Tele-MANAS line is dropped. It was confirmed to fail, all
four ways, against the policy it replaced.

**The legal pages follow the theme now, and the media query was the smaller
half of it.** `privacy_policy.html` and `terms_of_service.html` hard-coded a
light palette, so a dark-mode user opened a white page out of a dark app. The
obvious fix on its own would have been worse than the bug: with `targetSdk` 33
and up, WebView takes `prefers-color-scheme` from the Activity theme's
`isLightTheme`, not from the system, and `Theme.MyApplication`'s parent was
`android:Theme.DeviceDefault` — dark on every device — so both pages would have
gone dark in light mode too.

So `Theme.MyApplication` is a day/night pair: `android:Theme.DeviceDefault.Light.NoActionBar`
in `values`, plain `DeviceDefault` in `values-night`, for both the base style
and the v31 splash variant. Nothing else in the app reads that theme — every
screen is Compose and takes its colours from `LocalAstroColors` — and
`enableEdgeToEdge()` decides the status-bar icons itself, so the parent change
reaches the WebView and little else. The dialog's own backdrop behind the page
was `Color.White`, which flashed white in front of a dark page, and follows the
palette now.

`LegalPagesDarkModeTest` holds the two halves together, because each is
invisible from the other's file: a media query with no night theme is a lie, and
a night theme with no media query does nothing.

**Confirmed on a device on 12 Sep 2026, both ways round** — which is the only
check worth anything here, since the failure mode of the naive fix is that the
page goes dark in light mode too. On the phone in dark mode the policy opens
dark, and with `cmd uimode night no` it opens white. The dialog's own backdrop
follows as well.

The CSS half is checked: both pages were served over localhost and rendered at
320px wide under an emulated light and dark scheme, and in dark they come out
`#0B0E1A` behind a `#131728` card with `#4A8FE8` links, no horizontal overflow
(`scrollWidth` 320 against a 320 viewport, the long account-deletion URL
wrapping inside 266px), and light is byte-for-byte what it always was. **What a
browser cannot settle is the half that matters**: whether WebView on the phone
reports the scheme the system is actually in, now that the Activity theme is a
pair. That wants a device.

**The Data safety form is the policy's other half**, and was redone on 11 Sep
2026 against the code and against Google's own SDK disclosures
(`developers.google.com/admob/android/privacy/play-data-disclosure`,
`firebase.google.com/docs/android/play-data-disclosure`). It had said accounts
could only be made through OAuth, though email sign-up exists; pointed both
deletion links at the pre-rename repo; marked app interactions and device IDs
*optional* although the app offers no way to turn either off; and declared no
location and nothing for the AI box. What it declares now, and why:

    Approximate location   collected + shared   analytics, ads, fraud   Analytics derives it from the IP;
                                                                        AdMob collects the IP to estimate it
    Other user-generated   collected, optional  app functionality       the typed AI question; Firebase is a
      content                                                           service provider, so not "shared"
    App interactions,      required             + ads, fraud            AdMob's stated purposes, on every
      Device IDs,                                                       type it touches
      Diagnostics
    User IDs               optional             + analytics             Crashlytics tags a signed-in user's
                                                                        crash reports with the uid

Play counts whatever an SDK sends off the device, not only what the app sends.
*Precise location* is not declared: the GPS fix reaches nothing but Android's
geocoder. Edit the form and the policy together, in that order of care — the
form is what users see on the listing.

What the policy says about location rests on one fact worth protecting: the
device's location never leaves it — not to the AI (which gets a name, a date of
birth and a lagna), not to Firestore, not to Analytics. Only Android's own
geocoder sees the coordinates, to name the city. See `logPanchangView`. The
Data safety form still declares *Approximate location*, and correctly: Google's
own disclosures say Analytics derives a coarse location from the IP address and
AdMob collects the IP address to estimate one. That is the SDKs, not the app,
and the policy says so.

Release notes, and the rules for writing them, live in `docs/RELEASE_NOTES.md`.
Play takes 500 characters per language and will not publish with only one of the
two filled in.

---

## Admin panel (26 Sep 2026)

Users with ban and undo, the audit log, and where to send a message to
everyone. Settings → Account & data controls → **Admin panel**, shown only to an
allowlisted address. English only: the owner is the only reader.

**The project is on the Spark plan, so `firebase/firestore.rules` is the whole
server.** No Cloud Functions, so no Admin SDK: nothing can list accounts,
disable one, set a custom claim or send a push. Everything is shaped by that:

- **Admin is granted by email.** `allowlisted()` reads the `emails` array of
  `config/admins` (eight addresses, the owner's, 26 Sep 2026: each one is an
  admin whenever it signs up, with no further step). The list is entered in the
  console, never in this public repository — a public list of admin addresses
  is a phishing list. No client can read `config/`. It requires
  `email_verified`, so an email sign-up must click the link first. There is no
  second copy: the app asks for `admin_gate/probe` (a document that never
  exists) and being allowed to ask is the answer.
- **The panel needs an authenticator code too.** `isAdmin()` adds
  `request.auth.token.firebase.sign_in_second_factor == 'totp'`, so a stolen
  Gmail password alone opens nothing. TOTP needs the Identity Platform upgrade
  and a one-off config call (`docs/ADMIN_SETUP.md`). Once an admin enrols,
  **every** sign-in on that account stops half way: `FirebaseAuthService`
  turns `FirebaseAuthMultiFactorException` into `CodeRequired`, and
  AuthScreen's `SignInCodeCard` finishes it. A new sign-in path that catches
  `Exception` before `FirebaseAuthMultiFactorException` locks the admin out.
- **Users are listed from `directory/{uid}`**, which each signed-in app writes
  at every sync (`touchDirectory`). The rules pin the email to the token's and
  the time to the server's. An account shows up only after it has opened a
  build with this code; the complete list is Firebase Console → Authentication.
- **A ban is `bans/{uid}.banned`, never deleted, only switched off.** The rules
  refuse a ban write without an `admin_audit` entry written in the same batch
  (`getAfter`, same actor, same `request.time`), and refuse an audit entry that
  has no ban beside it. A ban stops the account's backups in the rules; the app
  reads it and says so. A banned account can still read and delete its data and
  delete itself, on purpose. For a real lock-out, Firebase Console →
  Authentication → the user → Disable account.
- **Message everyone is Firebase Console → Messaging**, not the app: sending
  FCM needs a server. The app carries `firebase-messaging` and
  `AnnouncementService` only to show what arrives. No token is stored.
  `PrivacyPolicyTest` fails if the policy stops naming Cloud Messaging.

The rules are pasted into the console by hand; nothing deploys them. Test
before pasting: `cd firebase && npm install && npm test` (emulator, 9 cases).

---

## Where it stands

Live on Play, production. **`versionCode 156` / `2.0` was uploaded by the owner
on 8 Sep 2026**, along with a new set of eight English store screenshots. It
supersedes `versionCode 11`, which the owner had put up earlier the same day,
and the `versionCode 5` / `1.2` that production had been on since 3 Sep. That 10
and 11 both went up inside a day is how the versionCode note below came to be
written.

It carries the navigation bar rebuilt to a design the owner supplied, verified
on a real device in both languages, both themes, portrait and landscape, and at
1.0x/1.3x/1.6x font scale.

Android developer verification for the Msunjay Enterprises account is done —
the owner confirmed it on 11 Sep 2026, ahead of Play's 30 Sep deadline, so the
"apps not registered will be removed" notification can be ignored if it lingers.

**2.2 / versionCode 191 went up to Play on 13 Sep 2026** — the test phone had it
installed from Play that evening, and the AI box and Google Sign-In's picker
were both checked working on that install. **2.3 / versionCode 199 is built and waiting for the owner to upload** (14 Sep
2026; it replaces 197, which was never uploaded): the PRO-only AI features, the
period-keyed insight and the numerology context, all checked on the device. Before
it, on normal WiFi, the debug build as a free user showed the banner test ad and
the interstitial on a sub-tab change after 30 seconds, and returned to the app
when closed. `PLAY_LICENSE_KEY` is still NOT_CONFIGURED in `.env`, so purchase
signatures are not checked (fail-open) until the key from Play Console goes in.

What 2.2 was, for the record: It supersedes 2.1 / 185, which was built the same morning and
never went up — do not upload 185. Everything since 156 is in 191:

- the daily Rashifal changing daily rather than weekly, and not throwing on a
  duplicate key after a language switch
- Muhurat computed for the reader's own city rather than Jaipur, and off the
  main thread
- Adhika months labelled; Abhijit and Brahma muhurta as real muhurtas
- a birth-year bound that does not expire on 1 January 2027
- the legal pages following the system theme
- the onboarding funnel recorded
- more ad inventory, with the interstitial and banner faults below fixed
- a tab bar that does not float 36dp off the bottom
- the four big screens split into sections, the nakshatra bar made real, and
  charts no longer swallowing the page's scroll
- the Room database encrypted with SQLCipher
- lighter images (WebP, drawable-nodpi)

The release APK was side-loaded once and opened without a crash — R8 and the
SQLCipher native library both fine — then uninstalled so the Play install is
clean. **What only a Play install can show, and should be checked on the first
one:** an update from 156 keeping its saved profiles (the plaintext-to-encrypted
migration on real data), the AI box answering, and Google Sign-In's picker.

The notes Play gets are three warm lines in each language, on the owner's
request; see `docs/RELEASE_NOTES.md`.

**Three commits from 13 Sep were not on GitHub at the end of that day** — every
push from `19cd1b8` onward came back `remote unpack failed: index-pack failed`
while the local objects were all intact, on the tethered connection. If
`git status` says ahead, push first.

The ad frequency was checked on the device on 13 Sep 2026, on a debug build
over ordinary WiFi, and the check found two faults, both fixed before the
release was built:

- **The interstitial asked the live unit for ads on a debug build.** It read
  `BuildConfig.ADMOB_INTERSTITIAL_ID` directly instead of going through
  `AdIds.resolve`, so development traffic went to the real unit — the thing
  `AdIds` exists to prevent — and came back `code=3 No fill`, because the
  `.debug` package does not own that unit. Through `AdIds` it loads the test
  unit and appears on the first sub-tab change past 30 seconds and not on one
  at 16. `AdIdsTest` now fails on any placement that reads an ad unit any
  other way.
- **The banner was refreshed twice.** See the banner line in the ad-formats
  table below.

The Muhurat screen, which now starts empty and fills from `Dispatchers.IO`,
still wants a look.

**`versionCode` is derived, never typed.** It is `git rev-list --count HEAD` —
the number of commits on the branch — resolved in `app/build.gradle.kts`. That
number only ever goes up, it goes up on every commit, and every release is
committed before it is built, so a code cannot repeat.

It is derived because typing it failed twice in a row. A release was built on
10 after 10 had gone up; the fix was "bump to 11", and 11 had gone up too. Both
times the only symptom was *"Version code N has already been used"* at the top
of the Play upload page, after a ten-minute build — and both times the mistake
was the same one: this machine cannot know what has been uploaded, because the
owner uploads, not the build. Guessing was the defect, not the guess.

`VERSION_CODE_FLOOR` is 20, above everything uploaded under the old scheme. If
git cannot answer — a source zip, a shallow CI checkout — the floor is used so
that tests and lint still run, and `assembleRelease` and `bundleRelease` refuse
outright, because the floor is by definition a number Play has already taken.
CI checks out with `fetch-depth: 0` for the same reason.

### What versionCode 156 changed

Two things. **`versionCode` is now derived from the commit count**, for the
reason above — which is why this release is 156 and not 12.

The other is **the bottom navigation bar**. Every tab now
carries its label under its icon, the selected one inside a tinted capsule, on
a floating capsule bar — the layout the owner asked for, matched from a
screenshot. The full account of how the label is fitted, and of the four
separate ways it was got wrong first, is in the navigation-bar note under
"Things that will bite you". The short version, because each is a general
lesson:

- A screenshot test that selects the *shortest* label proves nothing about a
  bar where only the selected tab draws a pill.
- Measuring text at a different weight than you draw it under-measures, in the
  direction that clips.
- A label inside a capsule is bounded by the curve, not by the width.
- Solving for the language on screen makes the bar change size when the
  language is switched. Solve for both.

The bar is also capped at 420dp and centres beyond that. No portrait phone
reaches that, but the same phone in landscape is 800dp, where it had never been
rendered and looked like a stretched band.

### What versionCode 10 changed

The release the September work adds up to: the September 2026 full-codebase
audit (versionCode 6), profile export/import, the narrow-phone layout fixes and
the rashifal variation fix, plus what a full pass on a real phone found —
Guna Milan missing one of the three Bhakoot doshas and never asking for a birth
time, the night Choghadiya being unreachable, the lucky time repeating across
four rashis at once, and the recent-search chip on Guna Milan never having
worked. All four ad formats were confirmed serving on a device in the same pass.

### What versionCode 6 changed

A line-by-line audit found one data-loss defect and a set of things that were
written, tested and never actually wired up. The individually interesting ones
have their own notes above ("Things that will bite you"); the shape of it:

- **Saved profiles were being destroyed by cloud sync** — the Firestore document
  was keyed on the Room autoGenerate id. See the `uuid` note above. Schema is
  now **version 6**, with `MIGRATION_5_6` backfilling a uuid for every existing
  row, plus the indexes the database had never had (there were none, on any
  table). `MigrationTest` covers it.
- **Notifications and widgets spoke Hindi to English users** — `LanguageManager`
  was only ever initialised from `MainActivity`. `RevatiApp` fixes it; see above.
- **`SecurityUtils` had zero call sites** while advertising OWASP and DPDP
  compliance, with seven passing tests. Root detection and a permanently broken
  SQL-injection regex (`"\b"` is backspace in Kotlin, not a word boundary) are
  deleted; the sanitiser is wired into `BirthData.parse` and both profile-save
  paths, which is where free text actually enters the app.
- **EEA/UK users could not withdraw ad consent.** `AdConsentManager` had
  `showPrivacyOptions` and `isPrivacyOptionsRequired` and nothing called either,
  so consent was collected once on first launch and locked in — a UMP policy
  violation. Settings → Legal & Privacy now shows the row, gated on the UMP
  requirement status, so it stays invisible in India.
- **"Delete my account" could destroy the cloud copy and then fail**, because
  `user.delete()` throws for a stale session *after* the Firestore wipe had
  already committed. It forces a token refresh first now, so a stale session
  fails with everything intact.
- **The panchang cache cost more than it saved** — a cache hit re-ran the entire
  ephemeris to recover the planet list, then threw the rest away. `planetsJson`
  holds them now. `deleteExpiredCache` existed in both DAOs and was never called
  by anything, so cache rows accumulated forever; the daily worker prunes them.
- **Settings said 7:00 AM and the notification arrived at 6:30**, because the
  worker and the ViewModel defaulted the same preference key differently.
- **The festival notification would never have fired on a Devanagari-numeral
  device** — it built an ISO date with `String.format` and no `Locale`, then
  compared it against real ISO dates.
- Removed: Retrofit, OkHttp, Moshi and Coil (zero imports, all four), three
  never-instantiated ViewModels, `tsconfig.json`, a stale root
  `google-services.json` that disagreed with the real one, and a
  `triggerTestCrash()` that shipped in release.
- Added `.github/workflows/ci.yml`, and dropped the `org.gradle.java.home` line
  that made the repo unbuildable on any machine but one.

**That release shipped inside versionCode 10.** The 5 → 6 migration it carries
has since run on a device that already had data, with saved profiles intact —
so the warning that used to stand here is discharged. The habit it asked for is
not: check a release on a real device in both languages and both themes before
uploading it.

`versionCode 4` fixed AdMob serving no ads in production at all: both AdMob
apps from the rename (old AstroVeda, current Revati) still exist, and `.env`'s
`ADMOB_APP_ID_ANDROID`, `ADMOB_BANNER_ID` and `ADMOB_INTERSTITIAL_ID` were all
three pointed at the old app's IDs — reference memory has the correct ones, and
where to find them again if `.env` is ever rebuilt. `versionCode 5` fixed a
second bug in the same area: `AdBanner` still gated its first request on
`AdsInitState.ready`, which the fix's own doc comment had already argued
against — a signal that never arrives should never be able to hold the banner
back forever. It doesn't gate any more; it asks immediately and retries.

Also in this pass: the festival calendar (`FestivalCalculator` /
`FestivalProvider`) now computes dates from each festival's tithi rule instead
of a table hardcoded to one year, verified against published panchang for
2025-2031 including an Adhika month and a nine-minute Raksha Bandhan window.
The old table ran out in November 2026 and had three dates wrong besides.

Working: on-device ephemeris, Panchang, Rashifal, Kundali, Guna Milan, Muhurat,
numerology, festivals calendar, widgets, notifications, Firestore profile sync,
Google + email sign-in, AdMob with UMP consent, App Check via Play Integrity.

Not working / not finished:

- **The AI box has never been exercised end to end on a device**, and cannot be
  from a side-loaded build: App Check refuses an unattested caller before the
  model is reached. That is not a gap in the API key restriction, which was
  proved correct against all five certificates and the AI endpoint itself — see
  *Signing and Firebase*. It is a gap in what a side-loaded build can show. Open
  the AI box once on the first Play install that carries this.


- **When PRO becomes purchasable, add *Financial info → Purchase history* to
  the Data safety form.** Google's disclosure for Analytics lists in-app
  purchases among what it collects automatically.
- **PRO subscription cannot exist yet.** Play Console refuses the Subscriptions
  page until a Google Payments merchant account is set up. Until then the PRO
  button leads nowhere. Product id the app queries:
  `astroveda_premium_pro_subscription`.
- **`PLAY_LICENSE_KEY` is unset**, so `PurchaseVerifier` skips signature
  checking and logs a warning. Moot until the above is done.
- Purchase verification is client-side only; a server checking tokens against
  the Play Developer API is the real fix and there is no backend. Nothing is
  purchasable until the merchant account exists, so this is not urgent — do it
  in the same pass as `PLAY_LICENSE_KEY`, not before. `PurchaseVerifier` returns
  **true** with no key configured — deliberate, because refusing every genuine
  purchase would be worse while nothing is purchasable at all. It logs at error
  level now (the device this is tested on records nothing below `E`), and
  `PurchaseVerifierTest` pins the behaviour so the switch has to be conscious.

**Ads: the serving block was a console problem, and it is cleared.** For weeks
no ad of any kind was served. It was never the code. AdMob's Verify app page
said it outright: *"We didn't find a developer website in your app listing on
Google Play."* The chain was

    no website on the Play listing
      -> AdMob cannot fetch app-ads.txt
        -> App verification: Not verified
          -> Approval status: Requires review
            -> no ads served

Fixed 4 Sep 2026 by publishing `https://sksaini946230-rgb.github.io/` (a GitHub
Pages user site serving `app-ads.txt` with
`google.com, pub-5513456541171739, DIRECT, f08c47fec0942fa0`), setting it as the
developer website on the Play listing, and running AdMob -> app -> Verify app ->
Check for updates. AdMob now reads **App verification: Verified**. Do not go
looking for this in the code again.

Two code faults were found afterwards and fixed, both the same mistake:

- **The banner was gated on `isStartupComplete`.** `AdBanner`'s own doc comment
  argues that a signal which never arrives must never be able to hold the banner
  back — the gate had been removed from inside `AdBanner` once already, and was
  then reintroduced one layer up, in `MainActivity`'s `bottomBar`. That flag is
  set at the end of a coroutine that first runs `recalculatePanchang()`, so one
  throw out of the ephemeris meant no banner for the entire session. The gate is
  gone, and `MainViewModel`'s startup block now sets the flag in a `finally` so
  it arrives whether startup succeeded or not.
- Startup *finishing* and startup *working* are different facts. Nothing should
  gate on the second one.

**Four ad formats now, and one gate between the two that cover the screen.**
Banner and interstitial were the only ones; App Open and Rewarded were added on
the owner's instruction after he created the units.

    banner        anchored adaptive; refresh is the AD UNIT's setting (AdMob, 45s)
    interstitial  tab AND sub-tab change, 30s / 75s / 8 per session
    app open      return to the foreground after 30s away, never on a cold start
    rewarded      opt-in, in front of the Guna Milan PDF

**The ad load was raised twice on the owner's instruction, and the second time
the useful change was not the numbers.** The bottom-bar tab was the only
interstitial trigger, so a user who opened Kundali and then moved between जन्म
कुण्डली, गुण मिलान, अंकशास्त्र and गोचर all session saw none at all. The sub-tab
pills are the same thing as the bottom bar — a deliberate tap on a navigation
control, arriving at a form or a list — so they are triggers too now. They are
only candidates; the three limits still decide, so what changed is that those
limits are now reachable.

Three things are deliberately not done, because they are what closes an AdMob
account rather than merely annoying people, and all three are named in Play's
disruptive-ads policy or AdMob's own guidance:

- **no full-screen ad on a cold start** — the app-open ad still needs 30 seconds
  in the background first, and Google's own guidance is that an app-open ad
  belongs over a loading screen someone is already waiting through
- **nothing between a tap and the result it asked for** — this is why the
  Rashifal's Today/Week/Month chips and the rashi selector are *not* triggers,
  and why the old "fire when the Kundali is generated" trigger was removed
- **never two stacked** — `FullScreenAdGate`, now a 45-second floor

The banner sits at 45s rather than AdMob's 30s minimum for the same reason: a
banner asking as fast as it is allowed to is what invalid-traffic detection
looks for, and the extra impressions are worth less than the account.

**That 45 seconds lives in the AdMob console, not in the code, since 13 Sep
2026.** `AdBanner` used to ask for a new ad every 45 seconds itself, and the
SDK refreshes a loaded banner on its own on whatever interval the ad unit
carries. The two stacked: the device logged six banner loads in three minutes,
two of them 13 seconds apart. The code timer is gone and only a *failed* load
is retried from code. The banner unit's *Automatic refresh* must be set to
Custom, 45 seconds — if it is ever found Disabled, the banner will not refresh
at all, which is the safe way round.

The next real increase is not another turn of these dials — it is **more
rewarded placements**, which the user opts into and which carry no policy risk
at all. The Guna Milan PDF is the only one today.

All four were confirmed serving on a real device on 7 Sep 2026: the banner
filled in landscape and refreshed on the minute, the interstitial appeared on a
tab change, the app-open ad appeared on a return to the foreground and *not* on
a cold start, and the rewarded ad played on the PDF button and produced the
report — as did the "just make it" button beside it.

Both new units answered `code=0 Internal error` for the first couple of hours
after being created and then began serving. That is propagation, not wiring: the
other formats were loading from the same app id throughout. Do not go looking
for a bug in the first hours of a new ad unit.

`FullScreenAdGate` is what stops the interstitial and the app-open ad arriving
together — they fire on unrelated events (a tab change, a return to the
foreground), so "come back to the app and change tab" was two full-screen ads
back to back. One at a time, and a 60s floor between any two of them.

`AdIds.resolve` decides what may be asked for. Debug always uses Google's test
units — development traffic on a live unit is what invalid-traffic enforcement
looks for, and the account is what is at risk. Release refuses a test unit, the
`NOT_CONFIGURED` sentinel, and anything not shaped like `ca-app-pub-…/…`,
because `.env.example` holds test ids and the secrets plugin falls back to it
for any key `.env` does not define — so a release built without `.env` would
otherwise serve test ads to real users, earn nothing, and look like it worked.
An unresolvable id means the placement is simply absent. `AdIdsTest` pins it.

**The rewarded ad must never cost the user the report.** The PDF has always been
free and is the output of a calculation they already ran. `RewardedAdManager`
reports the reward as earned whenever there is no ad to show, so a no-fill is
invisible: only an ad that actually played and was actually abandoned counts as
a refusal. The dialog offers "Watch ad" and "Just make it", and both produce the
report; PRO users never see it.

**An app-open ad cannot be shown from `onStart`, and barely from `onResume`.**
The SDK refuses with *"The ad can not be shown when app is not in foreground"*
and, as first written, that refusal also threw the loaded ad away. `onStart` is
where the background-to-foreground transition can be *detected*, but the process
has not reached foreground importance yet; `onResume` is closer and still too
early. It is posted 600ms after resume now, with one retry at 1.2s, and a show
failure keeps the ad — the response is valid for four hours, so discarding it
meant paying for a load and binning it.

Two false alarms are worth recording alongside that, because both looked like
the same bug. Bringing the app back with `adb shell am start` while the screen
is **asleep** produces exactly that message and is correct behaviour: check
`dumpsys power | grep mWakefulness` before believing it. And the message only
became visible at all because these callbacks log at error level.

**The app-open ad never shows on a cold start.** Google's guidance is that it
belongs over a loading screen someone is already waiting through, not in front
of an app they just launched — that is how these get reported as disruptive. It
also needs the process's foreground state, which an Activity cannot see, so it
is registered from `RevatiApp` and counts started Activities rather than using
ProcessLifecycleOwner: a rotation never drops the count to zero, so returning
from a rotation cannot be mistaken for returning from the launcher. Four-hour
expiry, because that is Google's documented validity window for the response.

**Every ad callback logs at error level, and that is deliberate.** The test
device keeps nothing below E — a dump of its buffer holds thousands of E lines
and not one W. `AdBanner` logged its failure at warning, so the message its own
comment called worth keeping was invisible in the only place anyone reads it;
the interstitial logged nothing at all. Both say `loaded` or `load failed:
code=… msg=…` now, and that is how `code=3 No fill` was identified in one run
instead of being guessed at.

**No fill is a fact about the minute, not the session.** The banner used to give
up after three retries and render nothing until the app restarted; the
interstitial retried only when a tab change happened to pass all three gates,
which during the first 45 seconds never happens. Both retry on a bounded
schedule now. On 7 Sep 2026 the banner took `No fill` for twenty minutes
straight in portrait while the interstitial filled immediately and the banner
itself had filled in landscape — demand is thin and uneven for a new app, which
is exactly why giving up is the wrong response.

**A failed `uploadCrashlyticsMappingFileRelease` does not mean a failed build.**
That task runs *after* `packageRelease` and `bundleRelease`, so the APK and the
AAB are already on disk when it fails. It failed repeatedly on 7 Sep 2026 with
`SocketException: Connection reset by peer` and `Broken pipe` while the machine
was on a tethered connection — the host answers, the upload does not complete.
The consequence is limited and worth knowing: Play deobfuscates its own crash
reports from the mapping inside the bundle
(`BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map`), so
Play Console is unaffected; only **Firebase Crashlytics** lacks the mapping for
that build until the task is re-run on a working network. Check the artifact's
timestamp before treating a red build as a broken one.

**Ads cannot be tested on the iPhone hotspot.** It resolves
`googleads.g.doubleclick.net` and `pagead2.googlesyndication.com` to 127.0.0.1,
so every request fails and it looks like the integration is broken. `adb shell
ping pagead2.googlesyndication.com` answers in 0.1ms when this is happening.
Switch the phone to normal WiFi before concluding anything about ads.

**Interstitials are wired to tab and sub-tab switches and are meant to be
sparse.** `MainActivity.showInterstitialAd` will not show one until the session
is 30 seconds old, keeps 75 seconds between them, and stops after 8 in a
session (see the ad-formats table above). "I switched tabs and got no ad" inside
the first 30 seconds is the design working, not a fault.

**The "native debug symbols" warning on upload cannot be fixed here.** Play warns
that the bundle has native code with no symbols. This app has no native code of
its own; the eight `.so` files come transitively from `androidx.graphics.path`
and `androidx.datastore`, and both ship **already stripped** — verified on the
versionCode 6 bundle, 0 symtab entries, ELF header says `stripped`.
`debugSymbolLevel` extracts symbols, it cannot invent them. The setting is left
on in `app/build.gradle.kts` because it is correct in principle, but the warning
will stay. It is advisory and does not block a release.

Deliberately left alone by the September 2026 audit, with reasons:

- ~~**The four screen composables are still one function each.**~~ **Done, 13
  Sep 2026.** Panchang, Kundali, Guna Milan and Rashifal are split into
  sections that take only the values they draw; KundaliScreen's date and time
  fields, which existed twice, are one `BirthPickerField`. Panchang and Kundali
  were checked on the device section by section. Doing it found two real bugs,
  both fixed: the nakshatra progress bar was a constant 65%
  (`nakshatraProgressPercent`, derived from the stored Moon, no migration), and
  a swipe starting on any `NorthIndianChart` stopped the page dead — the chart
  ran `detectTransformGestures` at rest under a comment saying it claimed no
  gestures there, and that detector consumes a one-finger pan past touch slop.
- ~~**Debug still shares the production Firebase project.**~~ **Done, 13 Sep
  2026 — `revati-debug`.** Debug builds sign in
  against the live Auth user pool and read and write the live Firestore. The
  security rules keep that to the developer's own `users/{uid}` — nobody else's
  data is reachable — but a debug build with a bad migration or sync bug writes
  it into a real account, and debug analytics land in the production stream.
  **An earlier note here gave the fix as `applicationIdSuffix = ".debug"` plus a
  second Android app in the same project. That would not separate anything that
  matters:** Firestore and the Auth users belong to the project, not to the app,
  so a second app in `astroveda-7126b` still reads and writes the same data. The
  real fix is a second Firebase *project* for debug — its own
  `app/src/debug/google-services.json`, Email and Google sign-in enabled, the
  same Firestore rules, Firebase AI Logic enabled, a debug-only
  `GOOGLE_WEB_CLIENT_ID`, and the App Check debug token registered — then the
  suffix. Console first, and a device to check sign-in, sync and the AI answer
  in the debug build before it is relied on.

  **`applicationIdSuffix` is also what makes this safe to do at all, and that
  was missed once.** Without it a debug build cannot be installed over a
  release-signed one — Android refuses on the signature — so putting a debug
  build on the owner's phone means uninstalling the app and taking his saved
  profiles, reports and recent searches with it. He is not signed in, so there
  is no cloud copy to restore from. With the suffix the debug build is a
  different package: it installs *beside* the release build and touches none of
  its data. That is why the suffix and the debug project are one piece of work.

  **What exists now.** Project `revati-debug` (number 130912941752, Spark plan,
  no Analytics), one Android app `com.aistudio.astroveda.kpvqzm.debug` carrying
  the debug SHA-1 `D3:46…`, Email/Password and Google sign-in enabled, Firestore
  with the *same* rules as production — `users/{userId}/{document=**}` behind
  `request.auth.uid == userId` — and Firebase AI Logic on the Gemini Developer
  API. `app/src/debug/google-services.json` is committed for the same reason the
  production one is: the key inside it ships in every APK and is not a password.

  **The web OAuth client is not in `.env` and must not be.** The secrets plugin
  sets `GOOGLE_WEB_CLIENT_ID` from `.env` for every variant and wins over a
  `buildConfigField` on the debug buildType — an override there is silently
  ignored, which was tried and produced a debug build holding the *production*
  client id. `FirebaseAuthService` reads `R.string.default_web_client_id`
  instead, which the Google Services plugin generates per variant out of that
  variant's own `google-services.json`; BuildConfig remains the fallback for a
  build with no config file. A release client id used against the debug project
  fails as "No Google account found on this device" — the same misleading
  message a certificate mismatch produced in production once.

  **Done on 13 Sep 2026:** the debug build is installed on the test phone and
  its App Check debug token (`Nova2-debug`) is registered in `revati-debug`;
  the owner checked the AI answer and Google Sign-In on it. The token never
  appears in this phone's logcat — Firebase logs it below E, which this device
  drops — so it was read from the app's own storage instead:
  `adb shell run-as com.aistudio.astroveda.kpvqzm.debug cat shared_prefs/com.google.firebase.appcheck.debug.store.*.xml`.
  In the console, `revati-debug` is under a different signed-in Google account
  from `astroveda-7126b`.
- ~~**The Room database is unencrypted.**~~ **Done, 13 Sep 2026 — SQLCipher.**
  `data/local/DatabaseEncryption.kt`. A random passphrase wrapped with AES-GCM
  under an Android Keystore key, in `revati_db_key.xml`, which is excluded from
  backup like the database. An existing plaintext database is converted on first
  launch with `sqlcipher_export` and replaced by an atomic rename only after the
  copy opens with the key and every table's row count and `user_version` match;
  anything short of that opens the plaintext file as before and tries again next
  launch. **The failure path has already been exercised for real:** the first
  device run failed on the ATTACH (the connection lacked `CREATE_IF_NECESSARY`,
  which an attached database inherits), fell back, and lost nothing. Verified
  after the fix on a debug database seeded with two profiles and a 350 KB WAL.
  **sqlcipher-android is pinned at 4.17.0** — 4.18.0 onward require compileSdk
  37 — and its `.so` files are 16 KB aligned, which Play requires. If the
  Keystore key is ever lost, the encrypted file cannot be read; it is renamed to
  `.unreadable` so the app starts, and cloud backup is the only way back. That is
  the trade SQLCipher is.

Decided rather than pending:

- **Sign-in is no longer a gate.** It was `MainActivity`, `currentUser == null`
  → `AuthScreen`, in front of everything, while the Play listing said most
  features work without an account. The gate is gone: `AuthScreen` now takes an
  optional `onDismiss` and opens as a full-screen dialog from
  `MainViewModel.showAuthScreen`, reached from the Saved Profiles cloud card and
  from Settings → Account & data controls. An account buys cloud backup and
  nothing else, so an account is what it now asks for. Signing out leaves the
  user inside the app rather than bouncing them to a login screen.
- **No age gate, deliberately.** The app is not child-directed and does not
  declare children in its Play target audience. `MainActivity` sets
  `MAX_AD_CONTENT_RATING_T`, `TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE` and
  `TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE`, which is what Play and AdMob actually
  ask for here. An age gate would be a screen in the way of nothing.
  **The ad rating is T since 14 Sep 2026; it was G**, and G is the Families
  rating: Play build 2.3/199 got `code=3 No fill` on every banner on two
  phones, WiFi and mobile data, while AdMob showed the app Ready, app-ads.txt
  verified and no policy issue. Do not put it back to G. T still excludes MA.

---

## Ground rules

**Tezzo is a different project.** Never touch its files, Firebase project, or
tokens. If a task might reach it, ask first.

Astrology output is presented as traditional interpretation, not prediction, and
the app must never imply a human astrologer is answering — the "answers are
generated automatically" line under the AI question box in `AstroDisclaimer.kt`
stays.

The Tele-MANAS 14416 helpline was on the end of that line and was removed in Sep
2026 on the owner's instruction, given twice after the reason for it was put to
him. It survives in two places that were not part of that instruction and should
not be quietly swept up with it: the crisis-response rule in the model prompt in
`GeminiAstroService.kt`, which only fires when a user sounds hopeless, and the
privacy policy.
