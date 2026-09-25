package com.example.astro

import com.example.data.model.CityLocation
import com.example.data.model.PanchangData
import com.example.data.model.PlanetPosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

object PanchangCalculator {

    val popularCities = listOf(
        CityLocation("Jaipur", "जयपुर", "Rajasthan", 26.9124, 75.7873),
        CityLocation("New Delhi", "नई दिल्ली", "Delhi", 28.6139, 77.2090),
        CityLocation("Jodhpur", "जोधपुर", "Rajasthan", 26.2389, 73.0243),
        CityLocation("Udaipur", "उदयपुर", "Rajasthan", 24.5854, 73.7125),
        CityLocation("Kota", "कोटा", "Rajasthan", 25.2138, 75.8648),
        CityLocation("Bikaner", "बीकानेर", "Rajasthan", 28.0229, 73.3119),
        CityLocation("Mumbai", "मुंबई", "Maharashtra", 19.0760, 72.8777),
        CityLocation("Ahmedabad", "अहमदाबाद", "Gujarat", 23.0225, 72.5714),
        CityLocation("Lucknow", "लखनऊ", "Uttar Pradesh", 26.8467, 80.9462),
        CityLocation("Kolkata", "कोलकाता", "West Bengal", 22.5726, 88.3639),
        CityLocation("Bengaluru", "बेंगलुरु", "Karnataka", 12.9716, 77.5946),
        CityLocation("Varanasi", "वाराणसी", "Uttar Pradesh", 25.3176, 82.9739),
        CityLocation("Chennai", "चेन्नई", "Tamil Nadu", 13.0827, 80.2707),
        CityLocation("Hyderabad", "हैदराबाद", "Telangana", 17.3850, 78.4867),
        CityLocation("Pune", "पुणे", "Maharashtra", 18.5204, 73.8567),
        CityLocation("Patna", "पटना", "Bihar", 25.5941, 85.1376),
        CityLocation("Bhopal", "भोपाल", "Madhya Pradesh", 23.2599, 77.4126),
        CityLocation("Chandigarh", "चंडीगढ़", "Chandigarh", 30.7333, 76.7794),
        CityLocation("Guwahati", "गुवाहाटी", "Assam", 26.1445, 91.7362),
        CityLocation("Bhubaneswar", "भुवनेश्वर", "Odisha", 20.2961, 85.8245),
        CityLocation("Kochi", "कोच्चि", "Kerala", 9.9312, 76.2673),
        CityLocation("Nagpur", "नागपुर", "Maharashtra", 21.1458, 79.0882),
        CityLocation("Indore", "इंदौर", "Madhya Pradesh", 22.7196, 75.8577),
        CityLocation("Surat", "सूरत", "Gujarat", 21.1702, 72.8311),
        CityLocation("Amritsar", "अमृतसर", "Punjab", 31.6340, 74.8723),
        CityLocation("Dehradun", "देहरादून", "Uttarakhand", 30.3165, 78.0322),
        CityLocation("Raipur", "रायपुर", "Chhattisgarh", 21.2514, 81.6296),
        CityLocation("Ranchi", "रांची", "Jharkhand", 23.3441, 85.3096),
        CityLocation("Srinagar", "श्रीनगर", "Jammu & Kashmir", 34.0837, 74.7973),
        CityLocation("Thiruvananthapuram", "तिरुवनन्तपुरम", "Kerala", 8.5241, 76.9366)
    )


    private val PLANET_ORDER = listOf(
        "Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn", "Rahu", "Ketu"
    )

    /**
     * The Panchang for a calendar day at a place.
     *
     * IMPORTANT — the five limbs are evaluated **at sunrise**, not at the moment
     * the screen happens to be open. That is the Vedic convention and the only
     * way this output can agree with a printed panchang: previously the headline
     * Tithi changed as the day went on, so the same day could read "Purnima,
     * Shukla" in the morning and "Pratipada, Krishna" in the evening.
     */
    /** Local midnight, sunrise and sunset of [date]'s Indian calendar day, as Julian Days. */
    internal data class SunWindow(val midnightJd: Double, val sunriseJd: Double, val sunsetJd: Double)

    internal fun sunWindow(date: Date, city: CityLocation): SunWindow {
        val cal = GregorianCalendar(AstroTime.IST).apply { time = date }
        val midnightJd = AstroTime.julianDayFromLocal(
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), 0, 0, AstroTime.IST
        )
        val sunTimes = RiseSetCalculator.sunRiseSet(midnightJd, city.latitude, city.longitude)
        // Fall back to 6am local only when the Sun genuinely does not rise (polar).
        return SunWindow(
            midnightJd,
            sunTimes.riseJd ?: (midnightJd + 0.25),
            sunTimes.setJd ?: (midnightJd + 0.75)
        )
    }

    fun calculatePanchang(date: Date, city: CityLocation, use24Hour: Boolean = false): PanchangData {
        val zone = AstroTime.IST
        val cal = GregorianCalendar(zone).apply { time = date }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val varIdx = (dayOfWeek - Calendar.SUNDAY).coerceIn(0, 6)

        val (midnightJd, sunriseJd, sunsetJd) = sunWindow(date, city)
        val moonTimes = RiseSetCalculator.moonRiseSet(midnightJd, city.latitude, city.longitude)

        // ---- the five limbs, at sunrise ----
        val tithiNum = PanchangElements.tithiNumber(sunriseJd)
        val isShukla = tithiNum <= 15
        val tithiIndex = (if (isShukla) tithiNum - 1 else tithiNum - 16).coerceIn(0, 14)
        val tithiHi = if (tithiNum == 30) AstroNames.AMAVASYA_HI else AstroNames.TITHI_HI[tithiIndex]
        val tithiEn = if (tithiNum == 30) AstroNames.AMAVASYA_EN else AstroNames.TITHI_EN[tithiIndex]

        val nakIdx = PanchangElements.nakshatraIndex(sunriseJd)
        val yogaIdx = PanchangElements.yogaIndex(sunriseJd)
        val karanaIdx = PanchangElements.karanaIndex(sunriseJd)

        // Progress through the current Tithi, for the UI ring.
        val elongation = AstroMath.elongation(sunriseJd)
        val tithiProgress = ((elongation % PanchangElements.TITHI_SPAN) /
            PanchangElements.TITHI_SPAN * 100.0).toFloat()

        val tithiEnd = PanchangElements.tithiEndJd(sunriseJd)
        val nakEnd = PanchangElements.nakshatraEndJd(sunriseJd)

        // ---- day divisions, all measured from real sunrise/sunset ----
        val sunriseMin = jdToLocalMinutes(sunriseJd, zone)
        val sunsetMin = jdToLocalMinutes(sunsetJd, zone)
        val dayDurationMin = if (sunsetMin > sunriseMin) sunsetMin - sunriseMin else 720
        val slotLen = dayDurationMin / 8

        // Weekday slot tables (verified against standard practice — do not reorder).
        val rahuSlots = mapOf(
            Calendar.SUNDAY to 7, Calendar.MONDAY to 1, Calendar.TUESDAY to 6,
            Calendar.WEDNESDAY to 4, Calendar.THURSDAY to 5, Calendar.FRIDAY to 3,
            Calendar.SATURDAY to 2
        )
        val gulikaSlots = mapOf(
            Calendar.SUNDAY to 6, Calendar.MONDAY to 5, Calendar.TUESDAY to 4,
            Calendar.WEDNESDAY to 3, Calendar.THURSDAY to 2, Calendar.FRIDAY to 1,
            Calendar.SATURDAY to 0
        )
        val yamaSlots = mapOf(
            Calendar.SUNDAY to 4, Calendar.MONDAY to 3, Calendar.TUESDAY to 2,
            Calendar.WEDNESDAY to 1, Calendar.THURSDAY to 0, Calendar.FRIDAY to 6,
            Calendar.SATURDAY to 5
        )

        fun slotRange(slot: Int): String {
            val start = sunriseMin + slot * slotLen
            return "${formatMinutesToTime(start, use24Hour)} - ${formatMinutesToTime(start + slotLen, use24Hour)}"
        }

        val rahuKaalStr = slotRange(rahuSlots[dayOfWeek] ?: 1)
        val gulikaKaalStr = slotRange(gulikaSlots[dayOfWeek] ?: 0)
        val yamaKaalStr = slotRange(yamaSlots[dayOfWeek] ?: 0)

        // A muhurta is a fifteenth of the day or of the night, not a fixed
        // forty-eight minutes. These two were written with the fixed number,
        // which is the equinox case and only the equinox case: in Jaipur the day
        // runs from about 10h20m to 13h40m, so a day muhurta is 41 to 55 minutes
        // and Abhijit was up to three minutes out at both ends — on the one
        // window in the day people use to decide when to begin something.
        val nightDurationMin = 1440 - dayDurationMin
        val dayMuhurtaMin = dayDurationMin / 15
        val nightMuhurtaMin = nightDurationMin / 15

        // Abhijit is the eighth of the fifteen day muhurtas, so it straddles
        // local midday.
        val midDay = sunriseMin + dayDurationMin / 2
        val abhijitStr = "${formatMinutesToTime(midDay - dayMuhurtaMin / 2, use24Hour)} - " +
            "${formatMinutesToTime(midDay + dayMuhurtaMin / 2, use24Hour)}"

        // Brahma is the fourteenth of the fifteen night muhurtas: it ends one
        // muhurta before sunrise and lasts one.
        val brahmaStr = "${formatMinutesToTime(sunriseMin - 2 * nightMuhurtaMin, use24Hour)} - " +
            "${formatMinutesToTime(sunriseMin - nightMuhurtaMin, use24Hour)}"

        // ---- planets, at sunrise ----
        val ascendant = KundaliCalculator.ascendantDegrees(sunriseJd, city.latitude, city.longitude)
        val ascendantRashiIdx = (ascendant / 30.0).toInt().coerceIn(0, 11)

        val planetDegrees = AstroMath.calculatePlanets(sunriseJd)
        val before = AstroMath.calculatePlanets(sunriseJd - 0.5)
        val after = AstroMath.calculatePlanets(sunriseJd + 0.5)

        val planetPositions = PLANET_ORDER.map { en ->
            val deg = planetDegrees[en] ?: 0.0
            val motion = AstroTime.wrap180((after[en] ?: 0.0) - (before[en] ?: 0.0))
            val rashiIdx = (deg / 30.0).toInt().coerceIn(0, 11)
            PlanetPosition(
                planetNameEn = en,
                planetNameHi = AstroNames.PLANET_HI[en] ?: en,
                rashiNumber = rashiIdx + 1,
                rashiNameHi = AstroNames.RASHI_HI[rashiIdx],
                rashiNameEn = AstroNames.RASHI_EN[rashiIdx],
                degree = String.format(Locale.US, "%.2f", deg % 30.0).toDouble(),
                houseNumber = ((rashiIdx - ascendantRashiIdx + 12) % 12) + 1,
                isRetrograde = when (en) {
                    "Rahu", "Ketu" -> true
                    "Sun", "Moon" -> false
                    else -> motion < 0.0
                },
                nakshatraHi = AstroNames.NAKSHATRA_HI[(deg / PanchangElements.NAKSHATRA_SPAN).toInt().coerceIn(0, 26)],
                nakshatraEn = AstroNames.NAKSHATRA_EN[(deg / PanchangElements.NAKSHATRA_SPAN).toInt().coerceIn(0, 26)]
            )
        }

        val masaIdx = PanchangElements.masaIndex(sunriseJd)
        // Adhika Jyeshtha 2026 and the Nija Jyeshtha behind it are both
        // "Jyeshtha" to masaIndex — a lunar month is Adhika precisely because
        // the Sun does not change sign during it, so the pair start with the Sun
        // in the same sign and take the same name. Fifty-nine days of ज्येष्ठ ran
        // past without the screen ever saying which one. Festivals already knew
        // the difference; the Panchang did not.
        val isAdhika = PanchangElements.isAdhikaMasa(sunriseJd)
        val sunDeg = planetDegrees["Sun"] ?: 0.0
        val moonDeg = planetDegrees["Moon"] ?: 0.0

        val dateFmt = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).apply { timeZone = zone }

        return PanchangData(
            dateString = dateFmt.format(date),
            dayOfWeek = AstroNames.VARA_EN[varIdx],
            dayOfWeekHindi = AstroNames.VARA_HI[varIdx],
            vikramSamvat = PanchangElements.vikramSamvat(sunriseJd, year),
            sakaSamvat = PanchangElements.sakaSamvat(sunriseJd, year),
            masaName = if (isAdhika) "Adhika ${AstroNames.MASA_EN[masaIdx]}" else AstroNames.MASA_EN[masaIdx],
            masaNameHindi = if (isAdhika) "अधिक ${AstroNames.MASA_HI[masaIdx]}" else AstroNames.MASA_HI[masaIdx],
            paksha = if (isShukla) AstroNames.SHUKLA_EN else AstroNames.KRISHNA_EN,
            pakshaHindi = if (isShukla) AstroNames.SHUKLA_HI else AstroNames.KRISHNA_HI,
            tithi = tithiEn,
            tithiHindi = tithiHi,
            tithiEndTime = formatEndTime(tithiEnd, midnightJd, zone, use24Hour),
            tithiProgressPercent = tithiProgress,
            nakshatra = AstroNames.NAKSHATRA_EN[nakIdx],
            nakshatraHindi = AstroNames.NAKSHATRA_HI[nakIdx],
            nakshatraEndTime = formatEndTime(nakEnd, midnightJd, zone, use24Hour),
            nakshatraPada = PanchangElements.nakshatraPada(sunriseJd),
            yoga = AstroNames.YOGA_EN[yogaIdx],
            yogaHindi = AstroNames.YOGA_HI[yogaIdx],
            karan = AstroNames.karanaEn(karanaIdx),
            karanHindi = AstroNames.karanaHi(karanaIdx),
            sunrise = formatJdTime(sunriseJd, zone, use24Hour),
            sunset = formatJdTime(sunsetJd, zone, use24Hour),
            // The Moon skips a rise or a set about once a month — say so rather
            // than inventing a time, which is what this field used to do.
            moonrise = moonTimes.riseJd?.let { formatJdTime(it, zone, use24Hour) } ?: "—",
            moonset = moonTimes.setJd?.let { formatJdTime(it, zone, use24Hour) } ?: "—",
            rahuKaal = rahuKaalStr,
            gulikaKaal = gulikaKaalStr,
            yamaganda = yamaKaalStr,
            abhijitMuhurat = abhijitStr,
            brahmaMuhurat = brahmaStr,
            sunSign = localRashi((sunDeg / 30.0).toInt().coerceIn(0, 11)),
            moonSign = localRashi((moonDeg / 30.0).toInt().coerceIn(0, 11)),
            locationName = city.cityNameHindi,
            latitude = city.latitude,
            longitude = city.longitude,
            planets = planetPositions
        )
    }

    // ------------------------------------------------------------------
    // Formatting
    // ------------------------------------------------------------------

    /** Minutes since local midnight for a Julian Day. */
    /** Sun/moon sign strings are shown as one value, so they follow the language. */
    private fun localRashi(idx: Int) =
        AstroNames.pick(AstroNames.RASHI_HI[idx], AstroNames.RASHI_EN[idx])

    private fun jdToLocalMinutes(jd: Double, zone: TimeZone): Int {
        val cal = GregorianCalendar(zone).apply { timeInMillis = AstroTime.millisFromJulianDay(jd) }
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    internal fun formatJdTime(jd: Double, zone: TimeZone, use24Hour: Boolean): String =
        formatMinutesToTime(jdToLocalMinutes(jd, zone), use24Hour)

    /**
     * "07:42 PM तक" or "अगले दिन 04:15 AM तक" — and now these are the real
     * crossing times, computed per day and per place.
     */
    private fun formatEndTime(
        endJd: Double?,
        midnightJd: Double,
        zone: TimeZone,
        use24Hour: Boolean
    ): String {
        if (endJd == null) return "—"
        val time = formatJdTime(endJd, zone, use24Hour)
        val daysAhead = Math.floor(endJd - midnightJd).toInt()
        return when {
            daysAhead <= 0 -> AstroNames.pick("$time तक", "until $time")
            daysAhead == 1 -> AstroNames.pick("अगले दिन $time तक", "until $time next day")
            else -> AstroNames.pick("$daysAhead दिन बाद $time तक", "until $time, $daysAhead days on")
        }
    }

    fun formatMinutesToTime(minutes: Int, use24Hour: Boolean = false): String {
        val m = ((minutes % 1440) + 1440) % 1440
        val hrs = m / 60
        val mins = m % 60
        if (use24Hour) {
            return String.format(Locale.US, "%02d:%02d", hrs, mins)
        }
        val ampm = if (hrs >= 12) "PM" else "AM"
        val displayHrs = if (hrs % 12 == 0) 12 else hrs % 12
        return String.format(Locale.US, "%02d:%02d %s", displayHrs, mins, ampm)
    }

    fun getMoonPhaseInfo(pakshaHindi: String, tithiHindi: String): MoonPhaseInfo {
        val isShukla = pakshaHindi.contains("शुक्ल") || pakshaHindi.contains("Shukla", ignoreCase = true)
        val isPurnima = tithiHindi.contains("पूर्णिमा") || tithiHindi.contains("Purnima", ignoreCase = true)
        val isAmavasya = tithiHindi.contains("अमावस्या") || tithiHindi.contains("Amavasya", ignoreCase = true)

        if (isPurnima) {
            return MoonPhaseInfo("🌕", AstroNames.pick("पूर्णिमा", "Full Moon"), "Full Moon", 100, true)
        }
        if (isAmavasya) {
            return MoonPhaseInfo("🌑", AstroNames.pick("अमावस्या", "New Moon"), "New Moon", 0, false)
        }

        val tithiNum = TITHI_LOOKUP.entries.firstOrNull { tithiHindi.contains(it.key) }?.value ?: 7

        return if (isShukla) {
            val illumination = ((tithiNum / 15.0) * 100).toInt().coerceIn(5, 95)
            when (tithiNum) {
                in 1..3 -> MoonPhaseInfo("🌒", AstroNames.pick("शुक्ल पक्ष", "Waxing Crescent"), "Waxing Crescent", illumination, true)
                in 4..7 -> MoonPhaseInfo("🌓", AstroNames.pick("शुक्ल पक्ष", "First Quarter"), "First Quarter", illumination, true)
                else -> MoonPhaseInfo("🌔", AstroNames.pick("शुक्ल पक्ष", "Waxing Gibbous"), "Waxing Gibbous", illumination, true)
            }
        } else {
            val illumination = (((15 - tithiNum) / 15.0) * 100).toInt().coerceIn(5, 95)
            when (tithiNum) {
                in 1..3 -> MoonPhaseInfo("🌖", AstroNames.pick("कृष्ण पक्ष", "Waning Gibbous"), "Waning Gibbous", illumination, false)
                in 4..7 -> MoonPhaseInfo("🌗", AstroNames.pick("कृष्ण पक्ष", "Third Quarter"), "Third Quarter", illumination, false)
                else -> MoonPhaseInfo("🌘", AstroNames.pick("कृष्ण पक्ष", "Waning Crescent"), "Waning Crescent", illumination, false)
            }
        }
    }

    private val TITHI_LOOKUP = linkedMapOf(
        "प्रतिपदा" to 1, "द्वितीया" to 2, "तृतीया" to 3, "चतुर्थी" to 4, "पंचमी" to 5,
        "षष्ठी" to 6, "सप्तमी" to 7, "अष्टमी" to 8, "नवमी" to 9, "दशमी" to 10,
        "एकादशी" to 11, "द्वादशी" to 12, "त्रयोदशी" to 13, "चतुर्दशी" to 14
    )
}

data class MoonPhaseInfo(
    val emoji: String,
    val nameHindi: String,
    val nameEn: String,
    val illuminationPercent: Int,
    val isWaxing: Boolean
)
