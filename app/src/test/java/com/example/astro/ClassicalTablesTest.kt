package com.example.astro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The classical tables, each checked against the rule it comes from rather than
 * against a copy of itself.
 *
 * These are the numbers a reader cannot check: nobody notices that their Yoni is
 * the wrong animal, or that one of the three Bhakoot doshas is missing — and
 * that one shipped, handing a couple in six the full seven points and telling
 * them there was no dosha. So the tables are restated here from the shastra,
 * grouped the way the texts group them, so that a diff between the two shows up
 * as a failure instead of as advice.
 */
class ClassicalTablesTest {

    // ---------------------------------------------------------------- Varna

    /** Water signs Brahmin, fire Kshatriya, earth Vaishya, air Shudra. */
    @Test
    fun `varna follows the element of the rashi`() {
        val brahmin = listOf(3, 7, 11)      // Karka, Vrischika, Meena
        val kshatriya = listOf(0, 4, 8)     // Mesha, Simha, Dhanu
        val vaishya = listOf(1, 5, 9)       // Vrishabha, Kanya, Makara
        val shudra = listOf(2, 6, 10)       // Mithuna, Tula, Kumbha
        brahmin.forEach { assertEquals("rashi $it", 4, KundaliMatchingCalculator.VARNA_RANKS[it]) }
        kshatriya.forEach { assertEquals("rashi $it", 3, KundaliMatchingCalculator.VARNA_RANKS[it]) }
        vaishya.forEach { assertEquals("rashi $it", 2, KundaliMatchingCalculator.VARNA_RANKS[it]) }
        shudra.forEach { assertEquals("rashi $it", 1, KundaliMatchingCalculator.VARNA_RANKS[it]) }
    }

    /** One point when the boy's varna is not below the girl's, none otherwise. */
    @Test
    fun `varna scores one only when the boy is not below the girl`() {
        for (b in 0..11) for (g in 0..11) {
            val expected =
                if (KundaliMatchingCalculator.VARNA_RANKS[b] >= KundaliMatchingCalculator.VARNA_RANKS[g]) 1.0 else 0.0
            assertEquals("$b/$g", expected, KundaliMatchingCalculator.calculateVarna(b, g), 0.0)
        }
    }

    // --------------------------------------------------------------- Vashya

    @Test
    fun `vashya groups the twelve rashis the classical way`() {
        //                       Me Vr Mi Ka Si Kn Tu Vs Dh Mk Ku Mn
        val expected = listOf(0, 0, 1, 2, 3, 1, 1, 4, 1, 0, 1, 2)
        assertEquals(expected, KundaliMatchingCalculator.VASHYA_GROUP)
    }

    // ----------------------------------------------------------------- Tara

    /**
     * Count from one nakshatra to the other, divide by nine; the third, fifth
     * and seventh taras — Vipat, Pratyari and Vadha — score nothing.
     */
    @Test
    fun `tara withholds points only for the third fifth and seventh`() {
        for (b in 0..26) for (g in 0..26) {
            fun tara(from: Int, to: Int) = (((to - from + 27) % 27) + 1) % 9
            val expected = listOf(tara(b, g), tara(g, b))
                .sumOf { if (it in listOf(3, 5, 7)) 0.0 else 1.5 }
            assertEquals("$b/$g", expected, KundaliMatchingCalculator.calculateTara(b, g), 0.0)
        }
    }

    /** A ninth tara is Parama Mitra, so a remainder of zero is the good one. */
    @Test
    fun `the ninth tara is auspicious, not the zeroth failure`() {
        assertEquals(3.0, KundaliMatchingCalculator.calculateTara(0, 8), 0.0)
    }

    // ----------------------------------------------------------------- Yoni

    @Test
    fun `every nakshatra has its classical yoni animal`() {
        val expected = listOf(
            0,  // Ashwini      horse
            1,  // Bharani      elephant
            2,  // Krittika     sheep
            3,  // Rohini       serpent
            3,  // Mrigashira   serpent
            4,  // Ardra        dog
            5,  // Punarvasu    cat
            2,  // Pushya       sheep
            5,  // Ashlesha     cat
            6,  // Magha        rat
            6,  // P Phalguni   rat
            7,  // U Phalguni   cow
            8,  // Hasta        buffalo
            9,  // Chitra       tiger
            8,  // Swati        buffalo
            9,  // Vishakha     tiger
            10, // Anuradha     deer
            10, // Jyeshtha     deer
            4,  // Moola        dog
            11, // P Ashadha    monkey
            12, // U Ashadha    mongoose
            11, // Shravana     monkey
            13, // Dhanishta    lion
            0,  // Shatabhisha  horse
            13, // P Bhadrapada lion
            7,  // U Bhadrapada cow
            1   // Revati       elephant
        )
        assertEquals(expected, KundaliMatchingCalculator.NAKSHATRA_YONI)
    }

    /** The seven sworn-enemy pairs, which are the only zero in the matrix. */
    @Test
    fun `the yoni enemies score nothing and nothing else does`() {
        val enemies = setOf(
            setOf(0, 8),   // horse    / buffalo
            setOf(1, 13),  // elephant / lion
            setOf(2, 11),  // sheep    / monkey
            setOf(3, 12),  // serpent  / mongoose
            setOf(4, 10),  // dog      / deer
            setOf(5, 6),   // cat      / rat
            setOf(7, 9)    // cow      / tiger
        )
        val nakOf = (0..13).associateWith { yoni ->
            KundaliMatchingCalculator.NAKSHATRA_YONI.indexOf(yoni)
        }
        for (a in 0..13) for (b in 0..13) {
            val score = KundaliMatchingCalculator.calculateYoni(nakOf[a]!!, nakOf[b]!!)
            if (setOf(a, b) in enemies) {
                assertEquals("yoni $a/$b are enemies", 0.0, score, 0.0)
            } else {
                assertTrue("yoni $a/$b should not be zero", score > 0.0)
            }
            if (a == b) assertEquals("same yoni", 4.0, score, 0.0)
        }
    }

    // ---------------------------------------------------------- Graha Maitri

    @Test
    fun `the rashi lords are the ones the texts give`() {
        // Mars, Venus, Mercury, Moon, Sun, Mercury, Venus, Mars, Jupiter,
        // Saturn, Saturn, Jupiter — with Sun 0, Moon 1, Mars 2, Mercury 3,
        // Jupiter 4, Venus 5, Saturn 6.
        val lords = listOf(2, 5, 3, 1, 0, 3, 5, 2, 4, 6, 6, 4)
        // Same-lord pairs are the full five points, and they are exactly the
        // pairs that share a lord.
        for (b in 0..11) for (g in 0..11) {
            if (lords[b] == lords[g]) {
                assertEquals(
                    "rashis $b and $g share a lord",
                    5.0, KundaliMatchingCalculator.calculateGrahaMaitri(b, g), 0.0
                )
            }
        }
        // Two enemies are zero: Sun (Simha) against Venus (Vrishabha).
        assertEquals(0.0, KundaliMatchingCalculator.calculateGrahaMaitri(4, 1), 0.0)
        // Two friends are five: Sun (Simha) against Jupiter (Dhanu).
        assertEquals(5.0, KundaliMatchingCalculator.calculateGrahaMaitri(4, 8), 0.0)
        // Moon has no enemy, so nothing with Karka in it can score zero.
        for (g in 0..11) {
            assertTrue(
                "Moon has no enemies, so Karka/$g cannot be zero",
                KundaliMatchingCalculator.calculateGrahaMaitri(3, g) > 0.0
            )
        }
    }

    // ----------------------------------------------------------------- Gana

    @Test
    fun `every nakshatra has its classical gana`() {
        val deva = listOf(0, 4, 6, 7, 12, 14, 16, 21, 26)
        val manushya = listOf(1, 3, 5, 10, 11, 19, 20, 24, 25)
        val rakshasa = listOf(2, 8, 9, 13, 15, 17, 18, 22, 23)
        assertEquals(27, (deva + manushya + rakshasa).distinct().size)
        deva.forEach { assertEquals("nak $it", 0, KundaliMatchingCalculator.NAKSHATRA_GANA[it]) }
        manushya.forEach { assertEquals("nak $it", 1, KundaliMatchingCalculator.NAKSHATRA_GANA[it]) }
        rakshasa.forEach { assertEquals("nak $it", 2, KundaliMatchingCalculator.NAKSHATRA_GANA[it]) }
    }

    /** Deva/Manushya 6, Manushya/Deva 5, Rakshasa boy with a Deva girl 1, rest 0. */
    @Test
    fun `gana scores the asymmetric classical table`() {
        // Ashwini is Deva, Bharani Manushya, Krittika Rakshasa.
        val deva = 0; val manushya = 1; val rakshasa = 2
        val g = KundaliMatchingCalculator::calculateGana
        assertEquals(6.0, g(deva, deva), 0.0)
        assertEquals(6.0, g(manushya, manushya), 0.0)
        assertEquals(6.0, g(rakshasa, rakshasa), 0.0)
        assertEquals(6.0, g(deva, manushya), 0.0)
        assertEquals(5.0, g(manushya, deva), 0.0)
        assertEquals(1.0, g(rakshasa, deva), 0.0)
        assertEquals(0.0, g(deva, rakshasa), 0.0)
        assertEquals(0.0, g(manushya, rakshasa), 0.0)
        assertEquals(0.0, g(rakshasa, manushya), 0.0)
    }

    // --------------------------------------------------------------- Bhakoot

    /** 2/12 Dwirdwadasha, 5/9 Nav-Pancham, 6/8 Shadashtaka — all three. */
    @Test
    fun `bhakoot withholds points for all three doshas and no others`() {
        val dosha = setOf(2, 12, 5, 9, 6, 8)
        for (b in 0..11) for (g in 0..11) {
            val dist = ((g - b + 12) % 12) + 1
            val expected = if (dist in dosha) 0.0 else 7.0
            assertEquals("$b/$g dist $dist", expected, KundaliMatchingCalculator.calculateBhakoot(b, g), 0.0)
        }
        // Same rashi is distance 1 and carries no dosha.
        assertEquals(7.0, KundaliMatchingCalculator.calculateBhakoot(5, 5), 0.0)
    }

    // ----------------------------------------------------------------- Nadi

    @Test
    fun `every nakshatra has its classical nadi`() {
        val adi = listOf(0, 5, 6, 11, 12, 17, 18, 23, 24)
        val madhya = listOf(1, 4, 7, 10, 13, 16, 19, 22, 25)
        val antya = listOf(2, 3, 8, 9, 14, 15, 20, 21, 26)
        assertEquals(27, (adi + madhya + antya).distinct().size)
        adi.forEach { assertEquals("nak $it", 0, KundaliMatchingCalculator.NAKSHATRA_NADI[it]) }
        madhya.forEach { assertEquals("nak $it", 1, KundaliMatchingCalculator.NAKSHATRA_NADI[it]) }
        antya.forEach { assertEquals("nak $it", 2, KundaliMatchingCalculator.NAKSHATRA_NADI[it]) }
    }

    @Test
    fun `nadi is all eight points or none`() {
        for (b in 0..26) for (g in 0..26) {
            val same = KundaliMatchingCalculator.NAKSHATRA_NADI[b] == KundaliMatchingCalculator.NAKSHATRA_NADI[g]
            assertEquals("$b/$g", if (same) 0.0 else 8.0, KundaliMatchingCalculator.calculateNadi(b, g), 0.0)
        }
    }

    // ---------------------------------------------------------- The 36 total

    @Test
    fun `the eight maxima add to thirty-six`() {
        val maxima = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0)
        assertEquals(36.0, maxima.sum(), 0.0)
        // And every koot can actually reach its own maximum.
        assertEquals(1.0, KundaliMatchingCalculator.calculateVarna(3, 0), 0.0)
        assertEquals(2.0, KundaliMatchingCalculator.calculateVashya(0, 0), 0.0)
        assertEquals(3.0, KundaliMatchingCalculator.calculateTara(0, 1), 0.0)
        assertEquals(4.0, KundaliMatchingCalculator.calculateYoni(0, 0), 0.0)
        assertEquals(5.0, KundaliMatchingCalculator.calculateGrahaMaitri(0, 0), 0.0)
        assertEquals(6.0, KundaliMatchingCalculator.calculateGana(0, 0), 0.0)
        assertEquals(7.0, KundaliMatchingCalculator.calculateBhakoot(0, 0), 0.0)
        assertEquals(8.0, KundaliMatchingCalculator.calculateNadi(0, 1), 0.0)
    }

    // --------------------------------------------------------- Vimshottari

    @Test
    fun `the vimshottari cycle is the classical order and adds to a hundred and twenty`() {
        val expected = listOf(
            "Ketu" to 7.0, "Venus" to 20.0, "Sun" to 6.0, "Moon" to 10.0, "Mars" to 7.0,
            "Rahu" to 18.0, "Jupiter" to 16.0, "Saturn" to 19.0, "Mercury" to 17.0
        )
        assertEquals(
            expected,
            VimshottariDashaCalculator.VIMSHOTTARI_PLANETS.map { it.nameEn to it.durationYears }
        )
        assertEquals(
            VimshottariDashaCalculator.TOTAL_VIMSHOTTARI_YEARS,
            expected.sumOf { it.second },
            0.0
        )
    }

    /** Ashwini is Ketu's, and the nine repeat three times over the twenty-seven. */
    @Test
    fun `each nakshatra takes its lord from the cycle`() {
        for (nak in 0..26) {
            val degree = nak * (360.0 / 27.0) + 1.0
            assertEquals(
                "nakshatra $nak",
                VimshottariDashaCalculator.VIMSHOTTARI_PLANETS[nak % 9].nameEn,
                VimshottariDashaCalculator.getNakshatraInfo(degree).lordNameEn
            )
        }
    }

    // ------------------------------------------------------------- Panchang

    /** Sixty half-tithis: Kimstughna, then seven movable eight times, then three fixed. */
    @Test
    fun `the karana cycle is one fixed, fifty-six movable and three fixed`() {
        assertEquals("Kimstughna", AstroNames.karanaEn(0))
        assertEquals("Shakuni", AstroNames.karanaEn(57))
        assertEquals("Chatushpada", AstroNames.karanaEn(58))
        assertEquals("Naga", AstroNames.karanaEn(59))
        assertEquals("Bava", AstroNames.karanaEn(1))
        assertTrue(AstroNames.karanaEn(56).startsWith("Vishti"))
        // Festival rules look for Bhadra in the Hindi name.
        assertTrue(PanchangElements.karanaName(56).contains("भद्रा"))
        (0..59).forEach { assertEquals(AstroNames.karanaHi(it), PanchangElements.karanaName(it)) }
        // The seven movable ones, and only those, fill 1..56 — eight times each.
        val movable = (1..56).map { PanchangElements.karanaName(it) }
        assertEquals(7, movable.distinct().size)
        movable.distinct().forEach { assertEquals(it, 8, movable.count { n -> n == it }) }
    }

    /**
     * Guna Milan reads the Moon's rashi and nakshatra back out of the chart by
     * looking the *Hindi name* up in the same table it was drawn from, and
     * `coerceAtLeast(0)` turns a miss into Mesha and Ashwini rather than into an
     * error. So the day someone localises `rashiNameHi` or adds a suffix to it,
     * every match in the app is silently computed for the wrong couple and
     * nothing fails. The round trip is the load-bearing part.
     */
    @Test
    fun `the moon sign and nakshatra survive the round trip through their names`() {
        AstroNames.RASHI_HI.forEachIndexed { i, name ->
            assertEquals("rashi $i", i, KundaliCalculator.RASHI_SHORT_HI.indexOf(name))
        }
        AstroNames.NAKSHATRA_HI.forEachIndexed { i, name ->
            assertEquals("nakshatra $i", i, KundaliCalculator.NAKSHATRAS.indexOf(name))
        }
        assertEquals(12, KundaliCalculator.RASHI_SHORT_HI.distinct().size)
        assertEquals(27, KundaliCalculator.NAKSHATRAS.distinct().size)
    }

    @Test
    fun `the name tables are the right length and in the right order`() {
        assertEquals(27, AstroNames.NAKSHATRA_EN.size)
        assertEquals(27, AstroNames.YOGA_EN.size)
        assertEquals(15, AstroNames.TITHI_EN.size)
        assertEquals(12, AstroNames.RASHI_EN.size)
        assertEquals(12, AstroNames.MASA_EN.size)
        assertEquals("Ashwini", AstroNames.NAKSHATRA_EN.first())
        assertEquals("Revati", AstroNames.NAKSHATRA_EN.last())
        assertEquals("Vishkumbha", AstroNames.YOGA_EN.first())
        assertEquals("Vaidhriti", AstroNames.YOGA_EN.last())
        assertEquals("Pratipada", AstroNames.TITHI_EN.first())
        assertEquals("Purnima", AstroNames.TITHI_EN.last())
        assertEquals("Chaitra", AstroNames.MASA_EN.first())
        assertEquals("Phalguna", AstroNames.MASA_EN.last())
    }
}
