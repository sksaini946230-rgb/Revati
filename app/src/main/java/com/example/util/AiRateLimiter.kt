package com.example.util

/**
 * How often one person may ask the model something.
 *
 * There was no limit of any kind. The question box is free text with a send
 * button, the Rashifal screen has a fetch button per sign, and every press went
 * straight to Firebase AI Logic. Nothing stopped a held finger, a stuck retry
 * loop, or simple curiosity from issuing hundreds of calls — and there is no
 * backend here, so the bill and the quota are the developer's directly. A quota
 * exhausted by one enthusiastic user takes the feature down for everyone else.
 *
 * Two limits, because they catch different things:
 *
 *  - a minimum gap, which stops double-taps and hammering;
 *  - a rolling hourly cap, which bounds what a single session can cost.
 *
 * The gap and the hourly cap are in-memory and per-process. Persisting those
 * would mean a user could be told to wait by an app they just opened, which is a
 * worse experience than the abuse it would prevent.
 *
 * **The daily cap is persisted, and that is the one that matters for cost.**
 * Since 13 Sep 2026 both AI features are PRO-only, which makes the model a paid
 * feature with a per-call cost and no server in front of it; an in-memory cap
 * alone is reset by swiping the app away. [DailyStore] keeps a count per
 * calendar day, so the ceiling on one subscriber's calls holds across restarts.
 *
 * The clock is injected so the behaviour can be tested without sleeping.
 */
class AiRateLimiter(
    private val minGapMs: Long = DEFAULT_MIN_GAP_MS,
    private val maxPerHour: Int = DEFAULT_MAX_PER_HOUR,
    private val maxPerDay: Int = DEFAULT_MAX_PER_DAY,
    private val dailyStore: DailyStore = DailyStore.InMemory(),
    private val now: () -> Long = System::currentTimeMillis
) {

    /** Where the per-day count lives. [dayKey] is the local calendar date. */
    interface DailyStore {
        fun count(dayKey: String): Int
        fun setCount(dayKey: String, count: Int)

        class InMemory : DailyStore {
            private var key = ""
            private var value = 0
            override fun count(dayKey: String) = if (dayKey == key) value else 0
            override fun setCount(dayKey: String, count: Int) { key = dayKey; value = count }
        }
    }

    companion object {
        /** Enough to stop a double-tap without being noticeable when reading. */
        const val DEFAULT_MIN_GAP_MS = 3_000L

        /** A real session of questions fits well inside this. */
        const val DEFAULT_MAX_PER_HOUR = 20

        /**
         * A subscriber's whole day. Twenty questions and all twelve signs' insights
         * for one period come to 32, so 50 leaves room for a curious evening and
         * still bounds what one account can spend. PremiumDialog states this
         * number rather than "unlimited", which it used to say.
         */
        const val DEFAULT_MAX_PER_DAY = 50

        /**
         * The longest question that will be sent to the model.
         *
         * The limiter above bounds how *often* someone can ask; nothing bounded
         * how *much* they could send. The question box accepted any length and
         * handed it straight to Firebase AI, which bills by token — so twenty
         * pasted pages an hour was within the rules as written, and the bill
         * lands here with no server in between.
         *
         * Length is also the room a prompt injection needs. GeminiAstroService's
         * system prompt states its boundaries, and a shorter question is less
         * space in which to argue with them.
         *
         * A real question — "मेरी नौकरी में पदोन्नति कब होगी?" — is well under
         * a hundred characters. Five hundred is generous.
         */
        const val MAX_QUESTION_CHARS = 500

        const val ONE_HOUR_MS = 60L * 60L * 1000L
    }

    sealed interface Decision {
        /** Go ahead; the call has been counted. */
        object Allow : Decision

        /** Asked again too quickly. [waitMs] is how long is left. */
        data class TooSoon(val waitMs: Long) : Decision

        /** The hourly allowance is spent. [retryAfterMs] is until the oldest call ages out. */
        data class HourlyCapReached(val retryAfterMs: Long) : Decision

        /** Today's allowance is spent. It resets at local midnight. */
        object DailyCapReached : Decision
    }

    private val calls = ArrayDeque<Long>()

    /**
     * Asks for permission and, when granted, counts the call. Callers that do
     * not go on to make the request will simply have spent one of the hour's
     * allowance, which is the safe direction to be wrong in.
     */
    @Synchronized
    fun tryAcquire(): Decision {
        val t = now()

        while (calls.isNotEmpty() && t - calls.first() >= ONE_HOUR_MS) {
            calls.removeFirst()
        }

        val last = calls.lastOrNull()
        if (last != null && t - last < minGapMs) {
            return Decision.TooSoon(minGapMs - (t - last))
        }

        if (calls.size >= maxPerHour) {
            val oldest = calls.first()
            return Decision.HourlyCapReached(ONE_HOUR_MS - (t - oldest))
        }

        val day = dayKey(t)
        val today = dailyStore.count(day)
        if (today >= maxPerDay) return Decision.DailyCapReached

        dailyStore.setCount(day, today + 1)
        calls.addLast(t)
        return Decision.Allow
    }

    private fun dayKey(t: Long): String {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = t }
        return "%04d-%02d-%02d".format(
            java.util.Locale.US,
            c.get(java.util.Calendar.YEAR),
            c.get(java.util.Calendar.MONTH) + 1,
            c.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    /** For a message to the user: whole minutes, rounded up, never zero. */
    fun minutesFrom(ms: Long): Int = ((ms + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
}
