package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GlassCardBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * A birth-place option from the geocoder, carrying the coordinates the chart needs.
 *
 * The autocomplete used to be a plain List<String>: the geocoder returned latitude
 * and longitude with every result and the code kept only the display label, after
 * which the calculator fell back to Jaipur for everyone.
 */
data class PlaceSuggestion(
    val label: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * The birth-place field with geocoder suggestions — one copy for the Kundali form
 * and the Add / Edit profile dialogs.
 *
 * Those dialogs used to take the place as bare text and store the Settings city's
 * coordinates with it, so a profile born in Chennai was saved at Jaipur and every
 * chart made from it later had the wrong Ascendant. A place now counts only once
 * it is picked from the list; typing afterwards calls [onValueChange], and the
 * caller drops the coordinates that came with the old pick.
 */
@Composable
fun BirthPlaceField(
    value: String,
    onValueChange: (String) -> Unit,
    onPicked: (PlaceSuggestion) -> Unit,
    label: String,
    placeholder: String,
    colors: TextFieldColors,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var suggestions by remember { mutableStateOf<List<PlaceSuggestion>>(emptyList()) }
    // Picking fills the field with the label; that must not search again.
    var picked by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(value) {
        if (value.length < 3 || value == picked) {
            suggestions = emptyList()
        } else {
            delay(400)
            suggestions = withContext(Dispatchers.IO) {
                try {
                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(value, 5)
                    addresses?.mapNotNull { addr ->
                        if (!addr.hasLatitude() || !addr.hasLongitude()) return@mapNotNull null
                        val city = addr.locality ?: addr.subAdminArea ?: addr.featureName
                        val label = listOfNotNull(city, addr.adminArea, addr.countryName)
                            .joinToString(", ")
                            .takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        PlaceSuggestion(label, addr.latitude, addr.longitude)
                    }?.distinctBy { it.label } ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            shape = RoundedCornerShape(14.dp),
            colors = colors,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
        if (suggestions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElevatedSurface)
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(12.dp))
            ) {
                suggestions.forEach { suggestion ->
                    Text(
                        text = suggestion.label,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                picked = suggestion.label
                                suggestions = emptyList()
                                onPicked(suggestion)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}
