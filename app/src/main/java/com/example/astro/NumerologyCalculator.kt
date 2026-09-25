package com.example.astro

import com.example.data.model.NumerologyData

object NumerologyCalculator {

    private val PLANETS_MAP = mapOf(
        1 to "सूर्य", 2 to "चन्द्र", 3 to "गुरु",
        4 to "राहु", 5 to "बुध", 6 to "शुक्र",
        7 to "केतु", 8 to "शनि", 9 to "मंगल"
    )

    private val READINGS = mapOf(
        1 to Pair("मूलांक 1 वाले व्यक्ति साहसी, नेतृत्व क्षमता से परिपूर्ण एवं स्वाभिमानी होते हैं। सूर्य देव की कृपा से प्रशासनिक एवं प्रबंधन कार्यों में उच्च सफलता मिलती है।", "भाग्यशाली दिन: रविवार एवं सोमवार। शुभ रंग: पीला एवं केसरिया।"),
        2 to Pair("मूलांक 2 वाले व्यक्ति कल्पनाशील, कलात्मक एवं भावुक होते हैं। चन्द्र देव की कृपा से रचनात्मक क्षेत्रों, काव्य, संगीत एवं जनसेवा में विशेष सफलता प्राप्त होती है।", "भाग्यशाली दिन: सोमवार एवं रविवार। शुभ रंग: सफेद एवं हल्का हरा।"),
        3 to Pair("मूलांक 3 के स्वामी देवगुरु बृहस्पति हैं। आप ज्ञानी, न्यायप्रिय, आध्यात्मिक एवं शिक्षण कार्य में निपुण होते हैं। समाज में मान-सम्मान प्राप्त होता है।", "भाग्यशाली दिन: गुरुवार एवं शुक्रवार। शुभ रंग: पीला एवं सुनहरा।"),
        4 to Pair("मूलांक 4 के स्वामी राहु हैं। आप व्यावहारिक, विश्लेषणात्मक एवं लीक से हटकर सोचने वाले होते हैं। तकनीकी, अनुसंधान एवं मीडिया क्षेत्र में सफलता मिलती है।", "भाग्यशाली दिन: रविवार एवं शनिवार। शुभ रंग: नीला एवं सलेटी।"),
        5 to Pair("मूलांक 5 के स्वामी बुध देव हैं। आप तीव्र बुद्धि, चतुर वाकपटुता एवं व्यापारिक कुशलता के धनी होते हैं। शेयर बाज़ार, कंसल्टेंसी एवं आईटी में प्रगति करते हैं।", "भाग्यशाली दिन: बुधवार एवं शुक्रवार। शुभ रंग: हरा एवं हल्का पीला।"),
        6 to Pair("मूलांक 6 के स्वामी दैत्यगुरु शुक्र हैं। आप सौंदर्यप्रेमी, कलात्मक, आकर्षक व्यक्तित्व एवं ऐश्वर्यप्रिय होते हैं। फैशन, डिजाइनिंग एवं फिल्म क्षेत्र में ख्याति मिलती है।", "भाग्यशाली दिन: शुक्रवार एवं मंगलवार। शुभ रंग: गुलाबी एवं सफेद।"),
        7 to Pair("मूलांक 7 के स्वामी केतु हैं। आप गूढ़ विद्या, शोध, दर्शनशास्त्र एवं योग-अध्यात्म में गहरा रुझान रखते हैं। स्वतंत्र विचारक एवं दूरदर्शी होते हैं।", "भाग्यशाली दिन: रविवार एवं गुरुवार। शुभ रंग: हल्का पीला एवं सफेद।"),
        8 to Pair("मूलांक 8 के स्वामी न्यायप्रिय शनिदेव हैं। आप कर्मठ, धैर्यवान, संघर्षशील एवं दूरदर्शी होते हैं। जीवन के उत्तरार्ध में विशाल संपत्ति व प्रतिष्ठा अर्जित करते हैं।", "भाग्यशाली दिन: शनिवार एवं शुक्रवार। शुभ रंग: गहरा नीला एवं काला।"),
        9 to Pair("मूलांक 9 के स्वामी पराक्रमी मंगल देव हैं। आप ऊर्जावान, निर्भीक, देशभक्त एवं रक्षक स्वभाव के होते हैं। सेना, पुलिस, खेल एवं रियल एस्टेट में नाम कमाते हैं।", "भाग्यशाली दिन: मंगलवार एवं रविवार। शुभ रंग: लाल एवं नारंगी।")
    )

    /** Matches the "शुभ रंग" already stated in each moolank's own reading text. */
    private val LUCKY_COLOURS_HI = mapOf(
        1 to "पीला एवं केसरिया",
        2 to "सफेद एवं हल्का हरा",
        3 to "पीला एवं सुनहरा",
        4 to "नीला एवं सलेटी",
        5 to "हरा एवं हल्का पीला",
        6 to "गुलाबी एवं सफेद",
        7 to "हल्का पीला एवं सफेद",
        8 to "गहरा नीला एवं काला",
        9 to "लाल एवं नारंगी"
    )

    private val PLANETS_MAP_EN = mapOf(
        1 to "Sun", 2 to "Moon", 3 to "Jupiter", 4 to "Rahu", 5 to "Mercury",
        6 to "Venus", 7 to "Ketu", 8 to "Saturn", 9 to "Mars"
    )

    /** English counterparts of READINGS: reading text, then lucky days. */
    private val READINGS_EN = mapOf(
        1 to Pair(
            "Moolank 1 people are bold, self-assured and natural leaders. With the Sun's favour they do well in administration, management and any role where someone has to decide and be answerable for it.",
            "Lucky days: Sunday and Monday."),
        2 to Pair(
            "Moolank 2 people are imaginative, artistic and quick to feel. The Moon's influence suits creative work, writing, music and anything involving care for others.",
            "Lucky days: Monday and Sunday."),
        3 to Pair(
            "Moolank 3 is ruled by Jupiter. You tend to be knowledgeable, fair-minded, drawn to the spiritual, and good at teaching. Respect in your community usually follows.",
            "Lucky days: Thursday and Friday."),
        4 to Pair(
            "Moolank 4 is ruled by Rahu. You are practical, analytical and inclined to think against the grain. Technology, research and media suit you.",
            "Lucky days: Sunday and Saturday."),
        5 to Pair(
            "Moolank 5 is ruled by Mercury. Quick-minded, persuasive and commercially sharp — consulting, markets and IT tend to reward you.",
            "Lucky days: Wednesday and Friday."),
        6 to Pair(
            "Moolank 6 is ruled by Venus. You have an eye for beauty, an easy charm and a taste for comfort. Fashion, design and film are natural ground.",
            "Lucky days: Friday and Tuesday."),
        7 to Pair(
            "Moolank 7 is ruled by Ketu. You lean towards research, philosophy, yoga and the esoteric. An independent thinker who sees further than most.",
            "Lucky days: Sunday and Thursday."),
        8 to Pair(
            "Moolank 8 is ruled by Saturn, the taskmaster. Hard-working, patient and built for the long haul — the second half of life usually brings the standing and the security.",
            "Lucky days: Saturday and Friday."),
        9 to Pair(
            "Moolank 9 is ruled by Mars. Energetic, fearless and protective by instinct. The forces, sport and real estate all suit that temperament.",
            "Lucky days: Tuesday and Sunday.")
    )

    private val LUCKY_COLOURS_EN = mapOf(
        1 to "Yellow and saffron", 2 to "White and pale green", 3 to "Yellow and gold",
        4 to "Blue and grey", 5 to "Green and light yellow", 6 to "Pink and white",
        7 to "Pale yellow and white", 8 to "Deep blue and black", 9 to "Red and orange"
    )

    fun calculateNumerology(name: String, dobString: String): NumerologyData {
        val digits = dobString.filter { it.isDigit() }

        // Moolank = Single digit sum of birth DAY
        val dayPart = dobString.split("-").getOrNull(2)?.filter { it.isDigit() } ?: "1"
        var moolankSum = dayPart.sumOf { it.toString().toInt() }
        while (moolankSum > 9) {
            moolankSum = moolankSum.toString().sumOf { it.toString().toInt() }
        }
        val moolank = if (moolankSum == 0) 1 else moolankSum

        // Bhagyank = Single digit sum of FULL DOB
        var bhagyankSum = digits.sumOf { it.toString().toInt() }
        while (bhagyankSum > 9) {
            bhagyankSum = bhagyankSum.toString().sumOf { it.toString().toInt() }
        }
        val bhagyank = if (bhagyankSum == 0) 1 else bhagyankSum

        // Name number.
        // Devanagari is transliterated first: the Chaldean table below only covers
        // A-Z, so a name typed in Hindi used to sum to zero and always come out as 1.
        val latinName = if (DevanagariTransliterator.containsDevanagari(name)) {
            DevanagariTransliterator.transliterate(name)
        } else {
            name
        }
        val nameDigits = latinName.uppercase().map { char ->
            when (char) {
                'A', 'I', 'J', 'Q', 'Y' -> 1
                'B', 'K', 'R' -> 2
                'C', 'G', 'L', 'S' -> 3
                'D', 'M', 'T' -> 4
                'E', 'H', 'N', 'X' -> 5
                'U', 'V', 'W' -> 6
                'O', 'Z' -> 7
                'F', 'P' -> 8
                else -> 0
            }
        }.sum()
        var nameNum = nameDigits
        while (nameNum > 9) {
            nameNum = nameNum.toString().sumOf { it.toString().toInt() }
        }
        if (nameNum == 0) nameNum = 1

        val rulingPlanet = PLANETS_MAP[moolank] ?: "सूर्य"
        val rulingPlanetEn = PLANETS_MAP_EN[moolank] ?: "Sun"
        val readingPair = READINGS[moolank] ?: READINGS[1]!!
        val readingPairEn = READINGS_EN[moolank] ?: READINGS_EN[1]!!

        val friendly = when (moolank) {
            1 -> listOf(1, 2, 3, 5, 9)
            2 -> listOf(1, 2, 3, 5)
            3 -> listOf(1, 2, 3, 9)
            4 -> listOf(1, 5, 6, 7)
            5 -> listOf(1, 5, 6)
            6 -> listOf(5, 6, 8)
            7 -> listOf(1, 4, 5, 7)
            8 -> listOf(5, 6)
            else -> listOf(1, 3, 9)
        }

        val enemy = when (moolank) {
            1 -> listOf(8)
            2 -> listOf(8)
            3 -> listOf(6)
            4 -> listOf(2, 8)
            5 -> listOf(2)
            6 -> listOf(3)
            7 -> listOf(2)
            8 -> listOf(1, 2, 9)
            else -> listOf(8)
        }

        return NumerologyData(
            personName = name,
            dateOfBirth = dobString,
            moolank = moolank,
            bhagyank = bhagyank,
            nameNumber = nameNum,
            rulingPlanetHi = rulingPlanet,
            rulingPlanetEn = rulingPlanetEn,
            luckyDaysHi = readingPair.second,
            luckyDaysEn = readingPairEn.second,
            // This was hardcoded to the same string for all nine moolanks, which
            // contradicted the per-moolank "शुभ रंग" already written into each
            // reading above — the app disagreed with itself on the same screen.
            luckyColorsHi = LUCKY_COLOURS_HI[moolank] ?: "पीला एवं सुनहरा",
            luckyColorsEn = LUCKY_COLOURS_EN[moolank] ?: "Yellow and gold",
            friendlyNumbers = friendly,
            enemyNumbers = enemy,
            moolankReadingHi = readingPair.first,
            moolankReadingEn = readingPairEn.first,
            bhagyankReadingHi = "भाग्यांक $bhagyank आपके जीवन पथ को दर्शाती है। व्यवसाय एवं व्यक्तिगत जीवन में मूलांक $moolank के स्वामी $rulingPlanet की दशा शुभ फल देगी।",
            bhagyankReadingEn = "Bhagyank $bhagyank describes the path your life tends to take. In work and in personal matters, periods ruled by $rulingPlanetEn — the lord of Moolank $moolank — should go well for you."
        )
    }
}
