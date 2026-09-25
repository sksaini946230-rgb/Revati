package com.example.astro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar

/**
 * The birth-year bound used to be the literal 2026.
 *
 * A hardcoded "this year" is only ever right for a few months. On 1 January
 * 2027 a baby born that morning could not be entered at all, and the message
 * would have told the parent the year was out of range — a defect with a date
 * on it, sitting in a validator nobody would think to re-read.
 */
class NumerologyValidatorDateTest {

    private fun on(y: Int, m: Int, d: Int): Calendar =
        GregorianCalendar().apply { clear(); set(y, m - 1, d, 10, 0, 0) }

    @Test
    fun `a birth year is accepted in the year the app is being used`() {
        assertNull(NumerologyValidator.validateDob("2027-01-01", on(2027, 1, 1)))
        assertNull(NumerologyValidator.validateDob("2031-06-15", on(2031, 12, 31)))
    }

    @Test
    fun `the message names the real range`() {
        val error = NumerologyValidator.validateDob("2035-01-01", on(2030, 5, 1))
        assertNotNull(error)
        for (text in listOf(error!!.hi, error.en)) {
            assertEquals(true, text.contains("2030"))
            assertEquals(true, text.contains("1900"))
        }
    }

    @Test
    fun `a date that has not happened yet is refused`() {
        assertNotNull(NumerologyValidator.validateDob("2026-09-13", on(2026, 9, 12)))
        // Today itself is fine — someone born this morning has a birth date.
        assertNull(NumerologyValidator.validateDob("2026-09-12", on(2026, 9, 12)))
    }

    @Test
    fun `the old bounds still hold at the bottom and for impossible dates`() {
        assertNotNull(NumerologyValidator.validateDob("1899-12-31", on(2026, 9, 12)))
        assertNotNull(NumerologyValidator.validateDob("2025-02-29", on(2026, 9, 12)))
        assertNull(NumerologyValidator.validateDob("2024-02-29", on(2026, 9, 12)))
        assertNotNull(NumerologyValidator.validateDob("12-05-1995", on(2026, 9, 12)))
    }
}
