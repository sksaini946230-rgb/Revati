# Feature parity checklist — Kotlin app → Expo app

Inventory of `~/Revati` as of 17 Sep 2026 (versionCode 202). Every row is
something a user of the Play app can see or rely on today. The Expo app is not
"done" until each row is ticked **on the owner's iPhone** (column iOS) and, if
Android ever moves over, on the test phone (column And).

Source paths are relative to `app/src/main/java/com/example/`. When a row is
built, read the Kotlin source it names — the inventory is a map, not the
territory. Every screen must exist in Hindi and English, light and dark.

Legend: `[ ]` not started · `[~]` built, not device-checked · `[x]` checked on device

---

## 1. App shell

| # | Feature | Kotlin source | iOS | And |
|---|---|---|---|---|
| 1.1 | Splash with the Revati logo | `ui/screens/SplashScreen.kt`, `res/drawable-v31/splash_logo.xml` | [ ] | [ ] |
| 1.2 | Onboarding: language choice (page itself bilingual), rashi choice, city + location permission, notification permission, Skip / "Enter Revati"; funnel events per page | `ui/screens/OnboardingScreen.kt`, `OnboardingStepsTest` | [ ] | [ ] |
| 1.3 | Five bottom tabs: Panchang, Rashifal, Kundali, Muhurat, More — labels under every icon | `MainActivity.kt` (`AppTab`), `ui/components/BottomNavBar.kt` | [ ] | [ ] |
| 1.4 | Sub-tab pills inside tabs (Panchang: Daily/Calendar; Kundali: Birth Chart/Matching/Numerology/Transits; More: Profiles/Settings) | `ui/components/TopHeaderBar.kt` (`SubTabHeader`) | [ ] | [ ] |
| 1.5 | Top header: logo, "Kundli & Panchang" subtitle, language toggle (हिंदी/ENG), PRO chip, settings gear (jumps to More → Settings), offline chip | `ui/components/TopHeaderBar.kt`, `OfflineStatusChip.kt` | [ ] | [ ] |
| 1.6 | Back button on a non-Panchang tab returns to Panchang (Android only; iOS has no back button — use swipe/tab) | `MainActivity.kt` | n/a | [ ] |
| 1.7 | Celestial background image and glass cards | `ui/components/CelestialBackground.kt`, `Glassmorphic.kt`, `drawable-nodpi/divine_cosmic_background_*.webp` | [ ] | [ ] |
| 1.8 | Loading skeletons (gold shimmer), astro loading indicator, empty states | `GoldShimmerSkeleton.kt`, `AstroLoadingIndicator.kt`, `EmptyStateComponent.kt` | [ ] | [ ] |
| 1.9 | Error boundary: a crashing section shows a bilingual fallback, not a dead app | `ui/components/ErrorBoundary.kt` | [ ] | [ ] |
| 1.10 | Feature discovery overlay (first-run hints), remembered as done. *(Owner, 26 Sep 2026: build it. The Muhurat card's "and ensure success" promise was removed in both apps.)* | `ui/components/FeatureDiscoveryOverlay.kt`, pref `is_discovery_completed` | [ ] | [ ] |
| 1.11 | Rate-us dialog after successful use, with feedback text, dismiss memory, opens store review | `ui/components/RateUsDialog.kt`, prefs `has_rated`, `rate_dialog_dismissed_at`, `successful_lookup_count`, `session_action_count` | [ ] | [ ] |
| 1.12 | PRO dialog listing only real PRO benefits (ad-free; AI horoscope day/week/month; guidance up to 50 questions/day; PDF without the ad) | `ui/components/PremiumDialog.kt` | [ ] | [ ] |
| 1.13 | Theme follows the system, light and dark both real | `ui/theme/*` | [ ] | [ ] |
| 1.14 | Devanagari never clipped (matras), 1.35× line height | `ui/theme/Type.kt` | [ ] | [ ] |
| 1.15 | Astrology disclaimer: traditional interpretation, answers generated automatically | `ui/components/AstroDisclaimer.kt` | [ ] | [ ] |

## 2. Panchang tab

| # | Feature | Kotlin source | iOS | And |
|---|---|---|---|---|
| 2.1 | Greeting ("Good evening", "Namaste, name 🙏"), city and guest/user line | `ui/screens/PanchangScreen.kt`, `DailyPanchangCard.kt` | [ ] | [ ] |
| 2.2 | Date + location card; previous/next day and Today; city picker; "use my location". *(No date picker: the Play app has none on this screen; another day is reached through the calendar. Checked 26 Sep 2026.)* | `PanchangScreen.kt` (`PanchangDateLocationCard`), `ui/components/DateTimePicker.kt` | [ ] | [ ] |
| 2.3 | Four shortcut tiles: Horoscope, Birth Chart, Kundli Milan, Muhurat (one column below 300dp content width) | `PanchangScreen.kt` | [ ] | [ ] |
| 2.4 | Today's horoscope card for the user's rashi, with lucky number and colour, "Full →" | `PanchangScreen.kt` | [ ] | [ ] |
| 2.5 | Daily insights pager (Panchang insight, Choghadiya & auspicious timings, Nakshatra energy & transit, Moon phase & Rahu Kaal warning) | `DailyInsightsPager` | [ ] | [ ] |
| 2.6 | Samvat bar: Vikram and Saka samvat, month (with **Adhika** prefix when it is one), paksha | `SamvatInfoBar`, `astro/PanchangElements.kt` (`isAdhikaMasa`) | [ ] | [ ] |
| 2.7 | Core elements: Tithi (+ end time, progress), Nakshatra (+ pada, end time, **real** progress from the Moon), Yoga, Karana, Moon phase waxing/waning | `CorePanchangElementsCard`, `data/model/PanchangData.kt` (`nakshatraProgressPercent`) | [ ] | [ ] |
| 2.8 | Sun and Moon: sunrise, sunset, moonrise, moonset, Sun sign, Moon sign | `SunMoonTimingsCard` | [ ] | [ ] |
| 2.9 | Muhurat timings: Rahu Kaal, Gulika, Yamaganda, Abhijit, Brahma (real muhurta = day/15) | `MuhuratTimingsCard`, `PanchangDayDivisionsTest` | [ ] | [ ] |
| 2.10 | Choghadiya day/night toggle and 8-slot strip with Best/Good/Avoid colouring and ruling planet | `ChoghadiyaDayNightToggle`, `ChoghadiyaStrip`, `astro/ChoghadiyaCalculator.kt` | [ ] | [ ] |
| 2.11 | Daily Lagna chart (North Indian) | `DailyLagnaChartCard`, `ui/components/NorthIndianChart.kt` | [ ] | [ ] |
| 2.12 | Bento fact tiles, equal height per row | `ui/components/BentoPanchangGrid.kt` | [ ] | [ ] |
| 2.13 | 12/24-hour clock everywhere times appear | pref, `MainViewModel` | [ ] | [ ] |
| 2.14 | Monthly calendar: month grid Mon–Sun, tithi per day, festivals marked, month navigation, current year shown (never a hardcoded year) | `ui/screens/CalendarScreen.kt` | [ ] | [ ] |
| 2.15 | Calendar region filter: All / North India / Rajasthan | `CalendarScreen.kt` | [ ] | [ ] |
| 2.16 | Festival detail sheet: date and tithi, religious significance, puja vidhi & rules. *(The Play app's "regional history" section is left out: every festival shows the same default sentence from `FestivalData`, so it says nothing about the festival. Found 26 Sep 2026.)* | `CalendarScreen.kt`, `astro/FestivalProvider.kt`, `data/model/FestivalLocalized.kt` | [ ] | [ ] |
| 2.17 | Festival dates computed from tithi rules (2025–2031 verified, Adhika months skipped) | `astro/FestivalCalculator.kt`, `FestivalCalculatorTest` | [ ] | [ ] |
| 2.18 | 30 built-in cities + free-text city search (geocoder) + GPS city | `astro/PanchangCalculator.kt`, `data/local/CityPreferences.kt`, `SettingsScreen.kt` | [ ] | [ ] |

## 3. Rashifal tab

| # | Feature | Kotlin source | iOS | And |
|---|---|---|---|---|
| 3.1 | Rashi selector row (12 signs, symbol, remembers the user's rashi) | `ui/screens/RashifalScreen.kt` (`RashiSelectorRow`), pref `user_rashi_id` | [ ] | [ ] |
| 3.2 | Period tabs: Today / This week / This month, with the date range | `RashifalPeriodTabs` | [ ] | [ ] |
| 3.3 | Reading cards: general, career & business, health, love, finance | `RashifalReadingCards`, `astro/RashifalProvider.kt` | [ ] | [ ] |
| 3.4 | Rating stars, lucky number, colour, stone, time, element, ruler — **varying across all 12 signs** | `RashifalProvider.kt`, `RashifalVariesByRashiTest` | [ ] | [ ] |
| 3.5 | Share Rashifal as text | `util/ShareUtils.kt` | [ ] | [ ] |
| 3.6 | AI personalised insight, PRO only; per sign + period + date; locked button says PRO and opens the PRO dialog; failure shows retry, never the offline keyword text | `ui/components/PersonalizedInsightCard.kt`, `MainViewModel.fetchPersonalizedInsight`, `AiInsightFallbackTest` | [ ] | [ ] |
| 3.7 | Horoscope cache keyed by the exact period, never 24 rows after a language switch | `data/local/AstroCacheRepository.kt`, `AstroCacheRepositoryTest` | [ ] | [ ] |

## 4. Kundali tab

| # | Feature | Kotlin source | iOS | And |
|---|---|---|---|---|
| 4.1 | Birth details form: full name*, date*, time*, place* (geocoded), validation messages in both languages. *(No gender field: the Play app has none and stores every profile as `MALE`; checked 26 Sep 2026.)* | `ui/screens/KundaliScreen.kt` (`KundaliDateTimeFields`, `BirthPickerField`) | [ ] | [ ] |
| 4.2 | Pick from saved profiles with search | `KundaliScreen.kt` | [ ] | [ ] |
| 4.3 | Recent searches chips (never show real data in screenshots) | `ui/components/RecentSearchesComponent.kt`, `data/local/RecentSearch*` | [ ] | [ ] |
| 4.4 | Chart header: name, "Edit details", "New chart", Save to profiles, saved state | `KundaliChartHeaderCard` | [ ] | [ ] |
| 4.5 | North / South Indian chart toggle, pinch zoom only (one-finger swipe scrolls the page) | `KundaliChartCanvasCard`, `NorthIndianChart.kt`, `SouthIndianChart.kt` | [ ] | [ ] |
| 4.6 | Planet glyphs localised at draw time, retrograde marked | `astro/AstroNames.kt` (`houseGlyph`) | [ ] | [ ] |
| 4.7 | Share chart as image ("Revati Kundali") | `util/KundaliImageGenerator.kt` | [ ] | [ ] |
| 4.8 | Lagna, Moon sign, Nakshatra summary tiles | `KundaliScreen.kt` | [ ] | [ ] |
| 4.9 | Planet table: planet, rashi, degree, house, retrograde | `KundaliPlanetTable` | [ ] | [ ] |
| 4.10 | Vimshottari Mahadasha timeline with Antardasha sub-periods, years | `ui/components/DashaHorizontalTimeline.kt`, `astro/VimshottariDashaCalculator.kt` | [ ] | [ ] |
| 4.11 | **Guna Milan** form: boy and girl name, date, optional time (card explains the cost of leaving it blank), place; recent-search chip that actually fills the calculation | `ui/screens/MatchingScreen.kt` | [ ] | [ ] |
| 4.12 | Guna score card (x/36), verdict, PDF button | `GunaScoreCard` | [ ] | [ ] |
| 4.13 | Dosha cards: Manglik (for the real place), Bhakoot (all three: 2/12, 5/9, 6/8), Nadi | `MatchingDoshaCards`, `BhakootDoshaTest` | [ ] | [ ] |
| 4.14 | Birth attribute comparison (Varna, Vashya, Yoni, Gana, Nadi…), summary card, eight-koot breakdown grid | `BirthAttributeComparisonCard`, `MatchingSummaryCard`, `KootBreakdownGrid` | [ ] | [ ] |
| 4.15 | PDF report: export/share/print; free users get "Watch ad" or "Just make it" — both produce the report; PRO sees no ad. *(Expo, 26 Sep 2026: built without the ad step until AdMob iOS exists — the same report "Just make it" gives. Two differences on purpose: no "Certified Report" subtitle, since nobody certifies it; and the koot table and reading follow the app language — the Play PDF prints them in Hindi even in English mode, a Play bug.)* | `service/MatchingPdfReportService.kt`, `service/RewardedAdManager.kt` | [ ] | [ ] |
| 4.16 | ~~Saved astrology reports~~ **Removed, owner 26 Sep 2026.** `saveReport` had no caller, so the list was always empty. Gone from the Play app (`f0b670d`, table kept for the schema) and not built in Expo. | — | — | — |
| 4.17 | **Numerology**: name + DOB (fields start EMPTY), Moolank + ruling planet, Bhagyank, name number (Chaldean), friendly/enemy numbers, reading | `ui/screens/NumerologyScreen.kt`, `astro/NumerologyCalculator.kt`, `NumerologyValidator.kt` | [ ] | [ ] |
| 4.18 | Birth-date validation follows today, never a fixed year | `NumerologyValidator.kt`, `NumerologyValidatorDateTest` | [ ] | [ ] |
| 4.19 | "Ask your question" AI box, PRO only; sample questions; context = today's date + numerology + "Birth chart: name, DOB; Lagna"; never time/place of birth; answer in app language | `NumerologyScreen.kt`, `MainViewModel.askAiAstrologer`, `data/ai/GeminiAstroService.kt` | [ ] | [ ] |
| 4.20 | **Transits (Gochar)**: transit wheel, table planet / birth rashi / transit rashi / house | `KundaliScreen.kt`, `ui/components/TransitWheelChart.kt`, `astro/TransitCalculator.kt` | [ ] | [ ] |

## 5. Muhurat tab

| # | Feature | Kotlin source | iOS | And |
|---|---|---|---|---|
| 5.1 | Day / night Choghadiya for the selected city and date | `ui/screens/MuhuratScreen.kt` | [ ] | [ ] |
| 5.2 | Upcoming auspicious muhurats for the **selected city**, computed off the main thread, refreshed on city/clock change | `astro/MuhuratCalculator.kt`, `MainViewModel.upcomingMuhurats` | [ ] | [ ] |
| 5.3 | Each muhurat shows date, tithi, nakshatra, time window, ruler | `MuhuratScreen.kt` | [ ] | [ ] |

## 6. More tab → Profiles

| # | Feature | Kotlin source | iOS | And |
|---|---|---|---|---|
| 6.1 | Saved profiles list; add, edit, delete (confirm); "Generate chart" from a profile; Use Boy / Use Girl for Guna Milan. **Fixed in the Play app 26 Sep 2026:** Add, Edit and "Save" on a chart used to store the Settings city's coordinates whatever the birth place (`MainViewModel.saveNewProfile`), so the lagna of every such profile was wrong. All three now take the place from the geocoder list (`ui/components/BirthPlaceField.kt`) and keep its coordinates, as the Expo app does. Profiles saved before the fix keep their old coordinates until re-picked in Edit. | `ui/screens/SavedProfilesScreen.kt`, `data/local/Kundali*` | [ ] | [ ] |
| 6.2 | Profile identity is `uuid`, never a row id | `data/local/ProfileMerge.kt`, `ProfileMergeTest` | [ ] | [ ] |
| 6.3 | ~~Saved reports list and delete~~ removed, see 4.16 | — | — | — |
| 6.4 | Cloud backup card: sign in, back up, restore, online/offline state, sign out (confirm) | `SavedProfilesScreen.kt`, `service/FirebaseAuthService.kt` | [ ] | [ ] |
| 6.5 | Export profiles to a file ("send profile") and import from a file; format `revati-profiles` v1 — **the Expo app must read and write the same format** | `data/local/ProfileTransfer.kt`, `ProfileTransferTest` | [ ] | [ ] |
| 6.6 | Delete account and all data (cloud + local), with token refresh first | `FirebaseAuthService.deleteUserDataAndAccount` | [ ] | [ ] |

## 7. More tab → Settings

| # | Feature | Kotlin source | iOS | And |
|---|---|---|---|---|
| 7.1 | PRO banner / "Upgrade to PRO" / PRO status | `ui/screens/SettingsScreen.kt` | [ ] | [ ] |
| 7.2 | Language (हिंदी / English), generated text re-made on switch, AI answers cleared | `util/LanguageManager.kt`, `MainViewModel.onLanguageChanged` | [ ] | [ ] |
| 7.3 | Time format 12/24 | `SettingsScreen.kt` | [ ] | [ ] |
| 7.4 | City: search and choose, or current location; "location updated" feedback; permission-denied message | `SettingsScreen.kt` | [ ] | [ ] |
| 7.5 | Main rashi — **Play app bug:** the choice is held in memory only (`MainViewModel._selectedRashiId`), never written to `user_rashi_id`, so it resets to Mesh on every launch and the daily notification always shows Mesh. The Expo app saves it. | pref `user_rashi_id` | [ ] | [ ] |
| 7.6 | Notifications: daily Panchang & Rahu Kaal alert (on/off + time, default **7:00 AM** — `NotificationDefaults.DAILY_HOUR`), Muhurat & Choghadiya alert (on/off only; its hour/minute keys exist but no screen sets them, so it is always 7:00), festival & vrat reminder (on/off, fixed 10:00) | `SettingsScreen.kt`, `util/NotificationDefaults.kt`, `worker/*` | [ ] | [ ] |
| 7.7 | Latest astro & astrology news (AI with search grounding, offline bilingual fallback, refresh) | `GeminiAstroService.fetchAstroNewsWithSearchGrounding`, `getOfflineAstroNews` | [ ] | [ ] |
| 7.8 | About Revati | `SettingsScreen.kt` | [ ] | [ ] |
| 7.9 | Rate on Play Store (App Store on iOS) | `SettingsScreen.kt` | [ ] | [ ] |
| 7.10 | Legal & privacy: Privacy Policy, Terms (bundled HTML, follows theme), ad privacy options row only where UMP requires it | `assets/privacy_policy.html`, `assets/terms_of_service.html`, `service/AdConsentManager.kt` | [ ] | [ ] |
| 7.11 | Account & data controls: signed-in email, sign in, delete all saved data, delete account | `SettingsScreen.kt` | [ ] | [ ] |

## 8. Services and background behaviour

| # | Feature | Kotlin source | iOS | And |
|---|---|---|---|---|
| 8.1 | Sign-in optional — only needed for cloud backup | `MainViewModel.showAuthScreen`, `ui/screens/AuthScreen.kt` | [ ] | [ ] |
| 8.2 | Google sign-in; email sign-up (name), sign-in, forgot password; **+ Sign in with Apple on iOS** | `FirebaseAuthService.kt` | [ ] | [ ] |
| 8.3 | Firestore `users/{uid}/kundali_profiles/{uuid}`, batched (450), rules `request.auth.uid == userId` | `FirebaseAuthService.kt`, `firebase/firestore.rules` | [ ] | [ ] |
| 8.4 | App Check on every AI call (Play Integrity → App Attest on iOS) | `RevatiApp.kt` | [ ] | [ ] |
| 8.5 | AI rate limit: 3 s gap, 20/hour, 50/day persisted | `util/AiRateLimiter.kt`, `AiRateLimiterTest` | [ ] | [ ] |
| 8.6 | AI prompt rules: app language, round brackets, crisis-response rule; the question box and ViewModel cut a question at **500** chars (`AiRateLimiter.MAX_QUESTION_CHARS`), the service's own backstop is 2,000 question / 1,000 detail chars; model `gemini-3.6-flash` (re-check at build time) | `data/ai/GeminiAstroService.kt` | [ ] | [ ] |
| 8.7 | Encrypted local database (SQLCipher), key in secure hardware storage, never backed up | `data/local/DatabaseEncryption.kt` | [ ] | [ ] |
| 8.8 | Panchang cache keyed with language + clock format; expired rows pruned daily | `data/local/PanchangCacheDao.kt` | [ ] | [ ] |
| 8.9 | Input sanitising at every free-text entry (profile save ×2, birth data parse, import) | `util/SecurityUtils.kt` | [ ] | [ ] |
| 8.10 | Analytics events: app open, screen view, onboarding step/complete, login (4 paths), kundali generated, horoscope view, matching, numerology, AI query, share, purchase initiated/success/failed, user properties; **no location in analytics** | `util/AstroAnalytics.kt` | [ ] | [ ] |
| 8.11 | Crash + non-fatal reporting on every data-loss path | `AstroAnalytics.recordNonFatal` | [ ] | [ ] |
| 8.12 | Ads: banner (height 0 until loaded; refresh set in the AdMob console), interstitial (tab + sub-tab change, 30 s / 75 s / 8 per session), app-open (return after 30 s away, never cold start), rewarded (PDF), one full-screen gate with a floor between two | `ui/components/AdBanner.kt`, `MainActivity.kt`, `service/AppOpenAdManager.kt`, `service/FullScreenAdGate.kt` | [ ] | [ ] |
| 8.13 | Debug builds use test ad units; release refuses test/sentinel ids | `service/AdIds.kt`, `AdIdsTest` | [ ] | [ ] |
| 8.14 | Ad content rating **T**, not child-directed | `MainActivity.kt` | [ ] | [ ] |
| 8.15 | UMP consent before ads init (EEA/UK), privacy options row | `service/AdConsentManager.kt` | [ ] | [ ] |
| 8.16 | PRO: subscription ₹199/year (Play `astroveda_premium_pro_subscription`, base plan `yearly`), restore on launch, revoke when absent, PRO hides ads | `service/BillingManager.kt`, `service/PurchaseVerifier.kt` | [ ] | [ ] |
| 8.17 | Daily Panchang notification (localised, city-aware) | `worker/AstroNotificationWorker.kt` | [ ] | [ ] |
| 8.18 | Muhurat notification | `worker/MuhuratNotificationWorker.kt` | [ ] | [ ] |
| 8.19 | Festival notification (ISO date built locale-safe) | `worker/FestivalNotificationWorker.kt` | [ ] | [ ] |
| 8.20 | Home-screen widget: Panchang (sunrise, sunset, tithi… localised labels). *(iOS needs a WidgetKit extension written in Swift plus an App Group so the app can hand it the day's data; the App Group has to be registered on the Apple team. Owner, 26 Sep 2026: **not now**.)* | `widget/PanchangWidgetProvider.kt`, `res/layout/panchang_widget.xml` | [ ] | [ ] |
| 8.21 | Home-screen widget: Tithi & Nakshatra | `widget/TithiNakshatraWidgetProvider.kt` | [ ] | [ ] |
| 8.22 | Widgets and notifications speak the chosen language even after reboot | `RevatiApp.kt`, `LanguageInitTest` | [ ] | [ ] |
| 8.23 | In-app review request | `RateUsDialog.kt` (Play Review) | [ ] | [ ] |
| 8.24 | Works offline for everything except sign-in, backup, AI, news, ads | whole app | [ ] | [ ] |

## 9. Stored settings to carry (names from the Kotlin app)

Checked against the code on 25 Sep 2026; the Expo side is `src/lib/settings.ts`,
same key names, same fallbacks.

Prefs file `astroveda_prefs`: `city_name` (Jaipur), `city_name_hi`, `city_state`
(Rajasthan), `city_lat` / `city_lon` (**stored as Java floats**, 26.9124 /
75.7873 — the engine sees float-rounded coordinates, and the Expo app does the
same so the answers match), `use_24_hour_format` (false), `user_rashi_id` (1,
read by the daily worker but never written — see 7.5),
`is_onboarding_completed`, `is_first_run`, `is_discovery_completed`, `is_pro`,
`daily_notification_enabled` / `notification_hour` (7) / `notification_minute`
(0), `muhurat_notification_enabled` / `muhurat_notification_hour` (7) /
`muhurat_notification_minute` (0), `festival_notification_enabled`, `has_rated`,
`rate_dialog_dismissed_at`, `successful_lookup_count`, `session_action_count`,
`ai_calls_day`, `ai_calls_count`, and `user_rating_value` /
`user_feedback_text` (written by the in-app feedback form and never read or sent
anywhere).

**`app_language` is in a different file, `astro_prefs`** (`LanguageManager`),
not `astroveda_prefs`. A handover that copies one file loses the language.

Not stored at all: the chart style (North/South is chosen per view;
`chart_style` is only an analytics parameter).

Deliberately not carried as a setting in Expo: `is_pro` — PRO comes from the
store receipt every time, never from a value the user can edit. Needed again
only if Android ever moves over (`ANDROID_HANDOVER.md`).

## 10. Local database (Room schema v7) to mirror

| Table | Columns |
|---|---|
| `saved_kundali_profiles` | id, **uuid**, name, gender, dateOfBirth, timeOfBirth, placeOfBirth, latitude, longitude, notes, createdAt |
| `panchang_cache` | cacheKey + the full Panchang row (tithi, nakshatra, yoga, karan, their Hindi forms and end times, samvat, masa, paksha, sun/moon times, Rahu/Gulika/Yamaganda, Abhijit, Brahma, sun/moon sign, location, `planetsJson`, cachedAtTimestamp) |
| `horoscope_cache` | cacheKey, rashiId, names, symbol, element, ruler, rating, lucky number/colour/stone/time, five readings × 2 languages, period, cachedAtTimestamp |
| `saved_astrology_reports` | id, title, reportType, profileName, summaryText, detailedJsonData, createdAt |
| `recent_searches` | id, type, data, createdAt |

Caches need not be carried over; profiles, reports and recent searches must be
(if Android moves). The Expo schema starts at its own version 1 with real
migrations from day one — never a destructive fallback. Version 1
(`src/services/storage/schema.ts`, 25 Sep 2026) is the three user-data tables,
checked column for column and index for index against Room's exported v7
schema; the caches join when a screen needs them.
