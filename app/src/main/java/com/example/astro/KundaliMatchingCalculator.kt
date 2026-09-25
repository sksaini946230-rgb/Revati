package com.example.astro

import com.example.util.LanguageManager
import com.example.data.model.GunaKootDetail
import com.example.data.model.GunaMatchingResult

/**
 * The eight koots and their tables are `internal` rather than private so that
 * `ClassicalTablesTest` can check each one against the rule it comes from
 * instead of against a copy of itself. The Bhakoot dosha set was wrong once and
 * the only test touching Guna Milan at the time counted slots.
 */
object KundaliMatchingCalculator {

    // Varna names & ranks
    internal val VARNA_RANKS = listOf(3, 2, 1, 4, 3, 2, 1, 4, 3, 2, 1, 4) // 4:Brahmin, 3:Kshatriya, 2:Vaishya, 1:Shudra
    private val VARNA_NAMES_HI = mapOf(4 to "ब्राह्मण", 3 to "क्षत्रिय", 2 to "वैश्य", 1 to "शूद्र")
    private val VARNA_NAMES_EN = mapOf(4 to "Brahmin", 3 to "Kshatriya", 2 to "Vaishya", 1 to "Shudra")

    // Vashya groups by Rashi
    // 0:Chatushpad, 1:Manav, 2:Jalchar, 3:Vanchar, 4:Keet
    internal val VASHYA_GROUP = listOf(0, 0, 1, 2, 3, 1, 1, 4, 1, 0, 1, 2)
    private val VASHYA_NAMES_HI = listOf("चतुष्पाद", "मानव", "जलचर", "वनचर", "कीट")
    private val VASHYA_NAMES_EN = listOf("Chatushpad (Quadruped)", "Manav (Human)", "Jalchar (Aquatic)", "Vanchar (Wild/Lion)", "Keet (Insect)")

    // 27 Nakshatras Yoni Animals (14 Animals)
    // 0:Ashwa, 1:Gaja, 2:Mesha, 3:Sarpa, 4:Shwan, 5:Marjara, 6:Mushaka, 7:Gau, 8:Mahisha, 9:Vyaghra, 10:Mriga, 11:Vanara, 12:Nakula, 13:Simha
    internal val NAKSHATRA_YONI = listOf(
        0, 1, 2, 3, 3, 4, 5, 2, 5, 6, 6, 7, 8, 9, 8, 9, 10, 10, 4, 11, 12, 11, 13, 0, 13, 7, 1
    )
    private val YONI_NAMES_HI = listOf(
        "अश्व", "गज", "मेष", "सर्प", "श्वान",
        "मार्जार", "मूषक", "गौ", "महिष", "व्याघ्र",
        "मृग", "वानर", "नकुल", "सिंह"
    )
    private val YONI_NAMES_EN = listOf(
        "Horse (Ashwa)", "Elephant (Gaja)", "Sheep (Mesha)", "Serpent (Sarpa)", "Dog (Shwan)",
        "Cat (Marjara)", "Rat (Mushaka)", "Cow (Gau)", "Buffalo (Mahisha)", "Tiger (Vyaghra)",
        "Deer (Mriga)", "Monkey (Vanara)", "Mongoose (Nakula)", "Lion (Simha)"
    )

    // 27 Nakshatras Gana (0:Deva, 1:Manushya, 2:Rakshasa)
    internal val NAKSHATRA_GANA = listOf(
        0, 1, 2, 1, 0, 1, 0, 0, 2, 2, 1, 1, 0, 2, 0, 2, 0, 2, 2, 1, 1, 0, 2, 2, 1, 1, 0
    )
    private val GANA_NAMES_HI = listOf("देव", "मनुष्य", "राक्षस")
    private val GANA_NAMES_EN = listOf("Deva (Divine)", "Manushya (Human)", "Rakshasa (Demonic)")

    // 27 Nakshatras Nadi (0:Adi, 1:Madhya, 2:Antya)
    internal val NAKSHATRA_NADI = listOf(
        0, 1, 2, 2, 1, 0, 0, 1, 2, 2, 1, 0, 0, 1, 2, 2, 1, 0, 0, 1, 2, 2, 1, 0, 0, 1, 2
    )
    private val NADI_NAMES_HI = listOf("आद्य", "मध्य", "अन्त्य")
    private val NADI_NAMES_EN = listOf("Adi (Vata)", "Madhya (Pitta)", "Antya (Kapha)")

    /**
     * The three Bhakoot doshas, as one-way rashi distances.
     *
     * 2/12 Dwirdwadasha, 5/9 Nav-Pancham, 6/8 Shadashtaka. Both ends of each
     * pair appear because [calculateBhakoot] measures the distance in one
     * direction only. Named here rather than inlined so the dosha label below
     * and the score cannot disagree about which distances are a dosha —
     * they did, and the label had no branch for Nav-Pancham at all.
     */
    internal val BHAKOOT_DOSHA_DISTANCES = listOf(2, 12, 5, 9, 6, 8)

    private val RASHI_NAMES_HI = listOf(
        "मेष", "वृषभ", "मिथुन", "कर्क",
        "सिंह", "कन्या", "तुला", "वृश्चिक",
        "धनु", "मकर", "कुंभ", "मीन"
    )
    private val RASHI_NAMES_EN = listOf(
        "Aries", "Taurus", "Gemini", "Cancer",
        "Leo", "Virgo", "Libra", "Scorpio",
        "Sagittarius", "Capricorn", "Aquarius", "Pisces"
    )

    /**
     * Ashtakoot Guna Milan.
     *
     * The eight koots depend only on each person's Moon — sign and nakshatra —
     * which is why the birth place does not change the 36-point score. Mangal
     * Dosha is different: it is read from Mars' house relative to the ASCENDANT,
     * and the Ascendant moves a whole sign every two hours and shifts with
     * latitude. This used to pass the literal string "Default" as the place, so
     * both charts were cast for Jaipur and the Manglik verdict was stated as fact
     * for two people who were almost certainly not born there.
     *
     * Pass [boyLat]/[boyLng] and [girlLat]/[girlLng] to get a real Manglik reading.
     * Without them the score is still correct and the Manglik section says plainly
     * that it needs birth place and exact time, instead of guessing.
     */
    fun matchKundali(
        boyName: String,
        boyDob: String,
        boyTob: String,
        girlName: String,
        girlDob: String,
        girlTob: String,
        boyLat: Double? = null,
        boyLng: Double? = null,
        girlLat: Double? = null,
        girlLng: Double? = null
    ): GunaMatchingResult {
        val canJudgeManglik = boyLat != null && boyLng != null && girlLat != null && girlLng != null

        val boyChart = KundaliCalculator.generateKundali(
            boyName, boyDob, boyTob, "—",
            boyLat ?: BirthData.FALLBACK_LAT, boyLng ?: BirthData.FALLBACK_LNG
        )
        val girlChart = KundaliCalculator.generateKundali(
            girlName, girlDob, girlTob, "—",
            girlLat ?: BirthData.FALLBACK_LAT, girlLng ?: BirthData.FALLBACK_LNG
        )

        val boyMoonRashiIdx = boyChart.moonRashiHi.let { KundaliCalculator.RASHI_SHORT_HI.indexOf(it) }.coerceAtLeast(0)
        val girlMoonRashiIdx = girlChart.moonRashiHi.let { KundaliCalculator.RASHI_SHORT_HI.indexOf(it) }.coerceAtLeast(0)

        val boyNakshatraIdx = boyChart.moonNakshatraHi.let { KundaliCalculator.NAKSHATRAS.indexOf(it) }.coerceAtLeast(0)
        val girlNakshatraIdx = girlChart.moonNakshatraHi.let { KundaliCalculator.NAKSHATRAS.indexOf(it) }.coerceAtLeast(0)

        // 1. Varna (1 pt)
        val varnaPoints = calculateVarna(boyMoonRashiIdx, girlMoonRashiIdx)
        // 2. Vashya (2 pts)
        val vashyaPoints = calculateVashya(boyMoonRashiIdx, girlMoonRashiIdx)
        // 3. Tara (3 pts)
        val taraPoints = calculateTara(boyNakshatraIdx, girlNakshatraIdx)
        // 4. Yoni (4 pts)
        val yoniPoints = calculateYoni(boyNakshatraIdx, girlNakshatraIdx)
        // 5. Graha Maitri (5 pts)
        val grahaMaitriPoints = calculateGrahaMaitri(boyMoonRashiIdx, girlMoonRashiIdx)
        // 6. Gana (6 pts)
        val ganaPoints = calculateGana(boyNakshatraIdx, girlNakshatraIdx)
        // 7. Bhakoot (7 pts) & Bhakoot Dosha
        val bhakootPoints = calculateBhakoot(boyMoonRashiIdx, girlMoonRashiIdx)
        val hasBhakootDosha = bhakootPoints == 0.0
        // 8. Nadi (8 pts) & Nadi Dosha
        val nadiPoints = calculateNadi(boyNakshatraIdx, girlNakshatraIdx)
        val hasNadiDosha = nadiPoints == 0.0

        val totalGuna = varnaPoints + vashyaPoints + taraPoints + yoniPoints + grahaMaitriPoints + ganaPoints + bhakootPoints + nadiPoints

        // Manglik Dosha
        val boyMarsHouse = boyChart.planets.find { it.planetNameEn == "Mars" }?.houseNumber ?: 0
        val girlMarsHouse = girlChart.planets.find { it.planetNameEn == "Mars" }?.houseNumber ?: 0
        val manglikHouses = listOf(1, 4, 7, 8, 12)
        val isBoyManglik = canJudgeManglik && boyMarsHouse in manglikHouses
        val isGirlManglik = canJudgeManglik && girlMarsHouse in manglikHouses

        val mangalStatusHi = when {
            !canJudgeManglik ->
                "मंगल दोष की गणना नहीं की जा सकी। इसके लिए दोनों का जन्म स्थान एवं सटीक जन्म समय आवश्यक है, " +
                    "क्योंकि मंगल दोष लग्न से देखा जाता है और लग्न हर दो घंटे में बदलता है। " +
                    "ऊपर दिए 36 गुण चंद्रमा पर आधारित हैं और इनके लिए जन्म स्थान आवश्यक नहीं।"
            isBoyManglik && isGirlManglik -> "दोनों मांगलिक हैं (मंगल दोष निरस्त / मांगलिक सामंजस्य)"
            isBoyManglik -> "वर मांगलिक हैं, कन्या मांगलिक नहीं हैं (सावधानी अपेक्षित)"
            isGirlManglik -> "कन्या मांगलिक हैं, वर मांगलिक नहीं हैं (सावधानी अपेक्षित)"
            else -> "दोनों मांगलिक नहीं हैं (कोई मंगल दोष नहीं)"
        }

        val mangalStatusEn = when {
            !canJudgeManglik ->
                "Mangal Dosha could not be determined. It is read from Mars' house relative to the " +
                    "Ascendant, which changes every two hours and depends on the birth place, so both " +
                    "birth places and exact birth times are needed. The 36 Guna above come from the " +
                    "Moon and do not need the birth place."
            isBoyManglik && isGirlManglik -> "Both are Manglik (Mangal Dosha canceled / Compatible)"
            isBoyManglik -> "Boy is Manglik, Girl is Non-Manglik (Remedy Recommended)"
            isGirlManglik -> "Girl is Manglik, Boy is Non-Manglik (Remedy Recommended)"
            else -> "Both are Non-Manglik (No Mangal Dosha)"
        }

        // Nadi Dosha Status Text
        val boyNadiStrHi = NADI_NAMES_HI[NAKSHATRA_NADI[boyNakshatraIdx]]
        val girlNadiStrHi = NADI_NAMES_HI[NAKSHATRA_NADI[girlNakshatraIdx]]
        val boyNadiStrEn = NADI_NAMES_EN[NAKSHATRA_NADI[boyNakshatraIdx]]
        val girlNadiStrEn = NADI_NAMES_EN[NAKSHATRA_NADI[girlNakshatraIdx]]

        val nadiDoshaStatusHi = if (hasNadiDosha) {
            "⚠️ नाड़ी दोष उपस्थित है (दोनों की नाड़ी '$boyNadiStrHi' समान है)। यह संतान स्वास्थ्य एवं आनुवंशिक अनुकूलता में बाधा का संकेत देता है। योग्य ज्योतिषी से नाड़ी दोष शांति अनुष्ठान का परामर्श लें।"
        } else {
            "✅ नाड़ी दोष नहीं है (वर: $boyNadiStrHi, कन्या: $girlNadiStrHi)। संतान सुख एवं स्वास्थ्य के लिए पूर्ण अनुकूलता है।"
        }

        val nadiDoshaStatusEn = if (hasNadiDosha) {
            "⚠️ Nadi Dosha Present (Both have identical '$boyNadiStrEn' Nadi). This traditionally impacts genetic harmony and child wellbeing. Astrological remedies are advised."
        } else {
            "✅ No Nadi Dosha (Boy: $boyNadiStrEn, Girl: $girlNadiStrEn). Auspicious for family health and progeny."
        }

        // Bhakoot Dosha Status Text
        val boyRashiNameHi = RASHI_NAMES_HI[boyMoonRashiIdx]
        val girlRashiNameHi = RASHI_NAMES_HI[girlMoonRashiIdx]
        val boyRashiNameEn = RASHI_NAMES_EN[boyMoonRashiIdx]
        val girlRashiNameEn = RASHI_NAMES_EN[girlMoonRashiIdx]

        val bhakootDoshaStatusHi = if (hasBhakootDosha) {
            val rashiDist = ((girlMoonRashiIdx - boyMoonRashiIdx + 12) % 12) + 1
            val combo = when (rashiDist) {
                2, 12 -> "द्विर्द्वादश (2/12 - आर्थिक/व्यय दोष)"
                5, 9 -> "नवपंचम (5/9 - संतान एवं मनोमेल में बाधा)"
                else -> "षडाष्टक (6/8 - स्वास्थ्य/कलह दोष)"
            }
            "⚠️ भकूट दोष उपस्थित है ($combo - $boyRashiNameHi एवं $girlRashiNameHi)। पारिवारिक सामंजस्य एवं आर्थिक स्थिरता के लिए सावधानी व महामृत्युंजय जप परामर्श योग्य है।"
        } else {
            "✅ भकूट दोष नहीं है ($boyRashiNameHi व $girlRashiNameHi अनुकूल भाव में हैं)। पारिवारिक सौहार्द एवं समृद्धि के लिए पूर्ण 7 गुण प्राप्त हुए हैं।"
        }

        val bhakootDoshaStatusEn = if (hasBhakootDosha) {
            val rashiDist = ((girlMoonRashiIdx - boyMoonRashiIdx + 12) % 12) + 1
            val combo = when (rashiDist) {
                2, 12 -> "Dwidwadasha (2/12 - Financial/Expenditure discord)"
                5, 9 -> "Nav-Pancham (5/9 - Strain around children and temperament)"
                else -> "Shadashtaka (6/8 - Health/Relationship friction)"
            }
            "⚠️ Bhakoot Dosha Present ($combo between $boyRashiNameEn and $girlRashiNameEn). Prior prayers and Vedic remedies are recommended."
        } else {
            "✅ No Bhakoot Dosha ($boyRashiNameEn & $girlRashiNameEn are harmoniously placed). Full 7 points awarded for marital happiness."
        }

        val kootDetails = listOf(
            GunaKootDetail(
                kootNameHi = "वर्ण",
                kootNameEn = "Varna",
                maxPoints = 1.0,
                obtainedPoints = varnaPoints,
                descriptionHi = "आध्यात्मिक एवं मानसिक दृष्टिकोण का सामंजस्य। (वर: ${VARNA_NAMES_HI[VARNA_RANKS[boyMoonRashiIdx]]}, कन्या: ${VARNA_NAMES_HI[VARNA_RANKS[girlMoonRashiIdx]]})",
                descriptionEn = "Spiritual & ego compatibility. (Boy: ${VARNA_NAMES_EN[VARNA_RANKS[boyMoonRashiIdx]]}, Girl: ${VARNA_NAMES_EN[VARNA_RANKS[girlMoonRashiIdx]]})",
                isFavorable = varnaPoints == 1.0
            ),
            GunaKootDetail(
                kootNameHi = "वश्य",
                kootNameEn = "Vashya",
                maxPoints = 2.0,
                obtainedPoints = vashyaPoints,
                descriptionHi = "पारस्परिक आकर्षण, प्रभुत्व एवं भावनात्मक समर्पण। (वर: ${VASHYA_NAMES_HI[VASHYA_GROUP[boyMoonRashiIdx]]}, कन्या: ${VASHYA_NAMES_HI[VASHYA_GROUP[girlMoonRashiIdx]]})",
                descriptionEn = "Mutual attraction and control balance. (Boy: ${VASHYA_NAMES_EN[VASHYA_GROUP[boyMoonRashiIdx]]}, Girl: ${VASHYA_NAMES_EN[VASHYA_GROUP[girlMoonRashiIdx]]})",
                isFavorable = vashyaPoints >= 1.0
            ),
            GunaKootDetail(
                kootNameHi = "तारा",
                kootNameEn = "Tara",
                maxPoints = 3.0,
                obtainedPoints = taraPoints,
                descriptionHi = "भाग्य, दीर्घायु, स्वास्थ्य एवं ऊर्जा अनुकूलता।",
                descriptionEn = "Destiny, health, longevity, and star harmony.",
                isFavorable = taraPoints >= 1.5
            ),
            GunaKootDetail(
                kootNameHi = "योनि",
                kootNameEn = "Yoni",
                maxPoints = 4.0,
                obtainedPoints = yoniPoints,
                descriptionHi = "शारीरिक आकर्षण, अंतरंगता एवं दाम्पत्य सामंजस्य। (वर: ${YONI_NAMES_HI[NAKSHATRA_YONI[boyNakshatraIdx]]}, कन्या: ${YONI_NAMES_HI[NAKSHATRA_YONI[girlNakshatraIdx]]})",
                descriptionEn = "Physical, intimacy, and biological compatibility. (Boy: ${YONI_NAMES_EN[NAKSHATRA_YONI[boyNakshatraIdx]]}, Girl: ${YONI_NAMES_EN[NAKSHATRA_YONI[girlNakshatraIdx]]})",
                isFavorable = yoniPoints >= 2.0
            ),
            GunaKootDetail(
                kootNameHi = "ग्रह मैत्री",
                kootNameEn = "Graha Maitri",
                maxPoints = 5.0,
                obtainedPoints = grahaMaitriPoints,
                descriptionHi = "मानसिक विचार, बौद्धिक मित्रता एवं आपसी समझ।",
                descriptionEn = "Intellectual rapport and mental wavelength between Moon lords.",
                isFavorable = grahaMaitriPoints >= 3.0
            ),
            GunaKootDetail(
                kootNameHi = "गण",
                kootNameEn = "Gana",
                maxPoints = 6.0,
                obtainedPoints = ganaPoints,
                descriptionHi = "स्वभाव, आचरण, व्यवहार एवं चरित्र सामंजस्य। (वर: ${GANA_NAMES_HI[NAKSHATRA_GANA[boyNakshatraIdx]]}, कन्या: ${GANA_NAMES_HI[NAKSHATRA_GANA[girlNakshatraIdx]]})",
                descriptionEn = "Temperament and lifestyle compatibility. (Boy: ${GANA_NAMES_EN[NAKSHATRA_GANA[boyNakshatraIdx]]}, Girl: ${GANA_NAMES_EN[NAKSHATRA_GANA[girlNakshatraIdx]]})",
                isFavorable = ganaPoints >= 5.0
            ),
            GunaKootDetail(
                kootNameHi = "भकूट",
                kootNameEn = "Bhakoot",
                maxPoints = 7.0,
                obtainedPoints = bhakootPoints,
                descriptionHi = if (hasBhakootDosha) "भकूट दोष उपस्थित है — पारिवारिक समृद्धि व भावनात्मक सुरक्षा में बाधा संभव।" else "पूर्ण भकूट सामंजस्य — पारिवारिक सुख एवं आर्थिक समृद्धि के लिए शुभ।",
                descriptionEn = if (hasBhakootDosha) "Bhakoot Dosha present — possible financial or emotional discord." else "Harmonious Bhakoot — auspicious for marital prosperity and joy.",
                isFavorable = !hasBhakootDosha
            ),
            GunaKootDetail(
                kootNameHi = "नाडी",
                kootNameEn = "Nadi",
                maxPoints = 8.0,
                obtainedPoints = nadiPoints,
                descriptionHi = if (hasNadiDosha) "नाड़ी दोष उपस्थित है (समान नाड़ी) — स्वास्थ्य एवं संतान पक्ष के लिए उपाय आवश्यक।" else "नाड़ी अनुकूलता पूर्ण है — आनुवंशिक स्वास्थ्य एवं संतान सुख के लिए सर्वथा शुभ।",
                descriptionEn = if (hasNadiDosha) "Nadi Dosha present (Same Nadi) — remedies recommended for health & progeny." else "Complete Nadi harmony — excellent genetic compatibility and progeny welfare.",
                isFavorable = !hasNadiDosha
            )
        )

        val scoreCategory: String
        val verdictHi: String
        val verdictEn: String
        val summaryHi: String
        val summaryEn: String

        when {
            totalGuna >= 33.0 -> {
                scoreCategory = "EXCELLENT"
                verdictHi = "सर्वोत्कृष्ट मिलान"
                verdictEn = "Excellent Match"
                summaryHi = "$boyName एवं $girlName की कुण्डली में $totalGuna / 36 गुण प्राप्त हुए हैं। यह विवाह वैदिक दृष्टि से अत्यंत शुभ, समृद्ध एवं सुखद दाम्पत्य जीवन का परिचायक है।"
                summaryEn = "An outstanding $totalGuna / 36 gunas match between $boyName and $girlName. Highly auspicious for lifelong marital harmony and prosperity."
            }
            totalGuna >= 25.0 -> {
                scoreCategory = "GOOD"
                verdictHi = "उत्तम एवं शुभ मिलान"
                verdictEn = "Good Match"
                summaryHi = "$boyName एवं $girlName की कुण्डली में $totalGuna / 36 गुण प्राप्त हुए हैं। यह एक उत्तम मिलान है और विवाह के लिए पूर्णतः अनुशंसित है।"
                summaryEn = "A solid $totalGuna / 36 gunas match between $boyName and $girlName. This is a very good match and is warmly recommended for marriage."
            }
            totalGuna >= 18.0 -> {
                scoreCategory = "AVERAGE"
                verdictHi = "मध्यम / सामान्य मिलान"
                verdictEn = "Average Match"
                summaryHi = "$boyName एवं $girlName की कुण्डली में $totalGuna / 36 गुण मिल रहे हैं। यह एक स्वीकार्य मिलान है। नाड़ी अथवा भकूट दोष होने पर शांति पूजा करवाना श्रेयस्कर रहेगा।"
                summaryEn = "$totalGuna / 36 gunas match. This is an acceptable average match. If any Doshas exist, performing Vedic remedial prayers is recommended."
            }
            else -> {
                scoreCategory = "POOR"
                verdictHi = "अशुभ / असहमत मिलान"
                verdictEn = "Poor Match (Not Recommended)"
                summaryHi = "$boyName एवं $girlName की कुण्डली में मात्र $totalGuna / 36 गुण प्राप्त हुए हैं (18 से कम)। विवाह पूर्व वरिष्ठ ज्योतिषी से विस्तृत परामर्श एवं दोष निवारण आवश्यक है।"
                summaryEn = "Only $totalGuna / 36 gunas match (below acceptable threshold of 18). Detailed astrological consultation and remedies are strongly advised."
            }
        }

        return GunaMatchingResult(
            boyName = boyName,
            girlName = girlName,
            totalObtainedGuna = totalGuna,
            maxGuna = 36.0,
            isManglikBoy = isBoyManglik,
            isManglikGirl = isGirlManglik,
            mangalDoshaStatusHi = mangalStatusHi,
            mangalDoshaStatusEn = mangalStatusEn,
            kootDetails = kootDetails,
            compatibilityVerdictHi = verdictHi,
            compatibilityVerdictEn = verdictEn,
            summaryReadingHi = summaryHi,
            summaryReadingEn = summaryEn,
            // The comparison table showed these raw Hindi even in English mode;
            // every one of them already had an English table beside it.
            boyMoonRashi = LanguageManager.getString(boyRashiNameHi, boyRashiNameEn),
            girlMoonRashi = LanguageManager.getString(girlRashiNameHi, girlRashiNameEn),
            boyNakshatra = LanguageManager.getString(boyChart.moonNakshatraHi, boyChart.moonNakshatraEn),
            girlNakshatra = LanguageManager.getString(girlChart.moonNakshatraHi, girlChart.moonNakshatraEn),
            boyNadi = LanguageManager.getString(boyNadiStrHi, boyNadiStrEn),
            girlNadi = LanguageManager.getString(girlNadiStrHi, girlNadiStrEn),
            boyGana = LanguageManager.getString(GANA_NAMES_HI[NAKSHATRA_GANA[boyNakshatraIdx]], GANA_NAMES_EN[NAKSHATRA_GANA[boyNakshatraIdx]]),
            girlGana = LanguageManager.getString(GANA_NAMES_HI[NAKSHATRA_GANA[girlNakshatraIdx]], GANA_NAMES_EN[NAKSHATRA_GANA[girlNakshatraIdx]]),
            boyYoni = LanguageManager.getString(YONI_NAMES_HI[NAKSHATRA_YONI[boyNakshatraIdx]], YONI_NAMES_EN[NAKSHATRA_YONI[boyNakshatraIdx]]),
            girlYoni = LanguageManager.getString(YONI_NAMES_HI[NAKSHATRA_YONI[girlNakshatraIdx]], YONI_NAMES_EN[NAKSHATRA_YONI[girlNakshatraIdx]]),
            boyVarna = LanguageManager.getString(VARNA_NAMES_HI[VARNA_RANKS[boyMoonRashiIdx]] ?: "", VARNA_NAMES_EN[VARNA_RANKS[boyMoonRashiIdx]] ?: ""),
            girlVarna = LanguageManager.getString(VARNA_NAMES_HI[VARNA_RANKS[girlMoonRashiIdx]] ?: "", VARNA_NAMES_EN[VARNA_RANKS[girlMoonRashiIdx]] ?: ""),
            boyVashya = LanguageManager.getString(VASHYA_NAMES_HI[VASHYA_GROUP[boyMoonRashiIdx]], VASHYA_NAMES_EN[VASHYA_GROUP[boyMoonRashiIdx]]),
            girlVashya = LanguageManager.getString(VASHYA_NAMES_HI[VASHYA_GROUP[girlMoonRashiIdx]], VASHYA_NAMES_EN[VASHYA_GROUP[girlMoonRashiIdx]]),
            hasNadiDosha = hasNadiDosha,
            nadiDoshaStatusHi = nadiDoshaStatusHi,
            nadiDoshaStatusEn = nadiDoshaStatusEn,
            hasBhakootDosha = hasBhakootDosha,
            bhakootDoshaStatusHi = bhakootDoshaStatusHi,
            bhakootDoshaStatusEn = bhakootDoshaStatusEn,
            scoreCategory = scoreCategory
        )
    }

    // 1. Varna (Max 1.0 pt)
    // Brahmin (Water: Cancer 3, Scorpio 7, Pisces 11) = 4
    // Kshatriya (Fire: Aries 0, Leo 4, Sagittarius 8) = 3
    // Vaishya (Earth: Taurus 1, Virgo 5, Capricorn 9) = 2
    // Shudra (Air: Gemini 2, Libra 6, Aquarius 10) = 1
    internal fun calculateVarna(b: Int, g: Int): Double {
        val bVarna = VARNA_RANKS[b]
        val gVarna = VARNA_RANKS[g]
        return if (bVarna >= gVarna) 1.0 else 0.0
    }

    // 2. Vashya (Max 2.0 pts)
    // 0: Chatushpad (Aries, Taurus, Sag-2nd half, Cap-1st half)
    // 1: Manav/Dwipada (Gemini, Virgo, Libra, Sag-1st half, Aquarius)
    // 2: Jalchar (Cancer, Pisces, Cap-2nd half)
    // 3: Vanchar (Leo)
    // 4: Keet (Scorpio)
    internal fun calculateVashya(b: Int, g: Int): Double {
        if (b == g) return 2.0
        val bGroup = VASHYA_GROUP[b]
        val gGroup = VASHYA_GROUP[g]
        if (bGroup == gGroup) return 2.0

        // Classical Rashi-level Vashya pairing matrix
        val vashyaScoreMatrix = arrayOf(
            // Aries (0) to 12 signs
            doubleArrayOf(2.0, 1.0, 0.5, 0.5, 1.0, 0.5, 0.5, 1.0, 1.0, 0.5, 0.5, 0.5),
            // Taurus (1)
            doubleArrayOf(1.0, 2.0, 0.5, 1.0, 0.5, 0.5, 1.0, 0.5, 0.5, 1.0, 0.5, 0.5),
            // Gemini (2)
            doubleArrayOf(0.5, 0.5, 2.0, 0.5, 0.0, 1.0, 1.0, 0.5, 0.5, 0.5, 1.0, 0.5),
            // Cancer (3)
            doubleArrayOf(0.5, 1.0, 0.5, 2.0, 0.0, 0.5, 0.5, 1.0, 1.0, 0.5, 0.5, 1.0),
            // Leo (4)
            doubleArrayOf(1.0, 0.5, 0.0, 0.0, 2.0, 0.0, 1.0, 0.0, 0.5, 0.5, 0.5, 0.0),
            // Virgo (5)
            doubleArrayOf(0.5, 0.5, 1.0, 0.5, 0.0, 2.0, 1.0, 0.5, 0.5, 0.5, 1.0, 1.0),
            // Libra (6)
            doubleArrayOf(0.5, 1.0, 1.0, 0.5, 1.0, 1.0, 2.0, 0.5, 0.5, 1.0, 1.0, 0.5),
            // Scorpio (7)
            doubleArrayOf(1.0, 0.5, 0.5, 1.0, 0.0, 0.5, 0.5, 2.0, 0.5, 0.5, 0.5, 0.5),
            // Sagittarius (8)
            doubleArrayOf(1.0, 0.5, 0.5, 1.0, 0.5, 0.5, 0.5, 0.5, 2.0, 0.5, 0.5, 1.0),
            // Capricorn (9)
            doubleArrayOf(0.5, 1.0, 0.5, 0.5, 0.5, 0.5, 1.0, 0.5, 0.5, 2.0, 1.0, 1.0),
            // Aquarius (10)
            doubleArrayOf(0.5, 0.5, 1.0, 0.5, 0.5, 1.0, 1.0, 0.5, 0.5, 1.0, 2.0, 0.5),
            // Pisces (11)
            doubleArrayOf(0.5, 0.5, 0.5, 1.0, 0.0, 1.0, 0.5, 0.5, 1.0, 1.0, 0.5, 2.0)
        )

        return vashyaScoreMatrix[b][g]
    }

    // 3. Tara (Max 3.0 pts)
    // 1-based distance from Boy to Girl % 9, and Girl to Boy % 9
    // Inauspicious taras: 3 (Vipat), 5 (Pratyak), 7 (Naidhana/Vadha) = 0 pt
    // Auspicious taras: 1, 2, 4, 6, 8, 0 (Parama Mitra) = 1.5 pts
    internal fun calculateTara(bNak: Int, gNak: Int): Double {
        val b2g = ((gNak - bNak + 27) % 27) + 1
        val g2b = ((bNak - gNak + 27) % 27) + 1
        val r1 = b2g % 9
        val r2 = g2b % 9
        val score1 = if (r1 in listOf(3, 5, 7)) 0.0 else 1.5
        val score2 = if (r2 in listOf(3, 5, 7)) 0.0 else 1.5
        return score1 + score2
    }

    // 4. Yoni (Max 4.0 pts)
    // Full 14x14 Yoni compatibility matrix (4, 3, 2, 1, 0 pts)
    internal fun calculateYoni(bNak: Int, gNak: Int): Double {
        val bYoni = NAKSHATRA_YONI[bNak]
        val gYoni = NAKSHATRA_YONI[gNak]

        if (bYoni == gYoni) return 4.0

        val yoniMatrix = arrayOf(
            // 0: Ashwa (Horse)
            doubleArrayOf(4.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 0.0, 1.0, 2.0, 2.0, 2.0, 1.0),
            // 1: Gaja (Elephant)
            doubleArrayOf(2.0, 4.0, 3.0, 3.0, 2.0, 2.0, 2.0, 2.0, 3.0, 1.0, 2.0, 2.0, 2.0, 0.0),
            // 2: Mesha (Sheep)
            doubleArrayOf(2.0, 3.0, 4.0, 2.0, 1.0, 2.0, 1.0, 3.0, 2.0, 1.0, 2.0, 0.0, 2.0, 1.0),
            // 3: Sarpa (Serpent)
            doubleArrayOf(2.0, 3.0, 2.0, 4.0, 2.0, 1.0, 1.0, 1.0, 2.0, 2.0, 2.0, 2.0, 0.0, 2.0),
            // 4: Shwan (Dog)
            doubleArrayOf(2.0, 2.0, 1.0, 2.0, 4.0, 2.0, 1.0, 2.0, 1.0, 1.0, 0.0, 2.0, 1.0, 1.0),
            // 5: Marjara (Cat)
            doubleArrayOf(2.0, 2.0, 2.0, 1.0, 2.0, 4.0, 0.0, 2.0, 2.0, 1.0, 3.0, 3.0, 2.0, 1.0),
            // 6: Mushaka (Rat)
            doubleArrayOf(2.0, 2.0, 1.0, 1.0, 1.0, 0.0, 4.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 1.0),
            // 7: Gau (Cow)
            doubleArrayOf(2.0, 2.0, 3.0, 1.0, 2.0, 2.0, 2.0, 4.0, 3.0, 0.0, 2.0, 3.0, 2.0, 1.0),
            // 8: Mahisha (Buffalo)
            doubleArrayOf(0.0, 3.0, 2.0, 2.0, 1.0, 2.0, 2.0, 3.0, 4.0, 1.0, 2.0, 2.0, 2.0, 1.0),
            // 9: Vyaghra (Tiger)
            doubleArrayOf(1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 2.0, 0.0, 1.0, 4.0, 1.0, 1.0, 2.0, 1.0),
            // 10: Mriga (Deer)
            doubleArrayOf(2.0, 2.0, 2.0, 2.0, 0.0, 3.0, 2.0, 2.0, 2.0, 1.0, 4.0, 2.0, 2.0, 1.0),
            // 11: Vanara (Monkey)
            doubleArrayOf(2.0, 2.0, 0.0, 2.0, 2.0, 3.0, 2.0, 3.0, 2.0, 1.0, 2.0, 4.0, 3.0, 2.0),
            // 12: Nakula (Mongoose)
            doubleArrayOf(2.0, 2.0, 2.0, 0.0, 1.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 3.0, 4.0, 2.0),
            // 13: Simha (Lion)
            doubleArrayOf(1.0, 0.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 4.0)
        )

        return yoniMatrix[bYoni][gYoni]
    }

    // 5. Graha Maitri (Max 5.0 pts)
    // Moon sign ruling planets:
    // Sun(0), Moon(1), Mars(2), Mercury(3), Jupiter(4), Venus(5), Saturn(6)
    internal fun calculateGrahaMaitri(b: Int, g: Int): Double {
        val lords = listOf(2, 5, 3, 1, 0, 3, 5, 2, 4, 6, 6, 4)
        val bLord = lords[b]
        val gLord = lords[g]

        if (bLord == gLord) return 5.0

        fun getRelation(p1: Int, p2: Int): Int { // 2:Friend, 1:Neutral, 0:Enemy
            return when (p1) {
                0 -> if (p2 in listOf(1, 2, 4)) 2 else if (p2 in listOf(5, 6)) 0 else 1
                1 -> if (p2 in listOf(0, 3)) 2 else 1
                2 -> if (p2 in listOf(0, 1, 4)) 2 else if (p2 == 3) 0 else 1
                3 -> if (p2 in listOf(0, 5)) 2 else if (p2 == 1) 0 else 1
                4 -> if (p2 in listOf(0, 1, 2)) 2 else if (p2 in listOf(3, 5)) 0 else 1
                5 -> if (p2 in listOf(3, 6)) 2 else if (p2 in listOf(0, 1)) 0 else 1
                6 -> if (p2 in listOf(3, 5)) 2 else if (p2 in listOf(0, 1, 2)) 0 else 1
                else -> 1
            }
        }

        val r1 = getRelation(bLord, gLord)
        val r2 = getRelation(gLord, bLord)
        val sum = r1 + r2
        return when (sum) {
            4 -> 5.0 // Friend-Friend
            3 -> 4.0 // Friend-Neutral
            2 -> if (r1 == 1 && r2 == 1) 3.0 else 1.0 // Neutral-Neutral vs Friend-Enemy
            1 -> 0.5 // Neutral-Enemy
            else -> 0.0 // Enemy-Enemy
        }
    }

    // 6. Gana (Max 6.0 pts)
    // 0:Deva, 1:Manushya, 2:Rakshasa
    internal fun calculateGana(bNak: Int, gNak: Int): Double {
        val bGana = NAKSHATRA_GANA[bNak]
        val gGana = NAKSHATRA_GANA[gNak]

        if (bGana == gGana) return 6.0
        if (bGana == 0 && gGana == 1) return 6.0
        if (bGana == 1 && gGana == 0) return 5.0
        if (bGana == 2 && gGana == 0) return 1.0
        return 0.0 // Deva-Rakshasa, Manushya-Rakshasa, Rakshasa-Manushya
    }

    // 7. Bhakoot (Max 7.0 pts)
    //
    // There are *three* Bhakoot doshas, not two, and this scored only two of
    // them. The classical set is 2/12 (Dwirdwadasha), 5/9 (Nav-Pancham) and
    // 6/8 (Shadashtaka); each is 0 points, everything else is the full 7.
    // Nav-Pancham was on the wrong side of the list — the comment here used to
    // name 5/9 among the *scoring* distances — so one couple in six was handed
    // seven points it should not have had and told "no Bhakoot dosha".
    //
    // Seven of thirty-six is enough to carry a match across the 18-point line
    // people read as the verdict, which is why this is not a rounding matter.
    //
    // The distance is measured one way only, so both ends of each pair have to
    // be listed: girl 2nd from boy is 2 and girl 12th from boy is 12, and the
    // same for 5/9 and 6/8.
    internal fun calculateBhakoot(b: Int, g: Int): Double {
        val dist = ((g - b + 12) % 12) + 1
        return if (dist in BHAKOOT_DOSHA_DISTANCES) 0.0 else 7.0
    }

    // 8. Nadi (Max 8.0 pts)
    // Same Nadi (Adi-Adi, Madhya-Madhya, Antya-Antya) = 0.0 pts (Nadi Dosha)
    // Different Nadi = 8.0 pts
    internal fun calculateNadi(bNak: Int, gNak: Int): Double {
        val bNadi = NAKSHATRA_NADI[bNak]
        val gNadi = NAKSHATRA_NADI[gNak]
        return if (bNadi != gNadi) 8.0 else 0.0
    }
}
