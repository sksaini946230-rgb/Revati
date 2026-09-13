# Release notes — how to write them, and what has been said

Play takes release notes per language, wrapped in locale tags, **500 characters
maximum each**. This app ships `en-US` and `hi-IN`, and Play will not let a
release out with only one of them filled in.

## The shape

```
<en-US>
…
</en-US>
<hi-IN>
…
</hi-IN>
```

## What belongs in them

Release notes are read by someone deciding whether to press Update. They are not
a changelog for the developer — the git history is that, and it is better at it.

- **Say what changed for them, not what changed in the code.** "Daily horoscope
  now differs for every rashi" is a reason to update. "Fixed a derived value
  that cancelled out" is not.
- **Lead with the thing they would notice.** If a reading was wrong and is now
  right, that is the first line.
- **Both languages carry the same promises.** A Hindi user reading a shorter,
  vaguer version of the English notes has been told less, and this app's whole
  point is that neither language is the afterthought.
- **Do not claim what is not there yet.** PRO is not purchasable until the
  merchant account exists; notes must not imply otherwise.
- **Keep the greeting.** "नमस्ते 🙏" costs eight characters and sets the tone the
  rest of the app keeps.

## Reusable skeleton

```
<en-US>
Namaste 🙏

<one line on the biggest user-visible change>

• <change>
• <change>
• <change>

Thank you for using Revati.
</en-US>
<hi-IN>
नमस्ते 🙏

<सबसे बड़े बदलाव की एक पंक्ति>

• <बदलाव>
• <बदलाव>
• <बदलाव>

रेवती चुनने के लिए धन्यवाद।
</hi-IN>
```

## Said so far

### Next release / 2.1

**Not uploaded yet.** Built on 13 Sep 2026 from the commit that added this
entry's parent; the versionCode is whatever `git rev-list --count HEAD` said at
build time. Muhurat leads because it was wrong for everyone outside Jaipur, on a
screen people use to decide when to begin something.

```
<en-US>
Namaste 🙏

Muhurat and horoscope are now truly for you:

• Shubh Muhurat is worked out for your own city, not Jaipur
• Daily horoscope now changes every day, not once a week
• Abhijit and Brahma muhurta follow the real length of the day
• Adhika (extra) lunar months are now labelled
• Privacy policy and terms follow dark mode
• Smaller, lighter app

Thank you for using Revati.
</en-US>
<hi-IN>
नमस्ते 🙏

मुहूर्त और राशिफल अब सचमुच आपके लिए:

• शुभ मुहूर्त अब आपके अपने शहर के अनुसार, जयपुर के नहीं
• दैनिक राशिफल अब हर दिन बदलता है, हफ़्ते में एक बार नहीं
• अभिजित व ब्रह्म मुहूर्त दिन की असली लंबाई से
• अधिक मास का नाम अब साफ़ लिखा आता है
• गोपनीयता नीति व नियम डार्क मोड में भी
• ऐप पहले से छोटा व हल्का

रेवती चुनने के लिए धन्यवाद।
</hi-IN>
```

### versionCode 156 / 2.0

**Uploaded** by the owner on 8 Sep 2026, together with a new set of eight
English store screenshots.

One user-visible change, so the notes say one thing. The bottom bar was rebuilt
to a design the owner supplied: every tab carries its label, on a floating
capsule. Everything else in this build is the arithmetic that keeps it from
clipping, which is not something anyone presses Update for.

The version *name* is unchanged — 10, 11 and 156 are all 2.0. Play allows that,
and it is right here: from a user's side this is the same release with the
navigation redone. The leap in `versionCode` is not a leap in the app. The
number is `git rev-list --count HEAD` now, so it is the commit count and nothing
more; it was typed by hand twice and collided with an upload both times. See
CLAUDE.md, "`versionCode` is derived, never typed".

```
<en-US>
Namaste 🙏

A new look for the bottom navigation:

• Every tab now shows its name, not only the one you are on
• A rounded, floating bar that sits clear of the screen edges
• Fits properly on small screens and at large text sizes
• Identical in Hindi and English, and in light and dark

Thank you for using Revati.
</en-US>
<hi-IN>
नमस्ते 🙏

नीचे के नेविगेशन का नया रूप:

• अब हर टैब का नाम दिखता है, केवल चुने हुए का नहीं
• गोल, तैरता हुआ बार जो किनारों से हटकर बैठता है
• छोटी स्क्रीन और बड़े टेक्स्ट साइज़ पर भी सही
• हिन्दी-अंग्रेज़ी और लाइट-डार्क, हर तरह एक जैसा

रेवती चुनने के लिए धन्यवाद।
</hi-IN>
```

en-US is 313 characters and hi-IN is 262, against the 500 Play allows.

### versionCode 10 / 2.0

**Uploaded** by the owner, on or before 8 Sep 2026. Everything in the 1.4 list
below, which never went up, plus what a full pass on a real phone found: Guna
Milan was
missing one of the three Bhakoot doshas and never asked for a birth time, the
night Choghadiya could not be opened at all, and the lucky time was the same for
four rashis at once.

Bhakoot leads because it changes an answer people act on. The night Choghadiya
is second because it is a whole half of a screen that has never worked.

```
<en-US>
Namaste 🙏

Kundli Milan and Choghadiya are both more accurate this time:

• Guna Milan now catches all three Bhakoot doshas — 5/9 Nav-Pancham was being scored as a full match
• Guna Milan accepts a birth time, which decides 21 of the 36 gunas
• Night Choghadiya opens properly — the tab used to show daytime hours
• Daily horoscope: lucky time now really differs by rashi
• Recent searches on Kundli Milan work again
• Steadier layout on compact screens

Thank you for using Revati.
</en-US>
<hi-IN>
नमस्ते 🙏

इस बार कुण्डली मिलान और चौघड़िया दोनों अधिक सटीक:

• गुण मिलान में तीनों भकूट दोष — 5/9 नवपंचम अब तक पूरे गुण दे रहा था
• गुण मिलान में जन्म समय भी — 36 में से 21 गुण इसी पर आधारित
• रात का चौघड़िया अब सही खुलता है — पहले दिन के ही समय दिखते थे
• दैनिक राशिफल: शुभ समय अब वाकई हर राशि के लिए अलग
• कुण्डली मिलान में हाल की खोजें फिर से काम करती हैं
• छोटी स्क्रीन पर बेहतर लेआउट

रेवती चुनने के लिए धन्यवाद।
</hi-IN>
```

### versionCode 9 / 1.4

Everything since the 1.2 that production is on: the per-rashi horoscope fix,
numerology no longer pre-filled with someone else's details, profile
export/import, in-app support, narrow-screen layout fixes, and notifications and
widgets finally honouring the chosen language.

```
<en-US>
Namaste 🙏

This update makes your readings truly your own:

• Daily horoscope now differs for every rashi — rating, lucky colour and stone come from your sign's own lord
• Numerology starts blank, so the result is yours alone
• Export and import your saved kundli profiles — no account needed
• Contact support right from Settings
• Smaller, faster app, and a better fit on compact screens
• Notifications and widgets now follow your chosen language

Thank you for using Revati.
</en-US>
<hi-IN>
नमस्ते 🙏

इस अपडेट में आपका फलादेश सचमुच आपका अपना:

• दैनिक राशिफल अब हर राशि के लिए अलग — रेटिंग, शुभ रंग व रत्न आपकी राशि के स्वामी से
• अंक ज्योतिष अब खाली शुरू होता है, परिणाम केवल आपका
• सहेजी गई कुण्डलियां एक्सपोर्ट व इम्पोर्ट करें — बिना खाते के
• सेटिंग्स से सीधे सहायता संपर्क
• ऐप छोटा व तेज़, छोटी स्क्रीन पर बेहतर
• सूचनाएं व विजेट अब आपकी चुनी हुई भाषा में

रेवती चुनने के लिए धन्यवाद।
</hi-IN>
```
