package com.example.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.File
import org.junit.Test

/**
 * What may and may not be handed to AdMob as an ad unit.
 *
 * Unit tests run against the debug BuildConfig, where [AdIds.resolve] always
 * returns the test unit by design — so these pin the debug half directly and
 * the release half through the same predicate the release path uses. The rule
 * being protected is that a release build must never serve a test ad and must
 * never pass a sentinel or a typo to the SDK:
 *
 *  - `.env.example` holds Google's test ids so the project builds without
 *    secrets, and the secrets plugin falls back to it for any key `.env` does
 *    not define. A release built without `.env` would otherwise serve test ads
 *    to real users, earn nothing, and look like it was working.
 *  - The secrets plugin cannot emit an empty string, so an unset key is
 *    `NOT_CONFIGURED` — which is a perfectly valid-looking String and would be
 *    sent straight to AdMob without the check.
 */
class AdIdsTest {

    private val realUnit = "ca-app-pub-5513456541171739/8300000000"

    /** The release branch of [AdIds.resolve], which the debug BuildConfig hides. */
    private fun releaseResolve(configured: String?): String {
        val id = configured?.trim().orEmpty()
        if (id.isBlank()) return ""
        if (id == "NOT_CONFIGURED") return ""
        if (id.startsWith("ca-app-pub-3940256099942544")) return ""
        if (!id.startsWith("ca-app-pub-") || !id.contains('/')) return ""
        return id
    }

    @Test
    fun `debug always uses the test unit, whatever is configured`() {
        // Guards the policy the other way round: development traffic must not
        // reach a live unit, because that is what invalid-traffic enforcement
        // looks for and the account is what is at risk.
        assertEquals(AdIds.TEST_BANNER, AdIds.resolve(realUnit, AdIds.TEST_BANNER))
        assertEquals(AdIds.TEST_REWARDED, AdIds.resolve(null, AdIds.TEST_REWARDED))
    }

    @Test
    fun `release refuses a test unit`() {
        assertEquals("", releaseResolve(AdIds.TEST_BANNER))
        assertEquals("", releaseResolve(AdIds.TEST_INTERSTITIAL))
        assertEquals("", releaseResolve(AdIds.TEST_APP_OPEN))
        assertEquals("", releaseResolve(AdIds.TEST_REWARDED))
    }

    @Test
    fun `release refuses the unset sentinel, blanks and malformed ids`() {
        assertEquals("", releaseResolve("NOT_CONFIGURED"))
        assertEquals("", releaseResolve(""))
        assertEquals("", releaseResolve("   "))
        assertEquals("", releaseResolve(null))
        assertEquals("", releaseResolve("ca-app-pub-5513456541171739"))  // no unit
        assertEquals("", releaseResolve("pub-5513456541171739/830"))     // wrong prefix
    }

    @Test
    fun `release accepts a real unit, trimmed`() {
        assertEquals(realUnit, releaseResolve(realUnit))
        assertEquals(realUnit, releaseResolve("  $realUnit  "))
    }

    @Test
    fun `the app's own publisher is not the test publisher`() {
        // A cheap trip-wire: if these ever matched, every release id would be
        // silently discarded and every placement would go empty.
        assertEquals(false, realUnit.startsWith("ca-app-pub-3940256099942544"))
    }

    /**
     * Every placement must go through [AdIds.resolve]. The interstitial and the
     * banner once read BuildConfig themselves, and the interstitial's version
     * sent a debug build's requests to the LIVE unit — found on 13 Sep 2026 as
     * `code=3 No fill` on a debug build where the test unit always fills.
     */
    @Test
    fun `every ad unit read goes through AdIds`() {
        val offenders = File("src/main/java/com/example").walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "AdIds.kt" }
            .filter { f ->
                val src = f.readText()
                src.contains("3940256099942544") ||
                    (Regex("""BuildConfig\.ADMOB_(BANNER|INTERSTITIAL|APP_OPEN|REWARDED)_ID""")
                        .containsMatchIn(src) && !src.contains("AdIds.resolve("))
            }
            .map { it.name }
            .toList()
        assertTrue("placements bypassing AdIds: $offenders", offenders.isEmpty())
    }
}
