package com.example.astro

import com.example.data.model.CityLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * A muhurat window is the part of its day that its reason lasts (owner's
 * decision, 25 Sep 2026). It used to run sunrise to sunset whatever the
 * nakshatra did, and its description named two fixed nakshatras whatever the
 * day's was.
 */
class MuhuratWindowTest {

    private val cities = listOf(
        CityLocation("Jaipur", "Jaipur", "Rajasthan", 26.9124, 75.7873),
        CityLocation("Guwahati", "Guwahati", "Assam", 26.1445, 91.7362),
    )

    private fun minutes(hhmm: String): Int = hhmm.split(":").let { it[0].toInt() * 60 + it[1].toInt() }

    @Test
    fun `each window ends at sunset or at the moment its tithi or nakshatra ends`() {
        val sdf = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.ENGLISH).apply { timeZone = AstroTime.IST }
        for (city in cities) {
            val items = MuhuratCalculator.getUpcomingMuhurats(city, use24Hour = true)
            assertTrue(items.isNotEmpty())
            for (m in items) {
                val date = sdf.parse("${m.dateString} 12:00")!!
                val sun = PanchangCalculator.sunWindow(date, city)
                val start = minutes(m.startTime)
                val end = minutes(m.endTime)
                val sunset = minutes(PanchangCalculator.formatJdTime(sun.sunsetJd, AstroTime.IST, true))
                val muhurta = (sunset - start) / 15

                assertTrue("${m.id} ${m.dateString}: ends after sunset", end <= sunset)
                assertTrue("${m.id} ${m.dateString}: shorter than a muhurta", end - start >= muhurta - 1)

                if (end < sunset) {
                    // Something must change within the minute the window closes.
                    val endJd = sun.midnightJd + (end + 1) / 1440.0
                    val changed =
                        PanchangElements.nakshatraIndex(endJd) != PanchangElements.nakshatraIndex(sun.sunriseJd) ||
                            PanchangElements.tithiNumber(endJd) != PanchangElements.tithiNumber(sun.sunriseJd)
                    assertTrue("${m.id} ${m.dateString}: closed at $end with nothing ending", changed)
                }
            }
        }
    }

    @Test
    fun `the description names the day's own nakshatra`() {
        for (m in MuhuratCalculator.getUpcomingMuhurats(cities.first(), use24Hour = true)) {
            assertTrue(m.descriptionHi, m.descriptionHi.startsWith(m.nakshatraHi + " "))
            assertTrue(m.descriptionEn, m.descriptionEn.contains(m.nakshatraEn))
        }
    }

    @Test
    fun `at least one window in sixty days is cut short by its nakshatra or tithi`() {
        // Otherwise the rule above could pass by never doing anything.
        val sdf = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.ENGLISH).apply { timeZone = AstroTime.IST }
        val cut = cities.sumOf { city ->
            MuhuratCalculator.getUpcomingMuhurats(city, use24Hour = true).count { m ->
                val sun = PanchangCalculator.sunWindow(sdf.parse("${m.dateString} 12:00")!!, city)
                minutes(m.endTime) < minutes(PanchangCalculator.formatJdTime(sun.sunsetJd, AstroTime.IST, true)) - 5
            }
        }
        assertEquals(true, cut > 0)
    }
}
