package com.example.ui.screens

import com.example.data.model.dateLocal
import com.example.data.model.tithiLocal
import com.example.data.model.nakshatraLocal
import com.example.data.model.yogaLocal
import com.example.data.model.karanaLocal
import com.example.data.model.masaLocal
import com.example.data.model.pakshaLocal
import com.example.data.model.varaLocal
import com.example.ui.components.AstroDisclaimer
import com.example.ui.components.DisclaimerScope
import android.Manifest
import android.content.pm.PackageManager
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.content.ContextCompat
import com.example.astro.PanchangCalculator
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.data.model.ChoghadiyaSlot
import com.example.data.model.CityLocation
import com.example.data.model.PanchangData
import com.example.ui.components.CelestialBackground
import com.example.ui.components.DailyPanchangCard
import com.example.ui.components.GlassBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.NorthIndianChart
import com.example.ui.components.PanchangLoadingSkeleton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SubTabHeader
import com.example.ui.theme.AstroVedaTheme
import com.example.ui.theme.DateTimeAccent
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.PrimaryButtonBackground
import com.example.ui.theme.RahuKaalDangerColor
import com.example.ui.theme.ShubhSuccessColor
import com.example.ui.theme.TextLink
import com.example.util.LanguageManager
import java.util.Calendar
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PanchangScreen(
    viewModel: MainViewModel,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val panchang by viewModel.panchangState.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    val isChoghadiyaDaytime by viewModel.choghadiyaDaytime.collectAsState()
    val choghadiyaSlots by viewModel.choghadiyaSlots.collectAsState()
    val currentSubTab by viewModel.panchangSubTab.collectAsState()

    val currentUser by viewModel.currentUser.collectAsState()
    val selectedRashiId by viewModel.selectedRashiId.collectAsState()
    val isPanchangLoading by viewModel.isPanchangLoading.collectAsState()
    val isStartupComplete by viewModel.isStartupComplete.collectAsState()

    val context = LocalContext.current

    val locationPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.detectGPSLocation(context) { success ->
                if (!success) {
                    Toast.makeText(context, LanguageManager.getString("स्थिति प्राप्त करने में असमर्थ", "Unable to fetch location"), Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, LanguageManager.getString("अनुमति अस्वीकार कर दी गई", "Permission denied"), Toast.LENGTH_SHORT).show()
        }
    }

    val selectedDate by viewModel.selectedDate.collectAsState()

    val requestCurrentLocation: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            viewModel.detectGPSLocation(context) { success ->
                if (!success) {
                    Toast.makeText(context, LanguageManager.getString("स्थिति प्राप्त करने में असमर्थ", "Unable to fetch location"), Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            locationPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Auto-detect location on first launch if user has never set a city
    LaunchedEffect(Unit) {
        if (!viewModel.hasUserSetCity()) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                viewModel.detectGPSLocation(context) { }
            } else {
                locationPermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    run {
        Scaffold(
            // The outer Scaffold in MainActivity already applies the status bar inset
            // through TopHeaderBar's statusBarsPadding(). Letting this inner Scaffold
            // apply it again is what put an empty band above every sub-tab row.
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                SubTabHeader(
                    selectedTab = currentSubTab,
                    tabs = listOf(
                        LanguageManager.getString("दैनिक पंचांग", "Daily Panchang"),
                        LanguageManager.getString("मासिक कैलेंडर", "Calendar")
                    ),
                    onTabSelected = { viewModel.setPanchangSubTab(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                if (currentSubTab == 1) {
                    CalendarScreen(viewModel)
                } else {
                    PullToRefreshBox(
                        isRefreshing = isPanchangLoading,
                        onRefresh = { viewModel.refreshPanchang() },
                        modifier = Modifier.fillMaxSize().testTag("panchang_swipe_refresh")
                    ) {
                        if (isPanchangLoading && panchang.tithiLocal.isBlank()) {
                            PanchangLoadingSkeleton(modifier = Modifier.fillMaxSize())
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    top = 8.dp,
                                    end = 16.dp,
                                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 1. Personalized Greeting & Welcome Header
                                item {
                                    PersonalizedGreetingCard(
                                        userName = currentUser?.displayName ?: currentUser?.email?.substringBefore("@"),
                                        selectedRashiId = selectedRashiId,
                                        selectedCityName = LanguageManager.getString(selectedCity.cityNameHindi, selectedCity.cityName),
                                        onRashiClick = { viewModel.navigateToRashifal() }
                                    )
                                }

                                // 2. Quick-Access Action Shortcuts Grid (4 Features)
                                item {
                                    DashboardQuickShortcutsGrid(
                                        selectedRashiId = selectedRashiId,
                                        onNavigateRashifal = { viewModel.navigateToRashifal() },
                                        onNavigateKundali = { viewModel.navigateToKundali(0) },
                                        onNavigateMatching = { viewModel.navigateToMatching() },
                                        onNavigateMuhurat = { viewModel.navigateToMuhurat() }
                                    )
                                }

                                // 3. Daily Rashifal Highlight / Sneak Peek Card
                                item {
                                    val activeHoroscope = viewModel.dailyHoroscopes.find { it.rashiId == selectedRashiId }
                                        ?: viewModel.dailyHoroscopes.firstOrNull()
                                    if (activeHoroscope != null) {
                                        DailyRashifalHighlightCard(
                                            horoscope = activeHoroscope,
                                            onReadMoreClick = { viewModel.navigateToRashifal() }
                                        )
                                    }
                                }

                                // 4. Hero Date & Location Selector Header
                                item {
                                    PanchangDateLocationCard(
                                        panchang = panchang,
                                        selectedCity = selectedCity,
                                        selectedDate = selectedDate,
                                        onCitySelected = { viewModel.setCity(it) },
                                        onUseCurrentLocation = requestCurrentLocation,
                                        onDateChange = { viewModel.setDate(it) }
                                    )
                                }

                                // 5. Daily Panchang Summary Card
                                item {
                                    // Bento layout, replacing the single tall card.
                                    // To go back, swap this for DailyPanchangCard(
                                    //     panchang = panchang,
                                    //     locationName = selectedCity.cityNameHindi
                                    // ) — the old component is untouched.
                                    com.example.ui.components.BentoPanchangGrid(
                                        panchang = panchang
                                    )
                                }

                                // 6. Daily Astrology Insights Swiping Carousel
                                item { DailyInsightsPager(panchang) }

                                // 7. Vikram Samvat & Masa Info Bar
                                item { SamvatInfoBar(panchang) }

                                // 8. 5 Core Panchang Elements Section
                                item {
                                    SectionHeader(
                                        titleHi = "पंचांग के 5 मुख्य अंग",
                                        titleEn = "Core 5 Panchang Elements",
                                        subtitleHi = "तिथि, नक्षत्र, योग, करण एवं वार",
                                        subtitleEn = "Tithi, Nakshatra, Yoga, Karan & Var"
                                    )
                                }

                                item { CorePanchangElementsCard(panchang) }

                                // 9. Sun & Moon Timings
                                item {
                                    SectionHeader(
                                        titleHi = "सूर्य एवं चन्द्र समय",
                                        titleEn = "Sun & Moon Timings"
                                    )
                                }

                                item { SunMoonTimingsCard(panchang) }

                                // 10. Auspicious / Inauspicious Muhurats
                                item {
                                    SectionHeader(
                                        titleHi = "शुभ एवं अशुभ मुहूर्त",
                                        titleEn = "Auspicious & Inauspicious Times"
                                    )
                                }

                                item { MuhuratTimingsCard(panchang) }

                                // 11. Choghadiya Strip Section
                                item {
                                    SectionHeader(
                                        titleHi = "आज का चौघड़िया",
                                        titleEn = "Choghadiya Time Strip",
                                        subtitleHi = "शुभ, अमृत, लाभ, चर, उद्वेग, काल व रोग",
                                        subtitleEn = "Real-time Choghadiya calculations for ${panchang.locationName}"
                                    )
                                }

                                item {
                                    ChoghadiyaDayNightToggle(
                                        isDaytime = isChoghadiyaDaytime,
                                        onToggle = { viewModel.toggleChoghadiyaDayNight(it) }
                                    )
                                }

                                // Horizontal Choghadiya Strip Cards
                                item { ChoghadiyaStrip(choghadiyaSlots) }

                                // 12. Daily Lagna Chart & Planetary Positions
                                item {
                                    SectionHeader(
                                        titleHi = "दैनिक लग्न कुण्डली व गोचर स्थिति",
                                        titleEn = "Daily Lagna Kundali & Transits",
                                        icon = Icons.Default.AutoAwesome
                                    )
                                }

                                item {
                                    DailyLagnaChartCard(
                                        panchangDate = selectedDate,
                                        selectedCity = selectedCity,
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        onOpenKundali = { viewModel.selectTab(AppTab.KUNDALI) }
                                    )
                                }

                                // 13. Planetary Positions List
                                item {
                                    PlanetaryPositionsCard(planets = panchang.planets)
                                }

                                item { AstroDisclaimer(scope = DisclaimerScope.TIMINGS) }

                                item {
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// The Daily Panchang's sections. Each takes only the values it draws, so a
// change to one (the Choghadiya day/night toggle, say) no longer recomposes the
// whole screen — PanchangScreen used to be a single 900-line function.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PanchangDateLocationCard(
    panchang: PanchangData,
    selectedCity: CityLocation,
    selectedDate: java.util.Date,
    onCitySelected: (CityLocation) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onDateChange: (java.util.Date) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = panchang.dateLocal,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = "${panchang.masaLocal} | ${panchang.pakshaLocal}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // City Location Picker Pill
            var expanded by remember { mutableStateOf(false) }
            var searchQuery by remember(selectedCity, LanguageManager.currentLanguage) {
                mutableStateOf(
                    LanguageManager.getString(selectedCity.cityNameHindi, selectedCity.cityName)
                )
            }
            val cities = PanchangCalculator.popularCities
            val filteredCities = if (searchQuery.isBlank()) {
                cities
            } else {
                cities.filter { it.cityNameHindi.contains(searchQuery, ignoreCase = true) || it.cityName.contains(searchQuery, ignoreCase = true) }
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        expanded = true
                    },
                    label = { Text(LanguageManager.getString("स्थान चुनें", "Choose Location"), fontSize = 12.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = GlassCardBorder,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    DropdownMenuItem(
                        text = { Text(LanguageManager.getString("📍 वर्तमान स्थान", "📍 Current location (GPS)")) },
                        onClick = {
                            expanded = false
                            onUseCurrentLocation()
                        }
                    )

                    filteredCities.forEach { city ->
                        DropdownMenuItem(
                            text = { Text(LanguageManager.getString(city.cityNameHindi, city.cityName)) },
                            onClick = {
                                onCitySelected(city)
                                searchQuery = LanguageManager.getString(city.cityNameHindi, city.cityName)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Date Navigation Row (Previous Day, Today, Next Day)
                    Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { time = selectedDate }
                        cal.add(Calendar.DAY_OF_YEAR, -1)
                        onDateChange(cal.time)
                    },
                    modifier = Modifier.testTag("panchang_prev_day_button")
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(LanguageManager.getString("पिछला दिन", "Previous"), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }

                TextButton(
                    onClick = {
                        onDateChange(java.util.Date())
                    },
                    modifier = Modifier.testTag("panchang_today_button")
                ) {
                    Icon(Icons.Default.Today, contentDescription = "Today", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(LanguageManager.getString("आज", "Today"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }

                TextButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { time = selectedDate }
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                        onDateChange(cal.time)
                    },
                    modifier = Modifier.testTag("panchang_next_day_button")
                ) {
                    Text(LanguageManager.getString("अगला दिन", "Next"), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun DailyInsightsPager(panchang: PanchangData) {
    val insightsPagerState = rememberPagerState(pageCount = { 4 })

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = LanguageManager.getString("✨ दैनिक ज्योतिष अंतर्दृष्टि ✨", "✨ Daily Insights ✨"),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0..3).forEach { index ->
                    val isSelected = insightsPagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 8.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )
                }
            }
        }

        HorizontalPager(
            state = insightsPagerState,
            modifier = Modifier.fillMaxWidth().testTag("panchang_insights_pager"),
            pageSpacing = 12.dp
        ) { page ->
            val pageOffset = ((insightsPagerState.currentPage - page) + insightsPagerState.currentPageOffsetFraction)
            val absOffset = pageOffset.absoluteValue

            val cardScale = lerp(0.93f, 1f, 1f - absOffset.coerceIn(0f, 1f))
            val cardAlpha = lerp(0.6f, 1f, 1f - absOffset.coerceIn(0f, 1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                        alpha = cardAlpha
                    }
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    when (page) {
                        0 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(LanguageManager.getString("आज का शुभ विचार व पंचांग ज्ञान", "Today’s Panchang Insight"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
                                }
                                Text(
                                    text = LanguageManager.getString(
                                        "तिथि ${panchang.tithiLocal} (${panchang.pakshaLocal}) में शुभ कार्यों का शुभारंभ फलदायी रहता है। आज ${panchang.nakshatraLocal} नक्षत्र एवं ${panchang.yogaLocal} योग का प्रभाव रहेगा।",
                                        "${panchang.tithiLocal} (${panchang.pakshaLocal}) is favourable for beginning auspicious work. Today carries the influence of ${panchang.nakshatraLocal} Nakshatra and ${panchang.yogaLocal} Yoga."
                                    ),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, lineHeight = 19.sp)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GlassBadge(LanguageManager.getString("सूर्योदय: ${panchang.sunrise}", "Sunrise: ${panchang.sunrise}"))
                                    GlassBadge(LanguageManager.getString("सूर्यास्त: ${panchang.sunset}", "Sunset: ${panchang.sunset}"))
                                }
                            }
                        }
                        1 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.NightsStay, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(LanguageManager.getString("चंद्र कला व राहु काल सतर्कता", "Moon phase & Rahu Kaal warning"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 15.sp)
                                }
                                Text(
                                    text = LanguageManager.getString(
                                        "चंद्र राशि: ${panchang.moonSign} (${panchang.pakshaLocal})। चंद्रोदय: ${panchang.moonrise}।",
                                        "Moon sign: ${panchang.moonSign} (${panchang.pakshaLocal}). Moonrise: ${panchang.moonrise}."
                                    ),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                )
                                GlassBadge(
                                    text = LanguageManager.getString("⚠️ राहु काल: ${panchang.rahuKaal} (अशुभ समय)", "⚠️ Rahu Kaal: ${panchang.rahuKaal} (avoid)"),
                                    textColor = RahuKaalDangerColor,
                                    borderColor = RahuKaalDangerColor
                                )
                            }
                        }
                        2 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = ShubhSuccessColor)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(LanguageManager.getString("चौघड़िया व शुभ मुहूर्त विचार", "Choghadiya & auspicious timings"), fontWeight = FontWeight.Bold, color = ShubhSuccessColor, fontSize = 15.sp)
                                }
                                Text(
                                    text = LanguageManager.getString(
                                        "अभिजीत मुहूर्त: ${panchang.abhijitMuhurat} (सर्वश्रेष्ठ मुहूर्त)। आज यमगण्‍ड काल ${panchang.yamaganda} में रहेगा।",
                                        "Abhijit Muhurta: ${panchang.abhijitMuhurat} (the most auspicious window). Yamaganda runs ${panchang.yamaganda} today."
                                    ),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                )
                                GlassBadge(
                                    text = LanguageManager.getString("🌟 अभिजीत मुहूर्त: ${panchang.abhijitMuhurat}", "🌟 Abhijit Muhurta: ${panchang.abhijitMuhurat}"),
                                    textColor = ShubhSuccessColor,
                                    borderColor = ShubhSuccessColor
                                )
                            }
                        }
                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryButtonBackground)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(LanguageManager.getString("नक्षत्र ऊर्जा व गोचर प्रभाव", "Nakshatra energy & transit effect"), fontWeight = FontWeight.Bold, color = PrimaryButtonBackground, fontSize = 15.sp)
                                }
                                Text(
                                    text = LanguageManager.getString(
                                        "नक्षत्र ${panchang.nakshatraLocal} (चरण ${panchang.nakshatraPada}) चंद्र प्रभाव ${panchang.moonSign} राशि में कार्यसिद्धि प्रदान करता है।",
                                        "${panchang.nakshatraLocal} Nakshatra (Pada ${panchang.nakshatraPada}) with the Moon in ${panchang.moonSign} supports getting things done."
                                    ),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                )
                                GlassBadge(LanguageManager.getString("सूर्य राशि: ${panchang.sunSign}", "Sun sign: ${panchang.sunSign}"), textColor = PrimaryButtonBackground, borderColor = PrimaryButtonBackground)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SamvatInfoBar(panchang: PanchangData) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoPill(LanguageManager.getString("विक्रम संवत", "Vikram Samvat"), "${panchang.vikramSamvat}")
            InfoPill(LanguageManager.getString("शक संवत", "Saka Samvat"), "${panchang.sakaSamvat}")
            InfoPill(LanguageManager.getString("मास", "Month"), panchang.masaLocal)
        }
    }
}

@Composable
private fun CorePanchangElementsCard(panchang: PanchangData) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Tithi Section
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LanguageManager.getString("तिथि", "TITHI"),
                        style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    )
                    GlassBadge(text = panchang.pakshaLocal.substringBefore(" "))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = panchang.tithiLocal,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = panchang.tithiEndTime,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (panchang.tithiProgressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f), thickness = 1.dp)

            // Nakshatra Section
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LanguageManager.getString("नक्षत्र", "NAKSHATRA"),
                        style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    )
                    GlassBadge(text = LanguageManager.getString("चंद्र राशि: ${panchang.moonSign}", "Moon sign: ${panchang.moonSign}"))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = LanguageManager.getString("${panchang.nakshatraLocal} (चरण ${panchang.nakshatraPada})", "${panchang.nakshatraLocal} (Pada ${panchang.nakshatraPada})"),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = panchang.nakshatraEndTime,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { 0.65f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f), thickness = 1.dp)

            // Yoga & Karan Section
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = LanguageManager.getString("योग", "YOGA"),
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = panchang.yogaLocal,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = LanguageManager.getString("करण", "KARANA"),
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = panchang.karanaLocal,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SunMoonTimingsCard(panchang: PanchangData) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TimingColumn(LanguageManager.getString("सूर्योदय", "Sunrise"), panchang.sunrise, Icons.Default.WbSunny, MaterialTheme.colorScheme.primary)
            TimingColumn(LanguageManager.getString("सूर्यास्त", "Sunset"), panchang.sunset, Icons.Default.WbSunny, MaterialTheme.colorScheme.secondary)
            TimingColumn(LanguageManager.getString("चन्द्रास्त", "Moonset"), panchang.moonset, Icons.Default.NightsStay, MaterialTheme.colorScheme.onSurfaceVariant)
            TimingColumn(LanguageManager.getString("चन्द्रोदय", "Moonrise"), panchang.moonrise, Icons.Default.NightsStay, MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun MuhuratTimingsCard(panchang: PanchangData) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val timings = listOf(
                Triple(LanguageManager.getString("अभिजित मुहूर्त", "Abhijit Muhurat"), panchang.abhijitMuhurat, Pair(LanguageManager.getString("अति शुभ", "Best"), ShubhSuccessColor)),
                Triple(LanguageManager.getString("राहुकाल", "Rahu Kaal"), panchang.rahuKaal, Pair(LanguageManager.getString("अशुभ", "Avoid"), RahuKaalDangerColor)),
                Triple(LanguageManager.getString("गुलिक काल", "Gulika Kaal"), panchang.gulikaKaal, Pair(LanguageManager.getString("अशुभ", "Avoid"), RahuKaalDangerColor)),
                Triple(LanguageManager.getString("यमगण्ड", "Yamaganda"), panchang.yamaganda, Pair(LanguageManager.getString("अशुभ", "Avoid"), RahuKaalDangerColor))
            )

            timings.forEachIndexed { index, timing ->
                val (title, time, status) = timing
                val (statusText, color) = status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = time,
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Normal, fontSize = 15.sp)
                        )
                    }

                    GlassBadge(
                        text = statusText,
                        backgroundColor = color.copy(alpha = 0.15f),
                        textColor = color,
                        borderColor = color.copy(alpha = 0.4f)
                    )
                }

                if (index < timings.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f), thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun ChoghadiyaDayNightToggle(isDaytime: Boolean, onToggle: (Boolean) -> Unit) {
    val view = LocalView.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isDaytime) LanguageManager.getString("दिन का चौघड़िया", "Day Choghadiya")
else LanguageManager.getString("रात्रि चौघड़िया", "Night Choghadiya"),
            style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        )

        Row {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDaytime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onToggle(true)
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = LanguageManager.getString("दिन", "Day"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Normal,
                        color = if (isDaytime) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (!isDaytime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onToggle(false)
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = LanguageManager.getString("रात", "Night"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Normal,
                        color = if (!isDaytime) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun ChoghadiyaStrip(slots: List<ChoghadiyaSlot>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(slots, key = { it.timeSlotString }) { slot ->
            val statusColor = when (slot.type.name) {
                "AMRIT", "SHUBH", "LABH" -> ShubhSuccessColor
                "CHAR" -> DateTimeAccent
                else -> RahuKaalDangerColor
            }

            GlassCard(
                modifier = Modifier.width(130.dp),
                borderColor = statusColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    Text(
                        text = slot.type.nameLocal,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp
                        )
                    )
                    Text(
                        text = slot.type.natureLocal,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = statusColor,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = slot.timeSlotString,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DailyLagnaChartCard(
    panchangDate: java.util.Date,
    selectedCity: CityLocation,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    onOpenKundali: () -> Unit
) {
    val view = LocalView.current
    val dailyLagnaChart = remember(panchangDate, selectedCity) {
        val cal = java.util.Calendar.getInstance(com.example.astro.AstroTime.IST)
            .apply { time = panchangDate }
        val midnightJd = com.example.astro.AstroTime.julianDayFromLocal(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH),
            0, 0,
            com.example.astro.AstroTime.IST
        )
        val sunriseJd = com.example.astro.RiseSetCalculator.sunRiseSet(
            midnightJd, selectedCity.latitude, selectedCity.longitude
        ).riseJd ?: (midnightJd + 0.25)

        com.example.astro.KundaliCalculator.chartForInstant(
            label = "Daily Lagna",
            jdUT = sunriseJd,
            placeName = selectedCity.cityNameHindi,
            latitude = selectedCity.latitude,
            longitude = selectedCity.longitude
        )
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        // Centred: the chart is a square inside a
        // full-width column, so left-aligning it left
        // all the slack on one side.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PrimaryButtonBackground,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = LanguageManager.getString("लग्न: ${dailyLagnaChart.ascendantRashiHi}", "Lagna: ${dailyLagnaChart.ascendantRashiEn}"),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                TextButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onOpenKundali()
                }) {
                    Text(
                        text = LanguageManager.getString("पूर्ण कुण्डली देखें →", "View full chart →"),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextLink,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            val chartModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    Modifier
                        .fillMaxWidth()
                        .sharedElement(
                            state = rememberSharedContentState(key = "kundali_chart_shared_element"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                }
            } else Modifier.fillMaxWidth()

            NorthIndianChart(
                chartData = dailyLagnaChart,
                modifier = chartModifier,
                onHouseClick = { _, _, _ -> onOpenKundali() }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-Components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PersonalizedGreetingCard(
    userName: String?,
    selectedRashiId: Int,
    selectedCityName: String,
    onRashiClick: () -> Unit
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetingHi = when (hour) {
        in 4..11 -> LanguageManager.getString("शुभ प्रभात", "Good Morning")
        in 12..16 -> LanguageManager.getString("शुभ दोपहर", "Good Afternoon")
        in 17..21 -> LanguageManager.getString("शुभ संध्या", "Good Evening")
        else -> LanguageManager.getString("शुभ रात्रि", "Good Night")
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = greetingHi,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (!userName.isNullOrBlank()) LanguageManager.getString("नमस्ते, $userName 🙏", "Namaste, $userName 🙏")
                        else "ॐ नमः शिवाय 🙏",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 17.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // City Chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "City",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = selectedCityName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp
                            )
                        )
                    }

                    // Account Status Chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User",
                            tint = if (userName != null) ShubhSuccessColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            // Signed out is a real state again now that sign-in is
                            // not a gate, and this chip claimed "Cloud sync on"
                            // to a user who had no account at all.
                            text = userName ?: LanguageManager.getString("मेहमान", "Guest"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (userName != null) ShubhSuccessColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // Quick Rashi Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                    .clickable { onRashiClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Rashi",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun DashboardQuickShortcutsGrid(
    selectedRashiId: Int,
    onNavigateRashifal: () -> Unit,
    onNavigateKundali: () -> Unit,
    onNavigateMatching: () -> Unit,
    onNavigateMuhurat: () -> Unit
) {
    // Two tiles per row assumes there is room for two. On a 320dp phone there is
    // not: each tile then gives its text column ~67dp, and every Hindi label in
    // the grid clipped — "दैनिक राशि…", "36 गुण व दोष…", "चौघड़िया व रा…". Shrinking
    // the type to fit would make it unreadable, so on the narrowest phones the
    // grid becomes a single column and each tile gets the full width. Taller
    // there, which is the right trade: the labels are the point of the tile.
    //
    // The threshold is on the width this grid is *given*, not the screen width —
    // the screen's own 16dp side padding is already subtracted here. 300dp keeps
    // the two-column layout on a 360dp phone, which is where most Android phones
    // sit and where two columns still leave ~87dp of label; only the 320dp floor
    // (288dp here) drops to one. Set against the screen width instead, this read
    // 360 and turned the common case single-column too.
    BoxWithConstraints {
        val singleColumn = maxWidth < 300.dp
        val perRow = if (singleColumn) 1 else 2

        val tiles: List<@Composable (Modifier) -> Unit> = listOf(
            { m ->
                ShortcutTile(
                    title = LanguageManager.getString("दैनिक राशिफल", "Horoscope"),
                    subtitle = LanguageManager.getString("12 राशियां व भविष्य", "All 12 signs"),
                    icon = Icons.Default.AutoAwesome,
                    iconColor = MaterialTheme.colorScheme.primary,
                    modifier = m,
                    onClick = onNavigateRashifal
                )
            },
            { m ->
                ShortcutTile(
                    title = LanguageManager.getString("जन्म कुण्डली", "Birth Chart"),
                    subtitle = LanguageManager.getString("लग्न चक्र व दशा", "Lagna & Dasha"),
                    icon = Icons.Default.Today,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    modifier = m,
                    onClick = onNavigateKundali
                )
            },
            { m ->
                ShortcutTile(
                    title = LanguageManager.getString("कुण्डली मिलान", "Kundli Milan"),
                    subtitle = LanguageManager.getString("36 गुण व दोष जांच", "36 Guna Match"),
                    icon = Icons.Default.Favorite,
                    iconColor = RahuKaalDangerColor,
                    modifier = m,
                    onClick = onNavigateMatching
                )
            },
            { m ->
                ShortcutTile(
                    title = LanguageManager.getString("शुभ मुहूर्त", "Muhurat"),
                    subtitle = LanguageManager.getString("चौघड़िया व राहुकाल", "Choghadiya"),
                    icon = Icons.Default.Schedule,
                    iconColor = ShubhSuccessColor,
                    modifier = m,
                    onClick = onNavigateMuhurat
                )
            }
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tiles.chunked(perRow).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { tile -> tile(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun ShortcutTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DailyRashifalHighlightCard(
    horoscope: com.example.data.model.RashifalData,
    onReadMoreClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReadMoreClick() },
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = horoscope.symbol,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(
                            text = LanguageManager.getString("आज का राशिफल — ${horoscope.rashiNameHi}", "Today's Horoscope — ${horoscope.rashiNameEn}"),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.5.sp
                            )
                        )
                        Text(
                            text = LanguageManager.getString("स्वामी: ${horoscope.rulerHi}", "Ruling Planet: ${horoscope.rulerEn}"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Text(
                    text = LanguageManager.getString("पूरा पढ़ें →", "Full →"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )
            }

            val readingText = LanguageManager.getString(horoscope.generalReadingHi, horoscope.generalReadingEn)
            Text(
                text = readingText.take(120) + "...",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassBadge(text = LanguageManager.getString("भाग्यशाली अंक: ${horoscope.luckyNumber}", "Lucky No: ${horoscope.luckyNumber}"))
                GlassBadge(text = LanguageManager.getString("शुभ रंग: ${horoscope.luckyColorHi}", "Lucky Color: ${horoscope.luckyColorEn}"))
            }
        }
    }
}

@Composable
fun InfoPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        )
    }
}

@Composable
fun TimingColumn(title: String, time: String, icon: ImageVector, iconColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = title, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp))
        Text(text = time, style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Normal, fontSize = 14.sp))
    }
}

@Composable
fun PlanetaryPositionsCard(planets: List<com.example.data.model.PlanetPosition>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            titleHi = "ग्रह स्थिति",
            titleEn = "Current Astrological Positions"
        )
        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                planets.forEach { planet ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // The name/nakshatra side takes the slack; without the
                        // weight it grew until the degree column had no room and
                        // broke "20.4°" across two lines and "Retrograde" across
                        // four.
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = planet.planetNameHi.substring(0, 1),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${planet.planetNameHi} (${planet.planetNameEn})",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 15.sp
                                    )
                                )
                                Text(
                                    text = LanguageManager.getString("नक्षत्र: ${planet.nakshatraHi} | राशि: ${planet.rashiNameHi}", "Nakshatra: ${planet.nakshatraEn} | Sign: ${planet.rashiNameEn}"),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${planet.degree}°",
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                            if (planet.isRetrograde) {
                                Text(
                                    text = LanguageManager.getString("वक्री", "Retrograde"),
                                    maxLines = 1,
                                    softWrap = false,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = RahuKaalDangerColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
