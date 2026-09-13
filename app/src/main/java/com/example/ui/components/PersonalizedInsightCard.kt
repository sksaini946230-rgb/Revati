package com.example.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PersonalizedInsightCard(
    insight: String,
    isLoading: Boolean,
    onFetchInsight: () -> Unit,
    modifier: Modifier = Modifier,
    /** Set when the last fetch for THIS sign failed; offers a retry rather than filler. */
    failed: Boolean = false,
    /**
     * True for a user without PRO. The button still shows — it is how they find
     * out the feature exists — and pressing it opens the PRO dialog.
     */
    locked: Boolean = false,
    /** "TODAY", "WEEK" or "MONTH", so the button names what it will fetch. */
    period: String = "TODAY",
    /** The rate limiter's own words when it was the limiter, not the network. */
    failureMessage: String? = null
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = com.example.util.LanguageManager.getString(
                        "आपके लिए विशेष AI विश्लेषण",
                        "AI Personalized Insight"
                    ),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            if (isLoading) {
                AstroLoadingIndicator(modifier = Modifier.padding(8.dp), size = 24.dp)
            } else if (failed) {
                // Saying the reading could not be fetched beats printing generic
                // text under a heading that promises a personalised one.
                Text(
                    text = failureMessage ?: com.example.util.LanguageManager.getString(
                        "अभी विश्लेषण नहीं मिल सका। इंटरनेट जांचें और दोबारा प्रयास करें।",
                        "Could not fetch the insight. Check your connection and try again."
                    ),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                )
                Button(onClick = onFetchInsight, modifier = Modifier.fillMaxWidth()) {
                    Text(com.example.util.LanguageManager.getString("दोबारा प्रयास करें", "Try again"))
                }
            } else if (insight.isEmpty()) {
                Button(
                    onClick = onFetchInsight,
                    modifier = Modifier.fillMaxWidth().testTag("ai_insight_button")
                ) {
                    if (locked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    val label = when (period) {
                        "WEEK" -> com.example.util.LanguageManager.getString("इस सप्ताह का AI विश्लेषण देखें", "Get this week's AI insight")
                        "MONTH" -> com.example.util.LanguageManager.getString("इस महीने का AI विश्लेषण देखें", "Get this month's AI insight")
                        else -> com.example.util.LanguageManager.getString("आज का AI विश्लेषण देखें", "Get today's AI insight")
                    }
                    Text(if (locked) "$label · PRO" else label)
                }
            } else {
                Text(
                    text = insight,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}
