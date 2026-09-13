package com.example.ui.screens

import androidx.compose.material.icons.filled.Lock
import com.example.ui.components.AstroDisclaimer
import com.example.ui.components.DisclaimerScope
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.astro.NumerologyValidator
import com.example.ui.MainViewModel
import com.example.ui.components.AstroLoadingIndicator
import com.example.ui.components.GlassBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.GoldGlowButton
import com.example.ui.components.SectionHeader
import com.example.util.LanguageManager

import com.example.ui.components.SubTabHeader
import com.example.ui.AppTab

import com.example.ui.components.OfflineStatusChip

/** A long name is still a name; a thousand characters is a paste accident. */
private const val MAX_NAME_CHARS = 60

/** "YYYY-MM-DD" is ten. Ten more is room to correct a typo, not to paste an essay. */
private const val MAX_DOB_CHARS = 20

@Composable
fun NumerologyScreen(viewModel: MainViewModel) {
    val numData by viewModel.numerologyData.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val isAiOffline by viewModel.isAiOffline.collectAsState()

    var nameInput by remember { mutableStateOf(viewModel.numName.value) }
    var dobInput by remember { mutableStateOf(viewModel.numDob.value) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var dobError by remember { mutableStateOf<String?>(null) }

    var userQuestion by remember { mutableStateOf("") }
    // The question box is PRO only. It stays visible for everyone — the chips
    // and the send button are how a free user finds it — and MainViewModel
    // opens the PRO dialog instead of asking the model.
    val isPro by viewModel.isProUser.collectAsState()

    // "इस वर्ष" rather than a year. The second of these read "2026", which is a
    // suggestion that ages: the same chip would have offered to ask about a year
    // already gone, and the model would have answered about it.
    val quickQuestions = listOf(
        LanguageManager.getString("मेरी नौकरी में पदोन्नति कब होगी?", "When will I get a promotion at work?"),
        LanguageManager.getString("क्या मेरा विवाह इस वर्ष संभव है?", "Is marriage likely for me this year?"),
        LanguageManager.getString("धन लाभ हेतु कौन सा उपाय करें?", "Which remedy helps with money?"),
        LanguageManager.getString("राहु दशा शांति के सरल उपाय क्या हैं?", "Simple remedies to calm a Rahu dasha?")
    )

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
        if (isAiOffline) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    OfflineStatusChip(
                        LanguageManager.getString(
                            "AI उपलब्ध नहीं — शास्त्रीय उत्तर दिखाया जा रहा है",
                            "AI unavailable — showing classical guidance"
                        )
                    )
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                titleHi = "अंक ज्योतिष",
                titleEn = "Vedic Numerology"
            )
        }

        // Numerology Input Form
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val tfColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        errorLabelColor = MaterialTheme.colorScheme.error
                    )

                    Column {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = {
                                // Bounded and single line. Neither field had a
                                // limit, so text pasted into one simply grew it:
                                // 700 characters in the date box made it 800px
                                // tall, pushed the Calculate button off the
                                // screen and left the form looking broken. A
                                // name and an ISO date both have a known size.
                                if (it.length <= MAX_NAME_CHARS) {
                                    nameInput = it
                                    viewModel.numName.value = it
                                    if (nameError != null) {
                                        nameError = NumerologyValidator.validateName(it)
                                    }
                                }
                            },
                            label = { Text(LanguageManager.getString("नाम", "Name")) },
                            isError = (nameError != null),
                            colors = tfColors,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_num_name")
                        )
                        if (nameError != null) {
                            Text(
                                text = nameError!!,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }

                    Column {
                        OutlinedTextField(
                            value = dobInput,
                            onValueChange = {
                                if (it.length <= MAX_DOB_CHARS) {
                                    dobInput = it
                                    viewModel.numDob.value = it
                                    if (dobError != null) {
                                        dobError = NumerologyValidator.validateDob(it)
                                    }
                                }
                            },
                            label = { Text(LanguageManager.getString("जन्म तिथि (YYYY-MM-DD)", "Date of birth (YYYY-MM-DD)")) },
                            isError = (dobError != null),
                            colors = tfColors,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_num_dob")
                        )
                        if (dobError != null) {
                            Text(
                                text = dobError!!,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }

                    GoldGlowButton(
                        text = LanguageManager.getString("अंक ज्योतिष गणना", "Calculate numbers"),
                        onClick = {
                            val valResult = NumerologyValidator.validateInput(nameInput, dobInput)
                            nameError = valResult.nameError
                            dobError = valResult.dobError
                            if (valResult.isValid) {
                                viewModel.calculateNumerology()
                            } else {
                                // A reading left over from the last valid date
                                // would otherwise sit under the error message,
                                // reading as the answer to what was just typed.
                                viewModel.clearNumerology()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "calculate_num_button"
                    )
                }
            }
        }

        // Results only after the user has asked for them. This block used to
        // render unconditionally against a pre-seeded calculation, so a first-time
        // visitor was shown a complete numerology reading for somebody else.
        if (numData != null) {

        // Moolank / Bhagyank / name number as bento tiles. The labels used to
        // be hardcoded Hindi even in English mode.
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedResultCard(
                    scaleKey = "${numData!!.moolank}_${numData!!.rulingPlanetHi}",
                    modifier = Modifier.weight(1f)
                ) {
                    com.example.ui.components.BentoTile(
                        label = LanguageManager.getString("मूलांक", "Moolank"),
                        value = "${numData!!.moolank}",
                        sub = LanguageManager.getString(
                            "स्वामी: ${numData!!.rulingPlanetHi}",
                            "Ruler: ${numData!!.rulingPlanetEn}"
                        ),
                        accent = MaterialTheme.colorScheme.primary,
                        valueSize = 34,
                        minHeight = 112,
                        emphasis = true,
                        singleLineValue = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                AnimatedResultCard(
                    scaleKey = "${numData!!.bhagyank}_${numData!!.nameNumber}",
                    modifier = Modifier.weight(1f)
                ) {
                    com.example.ui.components.BentoTile(
                        label = LanguageManager.getString("भाग्यांक", "Bhagyank"),
                        value = "${numData!!.bhagyank}",
                        sub = LanguageManager.getString(
                            "नाम अंक: ${numData!!.nameNumber}",
                            "Name number: ${numData!!.nameNumber}"
                        ),
                        accent = MaterialTheme.colorScheme.primary,
                        valueSize = 34,
                        minHeight = 112,
                        emphasis = true,
                        singleLineValue = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Moolank Reading Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = LanguageManager.getString("मूलांक ${numData!!.moolank} का फल:", "What Moolank ${numData!!.moolank} means:"),
                        style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = LanguageManager.getString(numData!!.moolankReadingHi, numData!!.moolankReadingEn),
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, lineHeight = 21.sp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = LanguageManager.getString(numData!!.luckyDaysHi, numData!!.luckyDaysEn),
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Normal, fontSize = 13.sp)
                    )
                }
            }
        }

        // AI Vedic Astrologer Chat Header
        }

        item {
            SectionHeader(
                titleHi = "ज्योतिष परामर्श",
                titleEn = "Astrological Guidance",
                subtitleHi = if (isPro) "आपके अंकों पर आधारित व्यक्तिगत उत्तर" else "PRO सुविधा — आपके अंकों पर आधारित व्यक्तिगत उत्तर",
                subtitleEn = if (isPro) "Personal answers based on your numbers" else "PRO feature — personal answers based on your numbers"
            )
        }

        // Quick Sample Questions Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickQuestions, key = { it }) { q ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                            .clickable {
                                userQuestion = q
                                viewModel.askAiAstrologer(q)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = q,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }

        // AI Chat Input Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val tfColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = userQuestion,
                        // Stops at the cap rather than letting someone type past it
                        // and have the tail silently dropped on the way out.
                        onValueChange = {
                            if (it.length <= com.example.util.AiRateLimiter.MAX_QUESTION_CHARS) {
                                userQuestion = it
                            }
                        },
                        label = { Text(LanguageManager.getString("अपना प्रश्न पूछें", "Ask your question")) },
                        colors = tfColors,
                        // The 500-character cap above bounds what is sent; this
                        // bounds what it does to the screen. Without it a long
                        // question grows the field until the send button is
                        // pushed out of view.
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth().testTag("ai_chat_input"),
                        trailingIcon = {
                            if (isAiLoading) {
                                AstroLoadingIndicator(modifier = Modifier.size(20.dp), size = 20.dp, color = MaterialTheme.colorScheme.primary)
                            } else {
                                IconButton(
                                    onClick = {
                                        if (userQuestion.isNotBlank()) {
                                            viewModel.askAiAstrologer(userQuestion)
                                        }
                                    },
                                    modifier = Modifier.testTag("ai_send_button")
                                ) {
                                    Icon(
                                        imageVector = if (isPro) Icons.AutoMirrored.Filled.Send else Icons.Default.Lock,
                                        contentDescription = if (isPro) LanguageManager.getString("भेजें", "Send") else LanguageManager.getString("PRO से खोलें", "Unlock with PRO"),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    )

                    AstroDisclaimer(scope = DisclaimerScope.AI)

                    if (aiResponse.isNotBlank()) {
                        val isAiOffline by viewModel.isAiOffline.collectAsState()
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = LanguageManager.getString("ज्योतिषाचार्य का उत्तर:", "The answer:"), style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Normal, fontSize = 14.sp))
                                }
                                // This used to be an empty 4dp spacer. The flag was
                                // computed in the ViewModel, collected here, and
                                // checked — and then produced four pixels of nothing,
                                // so a canned classical answer appeared under "the
                                // answer" with no sign the model had never been
                                // reached. The chip at the top of the list does say
                                // so, but a user reading the answer is scrolled far
                                // past it.
                                if (isAiOffline) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = LanguageManager.getString(
                                            "AI तक पहुंच नहीं हो सकी — यह शास्त्रीय मार्गदर्शन है, आपके प्रश्न का व्यक्तिगत उत्तर नहीं।",
                                            "The AI could not be reached — this is classical guidance, not a personal answer to your question."
                                        ),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = aiResponse,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, lineHeight = 20.sp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
}

@Composable
fun AnimatedResultCard(
    modifier: Modifier = Modifier,
    scaleKey: Any,
    content: @Composable () -> Unit
) {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(scaleKey) {
        scale.snapTo(0.75f)
        alpha.snapTo(0f)
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350)
            )
        }
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            }
    ) {
        content()
    }
}
