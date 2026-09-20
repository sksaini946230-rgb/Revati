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
            mapOf("key" to "GREGORIAN_MONTH_HI", "values" to AstroNames.GREGORIAN_MONTH_HI),
            mapOf("key" to "VARA_HI", "values" to AstroNames.VARA_HI),
            mapOf("key" to "VARA_EN", "values" to AstroNames.VARA_EN),
            // The karana tables are private and the two lookups fold a movable
            // cycle together with four fixed karanas, so the whole index range
            // is exported as a flat table rather than the pieces.
            mapOf("key" to "KARANA_HI", "values" to (0..59).map { AstroNames.karanaHi(it) }),
            mapOf("key" to "KARANA_EN", "values" to (0..59).map { AstroNames.karanaEn(it) }),
            mapOf("key" to "SINGLES", "values" to listOf(
                AstroNames.AMAVASYA_HI, AstroNames.AMAVASYA_EN,
                AstroNames.RETROGRADE_MARK, AstroNames.SHUKLA_HI, AstroNames.SHUKLA_EN,
                AstroNames.KRISHNA_HI, AstroNames.KRISHNA_EN,
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

    /**
     * Names used for both the transliterator and the numerology reading.
     *
     * Devanagari is the point: the Chaldean table only covers A-Z, so a name
     * typed in Hindi - which is what a Hindi-first app whose own placeholder
     * reads "उदा. राहुल शर्मा" invites - summed to zero and came out as Name
     * Number 1 for everybody. The pairs here also check the property the
     * transliterator exists for, that "राहुल" and "Rahul" agree.
     */
    private fun numerologyNames(): List<String> = listOf(
        "Rahul", "राहुल", "rahul", "RAHUL",
        "कमल", "Kamal", "शर्मा", "Sharma",
        "राहुल शर्मा", "Rahul Sharma",
        "सूर्य", "अनिता", "ऋषभ", "श्री", "कृष्ण", "विद्या",
        "प्रेम", "संजय", "गंगा", "सिंह", "चन्द्र", "मोहन",
        "क़ासिम", "ख़ान", "ज़ाहिर", "फ़रीद", "ग़ालिब", "बड़ा", "पढ़ना",
        "अंश", "अँगूठा", "दुःख", "स्वर्ग", "अक्षय", "विद्यालय",
        "देवी", "गौरव", "ऐश्वर्या", "औरत", "इंदु", "ईशा", "उमा", "ऊषा",
        "एकता", "ओम", "आरती", "अजय",
        "Rahul123", "R", "Zoe", "O'Brien", "Anne-Marie", "  Ravi  ",
        "राहुल Sharma", "12345", "!@#",
    )

    @Test
    fun exportDevanagariTransliteration() {
        write(
            "devanagari.json",
            numerologyNames().map { name ->
                mapOf(
                    "input" to name,
                    "containsDevanagari" to DevanagariTransliterator.containsDevanagari(name),
                    "latin" to DevanagariTransliterator.transliterate(name),
                )
            },
        )
    }

    @Test
    fun exportNumerology() {
        val dobs = listOf(
            "1994-08-25", "2000-01-01", "1999-12-31", "1980-10-09",
            "1900-01-01", "2024-02-29", "1977-07-07", "1966-06-06",
            "2011-11-11", "1988-08-08", "1955-05-05", "1943-03-03",
            // Shapes the calculator tolerates rather than refuses. The day
            // part is read by splitting on "-", so a malformed string falls
            // back to 1 rather than throwing, and that fallback is a real
            // answer the screen prints.
            "1994-08", "19940825", "", "1994-08-0", "1994-08-99",
        )
        val rows = mutableListOf<Map<String, Any?>>()
        for (name in numerologyNames()) {
            for (dob in dobs) {
                val d = NumerologyCalculator.calculateNumerology(name, dob)
                rows.add(
                    mapOf(
                        "name" to name, "dob" to dob,
                        "moolank" to d.moolank,
                        "bhagyank" to d.bhagyank,
                        "nameNumber" to d.nameNumber,
                        "rulingPlanetHi" to d.rulingPlanetHi,
                        "rulingPlanetEn" to d.rulingPlanetEn,
                        "luckyDaysHi" to d.luckyDaysHi,
                        "luckyDaysEn" to d.luckyDaysEn,
                        "luckyColorsHi" to d.luckyColorsHi,
                        "luckyColorsEn" to d.luckyColorsEn,
                        "friendlyNumbers" to d.friendlyNumbers,
                        "enemyNumbers" to d.enemyNumbers,
                        "moolankReadingHi" to d.moolankReadingHi,
                        "moolankReadingEn" to d.moolankReadingEn,
                        "bhagyankReadingHi" to d.bhagyankReadingHi,
                        "bhagyankReadingEn" to d.bhagyankReadingEn,
                        "personName" to d.personName,
                        "dateOfBirth" to d.dateOfBirth,
                    )
                )
            }
        }
        write("numerology.json", rows)
    }

    @Test
    fun exportNumerologyValidation() {
        // "Today" is pinned, because the upper year bound and the future
        // check both read it. It used to be the literal 2026, which would
        // have refused a baby born on 1 January 2027 and told the parent the
        // child's birth year was out of range.
        fun todayAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): java.util.Calendar =
            java.util.GregorianCalendar(AstroTime.IST).apply {
                clear()
                set(year, month - 1, day, hour, minute, 0)
            }

        val todays = listOf(
            todayAt(2026, 9, 20, 12, 0),
            todayAt(2027, 1, 1, 0, 1),
            todayAt(2027, 12, 31, 23, 59),
        )

        val names = listOf(
            "", "  ", "R", "Ra", "राहुल", "र", "रा", "12", "1", "!!", "!!!",
            "R2", "  Ravi  ", "O'B", "अ",
        )
        write(
            "numerology_name_errors.json",
            names.map { mapOf("name" to it, "error" to NumerologyValidator.validateName(it)) },
        )

        val dobs = listOf(
            "", "   ", "1994-08-25", "1994-8-25", "94-08-25", "1994/08/25",
            "1994-00-25", "1994-13-25", "1994-08-00", "1994-08-32",
            "1994-02-29", "1996-02-29", "1900-02-29", "2000-02-29",
            "1994-04-31", "1994-06-31", "1994-09-31", "1994-11-31",
            "1899-08-25", "1900-01-01", "2026-09-20", "2026-09-21",
            "2026-12-31", "2027-01-01", "2028-01-01", "abcd-ef-gh",
            " 1994-08-25 ", "1994-08-25x",
        )
        val rows = mutableListOf<Map<String, Any?>>()
        for ((index, today) in todays.withIndex()) {
            for (dob in dobs) {
                val result = NumerologyValidator.validateInput("Ravi", dob, today)
                rows.add(
                    mapOf(
                        "todayIndex" to index,
                        "todayYear" to today.get(java.util.Calendar.YEAR),
                        "todayMonth" to today.get(java.util.Calendar.MONTH) + 1,
                        "todayDay" to today.get(java.util.Calendar.DAY_OF_MONTH),
                        "todayHour" to today.get(java.util.Calendar.HOUR_OF_DAY),
                        "todayMinute" to today.get(java.util.Calendar.MINUTE),
                        "dob" to dob,
                        "dobError" to NumerologyValidator.validateDob(dob, today),
                        "isValid" to result.isValid,
                        "nameError" to result.nameError,
                        "resultDobError" to result.dobError,
                    )
                )
            }
        }
        write("numerology_dob_errors.json", rows)
        write("numerology_min_year.json", listOf(mapOf("minYear" to NumerologyValidator.MIN_YEAR)))
    }

    @Test
    fun exportPanchang() {
        // Every day of a whole year in six cities, plus a second year in one,
        // in both languages and in both clock formats. A year so that every
        // weekday meets every season: Rahu Kaal and the muhurtas are slices
        // of the real day length, which is the part a fixed 90-minute slot
        // would quietly get wrong.
        val rows = mutableListOf<Map<String, Any?>>()

        fun row(date: java.util.Date, city: com.example.data.model.CityLocation, use24: Boolean): Map<String, Any?> {
            com.example.util.LanguageManager.setLanguage(com.example.util.AppLanguage.HINDI)
            val hi = PanchangCalculator.calculatePanchang(date, city, use24)
            com.example.util.LanguageManager.setLanguage(com.example.util.AppLanguage.ENGLISH)
            val en = PanchangCalculator.calculatePanchang(date, city, use24)
            com.example.util.LanguageManager.setLanguage(com.example.util.AppLanguage.HINDI)

            val phaseHi = PanchangCalculator.getMoonPhaseInfo(hi.pakshaHindi, hi.tithiHindi)
            com.example.util.LanguageManager.setLanguage(com.example.util.AppLanguage.ENGLISH)
            val phaseEn = PanchangCalculator.getMoonPhaseInfo(hi.pakshaHindi, hi.tithiHindi)
            com.example.util.LanguageManager.setLanguage(com.example.util.AppLanguage.HINDI)

            return mapOf(
                "ms" to date.time,
                "city" to city.cityName,
                "cityHindi" to city.cityNameHindi,
                "lat" to city.latitude,
                "lng" to city.longitude,
                "use24Hour" to use24,
                "dateString" to hi.dateString,
                "dayOfWeek" to hi.dayOfWeek,
                "dayOfWeekHindi" to hi.dayOfWeekHindi,
                "vikramSamvat" to hi.vikramSamvat,
                "sakaSamvat" to hi.sakaSamvat,
                "masaName" to hi.masaName,
                "masaNameHindi" to hi.masaNameHindi,
                "paksha" to hi.paksha,
                "pakshaHindi" to hi.pakshaHindi,
                "tithi" to hi.tithi,
                "tithiHindi" to hi.tithiHindi,
                "tithiEndTimeHi" to hi.tithiEndTime,
                "tithiEndTimeEn" to en.tithiEndTime,
                "tithiProgressPercent" to hi.tithiProgressPercent.toDouble(),
                "nakshatra" to hi.nakshatra,
                "nakshatraHindi" to hi.nakshatraHindi,
                "nakshatraEndTimeHi" to hi.nakshatraEndTime,
                "nakshatraEndTimeEn" to en.nakshatraEndTime,
                "nakshatraPada" to hi.nakshatraPada,
                "yoga" to hi.yoga,
                "yogaHindi" to hi.yogaHindi,
                "karan" to hi.karan,
                "karanHindi" to hi.karanHindi,
                "sunrise" to hi.sunrise,
                "sunset" to hi.sunset,
                "moonrise" to hi.moonrise,
                "moonset" to hi.moonset,
                "rahuKaal" to hi.rahuKaal,
                "gulikaKaal" to hi.gulikaKaal,
                "yamaganda" to hi.yamaganda,
                "abhijitMuhurat" to hi.abhijitMuhurat,
                "brahmaMuhurat" to hi.brahmaMuhurat,
                "sunSignHi" to hi.sunSign,
                "sunSignEn" to en.sunSign,
                "moonSignHi" to hi.moonSign,
                "moonSignEn" to en.moonSign,
                "locationName" to hi.locationName,
                "phaseEmoji" to phaseHi.emoji,
                "phaseNameHi" to phaseHi.nameHindi,
                "phaseNamePicked" to phaseEn.nameHindi,
                "phaseNameEn" to phaseHi.nameEn,
                "phaseIllumination" to phaseHi.illuminationPercent,
                "phaseIsWaxing" to phaseHi.isWaxing,
                "planets" to hi.planets.map {
                    mapOf(
                        "en" to it.planetNameEn, "hi" to it.planetNameHi,
                        "rashiNumber" to it.rashiNumber,
                        "rashiNameHi" to it.rashiNameHi, "rashiNameEn" to it.rashiNameEn,
                        "degree" to it.degree, "house" to it.houseNumber,
                        "retro" to it.isRetrograde,
                        "nakshatraHi" to it.nakshatraHi, "nakshatraEn" to it.nakshatraEn,
                    )
                },
            )
        }

        // One year, six cities, 12-hour clock.
        var jd = AstroTime.julianDay(2026, 1, 1, 3.0)
        val end = AstroTime.julianDay(2027, 1, 1, 3.0)
        while (jd < end) {
            val date = java.util.Date(AstroTime.millisFromJulianDay(jd))
            for ((name, lat, lng) in cities) {
                val city = com.example.data.model.CityLocation(name, name, "—", lat, lng)
                rows.add(row(date, city, false))
            }
            jd += 1.0
        }

        // A second year in Jaipur on the 24-hour clock, so both formats and a
        // leap year are covered.
        val jaipur = com.example.data.model.CityLocation("Jaipur", "जयपुर", "Rajasthan", 26.9124, 75.7873)
        var jd2 = AstroTime.julianDay(2024, 1, 1, 3.0)
        val end2 = AstroTime.julianDay(2025, 1, 1, 3.0)
        while (jd2 < end2) {
            rows.add(row(java.util.Date(AstroTime.millisFromJulianDay(jd2)), jaipur, true))
            jd2 += 1.0
        }

        write("panchang.json", rows)

        write(
            "panchang_cities.json",
            PanchangCalculator.popularCities.map {
                mapOf(
                    "cityName" to it.cityName, "cityNameHindi" to it.cityNameHindi,
                    "state" to it.state, "latitude" to it.latitude, "longitude" to it.longitude,
                )
            },
        )

        // The minute formatter on its own, including the wrap either side of
        // midnight - Brahma Muhurta is computed as a negative minute count
        // before sunrise, so the negative branch is load-bearing.
        write(
            "panchang_minutes.json",
            (-200..1640 step 7).map {
                mapOf(
                    "minutes" to it,
                    "h12" to PanchangCalculator.formatMinutesToTime(it, false),
                    "h24" to PanchangCalculator.formatMinutesToTime(it, true),
                )
            },
        )
    }

    @Test
    fun exportMuhurats() {
        // `getUpcomingMuhurats` scans the sixty days after today, and today
        // comes from the clock. The export records the IST date it ran on and
        // refuses to write a fixture that straddles midnight, so the port can
        // be handed the same starting day and must find the same windows.
        val zone = AstroTime.IST
        val beforeMs = System.currentTimeMillis()
        val results = PanchangCalculator.popularCities.take(6).flatMap { city ->
            listOf(false, true).map { use24 ->
                Triple(city, use24, MuhuratCalculator.getUpcomingMuhurats(city, use24))
            }
        }
        val afterMs = System.currentTimeMillis()

        fun istDay(ms: Long): Int {
            val cal = java.util.GregorianCalendar(zone).apply { timeInMillis = ms }
            return cal.get(java.util.Calendar.YEAR) * 10000 +
                (cal.get(java.util.Calendar.MONTH) + 1) * 100 +
                cal.get(java.util.Calendar.DAY_OF_MONTH)
        }
        check(istDay(beforeMs) == istDay(afterMs)) {
            "the muhurat export crossed midnight; run it again"
        }

        write(
            "muhurats.json",
            results.map { (city, use24, items) ->
                mapOf(
                    "city" to city.cityName,
                    "lat" to city.latitude, "lng" to city.longitude,
                    "use24Hour" to use24,
                    "istDay" to istDay(beforeMs),
                    "items" to items.map {
                        mapOf(
                            "id" to it.id,
                            "categoryHi" to it.categoryHi, "categoryEn" to it.categoryEn,
                            "dateString" to it.dateString,
                            "dayOfWeekHi" to it.dayOfWeekHi, "dayOfWeekEn" to it.dayOfWeekEn,
                            "startTime" to it.startTime, "endTime" to it.endTime,
                            "tithiHi" to it.tithiHi, "tithiEn" to it.tithiEn,
                            "nakshatraHi" to it.nakshatraHi, "nakshatraEn" to it.nakshatraEn,
                            "qualityHi" to it.qualityHi, "qualityEn" to it.qualityEn,
                            "descriptionHi" to it.descriptionHi, "descriptionEn" to it.descriptionEn,
                        )
                    },
                )
            },
        )
    }

    @Test
    fun exportFestivals() {
        // The whole pipeline: the rule table, the date each rule resolves to,
        // and every string the screen shows. `getFestivals` covers this year
        // and next, which is what keeps the upcoming list from emptying in
        // December.
        write(
            "festivals.json",
            FestivalProvider.getFestivals().map {
                mapOf(
                    "id" to it.id,
                    "nameEn" to it.nameEn, "nameHi" to it.nameHi,
                    "dateString" to it.dateString, "dateIso" to it.dateIso,
                    "dayNameHi" to it.dayNameHi,
                    "monthNameHi" to it.monthNameHi,
                    "pakshaHi" to it.pakshaHi, "tithiHi" to it.tithiHi,
                    "regionFilter" to it.regionFilter,
                    "significanceEn" to it.significanceEn,
                    "significanceHi" to it.significanceHi,
                    "pujaVidhiHi" to it.pujaVidhiHi,
                    "pujaVidhiEn" to it.pujaVidhiEn,
                )
            },
        )

        // `dateFor` on its own, over the seven years FestivalCalculatorTest
        // checks against published panchang - chosen to include an Adhika
        // month in 2029 and the Raksha Bandhan edge cases of 2026 and 2031.
        // The month, paksha, tithi and observance of each rule are written
        // out here rather than read from the provider, because the rule table
        // is private; a mistake in one of them moves a date, and the dates
        // are what the comparison checks.
        val rules = listOf(
            listOf("f1", "श्रावण", "शुक्ल पक्ष", "पूर्णिमा", "RAKSHA_BANDHAN"),
            listOf("f2", "भाद्रपद", "कृष्ण पक्ष", "अष्टमी", "SUNRISE"),
            listOf("f3", "भाद्रपद", "शुक्ल पक्ष", "चतुर्थी", "MADHYAHNA"),
            listOf("f4", "आश्विन", "शुक्ल पक्ष", "प्रतिपदा", "SUNRISE"),
            listOf("f5", "आश्विन", "शुक्ल पक्ष", "दशमी", "APARAHNA"),
            listOf("f6", "कार्तिक", "कृष्ण पक्ष", "चतुर्थी", "CHANDRODAYA"),
            listOf("f7", "कार्तिक", "कृष्ण पक्ष", "त्रयोदशी", "PRADOSH"),
            listOf("f8", "कार्तिक", "अमावस्या", "अमावस्या", "PRADOSH"),
            listOf("f9", "कार्तिक", "शुक्ल पक्ष", "प्रतिपदा", "SUNRISE"),
            listOf("f10", "कार्तिक", "शुक्ल पक्ष", "षष्ठी", "SUNRISE"),
            listOf("f11", "चैत्र", "शुक्ल पक्ष", "तृतीया", "SUNRISE"),
            listOf("f12", "श्रावण", "शुक्ल पक्ष", "तृतीया", "SUNRISE"),
        )

        val rows = mutableListOf<Map<String, Any?>>()
        for (year in 2025..2031) {
            for (rule in rules) {
                val masa = FestivalCalculator.masaIndexFor(rule[1])
                val tithi = FestivalCalculator.tithiNumberFor(rule[2], rule[3])
                val observance = FestivalCalculator.Observance.valueOf(rule[4])
                val date = if (masa == null || tithi == null) null else {
                    FestivalCalculator.dateFor(masa, tithi, year, observance)
                }
                rows.add(
                    mapOf(
                        "ruleId" to rule[0],
                        "monthNameHi" to rule[1],
                        "pakshaHi" to rule[2],
                        "tithiHi" to rule[3],
                        "observance" to rule[4],
                        "year" to year,
                        "masaIndex" to masa,
                        "tithiNumber" to tithi,
                        "dateIso" to date?.let {
                            "%04d-%02d-%02d".format(
                                it.get(java.util.Calendar.YEAR),
                                it.get(java.util.Calendar.MONTH) + 1,
                                it.get(java.util.Calendar.DAY_OF_MONTH),
                            )
                        },
                    )
                )
            }
        }
        write("festival_dates.json", rows)

        // The two lookups on their own, including the names they refuse.
        val tithiNames = AstroNames.TITHI_HI + listOf("अमावस्या", "पूर्णिमा", "कुछ और", "")
        val pakshas = listOf("शुक्ल पक्ष", "कृष्ण पक्ष", "अमावस्या", "")
        write(
            "festival_lookups.json",
            buildList {
                for (paksha in pakshas) for (tithi in tithiNames) {
                    add(
                        mapOf(
                            "kind" to "tithi",
                            "paksha" to paksha, "tithi" to tithi,
                            "value" to FestivalCalculator.tithiNumberFor(paksha, tithi),
                        )
                    )
                }
                for (masa in AstroNames.MASA_HI + listOf("कुछ और", "")) {
                    add(
                        mapOf(
                            "kind" to "masa",
                            "paksha" to null, "tithi" to masa,
                            "value" to FestivalCalculator.masaIndexFor(masa),
                        )
                    )
                }
            },
        )
    }

    @Test
    fun exportRashifal() {
        // The five template tables, read out by reflection rather than
        // retyped. Each is 12 houses x 3 variations, in two languages - 360
        // paragraphs of Devanagari and English that the screen shows word for
        // word, and the only kind of content a numeric fixture would never
        // catch drifting. Reflection rather than parsing the source, because
        // it reads the same objects the app uses.
        @Suppress("UNCHECKED_CAST")
        fun table(name: String): List<List<String>> {
            val field = RashifalProvider::class.java.getDeclaredField(name)
            field.isAccessible = true
            return field.get(RashifalProvider) as List<List<String>>
        }

        val names = listOf(
            "GENERAL_HI", "GENERAL_EN", "CAREER_HI", "CAREER_EN",
            "FINANCE_HI", "FINANCE_EN", "LOVE_HI", "LOVE_EN",
            "HEALTH_HI", "HEALTH_EN",
        )
        write(
            "rashifal_templates.json",
            names.map { name -> mapOf("key" to name, "houses" to table(name)) },
        )

        // The gochar rating over its whole domain: both driver planets,
        // every house. It used to be `3 + ((rashiIdx + house) % 3)`, which
        // gave all twelve rashis the same score because rashiIdx cancels out
        // of the house formula entirely.
        write(
            "rashifal_rating.json",
            buildList {
                for (planet in listOf("Sun", "Moon", "Mars", "Jupiter")) {
                    for (house in 1..12) {
                        add(
                            mapOf(
                                "planet" to planet, "house" to house,
                                "rating" to RashifalProvider.gocharRating(planet, house),
                            )
                        )
                    }
                }
            },
        )

        // The whole pipeline, for the instant this ran. `getHoroscope` reads
        // the clock, so the export brackets it and refuses to write a fixture
        // the port could not reproduce: the calendar fields must fall on one
        // India day, and the transit house must be the same at both ends.
        val beforeMs = System.currentTimeMillis()
        val periods = listOf("TODAY", "WEEK", "MONTH")
        val readings = periods.associateWith { RashifalProvider.getHoroscope(it) }
        val afterMs = System.currentTimeMillis()

        val zone = AstroTime.IST
        fun dayFields(ms: Long): List<Int> {
            val cal = java.util.GregorianCalendar(zone).apply { timeInMillis = ms }
            return listOf(
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH),
                cal.get(java.util.Calendar.WEEK_OF_YEAR),
            )
        }
        check(dayFields(beforeMs) == dayFields(afterMs)) {
            "the rashifal export crossed a day boundary; run it again"
        }
        for (period in periods) {
            for (rashi in 0..11) {
                check(
                    TransitCalculator.getDriverHouse(rashi, period, java.util.Date(beforeMs)) ==
                        TransitCalculator.getDriverHouse(rashi, period, java.util.Date(afterMs))
                ) { "a transit house changed while the export ran; run it again" }
            }
        }

        // The calendar fields the rotation uses, recorded so the port can be
        // held to the same week number - Java's WEEK_OF_YEAR depends on the
        // locale's first day of the week and on how many days a first week
        // needs, and both are easy to get wrong.
        val fields = dayFields(beforeMs)
        write(
            "rashifal.json",
            periods.flatMap { period ->
                (readings[period] ?: emptyList()).map { r ->
                    mapOf(
                        "nowMs" to beforeMs,
                        "year" to fields[0], "monthIndex" to fields[1],
                        "dayOfMonth" to fields[2], "weekOfYear" to fields[3],
                        "period" to period,
                        "rashiId" to r.rashiId,
                        "rashiNameEn" to r.rashiNameEn, "rashiNameHi" to r.rashiNameHi,
                        "symbol" to r.symbol,
                        "elementHi" to r.elementHi, "elementEn" to r.elementEn,
                        "rulerHi" to r.rulerHi, "rulerEn" to r.rulerEn,
                        "ratingStars" to r.ratingStars,
                        "luckyNumber" to r.luckyNumber,
                        "luckyColorHi" to r.luckyColorHi, "luckyColorEn" to r.luckyColorEn,
                        "luckyStoneHi" to r.luckyStoneHi, "luckyStoneEn" to r.luckyStoneEn,
                        "luckyTimeHi" to r.luckyTimeHi, "luckyTimeEn" to r.luckyTimeEn,
                        "generalReadingHi" to r.generalReadingHi,
                        "generalReadingEn" to r.generalReadingEn,
                        "careerReadingHi" to r.careerReadingHi,
                        "careerReadingEn" to r.careerReadingEn,
                        "healthReadingHi" to r.healthReadingHi,
                        "healthReadingEn" to r.healthReadingEn,
                        "loveReadingHi" to r.loveReadingHi,
                        "loveReadingEn" to r.loveReadingEn,
                        "financeReadingHi" to r.financeReadingHi,
                        "financeReadingEn" to r.financeReadingEn,
                        "periodOut" to r.period,
                    )
                }
            },
        )

        // Java's week number, over eight whole years. The rotation reads it,
        // so the port has to agree on it everywhere - including the end of a
        // year, where a week straddling 1 January belongs to the new year.
        val weeks = mutableListOf<Map<String, Any?>>()
        val cal = java.util.GregorianCalendar(zone).apply {
            clear(); set(2024, java.util.Calendar.JANUARY, 1, 12, 0, 0)
        }
        val endCal = java.util.GregorianCalendar(zone).apply {
            clear(); set(2032, java.util.Calendar.JANUARY, 1, 12, 0, 0)
        }
        while (cal.before(endCal)) {
            weeks.add(
                mapOf(
                    "ms" to cal.timeInMillis,
                    "year" to cal.get(java.util.Calendar.YEAR),
                    "month" to cal.get(java.util.Calendar.MONTH) + 1,
                    "day" to cal.get(java.util.Calendar.DAY_OF_MONTH),
                    "weekOfYear" to cal.get(java.util.Calendar.WEEK_OF_YEAR),
                    "firstDayOfWeek" to cal.firstDayOfWeek,
                    "minimalDaysInFirstWeek" to cal.minimalDaysInFirstWeek,
                )
            )
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        write("week_of_year.json", weeks)
    }
}
