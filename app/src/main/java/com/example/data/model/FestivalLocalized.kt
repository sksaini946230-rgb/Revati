package com.example.data.model

import com.example.astro.AstroNames
import com.example.util.AppLanguage
import com.example.util.LanguageManager
/**
 * FestivalProvider only carries Hindi for the calendar fields — the month, the
 * paksha, the tithi and even the printed date ("28 अगस्त 2026"). In English
 * mode that left half of every festival card in Devanagari. These read the
 * Hindi and hand back the right form for the language in force.
 */

private val EN_MONTHS = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/**
 * minSdk is 24 and the module has no core library desugaring, so java.time is
 * off limits here — dateIso is a plain "yyyy-MM-dd", which splits fine.
 */
val FestivalData.dateLocal: String
    get() {
        if (LanguageManager.currentLanguage == AppLanguage.HINDI) return dateString
        val parts = dateIso.split("-")
        val month = parts.getOrNull(1)?.toIntOrNull() ?: return dateString
        if (parts.size != 3 || month !in 1..12) return dateString
        return "${parts[2]} ${EN_MONTHS[month - 1]} ${parts[0]}"
    }

val FestivalData.masaLocal: String
    get() = LanguageManager.getString(monthNameHi, AstroNames.masaEnFromHi(monthNameHi))

val FestivalData.tithiLocal: String
    get() = LanguageManager.getString(tithiHi, AstroNames.tithiEnFromHi(tithiHi))

val FestivalData.pakshaLocal: String
    get() = LanguageManager.getString(pakshaHi, AstroNames.pakshaEnFromHi(pakshaHi))

/**
 * The Hindi names used to carry a romanised echo — "करवा चौथ (Karwa Chauth)" —
 * cut off here for the tiles only, while the calendar and the reminder showed it
 * whole. The names are plain Hindi at the source now (25 Sep 2026), so every
 * screen reads them the same way.
 */
val FestivalData.nameLocal: String
    get() = LanguageManager.getString(nameHi, nameEn)
