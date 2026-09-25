package com.example.astro

import com.example.data.model.FestivalData
import java.util.Calendar

/**
 * The festival table.
 *
 * Dates are **computed**, not typed in. Each entry carries the rule that
 * actually defines the festival — its lunar month, paksha, tithi, and the
 * window of the day tradition uses to settle which date it lands on — and
 * [FestivalCalculator] turns that into a date for any year.
 *
 * The old table hardcoded one year of dates. It ran out in November 2026, and
 * three of its twelve entries were wrong besides: Govardhan Puja, Chhath and
 * Gangaur were each a day early against published panchang. Both problems go
 * away when the dates are derived rather than transcribed.
 *
 * [getFestivals] returns this year and next, so the upcoming list never empties
 * as the year turns.
 */
object FestivalProvider {

    private data class Rule(
        val id: String,
        val nameEn: String,
        val nameHi: String,
        val monthNameHi: String,
        val pakshaHi: String,
        val tithiHi: String,
        val observance: FestivalCalculator.Observance,
        val regionFilter: String,
        val significanceEn: String,
        val significanceHi: String,
        val pujaVidhiHi: String,
        val pujaVidhiEn: String
    )

    private val RULES = listOf(
        Rule(
            id = "f1",
            nameEn = "Raksha Bandhan",
            nameHi = "रक्षाबंधन",
            monthNameHi = "श्रावण",
            pakshaHi = "शुक्ल पक्ष",
            tithiHi = "पूर्णिमा",
            observance = FestivalCalculator.Observance.RAKSHA_BANDHAN,
            regionFilter = "ALL",
            significanceEn = "Festival of sibling bond and protection.",
            significanceHi = "भाई-बहन के पवित्र प्रेम एवं रक्षा सूत्र बांधने का पावन पर्व।",
            pujaVidhiHi = "भद्रा रहित समय में बहनें अपने भाइयों को तिलक लगाकर दाहिनी कलाई पर राखी बांधें।",
            pujaVidhiEn = "Sisters apply a tilak and tie the rakhi on the brother's right wrist, in a period free of Bhadra."
        ),
        Rule(
            id = "f2",
            nameEn = "Krishna Janmashtami",
            nameHi = "श्री कृष्ण जन्माष्टमी",
            monthNameHi = "भाद्रपद",
            pakshaHi = "कृष्ण पक्ष",
            tithiHi = "अष्टमी",
            observance = FestivalCalculator.Observance.SUNRISE,
            regionFilter = "ALL",
            // Janmashtami is the one festival here kept on two consecutive days:
            // Smarta on the night Ashtami holds nishita, Vaishnava on the day it
            // holds sunrise. This is the sunrise day, and the text says so
            // rather than letting a reader think the other one is a mistake.
            significanceEn = "Birth anniversary of Lord Krishna. Smarta and Vaishnava traditions often keep it on consecutive days; the Smarta observance may fall the day before this one.",
            significanceHi = "भगवान श्री कृष्ण का रोहिणी नक्षत्र में पावन अवतरण दिवस। स्मार्त एवं वैष्णव परम्परा प्रायः दो लगातार दिन मनाती हैं; स्मार्त व्रत इससे एक दिन पूर्व भी हो सकता है।",
            pujaVidhiHi = "मध्यरात्रि 12:00 बजे बाल गोपाल का पञ्चामृत स्नान कराकर माखन-मिश्री भोग अर्पित करें।",
            pujaVidhiEn = "At midnight, bathe the infant Krishna in panchamrit and offer makhan-mishri."
        ),
        Rule(
            id = "f3",
            nameEn = "Ganesh Chaturthi",
            nameHi = "गणेश चतुर्थी",
            monthNameHi = "भाद्रपद",
            pakshaHi = "शुक्ल पक्ष",
            tithiHi = "चतुर्थी",
            observance = FestivalCalculator.Observance.MADHYAHNA,
            regionFilter = "ALL",
            significanceEn = "Festival welcoming Lord Ganesha.",
            significanceHi = "विघ्नहर्ता भगवान श्री गणेश जी की स्थापना एवं 10 दिवसीय जन्मोत्सव।",
            pujaVidhiHi = "शुभ मुहूर्त में गणपति जी की मिट्टी की मूर्ति स्थापित कर दुर्वा व मोदक अर्पित करें।",
            pujaVidhiEn = "Install the clay Ganesha at the chosen muhurat and offer durva grass and modak."
        ),
        Rule(
            id = "f4",
            nameEn = "Sharad Navratri Start",
            nameHi = "शारदीय नवरात्रि घटस्थापना",
            monthNameHi = "आश्विन",
            pakshaHi = "शुक्ल पक्ष",
            tithiHi = "प्रतिपदा",
            observance = FestivalCalculator.Observance.SUNRISE,
            regionFilter = "ALL",
            significanceEn = "Beginning of 9 sacred night festival of Goddess Durga.",
            significanceHi = "मां आदिशक्ति जगदम्बा की आराधना एवं 9 स्वरूपों का महापर्व।",
            pujaVidhiHi = "शुभ मुहूर्त में कलश स्थापना, अखण्ड ज्योति प्रज्वलन एवं नवार्ण मन्त्र जप।",
            pujaVidhiEn = "Kalash sthapana at the muhurat, light the akhand jyoti and chant the Navarna mantra."
        ),
        Rule(
            id = "f5",
            nameEn = "Dussehra / Vijayadashami",
            nameHi = "विजयादशमी / दशहरा",
            monthNameHi = "आश्विन",
            pakshaHi = "शुक्ल पक्ष",
            tithiHi = "दशमी",
            observance = FestivalCalculator.Observance.APARAHNA,
            regionFilter = "ALL",
            significanceEn = "Victory of Good over Evil.",
            significanceHi = "अधर्म पर धर्म एवं असत्य पर सत्य की विजय का महान प्रतीक।",
            pujaVidhiHi = "शमी पूजन, अस्त्र-शस्त्र पूजन एवं रावण दहन के उपरांत अपराजिता देवी पूजन।",
            pujaVidhiEn = "Shami puja and worship of tools and weapons, then Aparajita Devi puja after the Ravana effigy is burnt."
        ),
        Rule(
            id = "f6",
            nameEn = "Karwa Chauth",
            nameHi = "करवा चौथ",
            monthNameHi = "कार्तिक",
            pakshaHi = "कृष्ण पक्ष",
            tithiHi = "चतुर्थी",
            observance = FestivalCalculator.Observance.CHANDRODAYA,
            regionFilter = "NORTH",
            significanceEn = "Fasting for spouse's longevity.",
            significanceHi = "पति की दीर्घायु एवं अखण्ड सौभाग्य हेतु सुहागिनों का निर्जला व्रत।",
            pujaVidhiHi = "सायंकाल माता पार्वती व शिव परिवार का पूजन तथा चन्द्रोदय पर अर्घ्यदान।",
            pujaVidhiEn = "Evening worship of Parvati and the Shiva family, then the arghya offering at moonrise."
        ),
        Rule(
            id = "f7",
            nameEn = "Dhanteras",
            nameHi = "धनतेरस / धन्वन्तरि जयन्ती",
            monthNameHi = "कार्तिक",
            pakshaHi = "कृष्ण पक्ष",
            tithiHi = "त्रयोदशी",
            observance = FestivalCalculator.Observance.PRADOSH,
            regionFilter = "ALL",
            significanceEn = "Festival of prosperity and health god.",
            significanceHi = "आरोग्य के देवता भगवान धन्वन्तरि व कुबेर देव का प्रकटीकरण पर्व।",
            pujaVidhiHi = "नवीन आभूषण, बर्तन एवं झाड़ू क्रय मुहूर्त। प्रदोष काल में यम दीपदान।",
            pujaVidhiEn = "An auspicious time to buy new jewellery, vessels and a broom. Offer the Yama lamp at pradosh."
        ),
        Rule(
            id = "f8",
            nameEn = "Diwali / Deepawali",
            nameHi = "दीपावली",
            monthNameHi = "कार्तिक",
            pakshaHi = "अमावस्या",
            tithiHi = "अमावस्या",
            observance = FestivalCalculator.Observance.PRADOSH,
            regionFilter = "ALL",
            significanceEn = "Festival of Lights and Wealth Goddess Lakshmi.",
            significanceHi = "प्रकाश का महापर्व, माता महालक्ष्मी एवं भगवान श्री गणेश की आराधना।",
            pujaVidhiHi = "प्रदोष काल एवं स्थिर वृषभ लग्न में महालक्ष्मी पूजन, बही-खाता पूजन व दीपमाला प्रज्वलन।",
            pujaVidhiEn = "Mahalakshmi puja in pradosh kaal and the fixed Taurus lagna, worship of the account books, and rows of lamps."
        ),
        Rule(
            id = "f9",
            nameEn = "Govardhan Puja / Annakut",
            nameHi = "गोवर्धन पूजा / अन्नकूट",
            monthNameHi = "कार्तिक",
            pakshaHi = "शुक्ल पक्ष",
            tithiHi = "प्रतिपदा",
            observance = FestivalCalculator.Observance.SUNRISE,
            regionFilter = "NORTH",
            significanceEn = "Worship of Govardhan hill and nature.",
            significanceHi = "भगवान श्री कृष्ण द्वारा गोवर्धन पर्वत धारण एवं प्रकृति संवर्धन उत्सव।",
            pujaVidhiHi = "गोबर से गोवर्धन पर्वत बनाकर पूजन, 56 भोग अथवा अन्नकूट अर्पित करें।",
            pujaVidhiEn = "Shape a Govardhan hill from cow dung and offer the 56 bhog or the annakut."
        ),
        Rule(
            id = "f10",
            nameEn = "Chhath Puja",
            nameHi = "छठ पूजा",
            monthNameHi = "कार्तिक",
            pakshaHi = "शुक्ल पक्ष",
            tithiHi = "षष्ठी",
            observance = FestivalCalculator.Observance.SUNRISE,
            regionFilter = "NORTH",
            significanceEn = "Sun worship festival.",
            significanceHi = "प्रत्यक्ष देवता भगवान सूर्य एवं छठी मइया का अति कठिन 36 घंटे का निर्जला महापर्व।",
            pujaVidhiHi = "नदी/तालाब तट पर अस्ताचलगामी एवं उदीयमान सूर्यदेव को अर्घ्यदान।",
            pujaVidhiEn = "Offer arghya to the setting and then the rising Sun at a river or pond bank."
        ),
        Rule(
            id = "f11",
            nameEn = "Gangaur Teej",
            nameHi = "गणगौर तीज (राजस्थान)",
            monthNameHi = "चैत्र",
            pakshaHi = "शुक्ल पक्ष",
            tithiHi = "तृतीया",
            observance = FestivalCalculator.Observance.SUNRISE,
            regionFilter = "RAJASTHAN",
            significanceEn = "Rajasthan's signature Gauri-Isar festival.",
            significanceHi = "राजस्थान का अति प्रसिद्ध लोकपर्व - माता गवरजा (पार्वती) एवं इसर जी (शिव) की भक्ति।",
            pujaVidhiHi = "होलिका की राख से गणगौर बनाकर 16 दिन सुहाग गीत गाकर पूजन व विसर्जन।",
            pujaVidhiEn = "Shape Gangaur from Holika ash, sing the suhag songs for sixteen days, then the visarjan."
        ),
        Rule(
            id = "f12",
            nameEn = "Teej Utsav (Hariyali Teej)",
            nameHi = "हरियाली तीज / कजरी तीज (राजस्थान)",
            monthNameHi = "श्रावण",
            pakshaHi = "शुक्ल पक्ष",
            tithiHi = "तृतीया",
            observance = FestivalCalculator.Observance.SUNRISE,
            regionFilter = "RAJASTHAN",
            significanceEn = "Monsoon swing festival of Rajasthan.",
            significanceHi = "राजस्थान एवं उत्तर भारत की महिलाओं का लहरिया, झूले एवं सुहाग पर्व।",
            pujaVidhiHi = "हरे वस्त्र-झूले धारण कर शिव-पार्वती पूजन एवं घेवर-फेणी मिष्ठान भोग।",
            pujaVidhiEn = "Wear green, take to the swings, worship Shiva and Parvati, and offer ghevar and pheni."
        )
    )

    /**
     * Every festival whose date falls in the current year or the next one,
     * earliest first. Two years is what keeps "upcoming" populated in December.
     */
    fun getFestivals(): List<FestivalData> {
        val thisYear = Calendar.getInstance(AstroTime.IST).get(Calendar.YEAR)
        return (thisYear..thisYear + 1)
            .flatMap { year -> RULES.mapNotNull { build(it, year) } }
            .sortedBy { it.dateIso }
    }

    private fun build(rule: Rule, year: Int): FestivalData? {
        val masa = FestivalCalculator.masaIndexFor(rule.monthNameHi) ?: return null
        val tithi = FestivalCalculator.tithiNumberFor(rule.pakshaHi, rule.tithiHi) ?: return null
        val date = FestivalCalculator.dateFor(masa, tithi, year, rule.observance) ?: return null

        val day = date.get(Calendar.DAY_OF_MONTH)
        val month = date.get(Calendar.MONTH)
        val weekday = date.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY

        return FestivalData(
            // The id has to stay unique across the two years, or the second
            // year's Diwali collides with the first year's in any keyed list.
            id = "${rule.id}_$year",
            nameEn = rule.nameEn,
            nameHi = rule.nameHi,
            dateString = "$day ${AstroNames.GREGORIAN_MONTH_HI[month]} $year",
            dateIso = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, day),
            dayNameHi = AstroNames.VARA_HI[weekday.coerceIn(0, 6)],
            monthNameHi = rule.monthNameHi,
            pakshaHi = rule.pakshaHi,
            tithiHi = rule.tithiHi,
            regionFilter = rule.regionFilter,
            significanceEn = rule.significanceEn,
            significanceHi = rule.significanceHi,
            pujaVidhiHi = rule.pujaVidhiHi,
            pujaVidhiEn = rule.pujaVidhiEn
        )
    }
}
