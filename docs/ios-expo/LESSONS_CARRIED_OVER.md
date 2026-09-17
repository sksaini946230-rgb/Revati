# Lessons the Kotlin app paid for — rules for the Expo app

Every rule below is a bug that already shipped (or nearly shipped) in
`~/Revati`. The full stories are in `CLAUDE.md`; this is the checklist form.
Each rule gets a test or a lint rule in the Expo project where one is possible.

## Calculation

1. The engine takes **UT**, converted once. Local time moves the Moon ~3°.
2. The ascendant uses **mean** obliquity; do not "add nutation".
3. Every per-rashi value is checked **across all twelve signs** — the rating,
   colour, stone and lucky time each once collapsed to one or two values for
   everybody. Count distinct values; never judge from one sign.
4. Choghadiya day sequence steps 1 through the Chaldean cycle, night steps 5;
   Sunday was wrong once. Test the sequence, not the slot count.
5. Bhakoot has **three** doshas (2/12, 5/9, 6/8), in one shared list for score
   and label.
6. Guna Milan asks for a birth time; noon is only the fallback and the UI says
   what that costs.
7. The Moon's rashi/nakshatra round-trip through names must never silently fall
   back to Mesha/Ashwini — throw instead.
8. A muhurta is a fifteenth of the real day/night, not 48 minutes.
9. Adhika months are labelled; festivals skip them.
10. Muhurats are for the **selected city**, never a hardcoded Jaipur, and never
    computed on the render path.
11. No hardcoded year anywhere (validators, taglines, calendar titles).
12. Nakshatra progress is derived from the Moon, not a constant.

## Language and text

13. No bare user-facing string: every string has Hindi and English.
14. Chart glyphs and analytics names are **language-neutral tokens**, localised
    at draw time.
15. Background entry points (notifications, widgets) read the saved language
    themselves — they do not inherit it from a screen.
16. Widget labels are set from code, never left at a layout default.
17. A language switch regenerates generated text (Panchang, Rashifal, news) and
    clears AI answers rather than leaving the other language under a new
    heading.
18. Caches that hold localised text include the language (and clock format) in
    the key.
19. Devanagari needs room: no clipped matras, generous line height; a clipped
    heading at the top of a scroll is the viewport, not the font.
20. English labels are the long ones — check tab/segment bars in English at the
    smallest width with the widest label selected, and size bars from both
    languages at once so switching language never changes their height.

## Data

21. A profile's identity is its **uuid**; never merge on a row id or on name +
    date of birth (twins).
22. Never a destructive database fallback; every schema change has a migration
    and a migration test.
23. The horoscope cache is read by the exact keys of the current period —
    never a date window (it once served last week's "today" and then 24 rows).
24. Delete account: refresh the session first, then wipe, then delete.
25. Every path that can lose a profile reports a non-fatal error.
26. Numerology and every form start **empty** — never pre-filled with someone's
    real data.
27. Recent-search chips must fill the state the calculation reads, not a copy.

## AI

28. PRO-gated, rate-limited (3 s, 20/hour, 50/day persisted).
29. Keyed per sign + period + date.
30. Offline keyword fallback is a failure with retry, not an "insight".
31. Only name, DOB, lagna and numerology numbers are sent — never time or place
    of birth, never location. The privacy policy depends on it.
32. The crisis-response rule stays in the prompt.
33. App Check on every call; a side-loaded or unattested build is refused, and
    that is correct.

## Ads

34. Development builds only ever use test units; release refuses test ids and
    placeholders (a release without secrets must show no ads, not test ads).
35. Every ad placement goes through the one resolver; a test fails otherwise.
36. Banner takes no space until an ad loads; refresh interval lives in the
    AdMob console, not in code.
37. Retry no-fill on a bounded schedule; never give up for the session.
38. No full-screen ad on cold start, none between a tap and its result, never
    two stacked (one gate, floor between them).
39. Rewarded ad never costs the user the PDF; no-fill counts as earned.
40. App-open ad is shown after the app is truly foreground, and a failed show
    keeps the loaded ad.
41. Ad content rating **T**; G starved the Play app of ads.
42. Ad callbacks log loudly enough to be seen on a real device.
43. Test ads on normal WiFi — the owner's iPhone hotspot blocks ad domains.

## Platform and build

44. Signing certificates and their fingerprints are recorded when created;
    Google Sign-In and API-key restrictions depend on them.
45. The API key restriction is a whitelist — adding a platform means adding its
    app, never removing the restriction.
46. Debug/development builds talk to `revati-debug`, never to production data.
47. Store screenshots use sample data only; the owner's real profiles never
    appear in a capture.
48. The privacy policy, the store privacy forms and the code are checked
    against each other (port `PrivacyPolicyTest`'s idea: every SDK shipped is
    named, and named ones ship).
49. A check proves something only when the worst case is inside it.
50. "Tests pass" is not "the screen works" — check on a device, both languages,
    both themes.
