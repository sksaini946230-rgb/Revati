package com.example.astro

import com.example.data.model.MuhuratItem
import com.example.data.model.CityLocation
import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

object MuhuratCalculator {

    /**
     * Tithis a muhurat is not offered on.
     *
     * The Rikta tithis are Chaturthi, Navami and Chaturdashi — the fourth,
     * ninth and fourteenth of each paksha, "empty" days on which nothing is
     * begun. This list had the ninth and the fourteenth but not the fourth:
     * "चतुर्दशी" does not contain "चतुर्थी", so Chaturthi was quietly being
     * offered as an auspicious day to marry or move house.
     *
     * Ashtami and Amavasya are not Rikta and are excluded on the separate,
     * ordinary ground that neither is used for beginnings.
     */
    /**
     * All five categories are gated on this. Vehicle and Travel were not, which
     * is why the device showed a शुभ यात्रा on a Chaturthi the same day this list
     * gained Chaturthi — the two are separate faults that hide each other, and
     * only the screen showed the second one.
     */
    private val AUSPICIOUS_TITHI_EXCLUSIONS = listOf(
        "चतुर्थी",    // Rikta
        "नवमी",      // Rikta
        "चतुर्दशी",   // Rikta
        "अष्टमी",
        "अमावस्या"
    )

    private class Category(
        val id: String,
        val categoryHi: String,
        val categoryEn: String,
        val nakshatras: List<String>,
        val qualityHi: String,
        val qualityEn: String,
        /** The day's own nakshatra is put in front: "रोहिणी" + " नक्षत्र में …". */
        val descriptionHi: (String) -> String,
        val descriptionEn: (String) -> String
    )

    /**
     * The descriptions used to name two fixed nakshatras whatever the day's was —
     * a Business window on an Ashvini day read "under Hasta or Pushya". Each now
     * names the nakshatra that made the day qualify.
     */
    private val CATEGORIES = listOf(
        Category(
            "m1", "विवाह मुहूर्त", "Wedding Muhurat",
            listOf("रोहिणी", "उत्तराफाल्गुनी", "स्वाती", "अनुराधा", "पुष्य"),
            "अति शुभ", "Highly auspicious",
            { "$it नक्षत्र में उत्तम विवाह लगन।" },
            { "A strong wedding window under $it Nakshatra." }
        ),
        Category(
            "m2", "गृह प्रवेश", "Housewarming",
            listOf("रोहिणी", "पुष्य", "चित्रा", "स्वाती"),
            "शुभ", "Auspicious",
            { "$it नक्षत्र में नया गृह प्रवेश फलदायी।" },
            { "Entering a new home under $it Nakshatra is considered fruitful." }
        ),
        Category(
            "m3", "व्यापार शुभारम्भ", "Business Launch",
            listOf("अश्विनी", "हस्त", "पुष्य", "श्रवण"),
            "अति शुभ", "Highly auspicious",
            { "$it नक्षत्र में व्यापार की शुरुआत।" },
            { "Beginning a venture under $it Nakshatra." }
        ),
        Category(
            "m4", "वाहन खरीद", "Vehicle Purchase",
            listOf("श्रवण", "धनिष्ठा", "शतभिषा", "चित्रा", "अश्विनी"),
            "शुभ", "Auspicious",
            { "$it नक्षत्र में नया वाहन क्रय मुहूर्त।" },
            { "A favourable window to buy a vehicle, under $it Nakshatra." }
        ),
        Category(
            "m5", "शुभ यात्रा", "Travel Muhurat",
            listOf("अश्विनी", "पुष्य", "स्वाती", "अनुराधा", "रोहिणी"),
            "शुभ", "Auspicious",
            { "$it नक्षत्र में यात्रा हेतु उत्तम समय।" },
            { "A good time to set out, under $it Nakshatra." }
        )
    )

    /**
     * The next auspicious window in each category, from where the user is.
     *
     * The place used to be a Jaipur literal declared inside this function, which
     * is the same mistake Guna Milan's Manglik reading made with the string
     * "Default". Sunrise in Guwahati is the better part of an hour before
     * Jaipur's, so a reader in Assam was told to begin at a time that had
     * already passed.
     *
     * **The window is the part of the day the reason for it lasts** (owner's
     * decision, 25 Sep 2026). A day qualifies on its sunrise tithi and
     * nakshatra, and every window used to run on to sunset regardless — so a
     * Rohini wedding window could carry on for hours after Rohini had ended.
     * It now ends at sunset, the tithi's end or the nakshatra's end, whichever
     * comes first, and a day whose window is shorter than one muhurta (a
     * fifteenth of that day) is not offered at all.
     *
     * It is sixty full Panchang computations, so call it off the main thread.
     */
    fun getUpcomingMuhurats(city: CityLocation, use24Hour: Boolean = false): List<MuhuratItem> {
        val muhurats = mutableListOf<MuhuratItem>()
        val found = HashSet<String>()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH)

        for (i in 1..60) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val panchang = PanchangCalculator.calculatePanchang(cal.time, city, use24Hour)

            // All five categories are gated on this. Vehicle and Travel were not.
            if (AUSPICIOUS_TITHI_EXCLUSIONS.any { panchang.tithiHindi.contains(it) }) continue

            val sun = PanchangCalculator.sunWindow(cal.time, city)
            val endJd = listOfNotNull(
                sun.sunsetJd,
                PanchangElements.tithiEndJd(sun.sunriseJd),
                PanchangElements.nakshatraEndJd(sun.sunriseJd)
            ).min()
            val muhurta = (sun.sunsetJd - sun.sunriseJd) / 15.0
            if (endJd - sun.sunriseJd < muhurta) continue
            val endTime = PanchangCalculator.formatJdTime(endJd, AstroTime.IST, use24Hour)

            for (c in CATEGORIES) {
                if (c.id in found || panchang.nakshatraHindi !in c.nakshatras) continue
                muhurats.add(
                    MuhuratItem(
                        id = c.id,
                        categoryHi = c.categoryHi, categoryEn = c.categoryEn,
                        dateString = sdf.format(cal.time),
                        dayOfWeekHi = panchang.dayOfWeekHindi, dayOfWeekEn = panchang.dayOfWeek,
                        startTime = panchang.sunrise, endTime = endTime,
                        tithiHi = panchang.tithiHindi, tithiEn = panchang.tithi,
                        nakshatraHi = panchang.nakshatraHindi, nakshatraEn = panchang.nakshatra,
                        qualityHi = c.qualityHi, qualityEn = c.qualityEn,
                        descriptionHi = c.descriptionHi(panchang.nakshatraHindi),
                        descriptionEn = c.descriptionEn(panchang.nakshatra)
                    )
                )
                found.add(c.id)
            }
        }

        // Sixty days with nothing at all is possible, and an empty screen says
        // nothing useful — so that day's Abhijit Muhurta, two days out, stands
        // in. It used to be sunrise to sunset under a "Sarvartha Siddhi" label
        // that nothing had checked.
        if (muhurats.isEmpty()) {
            val fallbackCal = Calendar.getInstance()
            fallbackCal.add(Calendar.DAY_OF_YEAR, 2)
            val panchang = PanchangCalculator.calculatePanchang(fallbackCal.time, city, use24Hour)
            muhurats.add(
                MuhuratItem(
                    id = "m1",
                    categoryHi = "शुभ कार्य मुहूर्त", categoryEn = "General Muhurat",
                    dateString = sdf.format(fallbackCal.time),
                    dayOfWeekHi = panchang.dayOfWeekHindi, dayOfWeekEn = panchang.dayOfWeek,
                    // abhijitMuhurat is "start - end", as PanchangCalculator writes it.
                    startTime = panchang.abhijitMuhurat.substringBefore(" - "),
                    endTime = panchang.abhijitMuhurat.substringAfter(" - "),
                    tithiHi = panchang.tithiHindi, tithiEn = panchang.tithi,
                    nakshatraHi = panchang.nakshatraHindi, nakshatraEn = panchang.nakshatra,
                    qualityHi = "शुभ", qualityEn = "Auspicious",
                    descriptionHi = "अभिजित मुहूर्त।",
                    descriptionEn = "The Abhijit Muhurta, the day's midday window."
                )
            )
        }
        return muhurats
    }
}
