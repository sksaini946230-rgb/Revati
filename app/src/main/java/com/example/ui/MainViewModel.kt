package com.example.ui

import kotlinx.coroutines.Dispatchers
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.astro.ChoghadiyaCalculator
import com.example.astro.FestivalProvider
import com.example.astro.KundaliCalculator
import com.example.astro.KundaliMatchingCalculator
import com.example.astro.MuhuratCalculator
import com.example.astro.NumerologyCalculator
import com.example.astro.PanchangCalculator
import com.example.astro.RashifalProvider
import com.example.data.ai.GeminiAstroService
import com.example.data.local.AppDatabase
import com.example.data.local.AstroCacheRepository
import com.example.data.local.DatabaseProvider
import com.example.data.local.KundaliEntity
import com.example.data.local.KundaliRepository
import com.example.data.local.ProfileMerge
import com.example.data.local.RecentSearchEntity
import com.example.data.local.RecentSearchRepository
import com.example.data.model.ChoghadiyaSlot
import com.example.data.model.CityLocation
import com.example.data.model.FestivalData
import com.example.data.model.GunaMatchingResult
import com.example.data.model.KundaliChartData
import com.example.data.model.MuhuratItem
import com.example.data.model.NumerologyData
import com.example.data.model.PanchangData
import com.example.data.model.RashifalData
import com.example.util.LanguageManager
import com.example.util.SecurityUtils
import com.example.service.BillingManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics

enum class AppTab {
    PANCHANG,
    RASHIFAL,
    KUNDALI,
    MUHURAT,
    MORE
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KundaliRepository
    private val recentSearchRepository: RecentSearchRepository
    private val cacheRepository: AstroCacheRepository
    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _isUsingCache = MutableStateFlow(false)
    val isUsingCache: StateFlow<Boolean> = _isUsingCache.asStateFlow()

    private val _isPanchangLoading = MutableStateFlow(false)
    val isPanchangLoading: StateFlow<Boolean> = _isPanchangLoading.asStateFlow()

    private val _isHoroscopeLoading = MutableStateFlow(false)
    val isHoroscopeLoading: StateFlow<Boolean> = _isHoroscopeLoading.asStateFlow()

    private val _isNewsLoading = MutableStateFlow(false)
    val isNewsLoading: StateFlow<Boolean> = _isNewsLoading.asStateFlow()

    val isSyncing: StateFlow<Boolean> = kotlinx.coroutines.flow.combine(
        _isPanchangLoading,
        _isHoroscopeLoading,
        _isNewsLoading
    ) { panchang, horoscope, news ->
        panchang || horoscope || news
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), false)

    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null

    private fun monitorNetwork() {
        val networkRequest = android.net.NetworkRequest.Builder()
            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                _isOffline.value = false
                if (_currentUser.value != null) {
                    syncCloudAndLocalProfiles()
                }
            }
            override fun onLost(network: android.net.Network) {
                _isOffline.value = true
            }
        }
        networkCallback = callback
        try {
            connectivityManager.registerNetworkCallback(networkRequest, callback)
        } catch (_: Exception) {}
        
        // Initial check
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        _isOffline.value = caps == null || !caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    private val sharedPrefs = application.getSharedPreferences("astroveda_prefs", Context.MODE_PRIVATE)

    private val _isFirstRun = MutableStateFlow(sharedPrefs.getBoolean("is_first_run", true))
    val isFirstRun: StateFlow<Boolean> = _isFirstRun.asStateFlow()

    private val _isFirstRunSyncing = MutableStateFlow(false)
    val isFirstRunSyncing: StateFlow<Boolean> = _isFirstRunSyncing.asStateFlow()

    fun completeFirstRunSync() {
        if (!_isFirstRun.value) return
        
        _isFirstRunSyncing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            // Simulate critical astronomical data download / pre-computation
            // This ensures offline capability immediately after installation
            try {
                // Initialize calculators and cache some basic data
                val defaultCity = CityLocation("New Delhi", "नई दिल्ली", "Delhi", 28.6139, 77.2090)
                PanchangCalculator.calculatePanchang(Date(), defaultCity)
                RashifalProvider.getDailyHoroscope()
                
                // Add a small delay to simulate network/db caching of ephemeris
                kotlinx.coroutines.delay(2000)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Ignore sync errors for onboarding, don't crash
            }
            
            _isFirstRunSyncing.value = false
            _isFirstRun.value = false
            sharedPrefs.edit().putBoolean("is_first_run", false).apply()
        }
    }

    private val _isProUser = MutableStateFlow(sharedPrefs.getBoolean("is_pro", false))
    val isProUser: StateFlow<Boolean> = _isProUser.asStateFlow()

    fun setProUser(isPro: Boolean) {
        _isProUser.value = isPro
        sharedPrefs.edit().putBoolean("is_pro", isPro).apply()
    }

    // Rate Us & Feedback States
    private val _showRateUsDialog = MutableStateFlow(false)
    val showRateUsDialog: StateFlow<Boolean> = _showRateUsDialog.asStateFlow()

    private val _hasRated = MutableStateFlow(sharedPrefs.getBoolean("has_rated", false))
    val hasRated: StateFlow<Boolean> = _hasRated.asStateFlow()

    fun showRateUs() {
        _showRateUsDialog.value = true
    }

    fun dismissRateUs() {
        _showRateUsDialog.value = false
        sharedPrefs.edit().putLong("rate_dialog_dismissed_at", System.currentTimeMillis()).apply()
    }

    fun markAsRated() {
        _showRateUsDialog.value = false
        _hasRated.value = true
        sharedPrefs.edit().putBoolean("has_rated", true).apply()
    }

    fun submitFeedback(rating: Int, feedback: String) {
        // Save to SharedPreferences and mark as rated
        sharedPrefs.edit()
            .putInt("user_rating_value", rating)
            .putString("user_feedback_text", feedback)
            .apply()
        markAsRated()
    }

    fun incrementLookupCount() {
        if (_hasRated.value) return

        val count = sharedPrefs.getInt("successful_lookup_count", 0) + 1
        sharedPrefs.edit().putInt("successful_lookup_count", count).apply()

        val dismissedAt = sharedPrefs.getLong("rate_dialog_dismissed_at", 0L)
        val oneDayMs = 24 * 60 * 60 * 1000L
        
        // Trigger Rate Us dialog after 3 successful lookups (Kundali/Panchang)
        if (count >= 3 && (System.currentTimeMillis() - dismissedAt) > oneDayMs) {
            _showRateUsDialog.value = true
        }
    }

    fun incrementSessionActionCount() {
        if (_hasRated.value) return

        val count = sharedPrefs.getInt("session_action_count", 0) + 1
        sharedPrefs.edit().putInt("session_action_count", count).apply()

        val dismissedAt = sharedPrefs.getLong("rate_dialog_dismissed_at", 0L)
        val oneDayMs = 24 * 60 * 60 * 1000L
        if (count >= 10 && (System.currentTimeMillis() - dismissedAt) > oneDayMs) {
            _showRateUsDialog.value = true
        }
    }

    // Google Play Billing Client Wrapper
    private val billingManager = BillingManager(application) { isUnlocked ->
        setProUser(isUnlocked)
    }

    val isBillingReady = billingManager.isReady
    val billingErrorMessage = billingManager.errorMessage
    val subscriptionProductDetails = billingManager.productDetails

    fun makePurchase(activity: android.app.Activity) {
        billingManager.launchPurchaseFlow(activity)
    }

    // Interstitial ad trigger
    private val _showInterstitialTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val showInterstitialTrigger: SharedFlow<Unit> = _showInterstitialTrigger.asSharedFlow()

    fun triggerInterstitial() {
        _showInterstitialTrigger.tryEmit(Unit)
    }

    // Global Error State for ErrorBoundary integration
    private val _globalError = MutableStateFlow<Throwable?>(null)
    val globalError: StateFlow<Throwable?> = _globalError.asStateFlow()

    fun reportError(t: Throwable) {
        _globalError.value = t
    }

    fun clearGlobalError() {
        _globalError.value = null
    }

    // Feature Discovery State
    private val _isDiscoveryCompleted = MutableStateFlow(sharedPrefs.getBoolean("is_discovery_completed", false))
    val isDiscoveryCompleted: StateFlow<Boolean> = _isDiscoveryCompleted.asStateFlow()

    fun completeDiscovery() {
        _isDiscoveryCompleted.value = true
        sharedPrefs.edit().putBoolean("is_discovery_completed", true).apply()
    }

    fun resetDiscovery() {
        _isDiscoveryCompleted.value = false
        sharedPrefs.edit().putBoolean("is_discovery_completed", false).apply()
    }

    // Splash Screen State — shown on every launch for 1.5s
    private val _isSplashCompleted = MutableStateFlow(false)
    val isSplashCompleted: StateFlow<Boolean> = _isSplashCompleted.asStateFlow()

    fun completeSplash() {
        _isSplashCompleted.value = true
    }

    // Onboarding State
    private val _isOnboardingCompleted = MutableStateFlow(sharedPrefs.getBoolean("is_onboarding_completed", false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    fun completeOnboarding() {
        _isOnboardingCompleted.value = true
        sharedPrefs.edit().putBoolean("is_onboarding_completed", true).apply()
        com.example.util.AstroAnalytics.logOnboardingComplete()
        completeFirstRunSync()
    }

    fun resetOnboarding() {
        _isOnboardingCompleted.value = false
    }

    // Active Tab
    private val _selectedTab = MutableStateFlow(AppTab.PANCHANG)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    // Sub-tab states
    private val _panchangSubTab = MutableStateFlow(0) // 0: Daily Panchang, 1: Monthly Calendar
    val panchangSubTab: StateFlow<Int> = _panchangSubTab.asStateFlow()

    private val _kundaliSubTab = MutableStateFlow(0) // 0: Kundali Chart, 1: Guna Matching, 2: Numerology & Guidance, 3: Transits
    val kundaliSubTab: StateFlow<Int> = _kundaliSubTab.asStateFlow()

    private val _transitKundali = MutableStateFlow<KundaliChartData?>(null)
    val transitKundali: StateFlow<KundaliChartData?> = _transitKundali.asStateFlow()

    fun calculateCurrentTransits() {
        viewModelScope.launch(Dispatchers.Default) {
            // A chart of this instant — no date/time strings to format and reparse.
            val transitData = KundaliCalculator.chartForInstant(
                label = "Current Transits",
                jdUT = com.example.astro.AstroTime.julianDayFromMillis(System.currentTimeMillis()),
                placeName = _selectedCity.value.cityName,
                latitude = _selectedCity.value.latitude,
                longitude = _selectedCity.value.longitude
            )
            _transitKundali.value = transitData
        }
    }

    private val _moreSubTab = MutableStateFlow(0) // 0: Saved Profiles, 1: Settings
    val moreSubTab: StateFlow<Int> = _moreSubTab.asStateFlow()

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    /**
     * A deliberate tab tap on the bottom bar — the only place an interstitial is
     * allowed.
     *
     * It used to fire the moment a Kundali or match result was produced, covering
     * the thing the user had just asked for. Hanging it off selectTab instead was
     * too broad in the other direction: every programmatic navigation counted,
     * including "पूर्ण कुण्डली देखें →" and a stray tap on the daily Lagna chart.
     */
    fun onBottomNavTabSelected(tab: AppTab) {
        val previous = _selectedTab.value
        _selectedTab.value = tab
        if (previous != tab) {
            triggerInterstitial()
            com.example.util.AstroAnalytics.logScreenView(tab.name)
        }
    }

    /**
     * The sub-tab rows count as breaks too, and that is where the extra ad
     * inventory came from.
     *
     * A tab tap on the bottom bar was the only trigger, so a user who opened
     * Kundali and then moved between जन्म कुण्डली, गुण मिलान, अंकशास्त्र and
     * गोचर all session never saw a single interstitial. Those pills are the
     * same thing as the bottom bar — a deliberate tap on a navigation control,
     * arriving at a form or a list rather than at a result the user is waiting
     * for — which is the case Google's guidance names as acceptable.
     *
     * These are only *candidates*. The three limits in MainActivity still
     * decide, so adding them does not make the app noisier than those numbers
     * allow; it makes those numbers actually reachable.
     *
     * The chips that are **not** here are the ones that fetch: the Rashifal's
     * Today/Week/Month and the rashi selector. An ad over a reading the user
     * just asked for is exactly the interruption that got the old
     * "fire on the result" trigger removed.
     */
    private fun setSubTab(current: MutableStateFlow<Int>, subTab: Int) {
        val previous = current.value
        current.value = subTab
        if (previous != subTab) triggerInterstitial()
    }

    fun setPanchangSubTab(subTab: Int) = setSubTab(_panchangSubTab, subTab)

    fun setKundaliSubTab(subTab: Int) = setSubTab(_kundaliSubTab, subTab)

    fun setMoreSubTab(subTab: Int) = setSubTab(_moreSubTab, subTab)

    fun navigateToPanchang(subTab: Int = 0) {
        _panchangSubTab.value = subTab
        _selectedTab.value = AppTab.PANCHANG
    }

    fun navigateToKundali(subTab: Int = 0) {
        _kundaliSubTab.value = subTab
        _selectedTab.value = AppTab.KUNDALI
    }

    fun navigateToMore(subTab: Int = 0) {
        _moreSubTab.value = subTab
        _selectedTab.value = AppTab.MORE
    }

    fun navigateToRashifal() {
        _selectedTab.value = AppTab.RASHIFAL
    }

    fun navigateToMuhurat() {
        _selectedTab.value = AppTab.MUHURAT
    }

    fun navigateToMatching() {
        navigateToKundali(subTab = 1)
    }

    fun navigateToNumerology() {
        navigateToKundali(subTab = 2)
    }

    // Language Toggle & Selection
    fun toggleLanguage() {
        LanguageManager.toggleLanguage()
        onLanguageChanged()
    }

    fun setLanguage(lang: com.example.util.AppLanguage) {
        LanguageManager.setLanguage(lang)
        com.example.util.AstroAnalytics.setUserProperties(
            language = lang.name,
            isPro = _isProUser.value,
            theme = "system"
        )
        onLanguageChanged()
    }

    /**
     * Some strings are baked in when the Panchang is computed and then cached —
     * the Tithi and Nakshatra end times, for instance, which read "09:57 AM तक"
     * in Hindi and "until 09:57 AM" in English. Recomposition alone cannot fix
     * those: the cached row still holds the text from whichever language was in
     * force when it was written. So a language change forces a recompute.
     */
    private fun onLanguageChanged() {
        recalculatePanchang(forceRefresh = true)
        loadHoroscopesWithCache(forceRefresh = true)

        // Same reason, three more places the old language was left sitting.
        //
        // The astro news is generated text held in a StateFlow. Switching to
        // English left the Hindi bulletins on the More tab verbatim — the one
        // Devanagari string a sweep of the whole app in English still found.
        // When the bulletins are the bundled offline copy this is free, because
        // that copy is bilingual and only needs re-reading; when they came from
        // the model it costs one call, and only on a language change.
        if (_astroNews.value.isNotBlank()) {
            if (_isNewsOffline.value) {
                _astroNews.value = com.example.data.ai.GeminiAstroService.getOfflineAstroNews()
            } else {
                fetchAstroNews()
            }
        }

        // An answer and a set of insights are not re-asked — the user's question
        // has already been answered and asking again would spend a model call to
        // say the same thing in the other language. Dropping them is honest;
        // leaving them is a Hindi paragraph under an English heading.
        _aiResponse.value = ""
        _aiRashifalInsights.value = emptyMap()
    }

    // Selected City Location for Panchang
    private val _selectedCity = MutableStateFlow(loadCityFromPrefs())
    val selectedCity: StateFlow<CityLocation> = _selectedCity.asStateFlow()

    fun hasUserSetCity(): Boolean = sharedPrefs.contains("city_name")

    private fun loadCityFromPrefs(): CityLocation {
        val lat = sharedPrefs.getFloat("city_lat", 26.9124f).toDouble()
        val lon = sharedPrefs.getFloat("city_lon", 75.7873f).toDouble()
        val name = sharedPrefs.getString("city_name", "Jaipur") ?: "Jaipur"
        val nameHi = sharedPrefs.getString("city_name_hi", "जयपुर") ?: "जयपुर"
        val state = sharedPrefs.getString("city_state", "Rajasthan") ?: "Rajasthan"
        return CityLocation(name, nameHi, state, lat, lon)
    }

    fun setCity(city: CityLocation) {
        _selectedCity.value = city
        sharedPrefs.edit()
            .putFloat("city_lat", city.latitude.toFloat())
            .putFloat("city_lon", city.longitude.toFloat())
            .putString("city_name", city.cityName)
            .putString("city_name_hi", city.cityNameHindi)
            .putString("city_state", city.state)
            .apply()
        recalculatePanchang()
        refreshMuhurats()
        com.example.widget.PanchangWidgetProvider.triggerUpdate(getApplication())
        com.example.widget.TithiNakshatraWidgetProvider.triggerUpdate(getApplication())
    }

    fun detectGPSLocation(context: Context, onComplete: (Boolean) -> Unit = {}) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onComplete(false)
            return
        }

        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    handleLocation(context, location.latitude, location.longitude, onComplete)
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            handleLocation(context, lastLoc.latitude, lastLoc.longitude, onComplete)
                        } else {
                            onComplete(false)
                        }
                    }.addOnFailureListener {
                        onComplete(false)
                    }
                }
            }.addOnFailureListener {
                onComplete(false)
            }
        } catch (e: SecurityException) {
            onComplete(false)
        } catch (e: Throwable) {
            onComplete(false)
        }
    }

    private fun handleLocation(context: Context, lat: Double, lon: Double, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var cityName = "Current Location"
            var stateName = "GPS"
            try {
                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    cityName = address.locality ?: address.subAdminArea ?: address.adminArea ?: "Current Location"
                    stateName = address.adminArea ?: "GPS"
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Ignore geocoding failure, fallback to default
            }

            val gpsCity = CityLocation(
                cityName = cityName,
                cityNameHindi = cityName, // Assuming Hindi translation isn't available from Geocoder easily, keeping same
                state = stateName,
                latitude = lat,
                longitude = lon
            )
            launch(Dispatchers.Main) {
                setCity(gpsCity)
                onComplete(true)
            }
        }
    }

    // Notification Toggles
    val dailyRahuKaalAlert = MutableStateFlow(sharedPrefs.getBoolean("daily_notification_enabled", true))
    val festivalRemindersAlert = MutableStateFlow(sharedPrefs.getBoolean("festival_notification_enabled", true))
    val muhuratAlertsEnabled = MutableStateFlow(sharedPrefs.getBoolean("muhurat_notification_enabled", true))
    
    // Time Format Preference (12-hour vs 24-hour)
    private val _use24HourFormat = MutableStateFlow(sharedPrefs.getBoolean("use_24_hour_format", false))
    val use24HourFormat: StateFlow<Boolean> = _use24HourFormat.asStateFlow()

    fun toggle24HourFormat() {
        val newValue = !_use24HourFormat.value
        _use24HourFormat.value = newValue
        sharedPrefs.edit().putBoolean("use_24_hour_format", newValue).apply()
        recalculatePanchang(forceRefresh = true)
        refreshMuhurats()
        com.example.widget.PanchangWidgetProvider.triggerUpdate(getApplication())
        com.example.widget.TithiNakshatraWidgetProvider.triggerUpdate(getApplication())
    }

    fun set24HourFormat(enable24Hour: Boolean) {
        if (_use24HourFormat.value != enable24Hour) {
            _use24HourFormat.value = enable24Hour
            sharedPrefs.edit().putBoolean("use_24_hour_format", enable24Hour).apply()
            recalculatePanchang(forceRefresh = true)
            refreshMuhurats()
            com.example.widget.PanchangWidgetProvider.triggerUpdate(getApplication())
            com.example.widget.TithiNakshatraWidgetProvider.triggerUpdate(getApplication())
        }
    }
    
    private val _notificationHour = MutableStateFlow(sharedPrefs.getInt("notification_hour", com.example.util.NotificationDefaults.DAILY_HOUR))
    val notificationHour: StateFlow<Int> = _notificationHour.asStateFlow()
    
    private val _notificationMinute = MutableStateFlow(sharedPrefs.getInt("notification_minute", 0))
    val notificationMinute: StateFlow<Int> = _notificationMinute.asStateFlow()

    fun toggleRahuKaalAlert() {
        dailyRahuKaalAlert.value = !dailyRahuKaalAlert.value
        sharedPrefs.edit().putBoolean("daily_notification_enabled", dailyRahuKaalAlert.value).apply()
        val context = getApplication<Application>()
        com.example.worker.AstroNotificationWorker.scheduleDailyNotification(context)
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        _notificationHour.value = hour
        _notificationMinute.value = minute
        sharedPrefs.edit()
            .putInt("notification_hour", hour)
            .putInt("notification_minute", minute)
            .apply()
        val context = getApplication<Application>()
        com.example.worker.AstroNotificationWorker.scheduleDailyNotification(context)
    }

    fun toggleFestivalAlert() {
        festivalRemindersAlert.value = !festivalRemindersAlert.value
        sharedPrefs.edit().putBoolean("festival_notification_enabled", festivalRemindersAlert.value).apply()
        val context = getApplication<Application>()
        com.example.worker.FestivalNotificationWorker.scheduleFestivalNotification(context)
    }

    fun toggleMuhuratAlert() {
        muhuratAlertsEnabled.value = !muhuratAlertsEnabled.value
        sharedPrefs.edit().putBoolean("muhurat_notification_enabled", muhuratAlertsEnabled.value).apply()
        val context = getApplication<Application>()
        com.example.worker.MuhuratNotificationWorker.scheduleMuhuratNotification(context)
    }

    // Selected Date
    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()

    fun setDate(date: Date) {
        _selectedDate.value = date
        recalculatePanchang()
    }

    // Panchang Data State
    private val _panchangState = MutableStateFlow(PanchangCalculator.calculatePanchang(Date(), PanchangCalculator.popularCities[0]))
    val panchangState: StateFlow<PanchangData> = _panchangState.asStateFlow()

    fun recalculatePanchang(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isPanchangLoading.value = true
            try {
                _panchangState.value = cacheRepository.getPanchangWith7DayCache(_selectedDate.value, _selectedCity.value, use24Hour = _use24HourFormat.value, forceRefresh = forceRefresh)
                incrementLookupCount()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                reportError(e)
            } finally {
                _isPanchangLoading.value = false
            }
        }
    }

    private fun recalculatePanchang() {
        recalculatePanchang(forceRefresh = false)
    }

    fun refreshPanchang() {
        recalculatePanchang(forceRefresh = true)
    }

    // Festivals
    val festivals: List<FestivalData> = FestivalProvider.getFestivals()

    // Rashifal
    private val _dailyHoroscopes = MutableStateFlow(RashifalProvider.getDailyHoroscope())
    val dailyHoroscopesState: StateFlow<List<RashifalData>> = _dailyHoroscopes.asStateFlow()
    val dailyHoroscopes: List<RashifalData> get() = _dailyHoroscopes.value

    fun loadHoroscopesWithCache(period: String = "TODAY", forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isHoroscopeLoading.value = true
            try {
                _dailyHoroscopes.value = cacheRepository.getHoroscopesWith7DayCache(period = period, forceRefresh = forceRefresh)
                incrementSessionActionCount()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                reportError(e)
            } finally {
                _isHoroscopeLoading.value = false
            }
        }
    }

    fun refreshHoroscopes(period: String = "TODAY") {
        loadHoroscopesWithCache(period = period, forceRefresh = true)
    }

    /**
     * The main rashi chosen in onboarding or Settings, kept across launches.
     *
     * It used to live only in [_selectedRashiId], in memory: Settings showed the
     * choice until the app was closed, then it was Mesh again, and the morning
     * notification (which reads `user_rashi_id`) always carried Mesh because
     * nothing ever wrote that key. It is kept apart from [selectedRashiId] on
     * purpose — looking at another sign on the Rashifal tab must not change it.
     */
    private val _defaultRashiId = MutableStateFlow(
        sharedPrefs.getInt("user_rashi_id", 1).takeIf { it in 1..12 } ?: 1
    )
    val defaultRashiId: StateFlow<Int> = _defaultRashiId.asStateFlow()

    fun setDefaultRashi(id: Int) {
        if (id !in 1..12) return
        _defaultRashiId.value = id
        sharedPrefs.edit().putInt("user_rashi_id", id).apply()
        selectRashi(id)
    }

    /** The sign on screen. Starts at the main rashi; the Rashifal tab moves it. */
    private val _selectedRashiId = MutableStateFlow(_defaultRashiId.value)
    val selectedRashiId: StateFlow<Int> = _selectedRashiId.asStateFlow()

    fun selectRashi(id: Int) {
        _selectedRashiId.value = id
        _dailyHoroscopes.value.firstOrNull { it.rashiId == id }?.let {
            com.example.util.AstroAnalytics.logHoroscopeView(it.rashiId, it.rashiNameEn, it.period)
        }
    }

    // Choghadiya
    private val _choghadiyaDaytime = MutableStateFlow(true)
    val choghadiyaDaytime: StateFlow<Boolean> = _choghadiyaDaytime.asStateFlow()

    /**
     * The eight Choghadiya slots, as observable state.
     *
     * This was a plain `get()`. It read four MutableStateFlows through `.value`,
     * which is not a snapshot read, so Compose never learned that the list
     * depended on them. The screen collected `choghadiyaDaytime` for the
     * toggle's highlight and read this getter once: tapping "रात का चौघड़िया"
     * lit the night pill and left all eight tiles showing the daytime sequence
     * and daytime hours. The night Choghadiya could not be reached at all.
     *
     * The same silence applied to the other three inputs — changing the city,
     * the date, or the 12/24-hour setting left this list stale until something
     * unrelated happened to recompose the screen.
     *
     * Combining them makes every input an actual dependency, so the list cannot
     * disagree with the controls that produce it.
     */
    val choghadiyaSlots: StateFlow<List<ChoghadiyaSlot>> = kotlinx.coroutines.flow.combine(
        _selectedDate, _choghadiyaDaytime, _selectedCity, _use24HourFormat
    ) { date, isDay, city, use24 ->
        ChoghadiyaCalculator.getChoghadiyaSlots(date, isDay, city.latitude, city.longitude, use24)
    }.stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        ChoghadiyaCalculator.getChoghadiyaSlots(
            _selectedDate.value,
            _choghadiyaDaytime.value,
            _selectedCity.value.latitude,
            _selectedCity.value.longitude,
            _use24HourFormat.value
        )
    )

    fun toggleChoghadiyaDayNight(isDay: Boolean) {
        _choghadiyaDaytime.value = isDay
    }

    // Muhurats
    //
    // This was a plain `val` initialised in the constructor, which made it two
    // problems in one line: sixty full Panchang computations ran synchronously
    // while the ViewModel was being built, and the list was then fixed for the
    // life of the screen — computed for Jaipur, in 12-hour time, whatever the
    // user had chosen. It follows the city and the clock format now, off the
    // main thread.
    private val _upcomingMuhurats = MutableStateFlow<List<MuhuratItem>>(emptyList())
    val upcomingMuhurats: StateFlow<List<MuhuratItem>> = _upcomingMuhurats.asStateFlow()

    fun refreshMuhurats() {
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    MuhuratCalculator.getUpcomingMuhurats(_selectedCity.value, _use24HourFormat.value)
                }
                _upcomingMuhurats.value = list
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                reportError(e)
            }
        }
    }

    // Kundali Generator Input State - Starts empty (no hardcoded dummy data)
    var kundaliName = MutableStateFlow("")
    var kundaliDob = MutableStateFlow("")
    var kundaliTob = MutableStateFlow("")
    var kundaliPlace = MutableStateFlow("")

    private val _generatedKundali = MutableStateFlow<KundaliChartData?>(null)
    val generatedKundali: StateFlow<KundaliChartData?> = _generatedKundali.asStateFlow()
    // The birth place's coordinates for the chart above, so saving it keeps them.
    private var generatedChartCoords: Pair<Double, Double>? = null

    fun resetKundaliForm() {
        kundaliName.value = ""
        kundaliDob.value = ""
        kundaliTob.value = ""
        kundaliPlace.value = ""
        _generatedKundali.value = null
    }

    private val _isCalculating = MutableStateFlow(false)
    val isCalculating: StateFlow<Boolean> = _isCalculating.asStateFlow()

    /** Validation message for the Kundali form, in the active language. Null when clear. */
    private val _kundaliInputError = MutableStateFlow<String?>(null)
    val kundaliInputError: StateFlow<String?> = _kundaliInputError.asStateFlow()

    fun clearKundaliInputError() { _kundaliInputError.value = null }

    /**
     * Generates a birth chart.
     *
     * [lat] and [lng] are the coordinates of the BIRTH place, resolved by the
     * form's geocoder. They used to be dropped on the floor, which meant every
     * chart in the app was cast for Jaipur regardless of where the person was
     * actually born — and the Ascendant is the most place-sensitive point in the
     * whole chart.
     */
    fun generateKundaliChart(
        name: String,
        dob: String,
        tob: String,
        place: String,
        lat: Double,
        lng: Double
    ) {
        val trimmedName = name.trim()
        val trimmedDob = dob.trim()
        val trimmedTob = tob.trim()
        val trimmedPlace = place.trim()

        kundaliName.value = trimmedName
        kundaliDob.value = trimmedDob
        kundaliTob.value = trimmedTob
        kundaliPlace.value = trimmedPlace
        _kundaliInputError.value = null

        viewModelScope.launch(Dispatchers.Default) {
            _isCalculating.value = true
            try {
                val birth = com.example.astro.BirthData.parse(
                    name = trimmedName,
                    dobString = trimmedDob,
                    tobString = trimmedTob,
                    placeName = trimmedPlace,
                    latitude = lat,
                    longitude = lng
                )
                val result = KundaliCalculator.generateKundali(birth)
                generatedChartCoords = lat to lng
                _generatedKundali.value = result
                com.example.util.AstroAnalytics.logKundaliGenerated(isNorthIndian = true, source = "form")
                addRecentSearch("KUNDALI", trimmedName, trimmedDob, trimmedTob, trimmedPlace, lat, lng)
                incrementLookupCount()
            } catch (e: com.example.astro.BirthDataException) {
                // A typo in the form is the user's to fix, not a crash to report.
                _kundaliInputError.value = com.example.util.LanguageManager.getString(
                    e.messageHi, e.messageEn
                )
                _generatedKundali.value = null
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                reportError(e)
            } finally {
                _isCalculating.value = false
            }
        }
    }

    // Kundali Matching / Guna Milan State - Starts clean and empty
    var matchBoyName = MutableStateFlow("")
    var matchBoyDob = MutableStateFlow("")
    var matchBoyTob = MutableStateFlow("12:00")

    var matchGirlName = MutableStateFlow("")
    var matchGirlDob = MutableStateFlow("")
    var matchGirlTob = MutableStateFlow("12:00")

    private val _matchingInputError = MutableStateFlow<String?>(null)
    val matchingInputError: StateFlow<String?> = _matchingInputError.asStateFlow()

    fun clearMatchingInputError() { _matchingInputError.value = null }

    private val _gunaResult = MutableStateFlow<GunaMatchingResult?>(null)
    val gunaResult: StateFlow<GunaMatchingResult?> = _gunaResult.asStateFlow()

    fun calculateGunaMatching() {
        val bName = matchBoyName.value.trim()
        val bDob = matchBoyDob.value.trim()
        val bTob = matchBoyTob.value.trim().ifBlank { "12:00" }
        val gName = matchGirlName.value.trim()
        val gDob = matchGirlDob.value.trim()
        val gTob = matchGirlTob.value.trim().ifBlank { "12:00" }

        if (bName.isNotBlank() && bDob.isNotBlank() && gName.isNotBlank() && gDob.isNotBlank()) {
            addRecentMatch(bName, bDob, bTob, gName, gDob, gTob)
        }

        _matchingInputError.value = null

        viewModelScope.launch(Dispatchers.Default) {
            _isCalculating.value = true
            try {
                val result = KundaliMatchingCalculator.matchKundali(
                    bName, bDob, bTob,
                    gName, gDob, gTob
                )
                _gunaResult.value = result
                com.example.util.AstroAnalytics.logKundaliMatching(
                    gunaScore = result.totalObtainedGuna,
                    isManglikMismatch = result.isManglikBoy != result.isManglikGirl
                )
                incrementSessionActionCount()
            } catch (e: com.example.astro.BirthDataException) {
                // A blank name is the user's to fix in the form; it used to take
                // down the whole screen with "Something went wrong".
                _matchingInputError.value = com.example.util.LanguageManager.getString(
                    e.messageHi, e.messageEn
                )
                _gunaResult.value = null
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                reportError(e)
            } finally {
                _isCalculating.value = false
            }
        }
    }

    // Numerology
    var numName = MutableStateFlow("")
    var numDob = MutableStateFlow("")

    /**
     * Seeded with a real calculation for "Sunil Saini", born 1995-07-22 — the
     * developer's own name, shipped to production. Every user who opened
     * Numerology was shown someone else's Moolank, Bhagyank and reading,
     * pre-filled and already calculated, until they typed over it. It is one of
     * the places the app appeared to "show everyone the same data", and it is
     * also a stranger's details on a stranger's screen.
     *
     * The fields start empty and the results stay hidden until the user asks
     * for them.
     */
    private val _numerologyData = MutableStateFlow<NumerologyData?>(null)
    val numerologyData: StateFlow<NumerologyData?> = _numerologyData.asStateFlow()

    /**
     * Drops a result that no longer belongs to what is in the form.
     *
     * Typing "25-08-1994" — the order most Indian users reach for first —
     * fails validation, and the error appeared *above a full reading computed
     * from the previous date*. Nothing said the numbers below were stale, so
     * the screen showed one date and answered a different one.
     */
    fun clearNumerology() {
        _numerologyData.value = null
    }

    fun calculateNumerology() {
        try {
            val result = NumerologyCalculator.calculateNumerology(numName.value, numDob.value)
            _numerologyData.value = result
            com.example.util.AstroAnalytics.logNumerologyCalculated(result.moolank, result.bhagyank)
            incrementSessionActionCount()
        } catch (e: Exception) {
            reportError(e)
        }
    }

    // AI Astrologer Chat
    private val _aiResponse = MutableStateFlow("")
    val aiResponse: StateFlow<String> = _aiResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _isAiOffline = MutableStateFlow(false)
    val isAiOffline: StateFlow<Boolean> = _isAiOffline.asStateFlow()

    // Personalized Rashifal Insight
    /**
     * Every model call in the app goes through this. See AiRateLimiter for why
     * there needs to be one at all — there is no server between this app and
     * the bill.
     */
    private val aiRateLimiter = com.example.util.AiRateLimiter(
        dailyStore = object : com.example.util.AiRateLimiter.DailyStore {
            override fun count(dayKey: String) =
                if (sharedPrefs.getString("ai_calls_day", null) == dayKey) sharedPrefs.getInt("ai_calls_count", 0) else 0
            override fun setCount(dayKey: String, count: Int) {
                sharedPrefs.edit().putString("ai_calls_day", dayKey).putInt("ai_calls_count", count).apply()
            }
        }
    )

    /**
     * Turns a refusal into something worth reading. Returns null when the call
     * may proceed.
     */
    private fun aiRefusalMessage(): String? =
        when (val d = aiRateLimiter.tryAcquire()) {
            is com.example.util.AiRateLimiter.Decision.Allow -> null
            is com.example.util.AiRateLimiter.Decision.TooSoon ->
                LanguageManager.getString(
                    "थोड़ा रुकें — कुछ ही क्षणों में दोबारा पूछ सकते हैं।",
                    "One moment — you can ask again in a few seconds."
                )
            is com.example.util.AiRateLimiter.Decision.HourlyCapReached ->
                LanguageManager.getString(
                    "इस घंटे के प्रश्न पूरे हो गए। लगभग ${aiRateLimiter.minutesFrom(d.retryAfterMs)} मिनट बाद पुनः प्रयास करें।",
                    "You have used this hour's questions. Please try again in about ${aiRateLimiter.minutesFrom(d.retryAfterMs)} minutes."
                )
            is com.example.util.AiRateLimiter.Decision.DailyCapReached ->
                LanguageManager.getString(
                    "आज के प्रश्नों की सीमा पूरी हो गई। कल फिर पूछें।",
                    "You have reached today's limit. Please ask again tomorrow."
                )
        }

    /**
     * AI insights, each one kept under the rashi it was written for.
     *
     * This was a single String shared by all twelve signs, and the effect was
     * that fetching an insight for Mesh then switching to Vrishabha left Mesh's
     * text on screen under Vrishabha's heading — with no way for the card to
     * know it was showing the wrong sign's reading. Every sign appeared to have
     * the same "personalised" insight. Keying by sign also means switching back
     * and forth does not re-ask the model for something already answered.
     */
    private val _aiRashifalInsights = MutableStateFlow<Map<String, String>>(emptyMap())
    val aiRashifalInsights: StateFlow<Map<String, String>> = _aiRashifalInsights.asStateFlow()

    /** The sign whose insight is in flight, or null. Per-sign for the same reason. */
    private val _rashifalAiLoadingFor = MutableStateFlow<String?>(null)
    val rashifalAiLoadingFor: StateFlow<String?> = _rashifalAiLoadingFor.asStateFlow()

    /** The sign whose last fetch failed, so only that card offers a retry. */
    private val _rashifalAiErrorFor = MutableStateFlow<String?>(null)
    val rashifalAiErrorFor: StateFlow<String?> = _rashifalAiErrorFor.asStateFlow()

    /**
     * Why the last insight was refused by the rate limiter, when that was the
     * reason. Null for a network failure. Without it a subscriber who had used
     * the day's allowance was told to check their internet connection.
     */
    private val _rashifalAiRefusal = MutableStateFlow<String?>(null)
    val rashifalAiRefusal: StateFlow<String?> = _rashifalAiRefusal.asStateFlow()

    /**
     * The key an insight is kept under: sign, period and the day it was asked.
     *
     * It was the sign alone, and the question always said "daily" — so the
     * weekly and monthly tabs showed the day's insight under a heading for the
     * week or the month. The date is in the key so yesterday's reading is not
     * served today.
     */
    fun insightKey(rashiName: String, period: String): String {
        val c = java.util.Calendar.getInstance()
        return "$rashiName|$period|${c.get(java.util.Calendar.YEAR)}-${c.get(java.util.Calendar.DAY_OF_YEAR)}"
    }

    /**
     * PRO only, since 13 Sep 2026. A free user pressing the button is shown the
     * PRO dialog instead; nothing is sent to the model.
     */
    fun fetchPersonalizedInsight(rashiName: String, period: String, readingEn: String) {
        if (!_isProUser.value) {
            showPremiumDialog.value = true
            return
        }
        val key = insightKey(rashiName, period)
        if (_aiRashifalInsights.value.containsKey(key)) return
        if (_rashifalAiLoadingFor.value != null) return
        aiRefusalMessage()?.let { refusal ->
            _rashifalAiRefusal.value = refusal
            _rashifalAiErrorFor.value = key
            return
        }

        viewModelScope.launch {
            _rashifalAiLoadingFor.value = key
            _rashifalAiErrorFor.value = null
            _rashifalAiRefusal.value = null
            try {
                val span = when (period) {
                    "WEEK" -> "this week"
                    "MONTH" -> "this month"
                    else -> "today"
                }
                val today = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale.ENGLISH).format(java.util.Date())
                val question = "Give a personalised horoscope insight for $rashiName for $span " +
                    "(today is $today). Build on the reading below rather than repeating it: " +
                    "one practical focus, one thing to be careful of, and a simple remedy."
                val details = "Moon sign (rashi): $rashiName. Reading for $span: ${readingEn.take(600)}"
                val answer = GeminiAstroService.getAiAstrologyInsight(
                    question, details,
                    answerLanguage = if (LanguageManager.isHindi) "hi" else "en"
                )

                // getAiAstrologyInsight falls back to getOfflineVedicResponse when
                // the model cannot be reached, and that fallback matches on
                // keywords — "career", "marriage", "health". This question carries
                // none of them, so every sign lands in its else branch and gets
                // one identical paragraph. Showing that under a heading reading
                // "AI Personalized Insight" tells the user something untrue, so
                // it is treated as a failure rather than as an insight.
                if (answer == GeminiAstroService.getOfflineVedicResponse(question)) {
                    _rashifalAiErrorFor.value = key
                } else {
                    _aiRashifalInsights.value = _aiRashifalInsights.value + (key to answer)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _rashifalAiErrorFor.value = key
            } finally {
                _rashifalAiLoadingFor.value = null
            }
        }
    }

    /**
     * The question box on the Numerology tab. PRO only, since 13 Sep 2026.
     *
     * It used to send the person's Kundali if one had been generated and
     * otherwise the words "General Vedic Chart" — never the numerology the user
     * was looking at, on the numerology screen. So a question under "Moolank 6"
     * was answered as if nothing about the person were known. It now sends the
     * calculated numbers, the chart when there is one, and today's date, which
     * "this year" questions need.
     */
    fun askAiAstrologer(rawQuestion: String) {
        if (!_isProUser.value) {
            showPremiumDialog.value = true
            return
        }
        if (_isAiLoading.value) return

        // Bounded here rather than only at the text field, so the sample-question
        // chips and anything added later are covered too. See MAX_QUESTION_CHARS.
        val question = rawQuestion.trim().take(com.example.util.AiRateLimiter.MAX_QUESTION_CHARS)
        if (question.isBlank()) return
        aiRefusalMessage()?.let { refusal ->
            // Said plainly in the answer area, where the user is already looking.
            _aiResponse.value = refusal
            _isAiOffline.value = false
            return
        }

        com.example.util.AstroAnalytics.logAiAstrologerQuery(question.length)

        viewModelScope.launch {
            _isAiLoading.value = true
            _isAiOffline.value = false
            try {
                val today = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale.ENGLISH).format(java.util.Date())
                val parts = mutableListOf("Today is $today.")
                _numerologyData.value?.let { n ->
                    parts += "Name: ${n.personName}. Date of birth: ${n.dateOfBirth}. " +
                        "Moolank (psychic number): ${n.moolank}, ruled by ${n.rulingPlanetEn.ifBlank { n.rulingPlanetHi }}. " +
                        "Bhagyank (destiny number): ${n.bhagyank}. Name number: ${n.nameNumber}. " +
                        "Friendly numbers: ${n.friendlyNumbers.joinToString()}. Unfriendly numbers: ${n.enemyNumbers.joinToString()}."
                }
                // Name, date of birth and lagna, and nothing more from the chart:
                // that is what docs/PRIVACY_POLICY.md says the AI receives, and it
                // says in so many words that the time and place of birth are not
                // sent. The first draft of this change sent both.
                _generatedKundali.value?.let { c ->
                    parts += "Birth chart: ${c.personName}, born ${c.dateOfBirth}; Lagna ${c.ascendantRashiEn}."
                }
                if (parts.size == 1) {
                    parts += "No numerology or birth chart has been calculated yet; answer in general terms and say that the numbers would make it specific."
                }
                val res = GeminiAstroService.getAiAstrologyInsight(
                    question, parts.joinToString(" "),
                    answerLanguage = if (LanguageManager.isHindi) "hi" else "en"
                )
                _aiResponse.value = res
                if (res == com.example.data.ai.GeminiAstroService.getOfflineVedicResponse(question)) {
                    _isAiOffline.value = true
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _isAiOffline.value = true
                _aiResponse.value = com.example.data.ai.GeminiAstroService.getOfflineVedicResponse(question)
                reportError(e)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    // Astro & astronomical news
    private val _astroNews = MutableStateFlow("")
    val astroNews: StateFlow<String> = _astroNews.asStateFlow()

    private val _isNewsOffline = MutableStateFlow(false)
    val isNewsOffline: StateFlow<Boolean> = _isNewsOffline.asStateFlow()

    fun fetchAstroNews() {
        viewModelScope.launch {
            _isNewsLoading.value = true
            _isNewsOffline.value = false
            try {
                val news = GeminiAstroService.fetchAstroNewsWithSearchGrounding()
                _astroNews.value = news
                if (news == com.example.data.ai.GeminiAstroService.getOfflineAstroNews()) {
                    _isNewsOffline.value = true
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _isNewsOffline.value = true
                _astroNews.value = com.example.data.ai.GeminiAstroService.getOfflineAstroNews()
                reportError(e)
            } finally {
                _isNewsLoading.value = false
            }
        }
    }

    // Room DB Recent Searches
    private val _recentSearches = MutableStateFlow<List<RecentSearchEntity>>(emptyList())
    val recentSearches: StateFlow<List<RecentSearchEntity>> = _recentSearches.asStateFlow()

    // Room DB Profiles
    private val _savedProfiles = MutableStateFlow<List<KundaliEntity>>(emptyList())
    val savedProfiles: StateFlow<List<KundaliEntity>> = _savedProfiles.asStateFlow()

    private fun loadRecentSearches() {
        viewModelScope.launch {
            recentSearchRepository.recentSearches.collect { list ->
                _recentSearches.value = list
            }
        }
    }

    /**
     * Records a search for the "recent" strip.
     *
     * Kundali searches carry the birth coordinates as two extra fields. Without
     * them, replaying a recent search could not reproduce the chart — it would
     * have to guess a location, which is the bug this whole change removes.
     * Older rows have four fields and are still readable; the screen skips
     * replaying those rather than casting them for the wrong place.
     */
    fun addRecentSearch(
        type: String,
        name: String,
        dob: String,
        tob: String,
        place: String,
        lat: Double? = null,
        lng: Double? = null
    ) {
        val data = buildString {
            append("$name|$dob|$tob|$place")
            if (lat != null && lng != null) append("|$lat|$lng")
        }
        viewModelScope.launch {
            recentSearchRepository.insertSearch(RecentSearchEntity(type = type, data = data))
        }
    }

    /**
     * A Guna Milan search, in its own shape.
     *
     * This used to go through [addRecentSearch], whose four fields are named
     * for a kundali — so a match was stored with the girl's name in `tob` and
     * her date in `place`. It read back correctly by accident, and then the
     * birth times were added to the matching form and there was nowhere to put
     * them: restoring a recent match would have quietly dropped both times back
     * to noon and produced a different answer than the one that was saved.
     *
     * Six fields, in the order the form asks for them. Rows written before this
     * have four; [MatchingScreen] accepts both.
     */
    fun addRecentMatch(
        boyName: String,
        boyDob: String,
        boyTob: String,
        girlName: String,
        girlDob: String,
        girlTob: String
    ) {
        val data = "$boyName|$boyDob|$girlName|$girlDob|$boyTob|$girlTob"
        viewModelScope.launch {
            recentSearchRepository.insertSearch(RecentSearchEntity(type = "MATCHING", data = data))
        }
    }

    private val _isFirestoreSyncing = MutableStateFlow(false)
    val isFirestoreSyncing: StateFlow<Boolean> = _isFirestoreSyncing.asStateFlow()

    private var backupJob: kotlinx.coroutines.Job? = null

    /**
     * What was last written to Firestore, so an emission that changed nothing
     * does not pay for a round of writes.
     */
    private var lastBackedUpSnapshot: List<KundaliEntity> = emptyList()

    private fun triggerBackgroundBackup(profiles: List<KundaliEntity>) {
        // This is driven by a Room Flow, which emits on every write — so editing
        // one note re-uploaded every profile the user has. Skip an emission that
        // is identical to what the cloud already holds, and let the debounce
        // below collapse a burst of edits into one upload.
        if (profiles == lastBackedUpSnapshot) return

        backupJob?.cancel()
        backupJob = viewModelScope.launch {
            kotlinx.coroutines.delay(BACKUP_DEBOUNCE_MS)
            _isFirestoreSyncing.value = true
            try {
                authService.backupProfilesToCloud(profiles)
                    .onSuccess { lastBackedUpSnapshot = profiles }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Log.e alone means the developer never learns this happened to
                // a real user — the test device records nothing below E, and
                // nobody is reading a stranger's logcat. Every path here can
                // lose or fail to save someone's saved profiles, so each one
                // also goes to Crashlytics as a non-fatal.
                android.util.Log.e("MainViewModel", "Background backup failed", e)
                com.example.util.AstroAnalytics.recordNonFatal(e, "backgroundBackup")
                Firebase.crashlytics.recordException(e)
            } finally {
                _isFirestoreSyncing.value = false
            }
        }
    }

    private fun loadSavedProfiles() {
        viewModelScope.launch {
            repository.allProfiles.collect { list ->
                _savedProfiles.value = list
                if (_currentUser.value != null) {
                    triggerBackgroundBackup(list)
                }
            }
        }
    }

    /** Saves the chart on screen as a profile, at the place it was cast for. */
    fun saveGeneratedChart() {
        val chart = _generatedKundali.value ?: return
        val (lat, lng) = generatedChartCoords ?: return
        saveNewProfile(chart.personName, chart.dateOfBirth, chart.timeOfBirth, chart.placeOfBirth, lat, lng)
    }

    /**
     * [latitude] and [longitude] are the BIRTH place's, picked from the geocoder.
     * This used to store the Settings city's instead, so every added or saved
     * profile was cast for wherever the phone's Panchang was set.
     */
    fun saveNewProfile(name: String, dob: String, tob: String, place: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val entity = KundaliEntity(
                name = SecurityUtils.sanitizeTextInput(name),
                gender = "MALE",
                dateOfBirth = dob,
                timeOfBirth = tob,
                placeOfBirth = SecurityUtils.sanitizeTextInput(place),
                latitude = latitude,
                longitude = longitude,
                notes = "Saved Profile"
            )
            repository.saveProfile(entity)
        }
    }

    fun updateProfile(entity: KundaliEntity) {
        viewModelScope.launch {
            repository.updateProfile(entity)
        }
    }

    fun deleteProfile(entity: KundaliEntity) {
        viewModelScope.launch {
            repository.deleteProfile(entity)
        }
    }

    // Firebase Auth & Cloud Backup
    private val authService = com.example.service.FirebaseAuthService()
    private val _currentUser = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(authService.currentUser)
    val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = _currentUser.asStateFlow()

    private val _backupStatusMessage = MutableStateFlow<String?>(null)
    val backupStatusMessage: StateFlow<String?> = _backupStatusMessage.asStateFlow()

    // Sign-in is no longer a gate in front of the app; it is a screen the user
    // opens from Saved Profiles or Settings when they want cloud backup.
    private val _showAuthScreen = MutableStateFlow(false)
    val showAuthScreen: StateFlow<Boolean> = _showAuthScreen.asStateFlow()

    fun openAuthScreen() {
        clearAuthMessages()
        _showAuthScreen.value = true
    }

    fun closeAuthScreen() { _showAuthScreen.value = false }

    fun signInWithGoogle(context: android.content.Context, webClientId: String = "") {
        viewModelScope.launch {
            _backupStatusMessage.value = LanguageManager.getString("Google से साइन-इन हो रहा है...", "Signing in with Google...")
            val result = authService.signInWithGoogle(context, webClientId)
            result.onSuccess { user ->
                _currentUser.value = user
                _backupStatusMessage.value = LanguageManager.getString("साइन इन सफल: ${user.displayName ?: user.email}", "Signed in: ${user.displayName ?: user.email}")
                Firebase.crashlytics.setUserId(user.uid)
                com.example.util.AstroAnalytics.logLogin(method = "google", isSuccess = true)
                syncCloudAndLocalProfiles()
            }.onFailure { err ->
                _backupStatusMessage.value = LanguageManager.getString("साइन-इन विफल: ${err.message}", "Sign-in failed: ${err.message}")
                com.example.util.AstroAnalytics.logLogin(method = "google", isSuccess = false)
                com.example.util.AstroAnalytics.recordNonFatal(err, "signInWithGoogle")
            }
        }
    }

    // ---- Sign-in ------------------------------------------------------
    // Sign-in is no longer a gate in front of the app — an account buys cloud
    // backup and nothing else, and AuthScreen opens as a dialog from Saved
    // Profiles and from Settings. These carry what that dialog needs: whether a
    // call is in flight, and what to say when it fails.

    private val _isAuthInProgress = MutableStateFlow(false)
    val isAuthInProgress: StateFlow<Boolean> = _isAuthInProgress.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authNotice = MutableStateFlow<String?>(null)
    val authNotice: StateFlow<String?> = _authNotice.asStateFlow()

    fun clearAuthMessages() {
        _authError.value = null
        _authNotice.value = null
    }

    fun signInWithGoogleGate(context: android.content.Context, webClientId: String = "") {
        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authError.value = null
            authService.signInWithGoogle(context, webClientId)
                .onSuccess { onSignedIn(it, method = "google") }
                .onFailure { err ->
                    _authError.value = err.message
                    com.example.util.AstroAnalytics.logLogin(method = "google", isSuccess = false)
                    com.example.util.AstroAnalytics.recordNonFatal(err, "signInWithGoogleGate")
                }
            _isAuthInProgress.value = false
        }
    }

    /**
     * Blank fields are the user's to fix, and Firebase answers them with
     * "Given String is empty or null" — a Java exception message, in English,
     * that tells nobody anything. Catch them before the call.
     */
    private fun credentialsMissing(email: String, password: String): Boolean {
        val message = when {
            email.isBlank() && password.isBlank() -> LanguageManager.getString(
                "ईमेल और पासवर्ड दोनों भरें।", "Enter your email and password."
            )
            email.isBlank() -> LanguageManager.getString("ईमेल भरें।", "Enter your email.")
            password.isBlank() -> LanguageManager.getString("पासवर्ड भरें।", "Enter your password.")
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                LanguageManager.getString(
                    "यह ईमेल पता सही नहीं लगता।", "That does not look like an email address."
                )
            else -> return false
        }
        _authError.value = message
        return true
    }

    fun signUpWithEmail(email: String, password: String, name: String) {
        if (credentialsMissing(email, password)) return
        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authError.value = null
            authService.signUpWithEmail(email, password, name)
                .onSuccess { onSignedIn(it, method = "email_signup") }
                .onFailure { err ->
                    _authError.value = err.message
                    com.example.util.AstroAnalytics.logLogin(method = "email_signup", isSuccess = false)
                }
            _isAuthInProgress.value = false
        }
    }

    fun signInWithEmail(email: String, password: String) {
        if (credentialsMissing(email, password)) return
        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authError.value = null
            authService.signInWithEmail(email, password)
                .onSuccess { onSignedIn(it, method = "email") }
                .onFailure { err ->
                    _authError.value = err.message
                    com.example.util.AstroAnalytics.logLogin(method = "email", isSuccess = false)
                }
            _isAuthInProgress.value = false
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _authError.value = LanguageManager.getString(
                "रीसेट लिंक भेजने के लिए पहले ईमेल भरें।",
                "Enter your email first, and the reset link goes there."
            )
            return
        }
        viewModelScope.launch {
            _isAuthInProgress.value = true
            _authError.value = null
            authService.sendPasswordReset(email)
                .onSuccess {
                    _authNotice.value = LanguageManager.getString(
                        "पासवर्ड रीसेट लिंक $email पर भेज दिया गया है।",
                        "A password reset link is on its way to $email."
                    )
                }
                .onFailure { err -> _authError.value = err.message }
            _isAuthInProgress.value = false
        }
    }

    private fun onSignedIn(user: com.google.firebase.auth.FirebaseUser, method: String) {
        _currentUser.value = user
        _authError.value = null
        Firebase.crashlytics.setUserId(user.uid)
        com.example.util.AstroAnalytics.logLogin(method = method, isSuccess = true)
        syncCloudAndLocalProfiles()
    }

    fun signOutFirebase() {
        authService.signOut()
        _currentUser.value = null
        _backupStatusMessage.value = LanguageManager.getString("साइन-आउट सफल। स्थानीय प्रोफाइल डिवाइस पर सुरक्षित हैं।", "Signed out. Your profiles stay on this device.")
    }

    fun syncCloudAndLocalProfiles() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _isFirestoreSyncing.value = true
            try {
                // 1. Fetch remote cloud profiles
                val restoreResult = authService.restoreProfilesFromCloud()
                val cloudProfiles = restoreResult.getOrDefault(emptyList())
                val localProfiles = repository.getSavedProfilesList()

                // 2. Merge: insert any cloud profiles not present locally.
                //
                // Matching is on uuid and nothing else. It used to be
                //   it.id == cloudProfile.id || (name matches && dateOfBirth matches)
                // and both halves lost data. The id half: Room ids are
                // autoGenerate, so both phones on an account have a profile with
                // id 1 — the cloud copy was read as "already here", skipped, and
                // then overwritten by the upload in step 3. The name+dob half
                // silently dropped one of a pair of twins, who share both.
                val toRestore = ProfileMerge.profilesToRestore(cloudProfiles, localProfiles)
                toRestore.forEach { repository.saveProfile(it) }
                val newlyRestored = toRestore.size

                // 3. Backup merged complete superset to cloud
                val updatedLocalProfiles = repository.getSavedProfilesList()
                val backupResult = authService.backupProfilesToCloud(updatedLocalProfiles)
                backupResult.onSuccess { totalSynced ->
                    if (newlyRestored > 0) {
                        _backupStatusMessage.value = LanguageManager.getString(
                    "क्लाउड सिंक पूर्ण: $newlyRestored नए प्रोफाइल डाउनलोड हुए, कुल $totalSynced क्लाउड पर सुरक्षित।",
                    "Sync complete: $newlyRestored new profiles downloaded, $totalSynced backed up."
                )
                    } else {
                        _backupStatusMessage.value = LanguageManager.getString("क्लाउड सिंक पूर्ण: कुल $totalSynced प्रोफाइल सुरक्षित।", "Sync complete: $totalSynced profiles backed up.")
                    }
                }.onFailure { err ->
                    // The raw exception used to be interpolated straight into this
                    // string — Firebase's developer-facing English pasted onto the
                    // end of a Hindi sentence, internals and all. It is logged now.
                    android.util.Log.e("MainViewModel", "Cloud backup failed", err)
                    com.example.util.AstroAnalytics.recordNonFatal(err, "cloudBackup")
                    _backupStatusMessage.value = LanguageManager.getString(
                        "क्लाउड बैकअप नहीं हो सका। कृपया बाद में पुनः प्रयास करें।",
                        "Cloud backup did not go through. Please try again later."
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Sync failed", e)
                com.example.util.AstroAnalytics.recordNonFatal(e, "profileSync")
                _backupStatusMessage.value = LanguageManager.getString(
                    "सिंक नहीं हो सका। कृपया बाद में पुनः प्रयास करें।",
                    "Sync did not go through. Please try again later."
                )
            } finally {
                _isFirestoreSyncing.value = false
            }
        }
    }

    fun backupProfilesToCloud() {
        val user = _currentUser.value
        if (user == null) {
            _backupStatusMessage.value = LanguageManager.getString("कृपया पहले Google से साइन-इन करें।", "Please sign in with Google first.")
            return
        }
        syncCloudAndLocalProfiles()
    }

    fun restoreProfilesFromCloud() {
        val user = _currentUser.value
        if (user == null) {
            _backupStatusMessage.value = LanguageManager.getString("कृपया पहले Google से साइन-इन करें।", "Please sign in with Google first.")
            return
        }
        syncCloudAndLocalProfiles()
    }

    fun deleteAccountAndData() {
        val user = _currentUser.value
        if (user == null) {
            deleteLocalDataOnly()
            return
        }
        viewModelScope.launch {
            _backupStatusMessage.value = LanguageManager.getString("खाता और डेटा हटाया जा रहा है...", "Deleting account and data...")
            val result = authService.deleteUserDataAndAccount()
            result.onSuccess {
                try {
                    repository.deleteAllProfiles()
                    recentSearchRepository.clearAllSearches()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "Error clearing local DB", e)
                    com.example.util.AstroAnalytics.recordNonFatal(e, "clearLocalDb")
                }
                _currentUser.value = null
                _backupStatusMessage.value = LanguageManager.getString("आपका खाता और सभी डेटा सफलतापूर्वक हटा दिया गया है।", "Your account and all data have been deleted.")
            }.onFailure { err ->
                _backupStatusMessage.value = LanguageManager.getString("हटाने में त्रुटि: ${err.message}", "Delete failed: ${err.message}")
            }
        }
    }

    /**
     * Writes every saved profile to a file and hands it to the share sheet.
     *
     * The local database is kept out of Android's Auto Backup and out of device
     * transfer, deliberately — birth details are the most personal thing this app
     * holds. The cost was that a user who never signed in had no way whatsoever
     * to carry their profiles to a new phone. This is that way, without making an
     * account the price of not losing your data.
     */
    fun exportProfiles(context: Context) {
        viewModelScope.launch {
            try {
                val profiles = repository.getSavedProfilesList()
                if (profiles.isEmpty()) {
                    _backupStatusMessage.value = LanguageManager.getString(
                        "अभी कोई सहेजी हुई प्रोफ़ाइल नहीं है।",
                        "There are no saved profiles yet."
                    )
                    return@launch
                }

                val json = com.example.data.local.ProfileTransfer.encode(profiles)
                val dir = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
                // Anything older than this export is a file the user has already
                // shared or abandoned; leaving them in the cache serves nobody.
                dir.listFiles()?.forEach { it.delete() }

                val stamp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .format(java.util.Date())
                val file = java.io.File(dir, "revati-profiles-$stamp.json")
                file.writeText(json)

                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    putExtra(
                        android.content.Intent.EXTRA_SUBJECT,
                        LanguageManager.getString(
                            "Revati — सहेजी हुई कुण्डली प्रोफ़ाइल",
                            "Revati — saved Kundali profiles"
                        )
                    )
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    android.content.Intent.createChooser(
                        share,
                        LanguageManager.getString("प्रोफ़ाइल भेजें", "Send profiles")
                    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                _backupStatusMessage.value = LanguageManager.getString(
                    "${profiles.size} प्रोफ़ाइल फ़ाइल में तैयार हैं।",
                    "${profiles.size} profiles are ready to send."
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Profile export failed", e)
                com.example.util.AstroAnalytics.recordNonFatal(e, "profileExport")
                _backupStatusMessage.value = LanguageManager.getString(
                    "प्रोफ़ाइल एक्सपोर्ट नहीं हो सकीं।",
                    "Could not export the profiles."
                )
            }
        }
    }

    /** Reads an export file the user picked and adds whatever is not already here. */
    fun importProfiles(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        // A picked file is arbitrary: cap what is read so a huge
                        // one cannot take the app down before parsing even starts.
                        input.bufferedReader().readText().take(MAX_IMPORT_CHARS)
                    }
                } ?: throw IllegalStateException("could not open the picked file")

                val incoming = com.example.data.local.ProfileTransfer.decode(text)
                val existing = repository.getSavedProfilesList()
                val (fresh, duplicates) = com.example.data.local.ProfileTransfer
                    .plan(incoming, existing)

                fresh.forEach { repository.saveProfile(it) }

                _backupStatusMessage.value = when {
                    fresh.isEmpty() && duplicates > 0 -> LanguageManager.getString(
                        "ये सभी प्रोफ़ाइल पहले से मौजूद हैं।",
                        "All of those profiles are already here."
                    )
                    fresh.isEmpty() -> LanguageManager.getString(
                        "इस फ़ाइल में कोई पढ़ी जा सकने वाली प्रोफ़ाइल नहीं मिली।",
                        "No readable profiles were found in that file."
                    )
                    duplicates > 0 -> LanguageManager.getString(
                        "${fresh.size} नई प्रोफ़ाइल जोड़ी गईं, $duplicates पहले से मौजूद थीं।",
                        "Added ${fresh.size} new profiles; $duplicates were already here."
                    )
                    else -> LanguageManager.getString(
                        "${fresh.size} प्रोफ़ाइल जोड़ी गईं।",
                        "Added ${fresh.size} profiles."
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: com.example.data.local.ProfileTransfer.TransferException) {
                _backupStatusMessage.value =
                    LanguageManager.getString(e.messageHi, e.messageEn)
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Profile import failed", e)
                com.example.util.AstroAnalytics.recordNonFatal(e, "profileImport")
                _backupStatusMessage.value = LanguageManager.getString(
                    "फ़ाइल पढ़ी नहीं जा सकी।",
                    "That file could not be read."
                )
            }
        }
    }

    fun deleteLocalDataOnly() {
        viewModelScope.launch {
            try {
                repository.deleteAllProfiles()
                recentSearchRepository.clearAllSearches()
                _backupStatusMessage.value = LanguageManager.getString("सभी स्थानीय डेटा और सहेजे गए प्रोफाइल हटा दिए गए हैं।", "All local data and saved profiles have been deleted.")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Local data deletion failed", e)
                com.example.util.AstroAnalytics.recordNonFatal(e, "localDataDeletion")
                _backupStatusMessage.value = LanguageManager.getString(
                    "स्थानीय डेटा नहीं हट सका। कृपया पुनः प्रयास करें।",
                    "Could not delete the local data. Please try again."
                )
            }
        }
    }

    fun clearBackupStatusMessage() {
        _backupStatusMessage.value = null
    }

    // Premium Dialog state
    var showPremiumDialog = MutableStateFlow(false)

    // Startup Performance
    private val _isStartupComplete = MutableStateFlow(false)
    val isStartupComplete: StateFlow<Boolean> = _isStartupComplete.asStateFlow()

    init {
        repository = DatabaseProvider.getKundaliRepository(application)
        recentSearchRepository = DatabaseProvider.getRecentSearchRepository(application)
        cacheRepository = DatabaseProvider.getAstroCacheRepository(application)
        
        viewModelScope.launch {
            // 1. Critical Initialization (Immediate)
            //
            // try/finally, not optimism. Anything gated on isStartupComplete —
            // and the ad banner used to be — is held forever if this flag never
            // arrives, and it would not arrive if recalculatePanchang() threw:
            // the exception would abandon the whole launch block with the flag
            // still false. Startup being *finished* and startup having *worked*
            // are different facts, and only the first one belongs here.
            try {
                monitorNetwork()
                loadSavedProfiles()
                recalculatePanchang() // Critical for home screen

                // Allow critical UI to render first
                kotlinx.coroutines.delay(800)
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (t: Throwable) {
                reportError(t)
            } finally {
                _isStartupComplete.value = true
            }

            // 2. Deferred Background Tasks (Non-critical)
            launch {
                loadRecentSearches()
                loadHoroscopesWithCache()
                fetchAstroNews()
                
                if (sharedPrefs.getBoolean("is_onboarding_completed", false)) {
                    com.example.worker.AstroNotificationWorker.scheduleDailyNotification(application)
                    com.example.worker.FestivalNotificationWorker.scheduleFestivalNotification(application)
                    com.example.worker.MuhuratNotificationWorker.scheduleMuhuratNotification(application)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
        try {
            billingManager.destroy()
        } catch (_: Exception) {}
    }

    private companion object {
        /** Collapses a burst of profile edits into a single cloud write. */
        const val BACKUP_DEBOUNCE_MS = 2_000L

        /** Ceiling on a picked import file; ~2000 profiles' worth of JSON. */
        const val MAX_IMPORT_CHARS = 4_000_000
    }
}
