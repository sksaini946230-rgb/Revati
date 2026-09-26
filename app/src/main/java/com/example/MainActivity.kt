package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.util.LanguageManager
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.AdBanner
import com.example.ui.components.BottomNavBar
import com.example.ui.components.PremiumDialog
import com.example.ui.components.RateUsDialog
import com.example.ui.components.TopHeaderBar
import com.example.ui.components.FeatureDiscoveryOverlay
import com.example.ui.components.DiscoveryStep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.KundaliScreen
import com.example.ui.screens.MatchingScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.MuhuratScreen
import com.example.ui.screens.NumerologyScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PanchangScreen
import com.example.ui.screens.RashifalScreen
import com.example.ui.screens.SavedProfilesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.AstroVedaTheme
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback


class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    private var mInterstitialAd: InterstitialAd? = null

    /** How many times the interstitial has failed to load since the last success. */
    private var interstitialLoadFailures = 0

    /** True while a load is in flight, so a retry and a tab change cannot double-request. */
    private var interstitialLoading = false
    private var lastInterstitialShowTime = 0L

    /** When this process started showing UI — the first ad waits for it. */
    private val sessionStartTime = System.currentTimeMillis()
    private var interstitialsShownThisSession = 0

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) scheduleNotificationWorkers()
        }

    private fun scheduleNotificationWorkers() {
        try {
            com.example.worker.AstroNotificationWorker.scheduleDailyNotification(this)
            com.example.worker.FestivalNotificationWorker.scheduleFestivalNotification(this)
            com.example.worker.MuhuratNotificationWorker.scheduleMuhuratNotification(this)
        } catch (_: Throwable) {
            // Scheduling is best-effort; a failure here must not take the app down.
        }
    }

    /**
     * Asks for notification permission — but only after onboarding.
     *
     * This used to fire from onCreate on the very first cold start, before the
     * splash had even cleared, so the system dialog appeared over a screen that
     * had not yet explained what the app does. Android only ever shows that dialog
     * once, so a reflexive "Don't allow" there is permanent.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            scheduleNotificationWorkers()
            return
        }
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (granted) {
            scheduleNotificationWorkers()
        } else {
            try {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } catch (_: Throwable) {
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Language and App Check are initialised in RevatiApp.onCreate, which runs
        // for every process entry — workers and widgets included, which this did
        // not cover.

        try {
            com.example.util.AstroAnalytics.init(applicationContext)
            com.example.util.AstroAnalytics.logAppOpen()
        } catch (_: Throwable) {}

        // The app is written for adults reading their own charts, and it is not
        // in the Families programme. The ceiling is T, not G: G is the Families
        // rating, and on this app it left almost no demand — the Play build
        // answered every banner with code=3 No fill on two phones, on WiFi and
        // mobile data, while AdMob showed the app Ready and app-ads.txt verified.
        // T still keeps mature (MA) ads out.
        try {
            MobileAds.setRequestConfiguration(
                com.google.android.gms.ads.RequestConfiguration.Builder()
                    .setMaxAdContentRating(
                        com.google.android.gms.ads.RequestConfiguration.MAX_AD_CONTENT_RATING_T
                    )
                    .setTagForChildDirectedTreatment(
                        com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
                    )
                    .setTagForUnderAgeOfConsent(
                        com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
                    )
                    .build()
            )
        } catch (_: Throwable) {}

        // Gather ad consent BEFORE initialising the ads SDK. UMP works this out by
        // region, so it is a no-op in India and shows the form in the EEA/UK, where
        // serving ads without one is an AdMob policy violation.
        try {
            com.example.service.AdConsentManager.gatherConsent(this) {
                try {
                    MobileAds.initialize(this) {
                        com.example.service.AdsInitState.markReady()
                    }
                    loadInterstitialAd()
                    onAdsInitialised()
                } catch (e: Throwable) {
                    // fail gracefully
                }
            }
        } catch (e: Throwable) {
            // If consent gathering itself blows up, do not silently lose all ads.
            try {
                MobileAds.initialize(this) {
                    com.example.service.AdsInitState.markReady()
                }
                loadInterstitialAd()
                onAdsInitialised()
            } catch (_: Throwable) {
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.showInterstitialTrigger.collect {
                    showInterstitialAd()
                }
            }
        }

        setContent {
            AstroVedaTheme {
                val selectedTab by mainViewModel.selectedTab.collectAsState()
                val showPremium by mainViewModel.showPremiumDialog.collectAsState()
                val showRateUsDialog by mainViewModel.showRateUsDialog.collectAsState()
                val isOnboardingCompleted by mainViewModel.isOnboardingCompleted.collectAsState()
                val isOffline by mainViewModel.isOffline.collectAsState()
                val isSyncing by mainViewModel.isSyncing.collectAsState()
                val isFirestoreSyncing by mainViewModel.isFirestoreSyncing.collectAsState()
                val isFirstRunSyncing by mainViewModel.isFirstRunSyncing.collectAsState()
                val isStartupComplete by mainViewModel.isStartupComplete.collectAsState()
                val isDiscoveryCompleted by mainViewModel.isDiscoveryCompleted.collectAsState()
                val currentUser by mainViewModel.currentUser.collectAsState()
                val isCloudBackupEnabled = currentUser != null
                val showAuthScreen by mainViewModel.showAuthScreen.collectAsState()

                val isSplashCompleted by mainViewModel.isSplashCompleted.collectAsState()

                if (!isSplashCompleted) {
                    SplashScreen(
                        onSplashComplete = { mainViewModel.completeSplash() }
                    )
                } else if (!isOnboardingCompleted) {
                    OnboardingScreen(
                        viewModel = mainViewModel,
                        onComplete = {
                            mainViewModel.completeOnboarding()
                            // Now the user knows what daily Panchang alerts are for.
                            requestNotificationPermissionIfNeeded()
                        }
                    )
                } else if (isFirstRunSyncing) {
                    FirstRunSyncingOverlay()
                } else {
                    // The app had no back handling at all — no BackHandler anywhere,
                    // and tabs are plain state rather than a nav graph — so pressing
                    // Back on any tab but Panchang closed the app outright. Back now
                    // dismisses whatever is open, then walks home, and only exits
                    // from Panchang itself.
                    androidx.activity.compose.BackHandler(enabled = true) {
                        when {
                            showPremium -> mainViewModel.showPremiumDialog.value = false
                            showRateUsDialog -> mainViewModel.dismissRateUs()
                            selectedTab != AppTab.PANCHANG -> mainViewModel.selectTab(AppTab.PANCHANG)
                            else -> finish()
                        }
                    }

                    // Notifications are requested once onboarding is behind us; for a
                    // returning user that happened on an earlier launch, so make sure
                    // the workers are scheduled either way.
                    LaunchedEffect(Unit) { requestNotificationPermissionIfNeeded() }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            Column {
                                TopHeaderBar(
                                    isOffline = isOffline,
                                    isSyncing = isSyncing,
                                    isFirestoreSyncing = isFirestoreSyncing,
                                    isCloudBackupEnabled = isCloudBackupEnabled,
                                    onLanguageToggle = { mainViewModel.toggleLanguage() },
                                    onPremiumClick = { mainViewModel.showPremiumDialog.value = true },
                                    onSettingsClick = { mainViewModel.navigateToMore(subTab = 1) }
                                )
                                if (isCloudBackupEnabled && isFirestoreSyncing) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.dp)
                                            .testTag("firestore_sync_progress_bar"),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        },
                        bottomBar = {
                            Column {
                                val isPro by mainViewModel.isProUser.collectAsState()
                                if (!isPro) {
                                    // Not gated on isStartupComplete any more, and that
                                    // matters. AdBanner's own doc says it plainly: a
                                    // signal that never arrives must never be able to
                                    // hold the banner back forever. The gate was removed
                                    // from inside AdBanner once already and then
                                    // reintroduced here, one layer up, where it did the
                                    // same damage — isStartupComplete is set at the end
                                    // of a coroutine that first runs recalculatePanchang(),
                                    // so a single throw in the ephemeris meant no banner
                                    // for the whole session. AdBanner asks immediately and
                                    // retries on its own; it needs no permission from us.
                                    AdBanner(
                                        onRemoveAdsClick = { mainViewModel.showPremiumDialog.value = true }
                                    )
                                }
                                BottomNavBar(
                                    selectedTab = selectedTab,
                                    onTabSelected = { mainViewModel.onBottomNavTabSelected(it) }
                                )
                            }
                        }
                    ) { innerPadding ->
                        val globalError by mainViewModel.globalError.collectAsState()

                        com.example.ui.components.ErrorBoundary(
                            externalError = globalError,
                            onClearError = { mainViewModel.clearGlobalError() },
                            onRetry = {
                                // Clear error and reset tab or rerun last query
                                mainViewModel.clearGlobalError()
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // The starfield was wrapped around Panchang and
                                // Kundali only, so those two tabs had a cosmic
                                // background and Horoscope, Muhurat and More were
                                // flat — the app looked like three different apps.
                                // One background behind every tab.
                                @OptIn(ExperimentalSharedTransitionApi::class)
                                com.example.ui.components.CelestialBackground(
                                    deferred = !isStartupComplete
                                ) {
                                SharedTransitionLayout {
                                    AnimatedContent(
                                        targetState = selectedTab,
                                        label = "TabTransition",
                                        transitionSpec = {
                                            fadeIn(animationSpec = tween(400))
                                                .togetherWith(fadeOut(animationSpec = tween(300)))
                                        }
                                    ) { tab ->
                                        when (tab) {
                                            AppTab.PANCHANG -> PanchangScreen(
                                                viewModel = mainViewModel,
                                                sharedTransitionScope = this@SharedTransitionLayout,
                                                animatedVisibilityScope = this@AnimatedContent
                                            )
                                            AppTab.RASHIFAL -> RashifalScreen(mainViewModel)
                                            AppTab.KUNDALI -> KundaliScreen(
                                                viewModel = mainViewModel,
                                                sharedTransitionScope = this@SharedTransitionLayout,
                                                animatedVisibilityScope = this@AnimatedContent
                                            )
                                            AppTab.MUHURAT -> MuhuratScreen(mainViewModel)
                                            AppTab.MORE -> MoreScreen(mainViewModel)
                                        }
                                    }
                                }
                                }

                                if (showPremium) {
                                    PremiumDialog(
                                        viewModel = mainViewModel,
                                        onDismiss = { mainViewModel.showPremiumDialog.value = false }
                                    )
                                }

                                if (showRateUsDialog) {
                                    RateUsDialog(
                                        viewModel = mainViewModel,
                                        onDismiss = { mainViewModel.dismissRateUs() }
                                    )
                                }

                                // Sign-in is optional now — an account only buys
                                // cloud backup — so it opens over the app instead
                                // of standing in front of it.
                                if (showAuthScreen) {
                                    androidx.compose.ui.window.Dialog(
                                        onDismissRequest = { mainViewModel.closeAuthScreen() },
                                        properties = androidx.compose.ui.window.DialogProperties(
                                            usePlatformDefaultWidth = false
                                        )
                                    ) {
                                        com.example.ui.screens.AuthScreen(
                                            viewModel = mainViewModel,
                                            onDismiss = { mainViewModel.closeAuthScreen() }
                                        )
                                    }
                                }

                                if (!isDiscoveryCompleted && isOnboardingCompleted && !isFirstRunSyncing) {
                                    FeatureDiscoveryOverlay(
                                        steps = listOf(
                                            DiscoveryStep(
                                                titleHi = "दैनिक पंचांग",
                                                titleEn = "Daily Panchang",
                                                descriptionHi = "तिथि, नक्षत्र और सूर्योदय के समय के साथ अपने दिन की शुरुआत दिव्य रूप से करें।",
                                                descriptionEn = "Start your day divinely with precise Tithi, Nakshatra, and Sunrise timings.",
                                                icon = Icons.Default.WbSunny
                                            ),
                                            DiscoveryStep(
                                                titleHi = "विस्तृत कुण्डली",
                                                titleEn = "Detailed Kundali",
                                                descriptionHi = "अपने जन्म विवरण के साथ अपनी विस्तृत जन्म कुण्डली और ग्रह स्थितियों का विश्लेषण करें।",
                                                descriptionEn = "Generate and analyze your detailed birth chart and planetary positions with ease.",
                                                icon = Icons.Default.AutoAwesome
                                            ),
                                            DiscoveryStep(
                                                titleHi = "शुभ मुहूर्त",
                                                titleEn = "Auspicious Muhurat",
                                                descriptionHi = "अपनी महत्वपूर्ण गतिविधियों के लिए सबसे शुभ समय खोजें।",
                                                descriptionEn = "Find the most auspicious timings for your important activities.",
                                                icon = Icons.Default.Schedule
                                            )
                                        ),
                                        onComplete = { mainViewModel.completeDiscovery() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The two formats that are not requested from a composable.
     *
     * The app-open ad is registered in RevatiApp because it watches the whole
     * process, but it cannot ask for anything until consent has been gathered
     * and the SDK is up — which happens here. The rewarded ad is preloaded so
     * the PDF button can offer to play one instead of making the user wait for
     * a request after they have already tapped.
     */
    private fun onAdsInitialised() {
        try {
            com.example.service.AppOpenAdManager.isProUser = { mainViewModel.isProUser.value }
            com.example.service.AppOpenAdManager.load()
            if (!mainViewModel.isProUser.value) {
                com.example.service.RewardedAdManager.load(this)
            }
        } catch (e: Throwable) {
            // Ads must never take the app down with them.
        }
    }

    private fun loadInterstitialAd() {
        // Through AdIds, like every other placement. This used to take the
        // BuildConfig value whenever it was non-blank — so a debug build asked
        // the LIVE interstitial unit for ads, the development traffic AdIds
        // exists to prevent, and answered code=3 No fill because the .debug
        // package is not the app that unit belongs to. It also let a release
        // pass the NOT_CONFIGURED sentinel straight to the SDK.
        val interstitialId = com.example.service.AdIds.resolve(
            try {
                app.revati.jyotish.BuildConfig.ADMOB_INTERSTITIAL_ID
            } catch (e: Throwable) {
                null
            },
            com.example.service.AdIds.TEST_INTERSTITIAL
        )

        if (interstitialId.isBlank()) return

        if (interstitialLoading || mInterstitialAd != null) return

        val adRequest = AdRequest.Builder().build()
        try {
            interstitialLoading = true
            InterstitialAd.load(this, interstitialId, adRequest, object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialLoading = false
                    mInterstitialAd = null

                    // At error level, like the banner's, and for the same
                    // reason: this device keeps nothing below E, so a load that
                    // said nothing left "why is no interstitial showing?"
                    // unanswerable. It used to say nothing at all.
                    android.util.Log.e(
                        "InterstitialAd",
                        "load failed: code=${adError.code} msg=${adError.message}"
                    )

                    // And retry, because nothing did. A failed load was only
                    // re-attempted when a tab change happened to get past all
                    // three gates — and the gates return before reaching the
                    // reload, so during the first 45 seconds no tab change
                    // retried anything at all. One no-fill at launch meant no
                    // interstitial for the session, which is the same fault the
                    // banner had.
                    interstitialLoadFailures++
                    if (interstitialLoadFailures <= MAX_INTERSTITIAL_LOAD_RETRIES) {
                        adHandler.postDelayed(
                            { loadInterstitialAd() },
                            INTERSTITIAL_RETRY_MS * interstitialLoadFailures
                        )
                    }
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    interstitialLoading = false
                    interstitialLoadFailures = 0
                    mInterstitialAd = interstitialAd
                    android.util.Log.e("InterstitialAd", "loaded")
                }
            })
        } catch (e: Throwable) {
            interstitialLoading = false
        }
    }

    /** Posts the interstitial retries. Cleared in onDestroy so none outlives the Activity. */
    private val adHandler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onDestroy() {
        // A retry posted here holds a reference to this Activity. On a rotation
        // that is a leaked Activity per pending callback, and the ad it loads
        // belongs to an instance nobody is looking at.
        adHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun showInterstitialAd() {
        val currentTime = System.currentTimeMillis()

        // Three gates, and the numbers behind them have moved twice.
        //
        // They began at a one-minute gap with no startup delay, which meant
        // looking around the five tabs produced a full-screen ad every minute
        // and the first could land seconds after launch — on an app people open
        // for ten seconds to read a tithi. That was pulled back hard: 90 seconds
        // before the first, three minutes between, three a session.
        //
        // The owner has asked for more inventory twice. They now sit at 30
        // seconds before the first, 75 between, eight a session — and, which
        // matters more than any of those numbers, the *sub*-tab rows count as
        // breaks too, so a user who never leaves the Kundali tab is no longer
        // worth nothing.
        //
        // What is deliberately not done, because it is what gets an AdMob
        // account closed rather than merely disliked: no full-screen ad on a
        // cold start, none between a tap and the result it asked for, and never
        // two stacked. Play's disruptive-ads policy and AdMob's own guidance
        // both name those three. Every ad here still lands on a deliberate tap
        // on a navigation control, which is the case Google gives as the
        // acceptable one.
        //
        // If retention or reviews turn, these three numbers are the dial.
        if (currentTime - sessionStartTime < FIRST_INTERSTITIAL_DELAY_MS) return
        if (currentTime - lastInterstitialShowTime < INTERSTITIAL_MIN_GAP_MS) return
        if (interstitialsShownThisSession >= MAX_INTERSTITIALS_PER_SESSION) return

        if (mainViewModel.isProUser.value) {
            return
        }

        // The app-open ad is the other thing that can cover the screen, and the
        // two are driven by unrelated events — a tab change and a return to the
        // foreground. Without this, doing both at once hands the user two
        // full-screen ads back to back.
        if (!com.example.service.FullScreenAdGate.canShow(currentTime)) return

        try {
            mInterstitialAd?.let { ad ->
                ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {
                        com.example.service.FullScreenAdGate.onShown()
                    }

                    override fun onAdDismissedFullScreenContent() {
                        com.example.service.FullScreenAdGate.onDismissed()
                    }

                    override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) {
                        com.example.service.FullScreenAdGate.onDismissed()
                    }
                }
                ad.show(this)
                mInterstitialAd = null
                lastInterstitialShowTime = currentTime
                interstitialsShownThisSession++
                loadInterstitialAd() // Preload the next one
            } ?: run {
                loadInterstitialAd()
            }
        } catch (e: Throwable) {
            // fail gracefully
        }
    }

    private companion object {
        const val FIRST_INTERSTITIAL_DELAY_MS = 30_000L
        const val INTERSTITIAL_MIN_GAP_MS = 75_000L
        const val MAX_INTERSTITIALS_PER_SESSION = 8

        /** 30s, 60s, 90s, 120s. Bounded, because an interstitial nobody can be
         *  shown yet is not worth requesting forever. */
        const val INTERSTITIAL_RETRY_MS = 30_000L
        const val MAX_INTERSTITIAL_LOAD_RETRIES = 4
    }
}

@Composable
fun FirstRunSyncingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
            androidx.compose.material3.Text(
                text = LanguageManager.getString("ब्रह्मांडीय डेटा सिंक हो रहा है...", "Getting today's sky ready..."),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.Text(
                text = com.example.util.LanguageManager.getString(
                    "ऑफ़लाइन उपयोग के लिए खगोलीय डेटा तैयार हो रहा है...",
                    "Syncing cosmic data for offline access..."
                ),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

