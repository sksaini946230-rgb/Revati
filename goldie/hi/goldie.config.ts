/**
 * The Hindi (hi-IN) Play Store screenshots.
 *
 * A separate config in its own directory, not a second locale in the English
 * one, because goldie renders every locale from ONE set of raw captures — and
 * the Hindi tiles need screens captured with the app in Hindi. Living here, its
 * captures go to goldie/hi/out/raw and its tiles to goldie/hi/out/screenshots,
 * and the English set is never touched.
 *
 * Everything but the copy and the font is the English config's. Montserrat has
 * no Devanagari, so the stack falls through to macOS's Kohinoor Devanagari for
 * those glyphs. "system" does not work: the canvas never falls back to system
 * fonts on its own, and every headline came out as tofu boxes.
 *
 * Same rule as the English set: nothing on screen is real user data.
 */
import base from "../goldie.config.ts";

const hi: Record<string, { headline: string; subhead: string }> = {
  panchang: {
    headline: "आज का पंचांग, फ़ोन पर",
    subhead: "आपके शहर के लिए तिथि, नक्षत्र, योग और करण — बिना इंटरनेट।",
  },
  elements: {
    headline: "हर अंग, एक नज़र में",
    subhead: "सूर्योदय, सूर्यास्त, राहु काल, अभिजित और ब्रह्म मुहूर्त एक साथ।",
  },
  choghadiya: {
    headline: "सही समय जानें",
    subhead: "दिन और रात का चौघड़िया, और शुभ कार्यों के मुहूर्त।",
  },
  horoscope: {
    headline: "आपकी राशि, आपका राशिफल",
    subhead: "सभी 12 राशियां — दैनिक, साप्ताहिक और मासिक, शुभ अंक, रंग और रत्न के साथ।",
  },
  kundali: {
    headline: "जन्म कुण्डली, पलों में",
    subhead: "उत्तर और दक्षिण भारतीय चार्ट, लग्न, चंद्र राशि, नक्षत्र और दशा के साथ।",
  },
  matching: {
    headline: "36 गुण, ईमानदारी से",
    subhead: "पूरा अष्टकूट मिलान — नाड़ी, भकूट और मंगल दोष के विश्लेषण के साथ।",
  },
  numerology: {
    headline: "अंक, और उनका अर्थ",
    subhead: "नाम और जन्म तिथि से मूलांक और भाग्यांक, सरल शब्दों में।",
  },
  festivals: {
    headline: "कोई व्रत-त्योहार न छूटे",
    subhead: "हर त्योहार की तिथि उसके नियम से गणना — बरसों आगे तक।",
  },
};

const config = {
  ...base,
  locales: ["hi-IN"],
  theme: { ...base.theme, fontFamily: "Montserrat, \"Kohinoor Devanagari\"" },
  store: {
    ...base.store,
    subtitle: { "hi-IN": "कुण्डली और पंचांग" },
    description: {
      "hi-IN":
        "रेवती एक वैदिक पंचांग और कुण्डली ऐप है जो पूरी तरह आपके फ़ोन पर चलता है। ग्रहों की स्थिति लाहिड़ी अयनांश के साथ फ़ोन पर ही गणना होती है — पंचांग, चौघड़िया, जन्म कुण्डली और गुण मिलान के लिए इंटरनेट की ज़रूरत नहीं।\n\nआपके शहर का दैनिक पंचांग, बारह राशियों का राशिफल, उत्तर और दक्षिण भारतीय कुण्डली, नाड़ी, भकूट और मंगल दोष सहित 36 गुण मिलान, अंक ज्योतिष, और त्योहारों का कैलेंडर। हिन्दी और अंग्रेज़ी दोनों में।",
    },
  },
  scenes: base.scenes.map((s) => ({
    ...s,
    headline: { "hi-IN": hi[s.id].headline },
    subhead: { "hi-IN": hi[s.id].subhead },
  })),
};

export default config;
