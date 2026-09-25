package com.example.ui.screens

import android.content.Intent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.astro.PanchangCalculator
import com.example.astro.RashifalProvider
import com.example.data.model.CityLocation
import com.example.ui.MainViewModel
import com.example.ui.components.AstroLoadingIndicator
import com.example.ui.components.GlassBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalAstroColors
import com.example.ui.theme.PrimaryButtonPressed
import com.example.ui.theme.PrimaryButtonText
import com.example.ui.theme.ProBadgeColor
import com.example.ui.theme.RahuKaalDangerColor
import com.example.ui.theme.ShubhSuccessColor
import com.example.ui.theme.TextPrimary
import com.example.util.AppLanguage
import com.example.util.LanguageManager

const val LOCAL_PRIVACY_POLICY_URL = "file:///android_asset/privacy_policy.html"

/** The address the privacy policy and the Play listing both name. Keep the three in step. */
const val SUPPORT_EMAIL = "supportrevati@gmail.com"
const val LOCAL_TERMS_OF_SERVICE_URL = "file:///android_asset/terms_of_service.html"

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onShowPremiumDialog: () -> Unit = {}
) {
    val context = LocalContext.current

    // Document picker for importing a profiles file. Registered here because a
    // launcher has to be created during composition, not inside a click handler.
    val importProfilesLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importProfiles(context, uri)
    }
    val haptic = LocalHapticFeedback.current

    val selectedCity by viewModel.selectedCity.collectAsState()
    val defaultRashiId by viewModel.defaultRashiId.collectAsState()
    val dailyRahuKaalAlert by viewModel.dailyRahuKaalAlert.collectAsState()
    val festivalRemindersAlert by viewModel.festivalRemindersAlert.collectAsState()
    val muhuratAlertsEnabled by viewModel.muhuratAlertsEnabled.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val backupStatusMessage by viewModel.backupStatusMessage.collectAsState()

    var showRashiDialog by remember { mutableStateOf(false) }
    var showLocationModal by remember { mutableStateOf(false) }
    var isRefreshingLocation by remember { mutableStateOf(false) }

    // What this button used to do, in full: set a spinner, wait a second, and
    // show "Location refreshed: <the city you already had>". Its own comment
    // said `// Simulate location refresh`. It never touched location services,
    // so it always reported success and the name it reported was whatever was
    // already set — which is exactly what a working refresh looks like from the
    // outside. The app declares ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION
    // and MainViewModel.detectGPSLocation is a complete, correct implementation
    // — fused provider, geocoder, last-known fallback, permission check. Nobody
    // had connected the two.
    //
    // Found by trying to put a city back after changing it, on a device where
    // the permission had never been granted: the button said it had refreshed
    // and nothing happened. PanchangScreen has had the real flow, launcher and
    // all, the whole time; this is the same flow.
    fun onGpsResult(success: Boolean) {
        isRefreshingLocation = false
        Toast.makeText(
            context,
            if (success) {
                LanguageManager.getString("स्थान अद्यतन हुआ", "Location updated")
            } else {
                LanguageManager.getString(
                    "स्थान प्राप्त नहीं हो सका — शहर खोज कर चुनें",
                    "Could not get your location — search for a city instead"
                )
            },
            Toast.LENGTH_SHORT
        ).show()
    }

    val locationPermissionsLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val ok = granted[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            isRefreshingLocation = true
            viewModel.detectGPSLocation(context) { success -> onGpsResult(success) }
        } else {
            Toast.makeText(
                context,
                LanguageManager.getString("अनुमति अस्वीकार कर दी गई", "Permission denied"),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun requestGpsLocation() {
        val hasPermission =
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            isRefreshingLocation = true
            viewModel.detectGPSLocation(context) { success -> onGpsResult(success) }
        } else {
            locationPermissionsLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    var showAboutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var webViewUrlToOpen by remember { mutableStateOf<String?>(null) }
    var webViewTitle by remember { mutableStateOf("") }

    LaunchedEffect(backupStatusMessage) {
        backupStatusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearBackupStatusMessage()
        }
    }

    val horoscopes = remember { RashifalProvider.getDailyHoroscope() }
    val currentRashi = horoscopes.find { it.rashiId == defaultRashiId } ?: horoscopes.first()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                titleHi = "ऐप सेटिंग्स",
                titleEn = "Settings",
                subtitleHi = LanguageManager.getString("आपकी पसंद एवं प्राथमिकताओं का अनुकूलन करें", "Customise your preferences and alerts"),
                subtitleEn = "Customize your preferences & notifications"
            )
        }

        // 7. Upgrade to PRO Premium Banner
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onShowPremiumDialog()
                    }
                    .testTag("settings_pro_card")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // weight(1f) is load-bearing. Without it this inner Row is
                        // measured against the full width — its Column child has
                        // weight(1f) and expands into all of it — leaving the PRO
                        // badge beside it exactly zero pixels. On a 320dp phone the
                        // badge simply was not drawn.
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            listOf(ProBadgeColor, PrimaryButtonPressed)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "PRO",
                                    tint = PrimaryButtonText,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = LanguageManager.getString("Revati PRO", "Revati PRO"),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = ProBadgeColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                )
                                Text(
                                    text = LanguageManager.getString("प्रीमियम वैदिक अनुभव अनलॉक करें", "Unlock the premium Vedic experience"),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        // Sits beside a long title; without this it squeezed to "P …".
                        GlassBadge(
                            text = "PRO",
                            backgroundColor = ProBadgeColor.copy(alpha = 0.15f),
                            textColor = ProBadgeColor,
                            borderColor = ProBadgeColor
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // The same four as PremiumDialog, in the same words. This
                        // list promised the 120-year dasha and the matching PDF, which
                        // every user already has, and "unlimited" guidance, which the
                        // daily cap makes untrue.
                        ProBenefitRow(text = LanguageManager.getString("🚫 100% विज्ञापन रहित अनुभव", "🚫 Completely ad-free"))
                        ProBenefitRow(text = LanguageManager.getString("✨ AI व्यक्तिगत राशिफल — दैनिक, साप्ताहिक, मासिक", "✨ AI personalised horoscope — daily, weekly, monthly"))
                        ProBenefitRow(text = LanguageManager.getString(
                            "🔢 अंकों पर आधारित ज्योतिष परामर्श — प्रतिदिन ${com.example.util.AiRateLimiter.DEFAULT_MAX_PER_DAY} प्रश्न तक",
                            "🔢 Guidance based on your numbers — up to ${com.example.util.AiRateLimiter.DEFAULT_MAX_PER_DAY} questions a day"))
                        ProBenefitRow(text = LanguageManager.getString("💖 PDF रिपोर्ट बिना विज्ञापन के", "💖 PDF reports without the ad"))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(ProBadgeColor, PrimaryButtonPressed)
                                )
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Carry the price here too, from Play. Sending someone to
                        // a dialog to find out what it costs is a wasted step.
                        val proPrice by viewModel.subscriptionProductDetails.collectAsState()
                        val proPriceLabel = proPrice
                            ?.subscriptionOfferDetails
                            ?.firstOrNull()
                            ?.pricingPhases
                            ?.pricingPhaseList
                            ?.firstOrNull()
                            ?.formattedPrice

                        Text(
                            text = if (proPriceLabel != null) {
                                LanguageManager.getString(
                                    "अभी PRO अपग्रेड करें — $proPriceLabel",
                                    "Upgrade to PRO — $proPriceLabel"
                                )
                            } else {
                                LanguageManager.getString("अभी PRO अपग्रेड करें", "Upgrade to PRO")
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = PrimaryButtonText,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp
                            ),
                            // The banner grows to fit rather than clipping a
                            // price — a truncated amount is worse than none.
                            maxLines = 2,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // 1. Language Toggle (ENG / हिं Switch)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Language Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = LanguageManager.getString("भाषा", "App Language"),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = if (LanguageManager.currentLanguage == AppLanguage.HINDI) "वर्तमान: हिन्दी (Hindi)" else "Current: English",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.toggleLanguage()
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("settings_language_toggle")
                        ) {
                            Text(
                                text = if (LanguageManager.currentLanguage == AppLanguage.HINDI) "English ⇄" else "हिन्दी ⇄",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. Default Rashi Selector Section
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = currentRashi.symbol, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = LanguageManager.getString("मुख्य राशि", "Default Rashi"),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = LanguageManager.getString(
                                    "${currentRashi.rashiNameHi} • स्वामी: ${currentRashi.rulerHi}",
                                    "${currentRashi.rashiNameEn} • Lord: ${currentRashi.rulerEn}"
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(16.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showRashiDialog = true
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("settings_change_rashi_button")
                    ) {
                        Text(
                            text = LanguageManager.getString("बदलें", "Change"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }

        // 3. Location Settings (Saved City + Refresh GPS + Manual Fallback)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // The name yields, the buttons do not.
                        //
                        // Neither side had a weight, so a long city name took
                        // whatever width it wanted and the two buttons were
                        // squeezed into what was left: with the location set by
                        // GPS to "Chak Basantpur (Uttar Pradesh)", "Search city"
                        // rendered as a single column of letters, one per line.
                        // A button is a fixed thing and a place name is not, so
                        // the name is the one that has to give.
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = LanguageManager.getString("स्थान", "Location"),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${selectedCity.nameLocal} (${selectedCity.state})",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    ),
                                    // Two lines, then an ellipsis. A place name
                                    // is worth wrapping for; it is not worth
                                    // pushing the buttons off the card for.
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Refresh Location Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        requestGpsLocation()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("settings_refresh_location")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isRefreshingLocation) {
                                        AstroLoadingIndicator(size = 12.dp, strokeWidth = 1.5.dp)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "GPS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            // Manual City Search Fallback Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showLocationModal = true
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("settings_manual_city_button")
                            ) {
                                Text(
                                    text = LanguageManager.getString("शहर खोजें", "Search city"),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp
                                    ),
                                    // Belt and braces: the weight above stops
                                    // this being squeezed, and this stops it
                                    // stacking one letter per line if it ever is.
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Time Format Toggle (12-hour vs 24-hour)
                    val use24HourFormat by viewModel.use24HourFormat.collectAsState()

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LanguageManager.getString("समय प्रारूप", "Time format"),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            )
                            Text(
                                text = if (use24HourFormat) LanguageManager.getString("24-घंटे प्रारूप (उदा. 18:30)", "24-hour (e.g. 18:30)") else LanguageManager.getString("12-घंटे AM/PM प्रारूप (उदा. 06:30 PM)", "12-hour AM/PM (e.g. 06:30 PM)"),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = !use24HourFormat,
                                onClick = {
                                    if (use24HourFormat) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.toggle24HourFormat()
                                    }
                                },
                                label = { Text("12h", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("settings_time_format_12h")
                            )
                            FilterChip(
                                selected = use24HourFormat,
                                onClick = {
                                    if (!use24HourFormat) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.toggle24HourFormat()
                                    }
                                },
                                label = { Text("24h", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("settings_time_format_24h")
                            )
                        }
                    }
                }
            }
        }

        // 4. Notifications Toggles (Daily Rahu Kaal Alert & Festival Reminders)
        item {
            val notificationHour by viewModel.notificationHour.collectAsState()
            val notificationMinute by viewModel.notificationMinute.collectAsState()
            
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = LanguageManager.getString("अधिसूचनाएं", "Notifications"),
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // Rahu Kaal Alert Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LanguageManager.getString("दैनिक राहुकाल एवं पंचांग अलर्ट", "Daily Rahu Kaal and Panchang alerts"),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            )
                            val amPm = if (notificationHour >= 12) "PM" else "AM"
                            val displayHour = if (notificationHour % 12 == 0) 12 else notificationHour % 12
                            val timeString = String.format(java.util.Locale.getDefault(), "%02d:%02d %s", displayHour, notificationMinute, amPm)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = LanguageManager.getString("समय: $timeString", "Time: $timeString"),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    modifier = Modifier.clickable {
                                        android.app.TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                viewModel.setNotificationTime(hourOfDay, minute)
                                            },
                                            notificationHour,
                                            notificationMinute,
                                            false
                                        ).show()
                                    }.padding(vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = LanguageManager.getString("(बदलें)", "(Change)"),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                        Switch(
                            checked = dailyRahuKaalAlert,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleRahuKaalAlert()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.surface,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("settings_toggle_rahu_kaal")
                        )
                    }

                    // Festival Reminders Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LanguageManager.getString("त्योहार व व्रत रिमाइंडर", "Festival and fast reminders"),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            )
                            Text(
                                text = LanguageManager.getString("प्रमुख एकादशी, पूर्णिमा व पर्व की पूर्व सूचना", "Advance notice of major Ekadashi, Purnima and festivals"),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        Switch(
                            checked = festivalRemindersAlert,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleFestivalAlert()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.surface,
                                checkedTrackColor = MaterialTheme.colorScheme.secondary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("settings_toggle_festivals")
                        )
                    }

                    // Auspicious Muhurta Alerts Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LanguageManager.getString("शुभ मुहूर्त एवं चौघड़िया अलर्ट", "Muhurta and Choghadiya alerts"),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            )
                            Text(
                                text = LanguageManager.getString("अभिजित मुहूर्त, शुभ चौघड़िया व ब्रह्म मुहूर्त की दैनिक अलर्ट", "Daily alerts for Abhijit Muhurta, auspicious Choghadiya and Brahma Muhurta"),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        Switch(
                            checked = muhuratAlertsEnabled,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleMuhuratAlert()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.surface,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("settings_toggle_muhurat_alerts")
                        )
                    }
                }
            }
        }

        // 4.5. Live Vedic Astrological & Astronomical News (Grounded via Google Search)
        item {
            val astroNews by viewModel.astroNews.collectAsState()
            val isNewsLoading by viewModel.isNewsLoading.collectAsState()
            val isNewsOffline by viewModel.isNewsOffline.collectAsState()

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = LanguageManager.getString("ताज़ा खगोलीय व ज्योतिष समाचार", "Latest astronomy and astrology news"),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                )
                                Text(
                                    text = LanguageManager.getString(
                                        "वैदिक ज्योतिष एवं खगोलीय समाचार",
                                        "Vedic Astro & Celestial News"
                                    ),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        GlassBadge(
                            text = LanguageManager.getString(
                                "🔍 Google से सत्यापित",
                                "🔍 Grounded by Google"
                            ),
                            textColor = ShubhSuccessColor,
                            borderColor = ShubhSuccessColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            if (isNewsOffline) {
                                Text(
                                    text = LanguageManager.getString(
                                        "ऑफ़लाइन: AI उपलब्ध नहीं, सहेजे गए समाचार दिखाए जा रहे हैं",
                                        "Offline: AI unavailable, showing saved news"
                                    ),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            if (isNewsLoading) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AstroLoadingIndicator(size = 18.dp, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = LanguageManager.getString("ताज़ा खगोलीय घटनाएँ लाई जा रही हैं...", "Fetching the latest celestial events..."),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            } else {
                                Text(
                                    text = astroNews.ifBlank { LanguageManager.getString("खगोलीय व ज्योतिषीय समाचार उपलब्ध हैं।", "Sky and astrology notes are available.") },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp
                                    )
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.fetchAstroNews()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("settings_refresh_astro_news"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh News",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = LanguageManager.getString("समाचार रीफ्रेश करें", "Refresh news"),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. About Section (Version, About App, Rate on Play Store)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAboutDialog = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LanguageManager.getString("Revati के बारे में", "About Revati"),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                // Read from the build, not typed in. This said "2026.1.0" while the
                                // app shipped as 1.4, so every user saw a version that matched no
                                // release and support could not tell which build they were on.
                                text = LanguageManager.getString(
                                    "संस्करण ${app.revati.jyotish.BuildConfig.VERSION_NAME} (${app.revati.jyotish.BuildConfig.VERSION_CODE}) • Meeus/ELP2000 खगोलीय गणना",
                                    "Version ${app.revati.jyotish.BuildConfig.VERSION_NAME} (${app.revati.jyotish.BuildConfig.VERSION_CODE}) • Meeus/ELP2000 astronomical engine"
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Rate Us Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.showRateUs()
                            }
                            .padding(vertical = 10.dp, horizontal = 12.dp)
                            .testTag("settings_rate_us_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                // Not "5★". Asking for a particular rating is the kind of
                                // manipulation Google Play's ratings policy prohibits, and this
                                // app has already had two policy violations.
                                text = LanguageManager.getString("प्ले स्टोर पर रेटिंग दें", "Rate this app on the Play Store"),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // 6. Legal Section (Privacy Policy & Terms of Service)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = LanguageManager.getString("कानूनी एवं गोपनीयता", "Legal & Privacy"),
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    // Contact support.
                    //
                    // There was no way to reach anybody from inside the app at
                    // all. The address appears in the privacy policy and on the
                    // Play listing, but a user with a wrong Panchang or a lost
                    // profile had nothing to tap. Play expects a support route
                    // and, more to the point, a user who cannot report a problem
                    // leaves a one-star review instead.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val subject = LanguageManager.getString(
                                    "Revati सहायता", "Revati support"
                                )
                                // Device and version go in the body because the
                                // first reply otherwise always asks for them.
                                val body = "\n\n---\nApp: " +
                                    app.revati.jyotish.BuildConfig.VERSION_NAME +
                                    " (" + app.revati.jyotish.BuildConfig.VERSION_CODE + ")" +
                                    "\nAndroid: " + android.os.Build.VERSION.RELEASE +
                                    "\nDevice: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL
                                // Subject and body go in the mailto URI, not only
                                // in the extras. Tested on a real device: Gmail
                                // opened from ACTION_SENDTO takes the recipient
                                // from the URI and silently drops EXTRA_SUBJECT
                                // and EXTRA_TEXT, so the version and device lines
                                // never arrived — which was the whole point of
                                // putting them there. The extras stay as well for
                                // clients that prefer them.
                                val mailto = "mailto:" + SUPPORT_EMAIL +
                                    "?subject=" + android.net.Uri.encode(subject) +
                                    "&body=" + android.net.Uri.encode(body)
                                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                    data = android.net.Uri.parse(mailto)
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
                                    putExtra(android.content.Intent.EXTRA_TEXT, body)
                                }
                                // No mail app is a real possibility on a cheap
                                // phone; failing silently would look like a dead
                                // button, so fall back to saying the address.
                                try {
                                    context.startActivity(intent)
                                } catch (_: android.content.ActivityNotFoundException) {
                                    android.widget.Toast.makeText(
                                        context, SUPPORT_EMAIL, android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp)
                            .testTag("settings_contact_support_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = LanguageManager.getString(
                                    "सहायता से संपर्क करें", "Contact support"
                                ),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Privacy Policy
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    webViewTitle = LanguageManager.getString("गोपनीयता नीति", "Privacy Policy")
                                    webViewUrlToOpen = LOCAL_PRIVACY_POLICY_URL
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                                .testTag("settings_privacy_policy_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PrivacyTip,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = LanguageManager.getString("गोपनीयता नीति", "Privacy Policy"),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        // Terms of Service
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    webViewTitle = LanguageManager.getString("सेवा की शर्तें", "Terms of Service")
                                    webViewUrlToOpen = LOCAL_TERMS_OF_SERVICE_URL
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                                .testTag("settings_terms_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = LanguageManager.getString("सेवा शर्तें", "Terms of Service"),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }

                    // Ad privacy options.
                    //
                    // AdConsentManager has had showPrivacyOptions and
                    // isPrivacyOptionsRequired since UMP was added, and its own doc
                    // comment says "the Settings entry should only appear then" —
                    // but that entry was never built, so neither function had a
                    // single call site. Consent was gathered once on first launch
                    // and could never be changed again. Google's UMP policy requires
                    // an ongoing way to reopen the form wherever the requirement
                    // status is REQUIRED, so serving the EEA/UK without this was a
                    // policy violation as well as a GDPR withdrawal gap.
                    //
                    // UMP reports REQUIRED only where the user's region gives them
                    // that ongoing right, so this row is invisible in India.
                    val privacyActivity = remember(context) { context.findActivity() }
                    if (privacyActivity != null &&
                        com.example.service.AdConsentManager.isPrivacyOptionsRequired(privacyActivity)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    com.example.service.AdConsentManager.showPrivacyOptions(privacyActivity)
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                                .testTag("settings_ad_privacy_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PrivacyTip,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = LanguageManager.getString(
                                        "विज्ञापन गोपनीयता विकल्प",
                                        "Ad privacy options"
                                    ),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. Account & Data Privacy Control (DPDP Act 2023)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = RahuKaalDangerColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = LanguageManager.getString("खाता एवं डेटा नियंत्रण", "Account & data controls"),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = if (currentUser != null) LanguageManager.getString("साइन इन: ${currentUser?.email}", "Signed in: ${currentUser?.email}")
                        else LanguageManager.getString("स्थानीय डेटा", "Stored on this device"),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    // Sign-in is no longer forced at launch, so this is where a
                    // user who wants cloud backup finds it.
                    if (currentUser == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.openAuthScreen()
                                }
                                .padding(vertical = 12.dp, horizontal = 14.dp)
                                .testTag("settings_sign_in_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = LanguageManager.getString(
                                    "साइन इन करें — क्लाउड बैकअप के लिए",
                                    "Sign in — for cloud backup"
                                ),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    // Export / import.
                    //
                    // The Room database is kept out of Auto Backup and out of
                    // device transfer on purpose — birth details are the most
                    // personal thing here. That left anyone who does not sign in
                    // with no way at all to move their profiles to a new phone,
                    // and an exact birth time is not something most people can
                    // look up again. This is the way out that does not make an
                    // account the price of keeping your own data.
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.exportProfiles(context)
                                }
                                .padding(vertical = 12.dp, horizontal = 10.dp)
                                .testTag("settings_export_profiles_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Upload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = LanguageManager.getString("प्रोफ़ाइल भेजें", "Export profiles"),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // Any MIME type: providers disagree about what
                                    // a .json file is, and a filter that looks
                                    // tidy here greys the file out in the picker.
                                    importProfilesLauncher.launch(arrayOf("*/*"))
                                }
                                .padding(vertical = 12.dp, horizontal = 10.dp)
                                .testTag("settings_import_profiles_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = LanguageManager.getString("फ़ाइल से जोड़ें", "Import profiles"),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RahuKaalDangerColor.copy(alpha = 0.1f))
                            .border(1.dp, RahuKaalDangerColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showDeleteAccountDialog = true
                            }
                            .padding(vertical = 12.dp, horizontal = 14.dp)
                            .testTag("settings_delete_account_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = RahuKaalDangerColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentUser != null) LanguageManager.getString("खाता एवं सभी डेटा हटाएं", "Delete account & all data")
                        else LanguageManager.getString("सभी सहेजा गया डेटा हटाएं", "Delete all saved data"),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = RahuKaalDangerColor,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // 2. Rashi Selector Grid Dialog
    if (showRashiDialog) {
        AlertDialog(
            onDismissRequest = { showRashiDialog = false },
            title = {
                Text(
                    text = LanguageManager.getString("अपनी मुख्य राशि चुनें", "Select your default Rashi"),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(modifier = Modifier.height(320.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(horoscopes, key = { it.rashiId }) { rashi ->
                            val isSelected = (rashi.rashiId == defaultRashiId)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.setDefaultRashi(rashi.rashiId)
                                        showRashiDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = rashi.symbol, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = rashi.rashiNameHi.substringBefore(" "),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRashiDialog = false }) {
                    Text(LanguageManager.getString("बंद करें", "Close"), color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // 3. Manual Location Search Fallback Dialog
    if (showLocationModal) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredCities = remember(searchQuery) {
            PanchangCalculator.popularCities.filter {
                it.cityName.contains(searchQuery, ignoreCase = true) ||
                        it.cityNameHindi.contains(searchQuery) ||
                        it.state.contains(searchQuery, ignoreCase = true)
            }
        }

        AlertDialog(
            onDismissRequest = { showLocationModal = false },
            title = {
                Text(
                    text = LanguageManager.getString("शहर खोजें एवं चुनें", "Search and select a city"),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(modifier = Modifier.height(340.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(LanguageManager.getString("शहर का नाम लिखें (जैसे जयपुर, वाराणसी)", "Type a city name (e.g. Jaipur, Varanasi)"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.primary,
                            unfocusedTextColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(filteredCities.size, key = { filteredCities[it].cityName }) { idx ->
                            val city = filteredCities[idx]
                            val isSelected = (city.cityName == selectedCity.cityName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.setCity(city)
                                        showLocationModal = false
                                        Toast.makeText(context, LanguageManager.getString("स्थान सेट किया: ${city.cityNameHindi}", "Location set: ${city.cityName}"), Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${city.cityNameHindi} (${city.cityName})",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    )
                                    Text(
                                        text = "${city.state} • Lat: ${city.latitude}, Lon: ${city.longitude}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLocationModal = false }) {
                    Text(LanguageManager.getString("रद्द करें", "Cancel"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // 5. About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                // "Revati 2026" — the year was a literal in an About box that
                // already shows the real version number below it.
                Text(text = "Revati", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = LanguageManager.getString(
                        "Revati (वैदिक पंचांग एवं कुण्डली) एक सटीक पंचांग ऐप है। ग्रहों की गणना Meeus/ELP2000 खगोलीय एल्गोरिदम एवं लाहिड़ी अयनांश पर आधारित है, और पूरी तरह आपके फ़ोन पर ही होती है — इंटरनेट की आवश्यकता नहीं। इसमें 12 राशियां, चौघड़िया, राहुकाल, व्रत-त्योहार, कुण्डली, गुण मिलान व ज्योतिष परामर्श शामिल हैं।",
                        "Revati is an accurate Panchang app. Planetary positions are computed with the Meeus/ELP2000 algorithms and the Lahiri ayanamsa, entirely on your phone — no internet needed. It covers all 12 Rashis, Choghadiya, Rahu Kaal, festivals and fasts, birth charts, Guna Milan and astrological guidance."
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(LanguageManager.getString("ठीक है", "OK"), color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // 6. Legal In-App WebView Dialog
    if (webViewUrlToOpen != null) {
        AlertDialog(
            onDismissRequest = { webViewUrlToOpen = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = webViewTitle,
                        style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    )
                    IconButton(onClick = { webViewUrlToOpen = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            text = {
                // The page paints itself a beat after the WebView is attached, so
                // whatever is behind it is what the user sees first. Color.White
                // here meant a white flash in front of a dark page in dark mode.
                // These are privacy_policy.html's own two body colours.
                val legalPageBackground =
                    if (LocalAstroColors.current.isLight) Color(0xFFF7FAFC) else Color(0xFF0B0E1A)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(legalPageBackground)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                // These two pages are static HTML in assets/. They have
                                // no scripts and no links, so JavaScript and DOM storage
                                // bought nothing and only widened the attack surface.
                                settings.javaScriptEnabled = false
                                settings.domStorageEnabled = false
                                settings.allowFileAccessFromFileURLs = false
                                settings.allowUniversalAccessFromFileURLs = false
                                settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                                webViewClient = object : WebViewClient() {
                                    // Keep this view pinned to the bundled legal pages;
                                    // anything else opens in the user's browser.
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView,
                                        request: android.webkit.WebResourceRequest
                                    ): Boolean {
                                        val url = request.url.toString()
                                        if (url == LOCAL_PRIVACY_POLICY_URL || url == LOCAL_TERMS_OF_SERVICE_URL) {
                                            return false
                                        }
                                        return try {
                                            ctx.startActivity(
                                                Intent(Intent.ACTION_VIEW, request.url)
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                            true
                                        } catch (_: Exception) {
                                            true
                                        }
                                    }
                                }
                                loadUrl(webViewUrlToOpen ?: LOCAL_PRIVACY_POLICY_URL)
                            }
                        },
                        update = { view ->
                            val url = webViewUrlToOpen ?: LOCAL_PRIVACY_POLICY_URL
                            if (view.url != url) {
                                view.loadUrl(url)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { webViewUrlToOpen = null }) {
                    Text(LanguageManager.getString("बंद करें", "Close"), color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // 7. Delete Account & Data Confirmation Dialog
    if (showDeleteAccountDialog) {
        val isGoogleUser = currentUser != null
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = RahuKaalDangerColor,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (isGoogleUser) LanguageManager.getString("खाता एवं डेटा स्थायी रूप से हटाएँ?", "Delete your account and all data?")
                else LanguageManager.getString("स्थानीय डेटा स्थायी रूप से हटाएँ?", "Delete all data on this device?"),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isGoogleUser) LanguageManager.getString(
                            "क्या आप अपना Revati खाता एवं सभी डेटा स्थायी रूप से हटाना चाहते हैं?\n\n• क्लाउड में सहेजे गए सभी कुण्डली प्रोफाइल\n• इस डिवाइस पर सहेजे गए सभी प्रोफाइल एवं रिपोर्ट\n• गूगल साइन-इन खाता एवं क्रेडेंशियल्स\n\nयह प्रक्रिया पूरी तरह से स्थायी है।",
                            "Permanently delete your Revati account and everything in it?\n\n• Every chart profile saved in the cloud\n• Every profile and report saved on this device\n• Your Google sign-in and credentials\n\nThis cannot be undone."
                        ) else LanguageManager.getString(
                            "क्या आप इस डिवाइस पर सहेजे गए सभी कुण्डली प्रोफाइल और रिपोर्ट स्थायी रूप से हटाना चाहते हैं?\n\nयह प्रक्रिया पूरी तरह से स्थायी है।",
                            "Permanently delete every chart profile and report saved on this device?\n\nThis cannot be undone."
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        if (isGoogleUser) {
                            viewModel.deleteAccountAndData()
                        } else {
                            viewModel.deleteLocalDataOnly()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RahuKaalDangerColor)
                ) {
                    Text(LanguageManager.getString("हां, हटाएँ", "Yes, delete"), color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text(LanguageManager.getString("रद्द करें", "Cancel"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun ProBenefitRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp
            )
        )
    }
}

/**
 * Walks up the ContextWrapper chain to the hosting Activity.
 *
 * LocalContext.current inside Compose is usually a ContextWrapper rather than the
 * Activity itself, so a plain `as? Activity` returns null on some devices and the
 * ad privacy row would silently never appear — which is the exact bug this row
 * exists to fix.
 */
private tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
