package com.example.ui.screens

import com.example.ui.components.AstroDisclaimer
import com.example.ui.components.DisclaimerScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.TextFieldColors
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.HapticFeedbackConstants
import com.example.data.model.GunaKootDetail
import com.example.data.model.GunaMatchingResult
import com.example.service.MatchingPdfReportService
import com.example.ui.MainViewModel
import com.example.ui.components.AstroLoadingIndicator
import com.example.ui.components.GlassBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.GoldGlowButton
import com.example.ui.components.M3DatePickerDialog
import com.example.ui.components.M3TimePickerDialog
import com.example.ui.components.RecentSearchesComponent
import com.example.ui.components.SectionHeader
import com.example.ui.theme.RahuKaalDangerColor
import com.example.ui.theme.ShubhSuccessColor
import com.example.util.LanguageManager

@Composable
fun MatchingScreen(viewModel: MainViewModel) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val gunaResult by viewModel.gunaResult.collectAsState()

    var boyName by remember { mutableStateOf(viewModel.matchBoyName.value) }
    var boyDob by remember { mutableStateOf(viewModel.matchBoyDob.value) }

    var boyTob by remember { mutableStateOf(viewModel.matchBoyTob.value) }

    var girlName by remember { mutableStateOf(viewModel.matchGirlName.value) }
    var girlDob by remember { mutableStateOf(viewModel.matchGirlDob.value) }
    var girlTob by remember { mutableStateOf(viewModel.matchGirlTob.value) }

    var showBoyDatePicker by remember { mutableStateOf(false) }
    var showGirlDatePicker by remember { mutableStateOf(false) }
    var showBoyTimePicker by remember { mutableStateOf(false) }
    var showGirlTimePicker by remember { mutableStateOf(false) }
    var showPdfRewardPrompt by remember { mutableStateOf(false) }

    if (showPdfRewardPrompt) {
        val ctx = LocalContext.current
        val gunaResultNow = gunaResult
        val isPro by viewModel.isProUser.collectAsState()

        // What this offers, and what it never does.
        //
        // The PDF is the output of a calculation the user has already run, and
        // it has always been free. So this asks — it does not charge. Watching
        // is one tap and skipping is the other, and skipping still produces the
        // report. RewardedAdManager also reports success when it has no ad to
        // show, so a no-fill is invisible here: nobody loses a report because
        // AdMob had nothing to serve.
        //
        // PRO users never see this at all.
        fun makeReport() {
            showPdfRewardPrompt = false
            val r = gunaResultNow ?: return
            val pdfFile = MatchingPdfReportService.generatePdfReport(ctx, r)
            if (pdfFile != null) MatchingPdfReportService.sharePdfReport(ctx, pdfFile)
        }

        if (isPro || !com.example.service.RewardedAdManager.isReady) {
            // Nothing to offer. Do not put a dialog in the way of a button that
            // used to just work.
            LaunchedEffect(Unit) { makeReport() }
        } else {
            AlertDialog(
                onDismissRequest = { showPdfRewardPrompt = false },
                title = {
                    Text(
                        LanguageManager.getString(
                            "PDF रिपोर्ट",
                            "PDF report"
                        )
                    )
                },
                text = {
                    Text(
                        LanguageManager.getString(
                            "एक छोटा विज्ञापन देखकर रिपोर्ट प्राप्त करें, या सीधे रिपोर्ट बनाएं — दोनों में रिपोर्ट पूरी मिलेगी।",
                            "Watch a short ad to support the app, or just make the report — either way you get the full report."
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val activity = ctx as? android.app.Activity
                        if (activity == null) {
                            makeReport()
                        } else {
                            showPdfRewardPrompt = false
                            com.example.service.RewardedAdManager.showForReward(activity) { granted ->
                                if (granted) {
                                    val r = gunaResultNow
                                    if (r != null) {
                                        val f = MatchingPdfReportService.generatePdfReport(ctx, r)
                                        if (f != null) MatchingPdfReportService.sharePdfReport(ctx, f)
                                    }
                                }
                            }
                        }
                    }) {
                        Text(LanguageManager.getString("विज्ञापन देखें", "Watch ad"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { makeReport() }) {
                        Text(LanguageManager.getString("सीधे बनाएं", "Just make it"))
                    }
                }
            )
        }
    }

    if (showBoyTimePicker) {
        M3TimePickerDialog(
            initialTimeString = boyTob.ifBlank { "12:00" },
            onTimeSelected = { selected ->
                boyTob = selected
                viewModel.matchBoyTob.value = selected
            },
            onDismiss = { showBoyTimePicker = false }
        )
    }

    if (showGirlTimePicker) {
        M3TimePickerDialog(
            initialTimeString = girlTob.ifBlank { "12:00" },
            onTimeSelected = { selected ->
                girlTob = selected
                viewModel.matchGirlTob.value = selected
            },
            onDismiss = { showGirlTimePicker = false }
        )
    }

    if (showBoyDatePicker) {
        M3DatePickerDialog(
            initialDateString = boyDob,
            onDateSelected = { selected ->
                boyDob = selected
                viewModel.matchBoyDob.value = selected
            },
            onDismiss = { showBoyDatePicker = false }
        )
    }

    if (showGirlDatePicker) {
        M3DatePickerDialog(
            initialDateString = girlDob,
            onDateSelected = { selected ->
                girlDob = selected
                viewModel.matchGirlDob.value = selected
            },
            onDismiss = { showGirlDatePicker = false }
        )
    }

    Scaffold(
        // The outer Scaffold in MainActivity already applies the status bar inset
        // through TopHeaderBar's statusBarsPadding(). Letting this inner Scaffold
        // apply it again is what put an empty band above every sub-tab row.
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
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
                    titleHi = "गुण मिलान",
                    titleEn = "Kundali Matching (Ashtakoot)"
                )
                val recentSearches by viewModel.recentSearches.collectAsState()
                RecentSearchesComponent(
                    recentSearches = recentSearches.filter { it.type == "MATCHING" },
                    onSearchSelected = { search ->
                        val parts = search.data.split("|")
                        // Six fields since the birth times were added, four in
                        // rows written before that; an older row simply has no
                        // times and keeps the noon default.
                        if (parts.size >= 4) {
                            // Both halves, and that is the point: these four
                            // assignments used to set only the local state, and
                            // calculateGunaMatching() reads the ViewModel. So
                            // tapping a recent search filled the form in front
                            // of the user and then refused it with "कृपया नाम
                            // दर्ज करें" — a name was plainly in the box. The
                            // feature had never worked from this screen.
                            //
                            // KundaliScreen does not have this because it hands
                            // the values to generateKundaliChart as arguments
                            // rather than through the ViewModel's fields.
                            boyName = parts[0]
                            boyDob = parts[1]
                            girlName = parts[2]
                            girlDob = parts[3]
                            viewModel.matchBoyName.value = parts[0]
                            viewModel.matchBoyDob.value = parts[1]
                            viewModel.matchGirlName.value = parts[2]
                            viewModel.matchGirlDob.value = parts[3]
                            boyTob = parts.getOrNull(4)?.takeIf { it.isNotBlank() } ?: "12:00"
                            girlTob = parts.getOrNull(5)?.takeIf { it.isNotBlank() } ?: "12:00"
                            viewModel.matchBoyTob.value = boyTob
                            viewModel.matchGirlTob.value = girlTob
                            viewModel.calculateGunaMatching()
                        }
                    }
                )
            }

            // Boy & Girl Details Form Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val tfColors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Boy Details
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Male, contentDescription = "Boy", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = LanguageManager.getString("वर का विवरण:", "Boy's Details:"),
                                style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            )
                        }

                        NameAndDobFields(
                            name = boyName,
                            onNameChange = {
                                boyName = it
                                viewModel.matchBoyName.value = it
                            },
                            nameLabel = LanguageManager.getString("वर का नाम", "Boy Name"),
                            dob = boyDob,
                            onDobChange = {
                                boyDob = it
                                viewModel.matchBoyDob.value = it
                            },
                            dobContentDescription = LanguageManager.getString(
                                "वर की जन्म तिथि चुनें", "Select boy's date of birth"
                            ),
                            iconTint = MaterialTheme.colorScheme.primary,
                            onDobClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                showBoyDatePicker = true
                            },
                            colors = tfColors,
                            nameTestTag = "input_boy_name",
                            dobTestTag = "input_boy_dob",
                            tob = boyTob,
                            tobContentDescription = LanguageManager.getString(
                                "वर का जन्म समय चुनें", "Select boy's time of birth"
                            ),
                            onTobClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                showBoyTimePicker = true
                            },
                            tobTestTag = "input_boy_tob"
                        )

                        // Girl Details
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Female, contentDescription = "Girl", tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = LanguageManager.getString("कन्या का विवरण:", "Girl's Details:"),
                                style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            )
                        }

                        NameAndDobFields(
                            name = girlName,
                            onNameChange = {
                                girlName = it
                                viewModel.matchGirlName.value = it
                            },
                            nameLabel = LanguageManager.getString("कन्या का नाम", "Girl Name"),
                            dob = girlDob,
                            onDobChange = {
                                girlDob = it
                                viewModel.matchGirlDob.value = it
                            },
                            dobContentDescription = LanguageManager.getString(
                                "कन्या की जन्म तिथि चुनें", "Select girl's date of birth"
                            ),
                            iconTint = MaterialTheme.colorScheme.secondary,
                            onDobClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                showGirlDatePicker = true
                            },
                            colors = tfColors,
                            nameTestTag = "input_girl_name",
                            dobTestTag = "input_girl_dob",
                            tob = girlTob,
                            tobContentDescription = LanguageManager.getString(
                                "कन्या का जन्म समय चुनें", "Select girl's time of birth"
                            ),
                            onTobClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                showGirlTimePicker = true
                            },
                            tobTestTag = "input_girl_tob"
                        )

                        val isCalculating by viewModel.isCalculating.collectAsState()
                        val inputError by viewModel.matchingInputError.collectAsState()
                        inputError?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = RahuKaalDangerColor,
                                    fontSize = 13.sp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                        }

                        if (isCalculating) {
                            AstroLoadingIndicator()
                        } else {
                            GoldGlowButton(
                                text = LanguageManager.getString("गुण मिलान करें", "Calculate 36 Guna Score"),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.matchBoyName.value = boyName
                                    viewModel.matchBoyDob.value = boyDob
                                    viewModel.matchGirlName.value = girlName
                                    viewModel.matchGirlDob.value = girlDob
                                    viewModel.calculateGunaMatching()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "calculate_guna_button"
                            )
                        }
                    }
                }
            }

            val result = gunaResult
            if (result == null) {
                item { MatchingIntroCard() }
            } else {
                // Score Card Summary
                item { GunaScoreCard(result, onPdfClick = { showPdfRewardPrompt = true }) }

                // Prominent Doshas Highlight Section (Nadi Dosha & Bhakoot Dosha & Mangal Dosha)
                item { MatchingDoshaCards(result) }

                item { AstroDisclaimer(scope = DisclaimerScope.MATCHING) }

                // Birth Attributes Comparison Card
                item { BirthAttributeComparisonCard(result) }

                // Summary Reading Card
                item { MatchingSummaryCard(result) }

                // Ashtakoot 8 Breakdown Table Header
                item {
                    SectionHeader(
                        titleHi = "अष्टकूट 36-गुण विवरण",
                        titleEn = "Ashtakoot 36-Score Breakdown"
                    )
                }

                // The eight koots as a bento. Each was a full-width card with a
                // progress bar and a paragraph, so reading the breakdown meant
                // eight scrolls. Two per row shows the whole score at once; the
                // description moves into the tile's sub-line.
                item { KootBreakdownGrid(result.kootDetails) }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Guna Milan's result sections. They sat inline in MatchingScreen, so every
// keystroke in either name field recomposed the score, all three dosha cards,
// the comparison table and the eight koot tiles below it.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MatchingIntroCard() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = LanguageManager.getString("✨ वैदिक अष्टकूट मिलान", "✨ Vedic Ashtakoot matching — 36 gunas"),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp
                )
            )
            Text(
                text = LanguageManager.getString(
                    "वर एवं कन्या का नाम, जन्म तिथि एवं जन्म समय दर्ज करके 'गुण मिलान करें' पर टैप करें। 8 कूट (वर्ण, वश्य, तारा, योनि, ग्रह मैत्री, गण, भकूट, नाड़ी) एवं नाड़ी व भकूट दोष का सम्पूर्ण विश्लेषण प्राप्त होगा।\n\nजन्म समय न देने पर दोपहर 12:00 माना जाता है। चन्द्रमा लगभग एक दिन में नक्षत्र बदलता है और 36 में से 21 गुण नक्षत्र पर ही आधारित हैं — इसलिए सही समय देने पर मिलान अधिक सटीक होता है।",
                    "Enter both names, dates of birth and times of birth, then tap 'Calculate 36 Guna' for the full Ashtakoot reading with Nadi Dosha, Bhakoot Dosha and Manglik analysis.\n\nWith no time given, noon is assumed. The Moon changes nakshatra roughly once a day and 21 of the 36 gunas are read from the nakshatra, so an accurate birth time makes a real difference to the result."
                ),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun GunaScoreCard(result: GunaMatchingResult, onPdfClick: () -> Unit) {
    val scoreColor = when {
        result.totalObtainedGuna >= 25.0 -> ShubhSuccessColor
        result.totalObtainedGuna >= 18.0 -> MaterialTheme.colorScheme.primary
        else -> RahuKaalDangerColor
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${result.boyName} ♥ ${result.girlName}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${result.totalObtainedGuna} / 36.0",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
            )

            Text(
                text = LanguageManager.getString("कुल प्राप्त गुण", "Total Guna Match Score"),
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (result.totalObtainedGuna / 36.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = scoreColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            GlassBadge(
                text = LanguageManager.getString(result.compatibilityVerdictHi, result.compatibilityVerdictEn),
                backgroundColor = scoreColor.copy(alpha = 0.2f),
                textColor = scoreColor,
                borderColor = scoreColor
            )

            Spacer(modifier = Modifier.height(14.dp))

            val haptic = LocalHapticFeedback.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPdfClick()
                    }
                    .padding(vertical = 12.dp)
                    .testTag("share_pdf_report_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF Report",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = LanguageManager.getString("PDF रिपोर्ट शेयर / प्रिंट करें", "Export / Share PDF Report"),
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchingDoshaCards(result: GunaMatchingResult) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. Nadi Dosha Card (Most Critical)
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (result.hasNadiDosha) 1.5.dp else 1.dp,
                    color = if (result.hasNadiDosha) RahuKaalDangerColor.copy(alpha = 0.8f) else ShubhSuccessColor.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (result.hasNadiDosha) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = "Nadi Dosha",
                            tint = if (result.hasNadiDosha) RahuKaalDangerColor else ShubhSuccessColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = LanguageManager.getString("नाड़ी दोष विचार", "Nadi Dosha Status"),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = if (result.hasNadiDosha) RahuKaalDangerColor else ShubhSuccessColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                    }
                    GlassBadge(
                        text = if (result.hasNadiDosha) LanguageManager.getString("दोष उपस्थित (0/8)", "Dosha Present (0/8)") else LanguageManager.getString("दोष मुक्त (8/8)", "No Dosha (8/8)"),
                        textColor = if (result.hasNadiDosha) RahuKaalDangerColor else ShubhSuccessColor,
                        borderColor = if (result.hasNadiDosha) RahuKaalDangerColor else ShubhSuccessColor
                    )
                }
                Text(
                    text = LanguageManager.getString(result.nadiDoshaStatusHi, result.nadiDoshaStatusEn),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.5.sp, lineHeight = 19.sp)
                )
            }
        }

        // 2. Bhakoot Dosha Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (result.hasBhakootDosha) 1.5.dp else 1.dp,
                    color = if (result.hasBhakootDosha) RahuKaalDangerColor.copy(alpha = 0.8f) else ShubhSuccessColor.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (result.hasBhakootDosha) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = "Bhakoot Dosha",
                            tint = if (result.hasBhakootDosha) RahuKaalDangerColor else ShubhSuccessColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = LanguageManager.getString("भकूट दोष विचार", "Bhakoot Dosha Status"),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = if (result.hasBhakootDosha) RahuKaalDangerColor else ShubhSuccessColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                    }
                    GlassBadge(
                        text = if (result.hasBhakootDosha) LanguageManager.getString("दोष उपस्थित (0/7)", "Dosha Present (0/7)") else LanguageManager.getString("दोष मुक्त (7/7)", "No Dosha (7/7)"),
                        textColor = if (result.hasBhakootDosha) RahuKaalDangerColor else ShubhSuccessColor,
                        borderColor = if (result.hasBhakootDosha) RahuKaalDangerColor else ShubhSuccessColor
                    )
                }
                Text(
                    text = LanguageManager.getString(result.bhakootDoshaStatusHi, result.bhakootDoshaStatusEn),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.5.sp, lineHeight = 19.sp)
                )
            }
        }

        // 3. Mangal Dosha Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Mangal Dosha",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = LanguageManager.getString("मंगल दोष विचार", "Mangal Dosha Analysis"),
                            style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        )
                    }
                }
                Text(
                    text = LanguageManager.getString(result.mangalDoshaStatusHi, result.mangalDoshaStatusEn),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.5.sp, lineHeight = 19.sp)
                )
            }
        }
    }
}

@Composable
private fun BirthAttributeComparisonCard(result: GunaMatchingResult) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = LanguageManager.getString("वर-कन्या ग्रह मिलान विवरण", "Birth Attribute Comparison"),
                style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = LanguageManager.getString("तत्व / गुण", "Attribute"), style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                Text(text = LanguageManager.getString("वर", "Boy"), style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.2f))
                Text(text = LanguageManager.getString("कन्या", "Girl"), style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.2f))
            }

            val attributes = listOf(
                Triple(LanguageManager.getString("चन्द्र राशि", "Moon Sign"), result.boyMoonRashi, result.girlMoonRashi),
                Triple(LanguageManager.getString("नक्षत्र", "Nakshatra"), result.boyNakshatra, result.girlNakshatra),
                Triple(LanguageManager.getString("नाड़ी", "Nadi"), result.boyNadi, result.girlNadi),
                Triple(LanguageManager.getString("गण", "Gana"), result.boyGana, result.girlGana),
                Triple(LanguageManager.getString("योनि", "Yoni"), result.boyYoni, result.girlYoni),
                Triple(LanguageManager.getString("वर्ण", "Varna"), result.boyVarna, result.girlVarna),
                Triple(LanguageManager.getString("वश्य", "Vashya"), result.boyVashya, result.girlVashya)
            )

            attributes.forEach { (attr, boyVal, girlVal) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = attr, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp), modifier = Modifier.weight(1f))
                    Text(text = boyVal, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp), modifier = Modifier.weight(1.2f))
                    Text(text = girlVal, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp), modifier = Modifier.weight(1.2f))
                }
            }
        }
    }
}

@Composable
private fun MatchingSummaryCard(result: GunaMatchingResult) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = LanguageManager.getString("विवाह निष्कर्ष रिपोर्ट:", "Summary Report & Guidance:"),
                style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            )
            Text(
                text = LanguageManager.getString(result.summaryReadingHi, result.summaryReadingEn),
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, lineHeight = 21.sp)
            )
        }
    }
}

@Composable
private fun KootBreakdownGrid(kootDetails: List<GunaKootDetail>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        kootDetails.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { koot ->
                    val kootColor = when {
                        koot.obtainedPoints == koot.maxPoints -> ShubhSuccessColor
                        koot.obtainedPoints > 0.0 -> MaterialTheme.colorScheme.primary
                        else -> RahuKaalDangerColor
                    }
                    com.example.ui.components.BentoTile(
                        label = LanguageManager.getString(koot.kootNameHi, koot.kootNameEn),
                        value = "${koot.obtainedPoints} / ${koot.maxPoints}",
                        // The descriptions end in "(Boy: X, Girl: Y)",
                        // which the comparison table above already
                        // shows; in a tile it only ate the line.
                        sub = LanguageManager.getString(koot.descriptionHi, koot.descriptionEn)
                            .substringBefore(" (").trim(),
                        accent = kootColor,
                        valueSize = 20,
                        minHeight = 108,
                        singleLineValue = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
                // Eight is even, but guard a lone tile anyway.
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * One person's name and date of birth, side by side when they fit and stacked
 * when they do not.
 *
 * The date used to sit in a `weight(1f)` half of the row at the default 16sp,
 * and "1995-06-15" did not fit: on a 360dp phone it wrapped to a second line,
 * so the field a user had just filled read "1995-06" over "-15". Both blocks
 * carried the same code, so the bug existed twice and had to be fixed twice.
 *
 * The threshold is measured rather than guessed. A dp cut-off cannot see the
 * font scale, and 320dp at 1.3x text is a real device: ask how wide the widest
 * date this field will ever hold actually renders, and compare that with the
 * room the narrow half leaves once the gap, the field's own padding and the
 * calendar icon are taken out. Sized this way it stays right when the user
 * turns text size up, which a fixed breakpoint does not.
 *
 * This mirrors what [com.example.ui.screens.KundaliScreen] does for its own
 * date and time pair; the two screens had the same clipping for the same
 * reason, and only one of them had been fixed.
 */
@Composable
private fun NameAndDobFields(
    name: String,
    onNameChange: (String) -> Unit,
    nameLabel: String,
    dob: String,
    onDobChange: (String) -> Unit,
    dobContentDescription: String,
    iconTint: Color,
    onDobClick: () -> Unit,
    colors: TextFieldColors,
    nameTestTag: String,
    dobTestTag: String,
    tob: String,
    tobContentDescription: String,
    onTobClick: () -> Unit,
    tobTestTag: String
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val measurer = rememberTextMeasurer()
        val valueStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
        val widestDate = measurer.measure(AnnotatedString("0000-00-00"), valueStyle).size.width
        // Half the row, less the gap, less the 66dp the field's own horizontal
        // padding and the trailing icon take. The same arithmetic as
        // KundaliScreen's date/time pair, deliberately: the two screens hold
        // the same ten-character date in the same kind of field, so they should
        // reach the same answer about whether it fits.
        //
        // 66dp is deliberately generous. Measured, the value needs 90dp at the
        // default text size and 112dp at 1.3x, and the true padding-plus-icon
        // overhead is somewhere between those two brackets — a tighter number
        // would let 360dp phones keep the pair side by side, but if it were too
        // tight by even a few dp the result is a clipped birth date, which is
        // the bug this exists to prevent. One extra row on a scrolling form is
        // the cheaper way to be wrong.
        val roomForDate = with(LocalDensity.current) {
            (((maxWidth - 8.dp) / 2) - 66.dp).toPx()
        }
        val stack = widestDate > roomForDate

        val nameField = @Composable { modifier: Modifier ->
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(nameLabel, maxLines = 1, softWrap = false) },
                colors = colors,
                singleLine = true,
                modifier = modifier.testTag(nameTestTag)
            )
        }

        val dobField = @Composable { modifier: Modifier ->
            Box(modifier = modifier) {
                OutlinedTextField(
                    value = dob,
                    onValueChange = onDobChange,
                    readOnly = true,
                    // "जन्म तिथि" does not fit this narrow field and wrapped to
                    // two lines, making the box taller than the name field
                    // beside it — only in Hindi.
                    label = {
                        Text(
                            LanguageManager.getString("तिथि", "DOB"),
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    placeholder = { Text("YYYY-MM-DD", maxLines = 1, softWrap = false) },
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = dobContentDescription,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = colors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag(dobTestTag)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(onClick = onDobClick)
                )
            }
        }

        val tobField = @Composable { modifier: Modifier ->
            Box(modifier = modifier) {
                OutlinedTextField(
                    value = tob,
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text(
                            LanguageManager.getString("समय", "Time"),
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    placeholder = { Text("12:00", maxLines = 1, softWrap = false) },
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = tobContentDescription,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = colors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag(tobTestTag)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(onClick = onTobClick)
                )
            }
        }

        // The name always gets the full width — it is the field most likely to
        // hold something long. The date and time share a row only when the date
        // measurably fits half of one; below that everything stacks. Both
        // branches were tried the other way round first and 320dp at 1.3x text
        // clipped "1995-06-15" to "1995-0", which is the same fault this
        // composable exists to fix.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            nameField(Modifier.fillMaxWidth())
            if (stack) {
                dobField(Modifier.fillMaxWidth())
                tobField(Modifier.fillMaxWidth())
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dobField(Modifier.weight(1f))
                    tobField(Modifier.weight(1f))
                }
            }
        }
    }
}
