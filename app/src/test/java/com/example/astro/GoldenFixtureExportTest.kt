package com.example.astro

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
}
