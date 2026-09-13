package com.example.astro

import com.example.data.model.CityLocation
import com.example.data.model.nakshatraProgressPercent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.GregorianCalendar

/**
 * The nakshatra bar was a constant 65%. It is derived from the stored Moon now,
 * so check it against the ephemeris directly, across a month, where a constant
 * cannot pass.
 */
class NakshatraProgressTest {

    private val jaipur = CityLocation("Jaipur", "जयपुर", "Rajasthan", 26.9124, 75.7873)

    @Test
    fun `progress follows the Moon at sunrise, and varies`() {
        val seen = mutableSetOf<Int>()
        for (day in 1..30) {
            val date = GregorianCalendar(AstroTime.IST).apply { clear(); set(2026, 8, day, 12, 0, 0) }.time
            val p = PanchangCalculator.calculatePanchang(date, jaipur, use24Hour = true)
            val progress = p.nakshatraProgressPercent
            assertNotNull(progress)
            progress!!

            val moon = p.planets.first { it.planetNameEn == "Moon" }
            val longitude = (moon.rashiNumber - 1) * 30.0 + moon.degree
            val index = (longitude / PanchangElements.NAKSHATRA_SPAN).toInt()
            assertEquals("day $day nakshatra", p.nakshatra, AstroNames.NAKSHATRA_EN[index])
            assertEquals("day $day", (longitude % PanchangElements.NAKSHATRA_SPAN) / PanchangElements.NAKSHATRA_SPAN * 100.0, progress.toDouble(), 0.5)
            seen += (progress / 10).toInt()
        }
        assert(seen.size >= 5) { "progress barely moves across a month: $seen" }
    }
}
