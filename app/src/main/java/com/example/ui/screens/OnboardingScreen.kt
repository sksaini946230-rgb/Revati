package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.revati.jyotish.R
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.GoldGlowButton
import com.example.ui.theme.ShubhSuccessColor
import com.example.util.AppLanguage
import com.example.util.AstroAnalytics
import com.example.util.LanguageManager

/**
 * The onboarding funnel, one language-neutral token per page, in the order they
 * are shown. These are analytics parameter values, so they must never be
 * localised — the same step logged as "भाषा" in Hindi and "language" in English
 * splits one funnel into two, which is the same mistake the chart glyphs made
 * before they were stored as neutral tokens. `totalPages` is this list's size:
 * a new page needs a name here, and `OnboardingStepsTest` pins the list so that
 * adding one without a page (which renders nothing) has to be deliberate.
 */
internal val ONBOARDING_STEPS = listOf("language", "rashi", "location_notifications")

@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var pageIndex by remember { mutableIntStateOf(0) }
    val totalPages = ONBOARDING_STEPS.size

    // The funnel: which of the three pages a user reaches before they leave.
    // "Skip" and "Enter Revati" both call onComplete(), so `onboarding_complete`
    // alone cannot tell a skip from a finish — the last step reached can.
    LaunchedEffect(pageIndex) {
        AstroAnalytics.logOnboardingStep(pageIndex, ONBOARDING_STEPS[pageIndex])
    }

    val selectedRashiId by viewModel.selectedRashiId.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()

    var locationPermissionGranted by remember { mutableStateOf(true) }
    var notificationPermissionGranted by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Bar: Page Indicators (Dots) & Skip Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Page Indicators (Dots)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(totalPages) { idx ->
                    Box(
                        modifier = Modifier
                            .size(if (idx == pageIndex) 24.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (idx == pageIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            // Skip Button Top Right
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onComplete() }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("onboarding_skip_button")
            ) {
                Text(
                    text = LanguageManager.getString("छोड़ें", "Skip"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                )
            }
        }

        // Onboarding Pages Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 55.dp, bottom = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            when (pageIndex) {
                0 -> LanguageSelectOnboardingPage(viewModel)
                1 -> RashiSelectOnboardingPage(viewModel)
                2 -> LocationNotificationOnboardingPage(
                    selectedCityName = selectedCity.nameLocal,
                    locationGranted = locationPermissionGranted,
                    notificationGranted = notificationPermissionGranted,
                    onToggleLocation = { locationPermissionGranted = !locationPermissionGranted },
                    onToggleNotification = { notificationPermissionGranted = !notificationPermissionGranted }
                )
            }
        }

        // Bottom Action Buttons (Next / Get Started)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) {
            if (pageIndex < totalPages - 1) {
                GoldGlowButton(
                    text = LanguageManager.getString("आगे बढ़ें", "Next"),
                    onClick = { pageIndex++ },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "onboarding_next_button"
                )
            } else {
                GoldGlowButton(
                    text = LanguageManager.getString("ऐप शुरू करें", "Enter Revati"),
                    onClick = { onComplete() },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "onboarding_start_button"
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page 0: Welcome & Language Selection
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LanguageSelectOnboardingPage(viewModel: MainViewModel) {
    val currentLang = LanguageManager.currentLanguage

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.background)
                    )
                )
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.revati_logo),
                contentDescription = "Revati Logo",
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Revati",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                fontSize = 32.sp
            )
        )

        Text(
            // Was "वैदिक पंचांग एवं कुण्डली 2026": a hardcoded year, and the only
            // Hindi-only line on a page that is otherwise deliberately bilingual
            // because the user has not chosen a language yet.
            text = LanguageManager.getString(
                "वैदिक पंचांग एवं कुण्डली",
                "Vedic Panchang & Kundali"
            ),
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "भाषा का चयन करें / Select App Language",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.5.sp
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Language Choice Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hindi Option
            val isHindi = currentLang == AppLanguage.HINDI
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.setLanguage(AppLanguage.HINDI) }
                    .border(
                        width = if (isHindi) 2.dp else 1.dp,
                        color = if (isHindi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = if (isHindi) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Hindi",
                        tint = if (isHindi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "हिन्दी",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = "Vedic Hindi",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // English Option
            val isEnglish = currentLang == AppLanguage.ENGLISH
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.setLanguage(AppLanguage.ENGLISH) }
                    .border(
                        width = if (isEnglish) 2.dp else 1.dp,
                        color = if (isEnglish) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = if (isEnglish) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "English",
                        tint = if (isEnglish) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "English",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = "International",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "💡 आप बाद में कभी भी सेटिंग्स से भाषा बदल सकते हैं\n(You can change the language anytime in Settings)",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page 1: Rashi / Zodiac Selection
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RashiSelectOnboardingPage(viewModel: MainViewModel) {
    val horoscopes = viewModel.dailyHoroscopes
    val selectedRashiId by viewModel.selectedRashiId.collectAsState()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = LanguageManager.getString("अपनी राशि चुनें", "Select Your Zodiac Sign (Rashi)"),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 19.sp
            )
        )

        Text(
            text = LanguageManager.getString(
                "दैनिक सटीक राशिफल एवं व्यक्तिगत सूचनाओं हेतु अपनी राशि का चयन करें",
                "Choose your Moon/Sun sign for personalized daily horoscope & transit insights"
            ),
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(horoscopes, key = { it.rashiId }) { item ->
                val isSelected = (item.rashiId == selectedRashiId)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            1.2.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.setDefaultRashi(item.rashiId) }
                        .padding(10.dp)
                        .testTag("onboarding_rashi_${item.rashiId}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = item.symbol,
                            fontSize = 24.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = LanguageManager.getString(item.rashiNameHi.substringBefore(" "), item.rashiNameEn),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page 2: Permissions Setup (Location & Notification)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LocationNotificationOnboardingPage(
    selectedCityName: String,
    locationGranted: Boolean,
    notificationGranted: Boolean,
    onToggleLocation: () -> Unit,
    onToggleNotification: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = LanguageManager.getString("अनुमतियां", "Permissions Setup"),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 19.sp
            )
        )

        Text(
            text = LanguageManager.getString(
                "सटीक सूर्योदय, सूर्यास्त व राहुकाल गणना हेतु स्थान एवं सूचना अनुमति आवश्यक है",
                "Location & notification permissions enable accurate local astronomical timings & alerts."
            ),
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleLocation() }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = LanguageManager.getString("स्थान अनुमति", "GPS Location Access"),
                            style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = LanguageManager.getString("वर्तमान शहर: $selectedCityName (सूर्योदय/राहुकाल)", "Current City: $selectedCityName (Sunrise/Rahu Kaal)"),
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (locationGranted) ShubhSuccessColor else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleNotification() }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = LanguageManager.getString("दैनिक पंचांग व मुहूर्त अलर्ट", "Daily Panchang & Muhurat Alerts"),
                            style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            // Said 06:00 while every default in the code said 07:00,
                            // so onboarding promised one time and the notification
                            // arrived at another. Read from the constant instead.
                            text = LanguageManager.getString(
                                "प्रातः ${com.example.util.NotificationDefaults.formatHour(com.example.util.NotificationDefaults.DAILY_HOUR)} बजे शुभ मुहूर्त व चौघड़िया सूचनाएं",
                                "Morning ${com.example.util.NotificationDefaults.formatHour(com.example.util.NotificationDefaults.DAILY_HOUR)} auspicious timings & festival alerts"
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (notificationGranted) ShubhSuccessColor else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
