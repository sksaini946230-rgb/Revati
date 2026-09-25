package com.example.astro

import com.example.data.model.ChoghadiyaSlot
import com.example.data.model.ChoghadiyaType
import java.util.Calendar
import java.util.Date

object ChoghadiyaCalculator {

    // Day Choghadiya by weekday, from sunrise.
    //
    // Every row is the same seven-Choghadiya cycle, rotated to begin with the
    // one ruled by that weekday's lord. The cycle is the Chaldean order of the
    // grahas — Sun, Venus, Mercury, Moon, Saturn, Jupiter, Mars — which in
    // Choghadiya names reads
    //
    //     Udveg, Char, Labh, Amrit, Kaal, Shubh, Rog
    //
    // **Sunday's row did not follow it.** Six of the seven were clean rotations
    // of that cycle and Sunday alone stepped through it three at a time, which
    // is what a copying slip looks like. The effect was not cosmetic: on a
    // Sunday the app called 07:34-09:09 Amrit, the most auspicious slot there
    // is, when the cycle makes it Char, merely neutral — and called 09:09-10:43
    // Rog, inauspicious, when it is Labh. People read this screen to choose when
    // to begin something.
    //
    // It survived because the only test on Choghadiya counted the slots. Eight
    // came back, so it passed, for one day in seven that was wrong.
    // ChoghadiyaSequenceTest now checks the sequence itself.
    private val DAY_SEQUENCES = mapOf(
        Calendar.SUNDAY to listOf(ChoghadiyaType.UDVEG, ChoghadiyaType.CHAR, ChoghadiyaType.LABH, ChoghadiyaType.AMRIT, ChoghadiyaType.KAAL, ChoghadiyaType.SHUBH, ChoghadiyaType.ROG, ChoghadiyaType.UDVEG),
        Calendar.MONDAY to listOf(ChoghadiyaType.AMRIT, ChoghadiyaType.KAAL, ChoghadiyaType.SHUBH, ChoghadiyaType.ROG, ChoghadiyaType.UDVEG, ChoghadiyaType.CHAR, ChoghadiyaType.LABH, ChoghadiyaType.AMRIT),
        Calendar.TUESDAY to listOf(ChoghadiyaType.ROG, ChoghadiyaType.UDVEG, ChoghadiyaType.CHAR, ChoghadiyaType.LABH, ChoghadiyaType.AMRIT, ChoghadiyaType.KAAL, ChoghadiyaType.SHUBH, ChoghadiyaType.ROG),
        Calendar.WEDNESDAY to listOf(ChoghadiyaType.LABH, ChoghadiyaType.AMRIT, ChoghadiyaType.KAAL, ChoghadiyaType.SHUBH, ChoghadiyaType.ROG, ChoghadiyaType.UDVEG, ChoghadiyaType.CHAR, ChoghadiyaType.LABH),
        Calendar.THURSDAY to listOf(ChoghadiyaType.SHUBH, ChoghadiyaType.ROG, ChoghadiyaType.UDVEG, ChoghadiyaType.CHAR, ChoghadiyaType.LABH, ChoghadiyaType.AMRIT, ChoghadiyaType.KAAL, ChoghadiyaType.SHUBH),
        Calendar.FRIDAY to listOf(ChoghadiyaType.CHAR, ChoghadiyaType.LABH, ChoghadiyaType.AMRIT, ChoghadiyaType.KAAL, ChoghadiyaType.SHUBH, ChoghadiyaType.ROG, ChoghadiyaType.UDVEG, ChoghadiyaType.CHAR),
        Calendar.SATURDAY to listOf(ChoghadiyaType.KAAL, ChoghadiyaType.SHUBH, ChoghadiyaType.ROG, ChoghadiyaType.UDVEG, ChoghadiyaType.CHAR, ChoghadiyaType.LABH, ChoghadiyaType.AMRIT, ChoghadiyaType.KAAL)
    )

    private val NIGHT_SEQUENCES = mapOf(
        Calendar.SUNDAY to listOf(ChoghadiyaType.SHUBH, ChoghadiyaType.AMRIT, ChoghadiyaType.CHAR, ChoghadiyaType.ROG, ChoghadiyaType.KAAL, ChoghadiyaType.LABH, ChoghadiyaType.UDVEG, ChoghadiyaType.SHUBH),
        Calendar.MONDAY to listOf(ChoghadiyaType.CHAR, ChoghadiyaType.ROG, ChoghadiyaType.KAAL, ChoghadiyaType.LABH, ChoghadiyaType.UDVEG, ChoghadiyaType.SHUBH, ChoghadiyaType.AMRIT, ChoghadiyaType.CHAR),
        Calendar.TUESDAY to listOf(ChoghadiyaType.KAAL, ChoghadiyaType.LABH, ChoghadiyaType.UDVEG, ChoghadiyaType.SHUBH, ChoghadiyaType.AMRIT, ChoghadiyaType.CHAR, ChoghadiyaType.ROG, ChoghadiyaType.KAAL),
        Calendar.WEDNESDAY to listOf(ChoghadiyaType.UDVEG, ChoghadiyaType.SHUBH, ChoghadiyaType.AMRIT, ChoghadiyaType.CHAR, ChoghadiyaType.ROG, ChoghadiyaType.KAAL, ChoghadiyaType.LABH, ChoghadiyaType.UDVEG),
        Calendar.THURSDAY to listOf(ChoghadiyaType.AMRIT, ChoghadiyaType.CHAR, ChoghadiyaType.ROG, ChoghadiyaType.KAAL, ChoghadiyaType.LABH, ChoghadiyaType.UDVEG, ChoghadiyaType.SHUBH, ChoghadiyaType.AMRIT),
        Calendar.FRIDAY to listOf(ChoghadiyaType.ROG, ChoghadiyaType.KAAL, ChoghadiyaType.LABH, ChoghadiyaType.UDVEG, ChoghadiyaType.SHUBH, ChoghadiyaType.AMRIT, ChoghadiyaType.CHAR, ChoghadiyaType.ROG),
        Calendar.SATURDAY to listOf(ChoghadiyaType.LABH, ChoghadiyaType.UDVEG, ChoghadiyaType.SHUBH, ChoghadiyaType.AMRIT, ChoghadiyaType.CHAR, ChoghadiyaType.ROG, ChoghadiyaType.KAAL, ChoghadiyaType.LABH)
    )

    private val RULERS = mapOf(
        ChoghadiyaType.AMRIT to "चन्द्र",
        ChoghadiyaType.SHUBH to "गुरु",
        ChoghadiyaType.LABH to "बुध",
        ChoghadiyaType.CHAR to "शुक्र",
        ChoghadiyaType.ROG to "मंगल",
        ChoghadiyaType.KAAL to "शनि",
        ChoghadiyaType.UDVEG to "सूर्य"
    )

    fun getChoghadiyaSlots(
        date: Date,
        isDaytime: Boolean = true,
        lat: Double = 26.9124,
        lon: Double = 75.7873,
        use24Hour: Boolean = false
    ): List<ChoghadiyaSlot> {
        val cal = Calendar.getInstance().apply { time = date }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        val sequence = if (isDaytime) {
            DAY_SEQUENCES[dayOfWeek] ?: DAY_SEQUENCES[Calendar.SUNDAY]!!
        } else {
            NIGHT_SEQUENCES[dayOfWeek] ?: NIGHT_SEQUENCES[Calendar.SUNDAY]!!
        }

        // Sunrise and sunset from the app's own ephemeris, not a second one.
        //
        // This used to carry its own inline solar formula — day-of-year, a
        // first-order equation of time, a fixed 82.5°E meridian — and it
        // disagreed with the Panchang by about nine minutes. On 7 Sep 2026 at
        // Rampur the widget and the Panchang said sunrise 05:54 AM while the
        // Choghadiya's first slot opened at 06:03. Every boundary on the screen
        // was shifted by that much, and both numbers were visible at once.
        //
        // Choghadiya slots are one eighth of the day from sunrise to sunset, so
        // they are only as good as those two moments. RiseSetCalculator is what
        // the rest of the app uses and is the tested one; there is no reason for
        // this file to have a second opinion about when the Sun comes up.
        val zone = AstroTime.IST
        val gcal = java.util.GregorianCalendar(zone).apply { time = date }
        val midnightJd = AstroTime.julianDayFromLocal(
            gcal.get(Calendar.YEAR), gcal.get(Calendar.MONTH) + 1, gcal.get(Calendar.DAY_OF_MONTH),
            0, 0, zone
        )
        val sunTimes = RiseSetCalculator.sunRiseSet(midnightJd, lat, lon)

        // Only reached where the Sun genuinely does not rise or set, which is
        // outside anywhere this app is used — the same fallback the Panchang has.
        val sunriseMin = sunTimes.riseJd?.let { minutesFromMidnight(it, midnightJd) } ?: 360
        val sunsetMin = sunTimes.setJd?.let { minutesFromMidnight(it, midnightJd) } ?: 1080

        val baseStartMin = if (isDaytime) sunriseMin else sunsetMin
        val totalDurationMin = if (isDaytime) (sunsetMin - sunriseMin) else (1440 - (sunsetMin - sunriseMin))
        val slotLen = totalDurationMin / 8.0

        return sequence.mapIndexed { idx, type ->
            val startMins = ((baseStartMin + idx * slotLen).toInt()) % 1440
            val endMins = ((baseStartMin + (idx + 1) * slotLen).toInt()) % 1440

            val startStr = formatMins(startMins, use24Hour)
            val endStr = formatMins(endMins, use24Hour)

            ChoghadiyaSlot(
                timeSlotString = "$startStr - $endStr",
                startTime = startStr,
                endTime = endStr,
                type = type,
                rulerPlanetHi = RULERS[type] ?: "सूर्य",
                isDay = isDaytime
            )
        }
    }

    /**
     * Local minutes past midnight for a UT Julian Day.
     *
     * Through Calendar, and truncating, because that is exactly what
     * PanchangCalculator.jdToLocalMinutes does — `Calendar.MINUTE` drops the
     * seconds. This used to round instead, and the difference showed: for
     * Mumbai the Panchang printed "Sunrise: 06:24 AM" while the Choghadiya
     * strip directly beneath it on the same screen opened at 06:25. One
     * instant, two numbers, a minute apart.
     *
     * Going through Calendar rather than arithmetic also keeps the two in step
     * on anything the zone does to the day, rather than only agreeing when
     * nothing unusual is happening.
     */
    private fun minutesFromMidnight(jd: Double, @Suppress("UNUSED_PARAMETER") midnightJd: Double): Int {
        val cal = java.util.GregorianCalendar(AstroTime.IST).apply {
            timeInMillis = AstroTime.millisFromJulianDay(jd)
        }
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    private fun formatMins(mins: Int, use24Hour: Boolean = false): String {
        val hrs = mins / 60
        val m = mins % 60
        if (use24Hour) {
            return String.format(java.util.Locale.US, "%02d:%02d", hrs % 24, m)
        }
        val ampm = if (hrs >= 12) "PM" else "AM"
        val displayHrs = if (hrs == 0) 12 else if (hrs > 12) hrs - 12 else hrs
        return String.format(java.util.Locale.US, "%02d:%02d %s", displayHrs, m, ampm)
    }
}
