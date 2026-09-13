package com.example.ui.components

import com.example.service.AdIds
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.service.AdsInitState
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.delay

/**
 * The banner.
 *
 * It used to give up permanently on its first failure: one `onAdFailedToLoad`
 * set a flag that was never cleared, so a transient no-fill — or simply asking
 * before the ads SDK had finished initialising, which is what happens on a cold
 * start — left the app with no banner for the rest of the session.
 *
 * The fix is the retry, not a gate. An earlier attempt at this refused to ask
 * until [AdsInitState] said the SDK was up, and that is worse: if the signal
 * never arrives the banner never appears at all, which is a bigger failure than
 * the one being fixed. So this asks straight away and asks again on failure, at
 * 4s, 12s and 36s. The readiness signal only earns an extra attempt when it
 * arrives — it can never hold the banner back.
 *
 * Giving up renders nothing rather than an empty grey strip.
 */
@Composable
fun AdBanner(
    onRemoveAdsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bannerId = remember {
        // Through AdIds, like every other placement: debug always asks for the
        // test unit, and release refuses a test id, the NOT_CONFIGURED sentinel
        // and anything malformed. This used to carry its own copy of the debug
        // half and none of the release half.
        AdIds.resolve(
            try {
                app.revati.jyotish.BuildConfig.ADMOB_BANNER_ID
            } catch (e: Throwable) {
                null
            },
            AdIds.TEST_BANNER
        )
    }

    val adsReady by AdsInitState.ready.collectAsState()
    
    // Bumping this re-requests through the update block below.
    var attempt by remember { mutableIntStateOf(0) }
    var failures by remember { mutableIntStateOf(0) }
    var loaded by remember { mutableStateOf(false) }

    // No fill is a fact about this minute, not about this session.
    //
    // This used to stop after three retries and render nothing for the rest of
    // the session. On the test device that is exactly what happens: four
    // requests in a row came back `code=3 No fill` inside the first minute, the
    // banner gave up, and the next twenty minutes of use carried no ad at all —
    // while the same build had filled a minute earlier in landscape. Demand for
    // a new app is thin and intermittent, and a policy of "gave up at 09:05"
    // turns a thin minute into an empty session.
    //
    // So the fast attempts stay for the cold-start case and then it settles to
    // one request a minute instead of stopping. One a minute is also what a
    // filled banner does — see the refresh below — so a failing banner costs no
    // more requests than a working one.
    LaunchedEffect(failures) {
        if (failures == 0) return@LaunchedEffect
        // 4s, 12s, 36s while it might be the SDK or the network coming up,
        // then a steady minute.
        val wait = if (failures <= BACKOFF.size) RETRY_BASE_MS * BACKOFF[failures - 1] else REFRESH_MS
        delay(wait)
        attempt++
    }

    // No refresh timer here, on purpose. There used to be one, asking for a
    // new ad every 45 seconds — and the AdMob SDK refreshes a loaded banner on
    // its own, on the interval set against the ad unit in the AdMob console.
    // The two stacked: on 13 Sep 2026 the device logged six loads in three
    // minutes, two of them 13 seconds apart, under AdMob's 30-second floor.
    // Refresh is the unit's setting now (Custom, 45 seconds) and nothing else.
    // Only a FAILED load is retried from here, because the SDK does not retry
    // those.

    // The SDK finishing initialisation is the most likely reason an early
    // request failed, so it is worth one attempt of its own.
    LaunchedEffect(adsReady) {
        if (adsReady && !loaded) attempt++
    }

    if (bannerId.isBlank()) {
        Box(modifier = Modifier.size(0.dp))
        return
    }

    // No ad, no space.
    //
    // An AdView that has been given an adaptive size reserves that height as
    // soon as it exists, ad or no ad — about 50dp, plus this padding. That used
    // to disappear on its own: after three failures the banner gave up and
    // rendered nothing. Now that it keeps retrying, the empty strip stayed, and
    // on a phone getting no fill it sat above the tab bar for the whole session
    // looking like a rendering fault. It is exactly the sort of thing the retry
    // fix was not supposed to introduce.
    //
    // The view stays in the tree so it can keep requesting; it simply takes no
    // room until an ad has actually arrived. Nothing is hidden from the user or
    // from AdMob — there is no impression to count while there is no ad.
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .then(if (loaded) Modifier.padding(vertical = 4.dp) else Modifier.height(0.dp))
            .testTag("ad_banner_container"),
        factory = { context ->
            AdView(context).apply {
                // Anchored adaptive, not the fixed 320x50 this used to ask for.
                //
                // AdSize.BANNER is the legacy size: 320dp wide whatever the
                // phone is, so on a 360dp screen it left 40dp of the width
                // unsold and sat visibly narrower than everything around it.
                // Adaptive takes the full width and picks a height to match,
                // and Google's own guidance is to prefer it — advertisers bid
                // on it, the fixed size is what is left over.
                //
                // Same ad unit, same placement, no policy difference; it is the
                // size that changes.
                val widthDp = run {
                    val m = context.resources.displayMetrics
                    (m.widthPixels / m.density).toInt().coerceAtLeast(320)
                }
                setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
                )
                adUnitId = bannerId
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        // Worth keeping: a banner that silently declines to
                        // appear is exactly the bug that took a day to find.
                        //
                        // At error level on purpose. This was Log.w, and the
                        // device this app is tested on keeps nothing below E —
                        // a logcat dump from it holds thousands of E lines and
                        // not one W. So the message the comment above calls
                        // worth keeping was invisible in the only place anyone
                        // reads it. Same reason PurchaseVerifier logs at error.
                        android.util.Log.e(
                            "AdBanner",
                            "load failed: code=${error.code} domain=${error.domain} " +
                                "msg=${error.message} cause=${error.cause}"
                        )
                        failures++
                    }

                    override fun onAdLoaded() {
                        // Also at error level, and only because "did the banner
                        // fill?" cannot be answered from this device otherwise:
                        // an absent failure line and an absent success line look
                        // identical when the buffer drops both.
                        android.util.Log.e("AdBanner", "loaded")
                        loaded = true
                        failures = 0
                    }
                }
                setTag(ATTEMPT_TAG, 0)
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            if (adView.getTag(ATTEMPT_TAG) != attempt) {
                adView.setTag(ATTEMPT_TAG, attempt)
                adView.loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { it.destroy() }
    )
}

private const val RETRY_BASE_MS = 4_000L
private val BACKOFF = longArrayOf(1, 3, 9)

/**
 * The steady retry interval for a banner that cannot fill, once the fast
 * cold-start attempts are spent. It matches the ad unit's refresh interval in
 * the AdMob console (45 seconds), so a banner that cannot fill never asks more
 * often than one that can. Thirty is AdMob's floor and is not used — a banner
 * asking as fast as it is allowed to is what invalid-traffic detection looks
 * for.
 */
private const val REFRESH_MS = 45_000L
private val ATTEMPT_TAG = "revati_ad_attempt".hashCode()
