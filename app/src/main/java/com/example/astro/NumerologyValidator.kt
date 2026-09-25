package com.example.astro

import java.util.Calendar
import java.util.GregorianCalendar

/** A refusal in both languages; the screen picks one. */
data class ValidationMessage(val hi: String, val en: String)

data class NumerologyValidationResult(
    val isValid: Boolean,
    val nameError: ValidationMessage? = null,
    val dobError: ValidationMessage? = null
)

object NumerologyValidator {

    fun validateName(name: String): ValidationMessage? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return ValidationMessage("नाम दर्ज करना अनिवार्य है", "Name is required")
        }
        if (trimmed.length < 2) {
            return ValidationMessage("नाम में कम से कम 2 अक्षर होने चाहिए", "Enter at least 2 characters")
        }
        if (!trimmed.any { it.isLetter() }) {
            return ValidationMessage("नाम में वैध अक्षर होने चाहिए", "Name must contain letters")
        }
        return null
    }

    /**
     * The upper bound used to be the literal 2026, which is a date this file
     * cannot know and was going to be wrong on 1 January 2027 — a baby born that
     * morning could never be entered, and the message would have told the parent
     * their child's birth year was out of range. It is today's date now, and a
     * date that has not happened yet is refused whatever year it falls in.
     */
    const val MIN_YEAR = 1900

    fun validateDob(dob: String, today: Calendar = Calendar.getInstance()): ValidationMessage? {
        val trimmed = dob.trim()
        if (trimmed.isEmpty()) {
            return ValidationMessage("जन्म तिथि अनिवार्य है", "Date of birth is required")
        }
        
        // Regex for YYYY-MM-DD
        val regex = Regex("""^(\d{4})-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])$""")
        val match = regex.matchEntire(trimmed)
        if (match == null) {
            return ValidationMessage("प्रारूप YYYY-MM-DD होना चाहिए", "Format: YYYY-MM-DD")
        }

        val (yearStr, monthStr, dayStr) = match.destructured
        val year = yearStr.toIntOrNull() ?: 0
        val month = monthStr.toIntOrNull() ?: 0
        val day = dayStr.toIntOrNull() ?: 0

        val maxYear = today.get(Calendar.YEAR)
        if (year < MIN_YEAR || year > maxYear) {
            return ValidationMessage("वर्ष $MIN_YEAR से $maxYear के बीच होना चाहिए", "Year must be between $MIN_YEAR and $maxYear")
        }

        // Days in month check
        val maxDays = when (month) {
            2 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

        if (day > maxDays) {
            return ValidationMessage("माह $month में अधिकतम $maxDays दिन होते हैं", "Month $month has at most $maxDays days")
        }

        val entered = GregorianCalendar(today.timeZone).apply {
            clear(); set(year, month - 1, day, 0, 0, 0)
        }
        val startOfTomorrow = (today.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, 1)
        }
        if (!entered.before(startOfTomorrow)) {
            return ValidationMessage("जन्म तिथि भविष्य की नहीं हो सकती", "Date of birth cannot be in the future")
        }

        return null
    }

    fun validateInput(
        name: String,
        dob: String,
        today: Calendar = Calendar.getInstance()
    ): NumerologyValidationResult {
        val nameErr = validateName(name)
        val dobErr = validateDob(dob, today)
        val valid = (nameErr == null && dobErr == null)
        return NumerologyValidationResult(
            isValid = valid,
            nameError = nameErr,
            dobError = dobErr
        )
    }
}
