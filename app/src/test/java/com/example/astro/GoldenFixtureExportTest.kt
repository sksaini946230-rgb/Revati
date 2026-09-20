package com.example.astro

import com.example.data.model.GunaMatchingResult
import java.io.File
import org.junit.Test

/**
 * Exports the answers this engine gives, so the TypeScript port can be held to
 * them exactly.
 *
 * **This is the only change the iPhone project makes to this repo.** It is a
 * unit test, it ships in no APK, it asserts nothing about behaviour and it
 * cannot fail a release. Deleting it would not change the app; it would only
 * remove the one thing that proves the rewrite gives the same answers.
 *
 * Run it on its own:
 *
 *     ./gradlew testDebugUnitTest --tests '*GoldenFixtureExportTest*'
 *
 * Output lands in `app/build/golden/`, which is already ignored by git. The
 * files are large by design — the point is coverage, not a sample.
 *
 * **Doubles are written with `Double.toString()`, never formatted.** Kotlin and
 * JavaScript both print the shortest decimal that round-trips to the same IEEE
 * double, so a value written here parses back in Node bit for bit. Rounding to
 * a few decimals here would hide exactly the kind of porting mistake these
 * fixtures exist to catch.
 *
 * Why this matters: an earlier version of this engine put **92 of 365 days on
 * the wrong Tithi and 71 on the wrong Nakshatra**, and it was found only by
 * diffing a year of output. A rewrite is the easiest way to bring that back.
 *
 * `docs/ios-expo/ENGINE_PORT.md` has the porting order and the comparison
 * rules.
 */
class GoldenFixtureExportTest {

    private val outputDir = File("build/golden").apply { mkdirs() }

    // ---------------------------------------------------------------- output

    private fun write(name: String, rows: List<Map<String, Any?>>) {
        val file = File(outputDir, name)
        file.bufferedWriter().use { out ->
            out.write("[\n")
            rows.forEachIndexed { index, row ->
                out.write("  ")
                out.write(jsonObject(row))
                if (index < rows.size - 1) out.write(",")
                out.write("\n")
            }
            out.write("]\n")
        }
        println("golden: ${file.name}  ${rows.size} rows  ${file.length() / 1024} KB")
    }

    private fun jsonObject(row: Map<String, Any?>): String =
        row.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "${jsonString(key)}:${jsonValue(value)}"
        }

    private fun jsonValue(value: Any?): String = when (value) {
        null -> "null"
        is Double -> if (value.isFinite()) value.toString() else "null"
        is Int, is Long, is Boolean -> value.toString()
        is Map<*, *> ->
            @Suppress("UNCHECKED_CAST")
            jsonObject(value as Map<String, Any?>)
        is List<*> -> value.joinToString(prefix = "[", postfix = "]") { jsonValue(it) }
        else -> jsonString(value.toString())
    }

    private fun jsonString(text: String): String {
        val sb = StringBuilder("\"")
        for (ch in text) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (ch < ' ') sb.append("\\u%04x".format(ch.code)) else sb.append(ch)
            }
        }
        return sb.append("\"").toString()
    }

    // ------------------------------------------------------------- the dates
    //
    // Two grids, chosen rather than random. The wide one is every 5 days from
    // 1950 to 2050, which is the range of birth dates the app realistically
    // sees plus the transits it projects. The dense one is every day of a few
    // whole years, because the bugs this engine has actually had were
    // day-boundary bugs — a tithi that rolls at the wrong hour looks fine in a
    // 5-day sample and wrong on the day it matters.

    private fun wideGrid(): List<Double> {
        val start = AstroTime.julianDay(1950, 1, 1, 0.0)
        val end = AstroTime.julianDay(2050, 1, 1, 0.0)
        return generateSequence(start) { it + 5.0 }.takeWhile { it <= end }.toList()
    }

    private fun denseGrid(): List<Double> {
        val years = listOf(2024, 2026, 2031)
        return years.flatMap { year ->
            val start = AstroTime.julianDay(year, 1, 1, 0.0)
            val end = AstroTime.julianDay(year + 1, 1, 1, 0.0)
            // Two readings a day: midnight and noon UT. A tithi can change in
            // between, and only the pair catches which side of noon it fell.
            generateSequence(start) { it + 0.5 }.takeWhile { it < end }.toList()
        }
    }

    // ------------------------------------------------------------- fixtures

    @Test
    fun exportAstroTime() {
        val rows = mutableListOf<Map<String, Any?>>()

        // Calendar to Julian day, including the Gregorian reform, leap years
        // and the year boundary — the places an off-by-one hides.
        val dates = listOf(
            intArrayOf(1582, 10, 15), intArrayOf(1600, 2, 29), intArrayOf(1700, 3, 1),
            intArrayOf(1900, 2, 28), intArrayOf(1970, 1, 1), intArrayOf(2000, 1, 1),
            intArrayOf(2000, 2, 29), intArrayOf(2024, 2, 29), intArrayOf(2026, 12, 31),
            intArrayOf(2031, 1, 1), intArrayOf(2050, 6, 15), intArrayOf(2100, 3, 1),
        )
        for (d in dates) {
            for (hour in listOf(0.0, 5.5, 11.999999, 12.0, 18.25, 23.999999)) {
                rows.add(
                    mapOf(
                        "year" to d[0], "month" to d[1], "day" to d[2], "hourUT" to hour,
                        "jd" to AstroTime.julianDay(d[0], d[1], d[2], hour),
                    )
                )
            }
        }
        write("astro_time_julian_day.json", rows)

        // deltaT, the centuries helper, and the two angle wrappers. The
        // wrappers get negatives and multiples of 360 on purpose: JavaScript's
        // % keeps the sign of the dividend and Kotlin's does too, but a port
        // that "simplifies" one of them breaks quietly.
        val misc = mutableListOf<Map<String, Any?>>()
        for (year in 1900..2100 step 1) {
            misc.add(mapOf("year" to year, "deltaTSeconds" to AstroTime.deltaTSeconds(year)))
        }
        write("astro_time_delta_t.json", misc)

        val angles = mutableListOf<Map<String, Any?>>()
        for (deg in listOf(
            -1080.0, -720.5, -360.0, -359.9, -180.0, -0.0001, 0.0, 0.0001,
            179.9, 180.0, 180.1, 359.9, 360.0, 360.1, 720.0, 1080.25,
        )) {
            angles.add(
                mapOf(
                    "deg" to deg,
                    "norm360" to AstroTime.norm360(deg),
                    "wrap180" to AstroTime.wrap180(deg),
                )
            )
        }
        write("astro_time_angles.json", angles)

        val centuries = wideGrid().map {
            mapOf(
                "jd" to it,
                "julianCenturies" to AstroTime.julianCenturies(it),
                "yearOf" to AstroTime.yearOf(it),
                "toEphemerisTime" to AstroTime.toEphemerisTime(it, AstroTime.yearOf(it)),
            )
        }
        write("astro_time_centuries.json", centuries)
    }

    @Test
    fun exportSunMoonAndAyanamsa() {
        val rows = wideGrid().map { jd ->
            val year = AstroTime.yearOf(jd)
            val jde = AstroTime.toEphemerisTime(jd, year)
            mapOf(
                "jd" to jd,
                "jde" to jde,
                "nutationInLongitude" to AstroMath.nutationInLongitude(AstroTime.julianCenturies(jde)),
                "meanObliquity" to AstroMath.meanObliquity(AstroTime.julianCenturies(jde)),
                "sunApparentLongitude" to AstroMath.sunApparentLongitude(jde),
                "moonApparentLongitude" to AstroMath.moonApparentLongitude(jde),
                "lahiriAyanamsa" to AstroMath.lahiriAyanamsa(jd),
                "sunSidereal" to AstroMath.sunSidereal(jd),
                "moonSidereal" to AstroMath.moonSidereal(jd),
                "elongation" to AstroMath.elongation(jd),
            )
        }
        write("sun_moon_ayanamsa.json", rows)
    }

    @Test
    fun exportPlanets() {
        val rows = wideGrid().map { jd ->
            val planets = AstroMath.calculatePlanets(jd)
            mapOf("jd" to jd, "planets" to planets.toSortedMap().toMap())
        }
        write("planets.json", rows)
    }

    @Test
    fun exportMoonPosition() {
        val rows = wideGrid().map { jd ->
            val jde = AstroTime.toEphemerisTime(jd, AstroTime.yearOf(jd))
            mapOf(
                "jd" to jd,
                "latitude" to MoonPosition.latitude(jde),
                "distanceKm" to MoonPosition.distanceKm(jde),
                "horizontalParallax" to MoonPosition.horizontalParallax(jde),
                "semiDiameter" to MoonPosition.semiDiameter(jde),
            )
        }
        write("moon_position.json", rows)
    }

    /**
     * Six cities spread across India, chosen so the fixture cannot pass by
     * accident: the east-west spread is nearly 30° of longitude (Guwahati to
     * Mumbai is over an hour of true solar time on a single clock), and the
     * north-south spread crosses 20° of latitude, which is what makes day
     * length differ by season at all.
     */
    private val cities = listOf(
        Triple("Jaipur", 26.9124, 75.7873),
        Triple("Guwahati", 26.1445, 91.7362),
        Triple("Thiruvananthapuram", 8.5241, 76.9366),
        Triple("Mumbai", 19.0760, 72.8777),
        Triple("Delhi", 28.6139, 77.2090),
        Triple("Kolkata", 22.5726, 88.3639),
    )

    @Test
    fun exportRiseSet() {
        // Every third day of three whole years, in six cities. Every third
        // rather than every fifth so both solstices and both equinoxes are
        // caught in each year — day length is the thing being checked, and it
        // turns at exactly those four points.
        val rows = mutableListOf<Map<String, Any?>>()
        for (year in listOf(2024, 2026, 2031)) {
            val start = AstroTime.julianDay(year, 1, 1, 0.0)
            val end = AstroTime.julianDay(year + 1, 1, 1, 0.0)
            var jd = start
            while (jd < end) {
                for ((name, lat, lng) in cities) {
                    // The app asks from local midnight, so the fixture does too:
                    // IST is UTC+5:30, so local midnight is 18:30 UT the day before.
                    val localMidnight = jd - 5.5 / 24.0
                    val sun = RiseSetCalculator.sunRiseSet(localMidnight, lat, lng)
                    val moon = RiseSetCalculator.moonRiseSet(localMidnight, lat, lng)
                    rows.add(
                        mapOf(
                            "jd" to localMidnight,
                            "city" to name,
                            "lat" to lat,
                            "lng" to lng,
                            "sunriseJd" to sun.riseJd,
                            "sunsetJd" to sun.setJd,
                            "moonriseJd" to moon.riseJd,
                            "moonsetJd" to moon.setJd,
                        )
                    )
                }
                jd += 3.0
            }
        }
        write("rise_set.json", rows)
    }

    @Test
    fun exportNames() {
        // Every displayed term, exported rather than retyped. The TypeScript
        // table is generated from this file, so a Devanagari character cannot
        // drift between the two engines through a transcription slip — which
        // is the one kind of error a numeric fixture would never catch.
        val rows = listOf(
            mapOf("key" to "TITHI_HI", "values" to AstroNames.TITHI_HI),
            mapOf("key" to "TITHI_EN", "values" to AstroNames.TITHI_EN),
            mapOf("key" to "NAKSHATRA_HI", "values" to AstroNames.NAKSHATRA_HI),
            mapOf("key" to "NAKSHATRA_EN", "values" to AstroNames.NAKSHATRA_EN),
            mapOf("key" to "YOGA_HI", "values" to AstroNames.YOGA_HI),
            mapOf("key" to "YOGA_EN", "values" to AstroNames.YOGA_EN),
            mapOf("key" to "MASA_HI", "values" to AstroNames.MASA_HI),
            mapOf("key" to "MASA_EN", "values" to AstroNames.MASA_EN),
            mapOf("key" to "RASHI_HI", "values" to AstroNames.RASHI_HI),
            mapOf("key" to "RASHI_EN", "values" to AstroNames.RASHI_EN),
            mapOf("key" to "RASHI_SANSKRIT", "values" to AstroNames.RASHI_SANSKRIT),
            mapOf("key" to "PLANET_ORDER", "values" to AstroNames.PLANET_HI.keys.toList()),
            mapOf("key" to "PLANET_HI", "values" to AstroNames.PLANET_HI.values.toList()),
            mapOf("key" to "PLANET_SHORT", "values" to AstroNames.PLANET_SHORT.values.toList()),
            mapOf("key" to "DASHA_NAKSHATRA_HI", "values" to VimshottariDashaCalculator.NAKSHATRA_NAMES_HI),
            mapOf("key" to "DASHA_PLANET_HI", "values" to VimshottariDashaCalculator.VIMSHOTTARI_PLANETS.map { it.nameHi }),
            mapOf("key" to "DASHA_PLANET_EN", "values" to VimshottariDashaCalculator.VIMSHOTTARI_PLANETS.map { it.nameEn }),
            mapOf("key" to "SINGLES", "values" to listOf(
                AstroNames.AMAVASYA_HI, AstroNames.AMAVASYA_EN,
                AstroNames.RETROGRADE_MARK, AstroNames.SHUKLA_HI, AstroNames.SHUKLA_EN,
            )),
        )
        write("names.json", rows)
    }

    /**
     * 400 birth moments: seeded, not random, so the same rows come back on any
     * machine. They span 1940-2025 and the whole day, because the ascendant
     * moves a full sign every two hours and is the most place- and
     * time-sensitive quantity in a chart.
     */
    private fun birthMoments(): List<Map<String, Any?>> {
        @Suppress("UNUSED_EXPRESSION")
        var seed = 20260920L
        fun next(bound: Int): Int {
            seed = (seed * 6364136223846793005L + 1442695040888963407L)
            return (((seed ushr 33).toInt() % bound) + bound) % bound
        }
        return (0 until 400).map {
            val year = 1940 + next(86)
            val month = 1 + next(12)
            val day = 1 + next(28)
            val hour = next(24)
            val minute = next(60)
            val (city, lat, lng) = cities[next(cities.size)]
            val jd = AstroTime.julianDayFromLocal(year, month, day, hour, minute, AstroTime.IST)
            mapOf(
                "jd" to jd, "year" to year, "month" to month, "day" to day,
                "hour" to hour, "minute" to minute,
                "city" to city, "lat" to lat, "lng" to lng,
            )
        }
    }

    @Test
    fun exportAscendantAndChart() {
        val rows = birthMoments().map { m ->
            val jd = m["jd"] as Double
            val lat = m["lat"] as Double
            val lng = m["lng"] as Double
            val chart = KundaliCalculator.chartForInstant("Fixture", jd, m["city"] as String, lat, lng)
            val extra: Map<String, Any?> = mapOf(
                "ascendantDegrees" to KundaliCalculator.ascendantDegrees(jd, lat, lng),
                "ascendantRashiNumber" to chart.ascendantRashiNumber,
                "ascendantRashiEn" to chart.ascendantRashiEn,
                "moonRashiEn" to chart.moonRashiEn,
                "moonNakshatraEn" to chart.moonNakshatraEn,
                "planets" to chart.planets.map {
                    mapOf(
                        "en" to it.planetNameEn, "hi" to it.planetNameHi,
                        "rashiNumber" to it.rashiNumber, "rashiEn" to it.rashiNameEn,
                        "degree" to it.degree, "house" to it.houseNumber,
                        "retro" to it.isRetrograde, "nakshatraEn" to it.nakshatraEn,
                    )
                },
                "housePlanets" to (1..12).associate { h ->
                    h.toString() to (chart.housePlanetsMap[h] ?: emptyList())
                },
            )
            m + extra
        }
        write("kundali.json", rows)
    }

    @Test
    fun exportVimshottariDasha() {
        // A fixed "now", or `isCurrent` would depend on the day the fixture was
        // exported and the comparison could never be exact.
        val nowMs = AstroTime.millisFromJulianDay(AstroTime.julianDay(2026, 9, 20, 12.0))
        val rows = birthMoments().take(200).map { m ->
            val jd = m["jd"] as Double
            val moon = AstroMath.calculatePlanets(jd)["Moon"] ?: 0.0
            val d = VimshottariDashaCalculator.calculateVimshottariDasha(moon, jd, nowMs)
            mapOf(
                "jd" to jd, "moonLongitude" to moon, "nowMs" to nowMs,
                "nakshatraIndex" to d.nakshatraInfo.index,
                "nakshatraNameHi" to d.nakshatraInfo.nameHi,
                "lordEn" to d.nakshatraInfo.lordNameEn,
                "degreeInNakshatra" to d.nakshatraInfo.degreeInNakshatra,
                "fractionRemaining" to d.nakshatraInfo.fractionRemaining,
                "balanceAtBirthYears" to d.balanceAtBirthYears,
                "balanceAtBirthFormatted" to d.balanceAtBirthFormatted,
                "currentMahadashaEn" to (d.currentMahadasha?.planetEn ?: "—"),
                "currentAntardashaEn" to (d.currentAntardasha?.planetEn ?: "—"),
                "mahadashas" to d.mahadashas.map {
                    mapOf(
                        "en" to it.planetEn, "start" to it.startDate, "end" to it.endDate,
                        "years" to it.durationYears, "isCurrent" to it.isCurrent,
                        "antardashas" to it.antardashas.map { a ->
                            mapOf(
                                "en" to a.planetEn, "start" to a.startDate, "end" to a.endDate,
                                "months" to a.durationMonths, "isCurrent" to a.isCurrent,
                            )
                        },
                    )
                },
            )
        }
        write("dasha.json", rows)
    }

    @Test
    fun exportChoghadiya() {
        // Every day of one whole year in three cities, day and night. A year
        // so that every weekday meets every season, because the sequence is
        // chosen by weekday and the slot length comes from that day's length.
        //
        // The bug this guards against was exactly here: Sunday's row stepped
        // through the Chaldean cycle three at a time instead of rotating it,
        // so one day in seven called 07:34-09:09 Amrit when the cycle makes it
        // Char. It survived because the only test counted the slots — eight
        // came back, so it passed.
        val rows = mutableListOf<Map<String, Any?>>()
        val start = AstroTime.julianDay(2026, 1, 1, 0.0)
        var jd = start
        while (jd < AstroTime.julianDay(2027, 1, 1, 0.0)) {
            val date = java.util.Date(AstroTime.millisFromJulianDay(jd))
            for ((name, lat, lng) in cities.take(3)) {
                for (isDay in listOf(true, false)) {
                    val slots = ChoghadiyaCalculator.getChoghadiyaSlots(date, isDay, lat, lng)
                    rows.add(
                        mapOf(
                            "jd" to jd,
                            "city" to name,
                            "lat" to lat,
                            "lng" to lng,
                            "isDay" to isDay,
                            "slots" to slots.map {
                                mapOf(
                                    "start" to it.startTime,
                                    "end" to it.endTime,
                                    "type" to it.type.name,
                                    "ruler" to it.rulerPlanetHi,
                                )
                            },
                        )
                    )
                }
            }
            jd += 1.0
        }
        write("choghadiya.json", rows)
    }

    @Test
    fun exportPanchangElements() {
        val rows = denseGrid().map { jd ->
            mapOf(
                "jd" to jd,
                "tithiNumber" to PanchangElements.tithiNumber(jd),
                "nakshatraIndex" to PanchangElements.nakshatraIndex(jd),
                "nakshatraPada" to PanchangElements.nakshatraPada(jd),
                "yogaIndex" to PanchangElements.yogaIndex(jd),
                "karanaIndex" to PanchangElements.karanaIndex(jd),
                "karanaName" to PanchangElements.karanaName(PanchangElements.karanaIndex(jd)),
                "tithiEndJd" to PanchangElements.tithiEndJd(jd),
                "nakshatraEndJd" to PanchangElements.nakshatraEndJd(jd),
                "yogaEndJd" to PanchangElements.yogaEndJd(jd),
                "karanaEndJd" to PanchangElements.karanaEndJd(jd),
                "lastNewMoonJd" to PanchangElements.lastNewMoonJd(jd),
                "masaIndex" to PanchangElements.masaIndex(jd),
                "isAdhikaMasa" to PanchangElements.isAdhikaMasa(jd),
                "isShuklaPaksha" to PanchangElements.isShuklaPaksha(jd),
            )
        }
        write("panchang_elements.json", rows)

        // The samvat rollover is a year boundary of its own and does not line
        // up with January, so it gets its own rows.
        val samvat = (1950..2050).map { year ->
            val newYear = PanchangElements.lunarNewYearJd(year)
            mapOf(
                "gregorianYear" to year,
                "lunarNewYearJd" to newYear,
                "vikramBefore" to PanchangElements.vikramSamvat(newYear - 1.0, year),
                "vikramAfter" to PanchangElements.vikramSamvat(newYear + 1.0, year),
                "sakaBefore" to PanchangElements.sakaSamvat(newYear - 1.0, year),
                "sakaAfter" to PanchangElements.sakaSamvat(newYear + 1.0, year),
            )
        }
        write("panchang_samvat.json", samvat)
    }

    @Test
    fun exportTransits() {
        // `midWeekDate` reads the JVM's default clock and the default locale's
        // idea of where a week starts, so both are pinned here — otherwise the
        // fixture would record this laptop rather than the engine. India, and
        // a Sunday-start week, which is what every Indian locale gives.
        val savedZone = java.util.TimeZone.getDefault()
        val savedLocale = java.util.Locale.getDefault()
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
        java.util.Locale.setDefault(java.util.Locale("hi", "IN"))
        try {
            val planets = listOf(
                "Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn", "Rahu", "Ketu",
            )

            // Which house each planet transits, seen from Mesh. The house for
            // every other rashi is a rotation of this, and the rotation itself
            // is covered below — this grid is here for the part that can drift,
            // which is the longitude and the sign it falls in.
            val perDay = denseGrid().map { jd ->
                val date = java.util.Date(AstroTime.millisFromJulianDay(jd))
                val row = mutableMapOf<String, Any?>("jd" to jd)
                for (p in planets) row[p] = TransitCalculator.getTransitHouse(0, p, date)
                row
            }
            write("transit_house.json", perDay)

            // All twelve rashis against all nine planets, so the rotation and
            // its wrap are checked rather than assumed.
            val allRashis = mutableListOf<Map<String, Any?>>()
            var jd = AstroTime.julianDay(2026, 1, 1, 6.0)
            while (jd < AstroTime.julianDay(2027, 1, 1, 6.0)) {
                val date = java.util.Date(AstroTime.millisFromJulianDay(jd))
                for (rashi in 0..11) {
                    val row = mutableMapOf<String, Any?>("jd" to jd, "rashiIdx" to rashi)
                    for (p in planets) row[p] = TransitCalculator.getTransitHouse(rashi, p, date)
                    allRashis.add(row)
                }
                jd += 7.0
            }
            write("transit_all_rashis.json", allRashis)

            // Mid-week, every day for four years. A whole run of consecutive
            // days is the only way to see which side of the week a Sunday and
            // a Saturday are thrown to, and the answers differ by six days.
            val midWeek = mutableListOf<Map<String, Any?>>()
            var ms = AstroTime.millisFromJulianDay(AstroTime.julianDay(2024, 1, 1, 3.5))
            val endMs = AstroTime.millisFromJulianDay(AstroTime.julianDay(2028, 1, 1, 3.5))
            while (ms < endMs) {
                val date = java.util.Date(ms)
                val cal = java.util.Calendar.getInstance()
                cal.time = date
                midWeek.add(
                    mapOf(
                        "ms" to ms,
                        "dayOfWeek" to cal.get(java.util.Calendar.DAY_OF_WEEK),
                        "midWeekMs" to TransitCalculator.midWeekDate(date).time,
                    )
                )
                ms += 86_400_000L
            }
            write("transit_mid_week.json", midWeek)

            // The driver planet and its house for each period, plus every
            // string the readings are built from.
            val drivers = mutableListOf<Map<String, Any?>>()
            var dms = AstroTime.millisFromJulianDay(AstroTime.julianDay(2026, 1, 1, 3.5))
            val dEnd = AstroTime.millisFromJulianDay(AstroTime.julianDay(2027, 1, 1, 3.5))
            while (dms < dEnd) {
                val date = java.util.Date(dms)
                for (period in listOf("TODAY", "WEEK", "MONTH")) {
                    for (rashi in 0..11) {
                        drivers.add(
                            mapOf(
                                "ms" to dms,
                                "period" to period,
                                "rashiIdx" to rashi,
                                "house" to TransitCalculator.getDriverHouse(rashi, period, date),
                                "driverPlanet" to TransitCalculator.driverPlanetForPeriod(period),
                                "periodWordHi" to TransitCalculator.periodWordHi(period),
                                "periodWordEn" to TransitCalculator.periodWordEn(period),
                                "periodWordEnCap" to TransitCalculator.periodWordEnCap(period),
                            )
                        )
                    }
                }
                dms += 86_400_000L
            }
            write("transit_driver.json", drivers)

            // The Hindi planet names, including the fallback for a name the
            // table does not carry.
            write(
                "transit_planet_names.json",
                (planets + listOf("Uranus", "")).map {
                    mapOf("planet" to it, "hi" to TransitCalculator.planetNameHi(it))
                },
            )
        } finally {
            java.util.TimeZone.setDefault(savedZone)
            java.util.Locale.setDefault(savedLocale)
        }
    }

    @Test
    fun exportBirthData() {
        // Every accepted birth moment, with the Julian day it resolves to.
        // The 1940s rows matter most: India ran on UTC+6:30 through the war,
        // so a fixed +5:30 in the port would land these on the wrong instant.
        val valid = birthMoments().map { m ->
            val year = m["year"] as Int
            val month = m["month"] as Int
            val day = m["day"] as Int
            val hour = m["hour"] as Int
            val minute = m["minute"] as Int
            val dob = "%04d-%02d-%02d".format(year, month, day)
            val tob = "%02d:%02d".format(hour, minute)
            val b = BirthData.parse(
                " Ravi  Kumar ", dob, tob, m["city"] as String,
                m["lat"] as Double, m["lng"] as Double,
            )
            mapOf(
                "dob" to dob, "tob" to tob,
                "lat" to b.latitude, "lng" to b.longitude,
                "name" to b.name, "placeName" to b.placeName,
                "year" to b.year, "month" to b.month, "day" to b.day,
                "hour" to b.hour, "minute" to b.minute,
                "julianDay" to b.julianDay,
                "dateString" to b.dateString, "timeString" to b.timeString,
            )
        }
        write("birth_data_valid.json", valid)

        // Every way the form can be wrong, and the exact sentence each one
        // produces. These are read by a person who has just been refused, so
        // the port may not paraphrase them.
        val bad = listOf(
            listOf("", "1994-08-25", "14:15", 26.9124, 75.7873),
            listOf("   ", "1994-08-25", "14:15", 26.9124, 75.7873),
            listOf("<script>alert(1)</script>", "1994-08-25", "14:15", 26.9124, 75.7873),
            listOf("Ravi", "25-08-1994", "14:15", 26.9124, 75.7873),
            listOf("Ravi", "1994/08/25", "14:15", 26.9124, 75.7873),
            listOf("Ravi", "1994-08", "14:15", 26.9124, 75.7873),
            listOf("Ravi", "abcd-ef-gh", "14:15", 26.9124, 75.7873),
            listOf("Ravi", "1799-08-25", "14:15", 26.9124, 75.7873),
            listOf("Ravi", "2201-08-25", "14:15", 26.9124, 75.7873),
            listOf("Ravi", "1994-00-25", "14:15", 26.9124, 75.7873),
            listOf("Ravi", "1994-13-25", "14:15", 26.9124, 75.7873),
            listOf("Ravi", "1994-02-30", "14:15", 26.9124, 75.7873),
            listOf("Ravi", "1900-02-29", "14:15", 26.9124, 75.7873),
            listOf("Ravi", "2000-02-29", "14:15", 26.9124, 75.7873),
            listOf("Ravi", "1994-04-31", "14:15", 26.9124, 75.7873),
            listOf("Ravi", "1994-08-00", "14:15", 26.9124, 75.7873),
            listOf("Ravi", "1994-08-25", "1415", 26.9124, 75.7873),
            listOf("Ravi", "1994-08-25", "14:15:00", 26.9124, 75.7873),
            listOf("Ravi", "1994-08-25", "ab:cd", 26.9124, 75.7873),
            listOf("Ravi", "1994-08-25", "24:00", 26.9124, 75.7873),
            listOf("Ravi", "1994-08-25", "-1:00", 26.9124, 75.7873),
            listOf("Ravi", "1994-08-25", "14:60", 26.9124, 75.7873),
            listOf("Ravi", "1994-08-25", "14:15", 90.1, 75.7873),
            listOf("Ravi", "1994-08-25", "14:15", -90.1, 75.7873),
            listOf("Ravi", "1994-08-25", "14:15", 26.9124, 180.1),
            listOf("Ravi", "1994-08-25", "14:15", 26.9124, -180.1),
        )
        write(
            "birth_data_errors.json",
            bad.map { row ->
                val name = row[0] as String
                val dob = row[1] as String
                val tob = row[2] as String
                val lat = row[3] as Double
                val lng = row[4] as Double
                val e = runCatching {
                    BirthData.parse(name, dob, tob, "Jaipur", lat, lng)
                }.exceptionOrNull() as? BirthDataException
                mapOf(
                    "name" to name, "dob" to dob, "tob" to tob,
                    "lat" to lat, "lng" to lng,
                    "messageHi" to e?.messageHi, "messageEn" to e?.messageEn,
                )
            },
        )

        // Free text goes to the database, the PDF and the share sheet. What
        // survives that cleaning, and what does not, is exported through the
        // name field rather than by reaching into SecurityUtils.
        val texts = listOf(
            "Ravi",
            "  Ravi  ",
            "Ravi" + "\t" + "Kumar",
            "<b>Ravi</b>",
            "<script>x</script>Ravi",
            "<SCRIPT>x</SCRIPT>Ravi",
            "Ravi<",
            "Ravi>",
            "a<b>c</b>d",
            "Ravi" + "\u0000" + "Kumar",
            "Ravi" + "\u007f" + "Kumar",
            "Ravi" + "\u001b" + "Kumar",
            "Ravi" + "\n" + "Kumar",
            "रवि कुमार",
            "Ravi & Co.",
            "O'Brien",
            "x".repeat(300),
            "<div>" + "y".repeat(300) + "</div>",
        )
        write(
            "birth_data_sanitised.json",
            texts.map { t ->
                val parsed = runCatching {
                    BirthData.parse(t, "1994-08-25", "14:15", t, 26.9124, 75.7873)
                }.getOrNull()
                mapOf(
                    "input" to t,
                    "name" to parsed?.name,
                    "placeName" to parsed?.placeName,
                    "refused" to (parsed == null),
                )
            },
        )
    }

    @Test
    fun exportGunaKoots() {
        // Each koot exhaustively, over every index it can be handed. The
        // tables are small enough that nothing needs sampling - 12x12 or
        // 27x27 is the whole domain, so a single wrong cell cannot hide.
        val rashiPairs = mutableListOf<Map<String, Any?>>()
        for (b in 0..11) for (g in 0..11) {
            rashiPairs.add(
                mapOf(
                    "b" to b, "g" to g,
                    "varna" to KundaliMatchingCalculator.calculateVarna(b, g),
                    "vashya" to KundaliMatchingCalculator.calculateVashya(b, g),
                    "grahaMaitri" to KundaliMatchingCalculator.calculateGrahaMaitri(b, g),
                    "bhakoot" to KundaliMatchingCalculator.calculateBhakoot(b, g),
                )
            )
        }
        write("guna_rashi_koots.json", rashiPairs)

        val nakPairs = mutableListOf<Map<String, Any?>>()
        for (b in 0..26) for (g in 0..26) {
            nakPairs.add(
                mapOf(
                    "b" to b, "g" to g,
                    "tara" to KundaliMatchingCalculator.calculateTara(b, g),
                    "yoni" to KundaliMatchingCalculator.calculateYoni(b, g),
                    "gana" to KundaliMatchingCalculator.calculateGana(b, g),
                    "nadi" to KundaliMatchingCalculator.calculateNadi(b, g),
                )
            )
        }
        write("guna_nakshatra_koots.json", nakPairs)

        write(
            "guna_tables.json",
            listOf(
                mapOf(
                    "varnaRanks" to KundaliMatchingCalculator.VARNA_RANKS,
                    "vashyaGroup" to KundaliMatchingCalculator.VASHYA_GROUP,
                    "nakshatraYoni" to KundaliMatchingCalculator.NAKSHATRA_YONI,
                    "nakshatraGana" to KundaliMatchingCalculator.NAKSHATRA_GANA,
                    "nakshatraNadi" to KundaliMatchingCalculator.NAKSHATRA_NADI,
                    "bhakootDoshaDistances" to KundaliMatchingCalculator.BHAKOOT_DOSHA_DISTANCES,
                )
            ),
        )
    }

    @Test
    fun exportGunaMatching() {
        // Whole matches, in both languages, with and without birth places.
        // Without them the Manglik verdict cannot be reached at all, and the
        // app says so rather than casting both charts for Jaipur - which is
        // what it used to do, and stated as fact.
        var seed = 314159265L
        fun next(bound: Int): Int {
            seed = (seed * 6364136223846793005L + 1442695040888963407L)
            return (((seed ushr 33).toInt() % bound) + bound) % bound
        }
        fun person(): Map<String, Any?> {
            val (city, lat, lng) = cities[next(cities.size)]
            return mapOf(
                "dob" to "%04d-%02d-%02d".format(1950 + next(60), 1 + next(12), 1 + next(28)),
                "tob" to "%02d:%02d".format(next(24), next(60)),
                "city" to city, "lat" to lat, "lng" to lng,
            )
        }

        val rows = mutableListOf<Map<String, Any?>>()
        for (i in 0 until 150) {
            val boy = person()
            val girl = person()
            val withPlace = i % 2 == 0
            val boyName = "Boy$i"
            val girlName = "Girl$i"

            fun runMatch(): GunaMatchingResult = KundaliMatchingCalculator.matchKundali(
                boyName, boy["dob"] as String, boy["tob"] as String,
                girlName, girl["dob"] as String, girl["tob"] as String,
                if (withPlace) boy["lat"] as Double else null,
                if (withPlace) boy["lng"] as Double else null,
                if (withPlace) girl["lat"] as Double else null,
                if (withPlace) girl["lng"] as Double else null,
            )

            com.example.util.LanguageManager.setLanguage(com.example.util.AppLanguage.HINDI)
            val hi = runMatch()
            com.example.util.LanguageManager.setLanguage(com.example.util.AppLanguage.ENGLISH)
            val en = runMatch()
            com.example.util.LanguageManager.setLanguage(com.example.util.AppLanguage.HINDI)

            rows.add(
                mapOf(
                    "boyName" to boyName, "girlName" to girlName,
                    "boyDob" to boy["dob"], "boyTob" to boy["tob"],
                    "girlDob" to girl["dob"], "girlTob" to girl["tob"],
                    "withPlace" to withPlace,
                    "boyLat" to (if (withPlace) boy["lat"] else null),
                    "boyLng" to (if (withPlace) boy["lng"] else null),
                    "girlLat" to (if (withPlace) girl["lat"] else null),
                    "girlLng" to (if (withPlace) girl["lng"] else null),
                    "totalGuna" to hi.totalObtainedGuna,
                    "maxGuna" to hi.maxGuna,
                    "scoreCategory" to hi.scoreCategory,
                    "isManglikBoy" to hi.isManglikBoy,
                    "isManglikGirl" to hi.isManglikGirl,
                    "mangalDoshaStatusHi" to hi.mangalDoshaStatusHi,
                    "mangalDoshaStatusEn" to hi.mangalDoshaStatusEn,
                    "hasNadiDosha" to hi.hasNadiDosha,
                    "nadiDoshaStatusHi" to hi.nadiDoshaStatusHi,
                    "nadiDoshaStatusEn" to hi.nadiDoshaStatusEn,
                    "hasBhakootDosha" to hi.hasBhakootDosha,
                    "bhakootDoshaStatusHi" to hi.bhakootDoshaStatusHi,
                    "bhakootDoshaStatusEn" to hi.bhakootDoshaStatusEn,
                    "verdictHi" to hi.compatibilityVerdictHi,
                    "verdictEn" to hi.compatibilityVerdictEn,
                    "summaryHi" to hi.summaryReadingHi,
                    "summaryEn" to hi.summaryReadingEn,
                    "boyMoonRashiHi" to hi.boyMoonRashi, "boyMoonRashiEn" to en.boyMoonRashi,
                    "girlMoonRashiHi" to hi.girlMoonRashi, "girlMoonRashiEn" to en.girlMoonRashi,
                    "boyNakshatraHi" to hi.boyNakshatra, "boyNakshatraEn" to en.boyNakshatra,
                    "girlNakshatraHi" to hi.girlNakshatra, "girlNakshatraEn" to en.girlNakshatra,
                    "boyNadiHi" to hi.boyNadi, "boyNadiEn" to en.boyNadi,
                    "girlNadiHi" to hi.girlNadi, "girlNadiEn" to en.girlNadi,
                    "boyGanaHi" to hi.boyGana, "boyGanaEn" to en.boyGana,
                    "girlGanaHi" to hi.girlGana, "girlGanaEn" to en.girlGana,
                    "boyYoniHi" to hi.boyYoni, "boyYoniEn" to en.boyYoni,
                    "girlYoniHi" to hi.girlYoni, "girlYoniEn" to en.girlYoni,
                    "boyVarnaHi" to hi.boyVarna, "boyVarnaEn" to en.boyVarna,
                    "girlVarnaHi" to hi.girlVarna, "girlVarnaEn" to en.girlVarna,
                    "boyVashyaHi" to hi.boyVashya, "boyVashyaEn" to en.boyVashya,
                    "girlVashyaHi" to hi.girlVashya, "girlVashyaEn" to en.girlVashya,
                    "koots" to hi.kootDetails.mapIndexed { idx, k ->
                        mapOf(
                            "index" to idx,
                            "nameHi" to k.kootNameHi, "nameEn" to k.kootNameEn,
                            "max" to k.maxPoints, "obtained" to k.obtainedPoints,
                            "descriptionHi" to k.descriptionHi,
                            "descriptionEn" to k.descriptionEn,
                            "isFavorable" to k.isFavorable,
                        )
                    },
                )
            )
        }
        write("guna_matching.json", rows)
    }
}
