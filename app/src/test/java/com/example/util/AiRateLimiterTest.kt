package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The limiter exists because there was none, and because the cost of a call
 * lands on the developer with no server in between. These pin the two things it
 * must not get wrong: letting a hammering caller through, and locking out a
 * normal one.
 */
class AiRateLimiterTest {

    private var clock = 0L
    private fun limiter(minGap: Long = 3_000L, maxPerHour: Int = 20) =
        AiRateLimiter(minGapMs = minGap, maxPerHour = maxPerHour, now = { clock })

    @Test
    fun `the first question is always allowed`() {
        assertTrue(limiter().tryAcquire() is AiRateLimiter.Decision.Allow)
    }

    @Test
    fun `a double tap is refused and says how long is left`() {
        val l = limiter()
        l.tryAcquire()
        clock += 500

        val d = l.tryAcquire()
        assertTrue("a second press 500ms later must not go through", d is AiRateLimiter.Decision.TooSoon)
        assertEquals(2_500L, (d as AiRateLimiter.Decision.TooSoon).waitMs)
    }

    @Test
    fun `asking again after the gap is allowed`() {
        val l = limiter()
        l.tryAcquire()
        clock += 3_000
        assertTrue(l.tryAcquire() is AiRateLimiter.Decision.Allow)
    }

    @Test
    fun `the hourly cap holds`() {
        val l = limiter(maxPerHour = 5)
        repeat(5) {
            assertTrue("call ${it + 1} of 5 should pass", l.tryAcquire() is AiRateLimiter.Decision.Allow)
            clock += 3_000
        }
        assertTrue(
            "the sixth call within the hour must be refused",
            l.tryAcquire() is AiRateLimiter.Decision.HourlyCapReached
        )
    }

    @Test
    fun `the window rolls, so an hour later the allowance is back`() {
        val l = limiter(maxPerHour = 2)
        l.tryAcquire()
        clock += 3_000
        l.tryAcquire()
        clock += 3_000
        assertTrue(l.tryAcquire() is AiRateLimiter.Decision.HourlyCapReached)

        // Past the first call's hour, one slot frees up — not all of them.
        clock += AiRateLimiter.ONE_HOUR_MS
        assertTrue("the oldest call has aged out", l.tryAcquire() is AiRateLimiter.Decision.Allow)
    }

    @Test
    fun `a refused call does not spend the allowance`() {
        val l = limiter(maxPerHour = 2)
        l.tryAcquire()
        clock += 100
        l.tryAcquire() // refused: too soon
        l.tryAcquire() // refused: too soon
        clock += 3_000

        assertTrue(
            "only the one accepted call should have counted",
            l.tryAcquire() is AiRateLimiter.Decision.Allow
        )
    }

    @Test
    fun `the question cap is short enough to matter and long enough to use`() {
        // Long enough for a real question, short enough that twenty an hour is
        // not a bill. "मेरी नौकरी में पदोन्नति कब होगी?" is 30 characters.
        assertTrue(
            "cap should leave room for a real question",
            AiRateLimiter.MAX_QUESTION_CHARS >= 200
        )
        assertTrue(
            "cap should stop someone pasting pages into a metered API",
            AiRateLimiter.MAX_QUESTION_CHARS <= 1000
        )
    }

    @Test
    fun `a wait is reported in whole minutes and never as zero`() {
        val l = limiter()
        assertEquals(1, l.minutesFrom(1))
        assertEquals(1, l.minutesFrom(60_000))
        assertEquals(2, l.minutesFrom(60_001))
        assertEquals(5, l.minutesFrom(5 * 60_000L))
    }

    @Test
    fun `the daily cap holds across a new limiter sharing the same store`() {
        val store = AiRateLimiter.DailyStore.InMemory()
        var clock = 1_000_000L
        val first = AiRateLimiter(minGapMs = 0, maxPerHour = 100, maxPerDay = 3, dailyStore = store, now = { clock })
        repeat(3) { org.junit.Assert.assertEquals(AiRateLimiter.Decision.Allow, first.tryAcquire()); clock += 10 }
        org.junit.Assert.assertEquals(AiRateLimiter.Decision.DailyCapReached, first.tryAcquire())

        // A process restart builds a new limiter; the day's count must survive it.
        val afterRestart = AiRateLimiter(minGapMs = 0, maxPerHour = 100, maxPerDay = 3, dailyStore = store, now = { clock })
        org.junit.Assert.assertEquals(AiRateLimiter.Decision.DailyCapReached, afterRestart.tryAcquire())

        clock += 25L * 60 * 60 * 1000
        org.junit.Assert.assertEquals(AiRateLimiter.Decision.Allow, afterRestart.tryAcquire())
    }
}
