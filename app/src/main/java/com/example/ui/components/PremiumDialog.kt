package com.example.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.MainViewModel
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.PrimaryButtonPressed
import com.example.ui.theme.PrimaryButtonText
import com.example.ui.theme.ProBadgeColor
import com.example.ui.theme.ShubhSuccessColor

@Composable
fun PremiumDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val billingError by viewModel.billingErrorMessage.collectAsState()

    // Show toast for error/status messages gracefully
    LaunchedEffect(billingError) {
        billingError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ElevatedSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, ProBadgeColor, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_premium_dialog")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = com.example.util.LanguageManager.getString("प्रीमियम डायलॉग बंद करें", "Close Premium Dialog"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(ProBadgeColor, PrimaryButtonPressed)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = PrimaryButtonText, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Revati PRO Gold",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = ProBadgeColor,
                        fontSize = 22.sp
                    )
                )
                Text(
                    text = com.example.util.LanguageManager.getString(
                        "सम्पूर्ण वैदिक अनुभव अनलॉक करें",
                        "Unlock the complete Vedic experience"
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Every line here used to overstate something. "Swiss Ephemeris
                // Micro-Second Precision" was doubly untrue — this project has never
                // used Swiss Ephemeris, and micro-second precision is not a thing an
                // ephemeris offers. "Gemini 1.5" named a model the app does not run.
                // These are the claims someone reads before paying, so they say what
                // the app actually does.
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeatureRow(com.example.util.LanguageManager.getString(
                        "पूरी तरह विज्ञापन रहित", "Completely ad-free"))
                    // Not "unlimited": AiRateLimiter caps a day at DEFAULT_MAX_PER_DAY,
                    // and a paid claim the app then refuses is the worst kind.
                    FeatureRow(com.example.util.LanguageManager.getString(
                        "AI व्यक्तिगत राशिफल — दैनिक, साप्ताहिक, मासिक", "AI personalised horoscope — daily, weekly, monthly"))
                    FeatureRow(com.example.util.LanguageManager.getString(
                        "अंकों पर आधारित ज्योतिष परामर्श — प्रतिदिन ${com.example.util.AiRateLimiter.DEFAULT_MAX_PER_DAY} प्रश्न तक",
                        "Guidance based on your numbers — up to ${com.example.util.AiRateLimiter.DEFAULT_MAX_PER_DAY} questions a day"))
                    // Only what a free user does not already have. This list also
                    // offered "unlimited saved birth charts" and "the full 120-year
                    // Vimshottari Dasha" — both free for everyone, with no limit in
                    // the code — and "PDF reports", which are free too, behind an
                    // optional ad. Promising someone paying ₹199 what they already
                    // had is the claim a refund request quotes back.
                    FeatureRow(com.example.util.LanguageManager.getString(
                        "PDF रिपोर्ट बिना विज्ञापन के", "PDF reports without the ad"))
                }
                Spacer(modifier = Modifier.height(18.dp))

                // The price comes from Play, never from a string in here. This
                // dialog once said "₹199/वर्ष" while Settings said "₹99/माह" —
                // two prices for one subscription, neither of them real.
                val productDetails by viewModel.subscriptionProductDetails.collectAsState()
                val phase = productDetails
                    ?.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.firstOrNull()
                val priceLabel = phase?.formattedPrice
                val periodLabel = phase?.billingPeriod?.let { billingPeriodLabel(it) }

                if (priceLabel != null) {
                    // Price, period and the fact that it renews — Play requires
                    // the renewal to be disclosed before the purchase sheet, and
                    // a subscription price with no period attached is meaningless.
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = priceLabel,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (periodLabel != null) {
                                Text(
                                    text = " / $periodLabel",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = com.example.util.LanguageManager.getString(
                                "स्वतः नवीनीकरण। Play Store से कभी भी रद्द कर सकते हैं।",
                                "Renews automatically. Cancel any time in the Play Store."
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    // Play has not answered — the product is not live yet, or the
                    // device is offline. Saying so beats an empty space, and
                    // beats inventing a number.
                    Text(
                        text = com.example.util.LanguageManager.getString(
                            "मूल्य Play Store से लिया जाता है। कीमत वहीं दिखेगी।",
                            "The price comes from the Play Store and is shown there."
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                GoldGlowButton(
                    text = if (priceLabel != null) {
                        com.example.util.LanguageManager.getString(
                            "PRO लें — $priceLabel", "Get PRO — $priceLabel"
                        )
                    } else {
                        com.example.util.LanguageManager.getString("PRO लें", "Get PRO")
                    },
                    onClick = {
                        val activity = context as? Activity
                        if (activity != null) {
                            viewModel.makePurchase(activity)
                        } else {
                            Toast.makeText(
                                context,
                                com.example.util.LanguageManager.getString(
                                    "भुगतान शुरू नहीं हो सका। ऐप दोबारा खोलकर प्रयास करें।",
                                    "Could not start checkout. Reopen the app and try again."
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "subscribe_pro_gold_button"
                )
            }
        }
    }
}

@Composable
fun FeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ShubhSuccessColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp
            )
        )
    }
}

/**
 * Turns Play's ISO-8601 billing period ("P1Y", "P1M", "P1W") into something a
 * reader recognises. Anything unexpected is returned as-is rather than guessed
 * at — a wrong period beside a real price is worse than a raw one.
 */
private fun billingPeriodLabel(isoPeriod: String): String = when (isoPeriod) {
    "P1Y" -> com.example.util.LanguageManager.getString("वर्ष", "year")
    "P6M" -> com.example.util.LanguageManager.getString("6 माह", "6 months")
    "P3M" -> com.example.util.LanguageManager.getString("3 माह", "3 months")
    "P1M" -> com.example.util.LanguageManager.getString("माह", "month")
    "P1W" -> com.example.util.LanguageManager.getString("सप्ताह", "week")
    else -> isoPeriod
}
