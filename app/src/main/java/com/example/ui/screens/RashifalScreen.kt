package com.example.ui.screens

import com.example.ui.components.AstroDisclaimer
import com.example.ui.components.DisclaimerScope
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RashifalData
import com.example.ui.MainViewModel
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.GlassBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.PersonalizedInsightCard
import com.example.ui.components.RashifalLoadingSkeleton
import com.example.ui.components.SectionHeader
import com.example.ui.theme.GlassCardBorder
import com.example.util.LanguageManager
import com.example.util.ShareUtils


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RashifalScreen(viewModel: MainViewModel) {
    val haptic = LocalHapticFeedback.current
    val horoscopes by viewModel.dailyHoroscopesState.collectAsState()
    val selectedRashiId by viewModel.selectedRashiId.collectAsState()

    val currentHoroscope = horoscopes.find { it.rashiId == selectedRashiId } ?: horoscopes.firstOrNull() ?: com.example.data.model.RashifalData(
        rashiId = 1,
        rashiNameEn = "Aries",
        rashiNameHi = "मेष",
        symbol = "♈",
        elementHi = "अग्नि", elementEn = "Fire",
        rulerHi = "मंगल", rulerEn = "Mars",
        ratingStars = 4,
        luckyNumber = 9,
        luckyColorEn = "Red",
        luckyColorHi = "लाल",
        luckyStoneHi = "मूंगा", luckyStoneEn = "Red Coral",
        generalReadingHi = "आज का दिन अच्छा रहेगा।",
        generalReadingEn = "Today will be a good day.",
        careerReadingHi = "",
        careerReadingEn = "",
        healthReadingHi = "",
        healthReadingEn = "",
        loveReadingHi = "",
        loveReadingEn = "",
        financeReadingHi = "",
        financeReadingEn = ""
    )

    var selectedPeriod by remember { mutableStateOf("TODAY") } // "TODAY", "WEEK", "MONTH"

    val dateRangeText = when (selectedPeriod) {
        "TODAY" -> LanguageManager.getString("आज का दैनिक राशिफल", "Today's horoscope")
        "WEEK" -> LanguageManager.getString("इस सप्ताह का राशिफल", "This week's horoscope")
        else -> LanguageManager.getString("इस महीने का राशिफल", "This month's horoscope")
    }

    // This was a HorizontalPager of all twelve signs, with neighbours peeking in
    // at the edges and a scale/alpha/rotationY animation on every frame. Inside a
    // vertically scrolling page it fought the scroll and snagged mid-swipe, and
    // half-drawn neighbouring cards read as clipped text. The sign is chosen from
    // the row of chips above; one card, no carousel.

    Scaffold(
        // MainActivity's Scaffold already applies the status bar inset via
        // TopHeaderBar's statusBarsPadding(); applying it again leaves a gap.
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        val isHoroscopeLoading by viewModel.isHoroscopeLoading.collectAsState()
        PullToRefreshBox(
            isRefreshing = isHoroscopeLoading,
            onRefresh = { viewModel.refreshHoroscopes(period = selectedPeriod) },
            modifier = Modifier.fillMaxSize().testTag("rashifal_swipe_refresh")
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = paddingValues.calculateTopPadding() + 8.dp,
                    end = 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                titleHi = "12 राशियां",
                titleEn = "Select Zodiac Sign",
                subtitleHi = "अपनी राशि चुनें",
                subtitleEn = "Choose your sign"
            )
        }

        // 1: Horizontal scrollable selector for all 12 rashis with zodiac icons
        item {
            RashiSelectorRow(
                horoscopes = horoscopes,
                selectedRashiId = selectedRashiId,
                onSelect = { viewModel.selectRashi(it) }
            )
        }

        // 2: Today / This Week / This Month Period Tabs
        item {
            RashifalPeriodTabs(
                selectedPeriod = selectedPeriod,
                onSelect = { code ->
                    selectedPeriod = code
                    viewModel.loadHoroscopesWithCache(period = code)
                }
            )
        }

        // 6: The selected sign's reading.
        item {
            RashifalReadingCards(
                horoscope = currentHoroscope,
                selectedPeriod = selectedPeriod,
                dateRangeText = dateRangeText
            )
        }

        item {
            // AI Personalized Insight, per sign AND period. Reading a shared
            // value here is what made every rashi show the same paragraph, and
            // keying by sign alone is what put the daily insight under the
            // weekly and monthly tabs.
            val aiInsights by viewModel.aiRashifalInsights.collectAsState()
            val aiLoadingFor by viewModel.rashifalAiLoadingFor.collectAsState()
            val aiErrorFor by viewModel.rashifalAiErrorFor.collectAsState()
            val isPro by viewModel.isProUser.collectAsState()
            val aiRefusal by viewModel.rashifalAiRefusal.collectAsState()
            val sign = currentHoroscope.rashiNameEn
            val key = viewModel.insightKey(sign, selectedPeriod)

            PersonalizedInsightCard(
                insight = aiInsights[key].orEmpty(),
                isLoading = aiLoadingFor == key,
                failed = aiErrorFor == key,
                failureMessage = aiRefusal,
                locked = !isPro,
                period = selectedPeriod,
                onFetchInsight = {
                    viewModel.fetchPersonalizedInsight(sign, selectedPeriod, currentHoroscope.generalReadingEn)
                }
            )
        }

        item { AstroDisclaimer(scope = DisclaimerScope.READING) }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
}
}

// ─────────────────────────────────────────────────────────────────────────────
// The Rashifal's sections. They were one function, so choosing a period or a
// sign recomposed the chip row, and every AI-insight state change re-ran the
// reading's seven entry animations' composition along with it.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RashiSelectorRow(
    horoscopes: List<RashifalData>,
    selectedRashiId: Int,
    onSelect: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(horoscopes, key = { it.rashiId }) { item ->
            val isSelected = (item.rashiId == selectedRashiId)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else GlassCardBorder, RoundedCornerShape(16.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect(item.rashiId)
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("rashi_selector_${item.rashiId}")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.symbol,
                        fontSize = 18.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = LanguageManager.getString(item.rashiNameHi.substringBefore(" "), item.rashiNameEn),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RashifalPeriodTabs(selectedPeriod: String, onSelect: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        listOf(
            Pair("TODAY", LanguageManager.getString("आज का राशिफल", "Today")),
            Pair("WEEK", LanguageManager.getString("इस सप्ताह", "This week")),
            Pair("MONTH", LanguageManager.getString("इस महीने", "This month"))
        ).forEach { (code, label) ->
            val isSelected = (selectedPeriod == code)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.secondary else GlassCardBorder, RoundedCornerShape(20.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect(code)
                    }
                    .padding(horizontal = 18.dp, vertical = 8.dp)
                    .testTag("period_toggle_$code")
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }
    }
}

@Composable
private fun RashifalReadingCards(
    horoscope: RashifalData,
    selectedPeriod: String,
    dateRangeText: String
) {
    val haptic = LocalHapticFeedback.current
    Box(modifier = Modifier.fillMaxWidth().testTag("rashifal_card_pager")) {

        val heroAlpha = remember(horoscope.rashiId, selectedPeriod) { Animatable(0f) }
        val heroTranslationY = remember(horoscope.rashiId, selectedPeriod) { Animatable(20f) }
        val overviewAlpha = remember(horoscope.rashiId, selectedPeriod) { Animatable(0f) }
        val overviewTranslationY = remember(horoscope.rashiId, selectedPeriod) { Animatable(20f) }
        val catAlpha = remember(horoscope.rashiId, selectedPeriod) { Animatable(0f) }
        val catTranslationY = remember(horoscope.rashiId, selectedPeriod) { Animatable(20f) }

        LaunchedEffect(horoscope.rashiId, selectedPeriod) {
            heroAlpha.animateTo(1f, animationSpec = tween(300, easing = FastOutSlowInEasing))
        }
        LaunchedEffect(horoscope.rashiId, selectedPeriod) {
            heroTranslationY.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy))
        }

        LaunchedEffect(horoscope.rashiId, selectedPeriod) {
            kotlinx.coroutines.delay(60L)
            overviewAlpha.animateTo(1f, animationSpec = tween(300, easing = FastOutSlowInEasing))
        }
        LaunchedEffect(horoscope.rashiId, selectedPeriod) {
            kotlinx.coroutines.delay(60L)
            overviewTranslationY.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy))
        }

        LaunchedEffect(horoscope.rashiId, selectedPeriod) {
            kotlinx.coroutines.delay(120L)
            catAlpha.animateTo(1f, animationSpec = tween(300, easing = FastOutSlowInEasing))
        }
        LaunchedEffect(horoscope.rashiId, selectedPeriod) {
            kotlinx.coroutines.delay(120L)
            catTranslationY.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy))
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Rashi Card with 5-Star Rating & Lucky Chips
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = heroAlpha.value
                        translationY = heroTranslationY.value
                    }
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // The weight keeps "Lord: Venus | Element: Earth"
                            // from growing into the stars beside it.
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = horoscope.symbol, fontSize = 28.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = LanguageManager.getString(horoscope.rashiNameHi, horoscope.rashiNameEn),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 20.sp
                                        )
                                    )
                                    Text(
                                        text = "${LanguageManager.getString("स्वामी", "Lord")}: ${LanguageManager.getString(horoscope.rulerHi, horoscope.rulerEn)} | ${LanguageManager.getString("तत्व", "Element")}: ${LanguageManager.getString(horoscope.elementHi, horoscope.elementEn)}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // 4: 5-Star Score Display with animated gold fill
                            AnimatedStarScoreDisplay(rating = horoscope.ratingStars)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Date Range Banner + Share Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📅 $dateRangeText",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            )
                            val context = LocalContext.current
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        ShareUtils.shareRashifalText(
                                            context = context,
                                            rashiName = horoscope.rashiNameEn,
                                            rashiNameHi = horoscope.rashiNameHi,
                                            period = selectedPeriod,
                                            general = LanguageManager.getString(horoscope.generalReadingHi, horoscope.generalReadingEn),
                                            career = LanguageManager.getString(horoscope.careerReadingHi, horoscope.careerReadingEn),
                                            health = LanguageManager.getString(horoscope.healthReadingHi, horoscope.healthReadingEn),
                                            love = LanguageManager.getString(horoscope.loveReadingHi, horoscope.loveReadingEn),
                                            finance = LanguageManager.getString(horoscope.financeReadingHi, horoscope.financeReadingEn),
                                            luckyNumber = horoscope.luckyNumber.toString(),
                                            luckyColor = LanguageManager.getString(horoscope.luckyColorHi, horoscope.luckyColorEn),
                                            luckyTime = LanguageManager.getString(horoscope.luckyTimeHi, horoscope.luckyTimeEn),
                                            score = horoscope.ratingStars
                                        )
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .testTag("share_rashifal_button"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = LanguageManager.getString("शेयर करें", "Share"),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // The four "lucky" facts as bento tiles. As badges
                        // on one row they were squeezed and truncated —
                        // "शुभ रत्न: मो…" — because a badge cannot shrink
                        // its content.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            com.example.ui.components.BentoTile(
                                label = LanguageManager.getString("शुभ अंक", "Lucky number"),
                                value = "${horoscope.luckyNumber}",
                                accent = MaterialTheme.colorScheme.primary,
                                valueSize = 22,
                                minHeight = 66,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            com.example.ui.components.BentoTile(
                                label = LanguageManager.getString("शुभ रंग", "Lucky colour"),
                                value = LanguageManager.getString(horoscope.luckyColorHi, horoscope.luckyColorEn),
                                accent = MaterialTheme.colorScheme.primary,
                                valueSize = 14,
                                minHeight = 66,
                                modifier = Modifier
                                    .weight(1.4f)
                                    .fillMaxHeight()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            com.example.ui.components.BentoTile(
                                label = LanguageManager.getString("शुभ रत्न", "Lucky stone"),
                                value = LanguageManager.getString(horoscope.luckyStoneHi, horoscope.luckyStoneEn),
                                accent = MaterialTheme.colorScheme.secondary,
                                valueSize = 14,
                                minHeight = 66,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            com.example.ui.components.BentoTile(
                                label = LanguageManager.getString("शुभ समय", "Lucky time"),
                                value = LanguageManager.getString(horoscope.luckyTimeHi, horoscope.luckyTimeEn),
                                accent = MaterialTheme.colorScheme.secondary,
                                valueSize = 13,
                                minHeight = 66,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }

            // 3: General Overview in Hindi
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = overviewAlpha.value
                        translationY = overviewTranslationY.value
                    }
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = LanguageManager.getString("सामान्य भविष्यफल", "General Overview"),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = LanguageManager.getString(
                                horoscope.generalReadingHi,
                                horoscope.generalReadingEn
                            ),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        )
                    }
                }
            }

            // 3: Domain Categories (Career, Health, Love, Finance) in Hindi
            SectionHeader(
                titleHi = "क्षेत्रवार फलादेश",
                titleEn = "Category Breakdown"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = catAlpha.value
                        translationY = catTranslationY.value
                    }
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val domains = listOf(
                            DomainItem(
                                LanguageManager.getString("करियर व व्यवसाय", "Career & Business"),
                                horoscope.careerReadingHi,
                                horoscope.careerReadingEn,
                                Icons.Default.Work,
                                MaterialTheme.colorScheme.primary
                            ),
                            DomainItem(
                                LanguageManager.getString("वित्त व धन लाभ", "Finance & Money"),
                                horoscope.financeReadingHi,
                                horoscope.financeReadingEn,
                                Icons.Default.MonetizationOn,
                                MaterialTheme.colorScheme.primary
                            ),
                            DomainItem(
                                LanguageManager.getString("प्रेम व संबंध", "Love & Relationships"),
                                horoscope.loveReadingHi,
                                horoscope.loveReadingEn,
                                Icons.Default.Favorite,
                                MaterialTheme.colorScheme.secondary
                            ),
                            DomainItem(
                                LanguageManager.getString("स्वास्थ्य एवं ऊर्जा", "Health & Fitness"),
                                horoscope.healthReadingHi,
                                horoscope.healthReadingEn,
                                Icons.Default.FitnessCenter,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )

                        domains.forEachIndexed { index, item ->
                            val itemAlpha = remember(horoscope.rashiId, selectedPeriod, index) { Animatable(0f) }
                            val itemTranslationY = remember(horoscope.rashiId, selectedPeriod, index) { Animatable(15f) }

                            LaunchedEffect(horoscope.rashiId, selectedPeriod, index) {
                                kotlinx.coroutines.delay(120L + (index * 50L))
                                itemAlpha.animateTo(1f, animationSpec = tween(250, easing = FastOutSlowInEasing))
                            }
                            LaunchedEffect(horoscope.rashiId, selectedPeriod, index) {
                                kotlinx.coroutines.delay(120L + (index * 50L))
                                itemTranslationY.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy))
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        alpha = itemAlpha.value
                                        translationY = itemTranslationY.value
                                    },
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(item.color.copy(alpha = 0.15f))
                                        .border(1.dp, item.color.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = item.color,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = LanguageManager.getString(item.readingHi, item.readingEn),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp
                                        )
                                    )
                                }
                            }

                            if (index < domains.lastIndex) {
                                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class DomainItem(
    val title: String,
    val readingHi: String,
    val readingEn: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun AnimatedStarScoreDisplay(rating: Int) {
    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(rating) {
        progressAnim.snapTo(0f)
        progressAnim.animateTo(
            targetValue = rating.toFloat(),
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = "$rating / 5",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row {
            repeat(5) { index ->
                val starFill = (progressAnim.value - index).coerceIn(0f, 1f)
                val scale = if (starFill > 0f) 1f + (starFill * 0.15f) else 1f

                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (starFill > 0.5f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            alpha = if (starFill > 0f) 1f else 0.4f
                        )
                )
            }
        }
    }
}


