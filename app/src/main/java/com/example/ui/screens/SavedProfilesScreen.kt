package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.PrimaryButtonText
import com.example.ui.theme.RahuKaalDangerColor
import com.example.ui.theme.ShubhSuccessColor
import com.example.ui.theme.TextPrimary
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
import com.example.data.local.KundaliEntity
import com.example.data.local.SavedReportEntity
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.GlassBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.GoldGlowButton
import com.example.ui.components.EmptyStateComponent
import com.example.ui.components.M3DatePickerDialog
import com.example.ui.components.BirthPlaceField
import com.example.ui.components.M3TimePickerDialog
import com.example.ui.components.SectionHeader
import com.example.util.LanguageManager
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.material.icons.filled.Edit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedProfilesScreen(viewModel: MainViewModel) {
    val profiles by viewModel.savedProfiles.collectAsState()
    val savedReports by viewModel.savedReports.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<KundaliEntity?>(null) }
    var deletingProfile by remember { mutableStateOf<KundaliEntity?>(null) }

    if (showAddDialog) {
        AddProfileDialog(
            onSave = { name, dob, tob, place, lat, lng ->
                viewModel.saveNewProfile(name, dob, tob, place, lat, lng)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingProfile?.let { profile ->
        EditProfileDialog(
            profile = profile,
            onSave = { updatedProfile ->
                viewModel.updateProfile(updatedProfile)
                editingProfile = null
            },
            onDismiss = { editingProfile = null }
        )
    }

    deletingProfile?.let { profile ->
        DeleteProfileConfirmationDialog(
            profile = profile,
            onConfirm = {
                viewModel.deleteProfile(profile)
                deletingProfile = null
            },
            onDismiss = { deletingProfile = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                titleHi = "सहेजी गई कुण्डलियां",
                titleEn = "Saved Birth Profiles",
                subtitleHi = "स्थानीय डेटाबेस - त्वरित लोड के लिए",
                subtitleEn = "Local Room Storage for Quick Access"
            )
        }

        item {
            CloudBackupCard(viewModel = viewModel)
        }

        item {
            GoldGlowButton(
                text = LanguageManager.getString("+ नया प्रोफाइल जोड़ें", "+ Add New Profile"),
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth().testTag("add_new_profile_button")
            )
        }

        if (profiles.isEmpty()) {
            item {
                EmptyStateComponent(
                    icon = Icons.Default.Bookmark,
                    title = LanguageManager.getString("कोई कुण्डली प्रोफाइल सहेजी नहीं गई है।", "No saved birth chart profiles found."),
                    subtitle = LanguageManager.getString("ऊपर दिए गए बटन से नया प्रोफाइल जोड़ें।", "Tap above to add a new birth profile.")
                )
            }
        } else {
            items(profiles, key = { it.id }) { profile ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { dismissValue ->
                        if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                            deletingProfile = profile
                            false
                        } else {
                            false
                        }
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val color = when (dismissState.dismissDirection) {
                            SwipeToDismissBoxValue.EndToStart, SwipeToDismissBoxValue.StartToEnd -> RahuKaalDangerColor.copy(alpha = 0.8f)
                            else -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(color)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Swipe to Delete Profile",
                                tint = PrimaryButtonText
                            )
                        }
                    }
                ) {
                    SavedProfileCard(
                        profile = profile,
                        onOpenKundali = {
                            viewModel.generateKundaliChart(
                                profile.name, profile.dateOfBirth, profile.timeOfBirth, profile.placeOfBirth,
                                profile.latitude, profile.longitude
                            )
                            viewModel.navigateToKundali(0)
                        },
                        onUseForMatchingBoy = {
                            viewModel.matchBoyName.value = profile.name
                            viewModel.matchBoyDob.value = profile.dateOfBirth
                            viewModel.navigateToKundali(1)
                        },
                        onUseForMatchingGirl = {
                            viewModel.matchGirlName.value = profile.name
                            viewModel.matchGirlDob.value = profile.dateOfBirth
                            viewModel.navigateToKundali(1)
                        },
                        onEdit = { editingProfile = profile },
                        onDelete = { deletingProfile = profile }
                    )
                }
            }
        }

        // Saved Astrology Reports Section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(
                titleHi = "सहेजी गई रिपोर्ट",
                titleEn = "Saved Astrology Reports",
                subtitleHi = "ऑफ़लाइन रिपोर्ट संग्रहण",
                subtitleEn = "Offline Stored Astrology Reports"
            )
        }

        if (savedReports.isEmpty()) {
            item {
                EmptyStateComponent(
                    icon = Icons.Default.CloudDownload,
                    title = LanguageManager.getString("कोई रिपोर्ट सहेजी नहीं गई है।", "No saved reports available.")
                )
            }
        } else {
            items(savedReports, key = { it.id }) { report ->
                SavedReportCard(
                    report = report,
                    onDelete = { viewModel.deleteReport(report) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AddProfileDialog(
    onSave: (String, String, String, String, Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var tob by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var coords by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        M3DatePickerDialog(
            initialDateString = dob.ifBlank { "1995-05-15" },
            onDateSelected = { dob = it },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        M3TimePickerDialog(
            initialTimeString = tob.ifBlank { "12:00" },
            onTimeSelected = { tob = it },
            onDismiss = { showTimePicker = false }
        )
    }

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Text(
                text = LanguageManager.getString("नया प्रोफाइल जोड़ें", "Add Birth Profile"),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(LanguageManager.getString("नाम", "Name")) },
                    placeholder = { Text(LanguageManager.getString("उदा. राहुल शर्मा", "e.g. Rahul Sharma"), fontSize = 13.sp) },
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth().testTag("add_profile_name")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = dob,
                            onValueChange = { dob = it },
                            readOnly = true,
                            label = { Text(LanguageManager.getString("जन्म तिथि", "DOB")) },
                            placeholder = { Text("YYYY-MM-DD", fontSize = 13.sp) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "DOB",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            colors = tfColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = tob,
                            onValueChange = { tob = it },
                            readOnly = true,
                            label = { Text(LanguageManager.getString("समय", "Time")) },
                            placeholder = { Text("HH:mm", fontSize = 13.sp) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "TOB",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            colors = tfColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showTimePicker = true }
                        )
                    }
                }

                BirthPlaceField(
                    value = place,
                    onValueChange = {
                        place = it
                        coords = null
                    },
                    onPicked = { suggestion ->
                        place = suggestion.label
                        coords = suggestion.latitude to suggestion.longitude
                    },
                    label = LanguageManager.getString("जन्म स्थान", "Place of Birth"),
                    placeholder = LanguageManager.getString("उदा. जयपुर, राजस्थान", "e.g. Jaipur, Rajasthan"),
                    colors = tfColors,
                    testTag = "add_profile_place"
                )
                PickPlaceHint(visible = place.isNotBlank() && coords == null)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val picked = coords
                    if (name.isNotBlank() && dob.isNotBlank() && tob.isNotBlank() && place.isNotBlank() && picked != null) {
                        onSave(name, dob, tob, place, picked.first, picked.second)
                    }
                },
                enabled = name.isNotBlank() && dob.isNotBlank() && tob.isNotBlank() && place.isNotBlank() && coords != null
            ) {
                Text(LanguageManager.getString("सहेजें", "Save"), color = if (name.isNotBlank() && dob.isNotBlank() && tob.isNotBlank() && place.isNotBlank() && coords != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(LanguageManager.getString("रद्द करें", "Cancel"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun EditProfileDialog(
    profile: KundaliEntity,
    onSave: (KundaliEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var dob by remember { mutableStateOf(profile.dateOfBirth) }
    var tob by remember { mutableStateOf(profile.timeOfBirth) }
    var place by remember { mutableStateOf(profile.placeOfBirth) }
    var coords by remember { mutableStateOf<Pair<Double, Double>?>(profile.latitude to profile.longitude) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        M3DatePickerDialog(
            initialDateString = dob.ifBlank { "1995-05-15" },
            onDateSelected = { dob = it },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        M3TimePickerDialog(
            initialTimeString = tob.ifBlank { "12:00" },
            onTimeSelected = { tob = it },
            onDismiss = { showTimePicker = false }
        )
    }

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Text(
                text = LanguageManager.getString("प्रोफाइल संपादित करें", "Edit Birth Profile"),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(LanguageManager.getString("नाम", "Name")) },
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_name")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = dob,
                            onValueChange = { dob = it },
                            readOnly = true,
                            label = { Text(LanguageManager.getString("जन्म तिथि", "DOB")) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "DOB",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            colors = tfColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = tob,
                            onValueChange = { tob = it },
                            readOnly = true,
                            label = { Text(LanguageManager.getString("समय", "Time")) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "TOB",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            colors = tfColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showTimePicker = true }
                        )
                    }
                }

                BirthPlaceField(
                    value = place,
                    onValueChange = {
                        place = it
                        coords = null
                    },
                    onPicked = { suggestion ->
                        place = suggestion.label
                        coords = suggestion.latitude to suggestion.longitude
                    },
                    label = LanguageManager.getString("जन्म स्थान", "Place of Birth"),
                    placeholder = "",
                    colors = tfColors,
                    testTag = "edit_profile_place"
                )
                PickPlaceHint(visible = place.isNotBlank() && coords == null)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val picked = coords
                    if (name.isNotBlank() && picked != null) {
                        onSave(
                            profile.copy(
                                name = name,
                                dateOfBirth = dob,
                                timeOfBirth = tob,
                                placeOfBirth = place,
                                latitude = picked.first,
                                longitude = picked.second
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && coords != null
            ) {
                Text(LanguageManager.getString("अपडेट करें", "Update"), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(LanguageManager.getString("रद्द करें", "Cancel"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun DeleteProfileConfirmationDialog(
    profile: KundaliEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                text = LanguageManager.getString("प्रोफाइल हटाएं?", "Delete Profile?"),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        },
        text = {
            Text(
                text = LanguageManager.getString(
                    "क्या आप '${profile.name}' का कुण्डली प्रोफाइल हटाना चाहते हैं?\nयह स्थानीय डिवाइस से हटा दिया जाएगा।",
                    "Are you sure you want to delete the birth profile for '${profile.name}'?\nThis will remove it from this device."
                ),
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = RahuKaalDangerColor)
            ) {
                Text(
                    text = LanguageManager.getString("हटाएं", "Delete"),
                    color = PrimaryButtonText,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = LanguageManager.getString("रद्द करें", "Cancel"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
fun SavedProfileCard(
    profile: KundaliEntity,
    onOpenKundali: () -> Unit,
    onUseForMatchingBoy: () -> Unit,
    onUseForMatchingGirl: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val animatedAlpha = remember { Animatable(0f) }
    val animatedTranslationY = remember { Animatable(30f) }

    LaunchedEffect(profile.id) {
        animatedAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )
    }
    LaunchedEffect(profile.id) {
        animatedTranslationY.animateTo(
            targetValue = 0f,
            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animatedAlpha.value
                translationY = animatedTranslationY.value
            }
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                            )
                            Text(
                                text = "${profile.dateOfBirth} | ${profile.timeOfBirth}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                            )
                            Text(
                                text = profile.placeOfBirth,
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_profile_${profile.id}")) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = LanguageManager.getString("प्रोफाइल संपादित करें", "Edit Profile"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_profile_${profile.id}")) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = LanguageManager.getString("प्रोफाइल हटाएं", "Delete Profile"), tint = RahuKaalDangerColor, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassBadge(
                        text = LanguageManager.getString("कुण्डली बनाएं", "Generate Chart"),
                        textColor = MaterialTheme.colorScheme.primary,
                        borderColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onOpenKundali() }
                    )

                    GlassBadge(
                        text = LanguageManager.getString("वर", "Use Boy"),
                        textColor = MaterialTheme.colorScheme.primary,
                        borderColor = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.clickable { onUseForMatchingBoy() }
                    )

                    GlassBadge(
                        text = LanguageManager.getString("कन्या", "Use Girl"),
                        textColor = MaterialTheme.colorScheme.secondary,
                        borderColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.clickable { onUseForMatchingGirl() }
                    )
                }
            }
        }
    }
}

@Composable
fun SavedReportCard(
    report: SavedReportEntity,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(report.createdAt) { dateFormat.format(Date(report.createdAt)) }

    val animatedAlpha = remember { Animatable(0f) }
    val animatedTranslationY = remember { Animatable(30f) }

    LaunchedEffect(report.id) {
        animatedAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )
    }
    LaunchedEffect(report.id) {
        animatedTranslationY.animateTo(
            targetValue = 0f,
            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animatedAlpha.value
                translationY = animatedTranslationY.value
            }
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = report.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 15.sp
                            )
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = report.profileName,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                            )
                            Text(
                                text = "• $formattedDate",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_report_${report.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Report", tint = RahuKaalDangerColor)
                    }
                }

                if (report.summaryText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = report.summaryText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun CloudBackupCard(
    viewModel: MainViewModel
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val backupStatusMessage by viewModel.backupStatusMessage.collectAsState()
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    val animatedAlpha = remember { Animatable(0f) }
    val animatedTranslationY = remember { Animatable(25f) }

    LaunchedEffect(Unit) {
        animatedAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
    }
    LaunchedEffect(Unit) {
        animatedTranslationY.animateTo(
            targetValue = 0f,
            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animatedAlpha.value
                translationY = animatedTranslationY.value
            }
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Cloud Backup",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = LanguageManager.getString("क्लाउड बैकअप", "Firebase Cloud Sync"),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    if (currentUser != null) {
                        GlassBadge(
                            text = LanguageManager.getString("ऑनलाइन", "Online"),
                            backgroundColor = ShubhSuccessColor.copy(alpha = 0.2f),
                            textColor = ShubhSuccessColor
                        )
                    } else {
                        GlassBadge(
                            text = LanguageManager.getString("ऑफ़लाइन", "Offline"),
                            backgroundColor = Color.Gray.copy(alpha = 0.2f),
                            textColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (currentUser != null) {
                    Text(
                        text = LanguageManager.getString(
                            "साइन इन किया: ${currentUser?.displayName ?: currentUser?.email ?: "उपयोगकर्ता"}",
                            "Signed in as: ${currentUser?.displayName ?: currentUser?.email ?: "User"}"
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            GoldGlowButton(
                                text = LanguageManager.getString("बैकअप", "Backup to Cloud"),
                                onClick = { viewModel.backupProfilesToCloud() },
                                modifier = Modifier.fillMaxWidth().testTag("backup_to_cloud_button")
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { viewModel.restoreProfilesFromCloud() },
                                modifier = Modifier.fillMaxWidth().testTag("restore_from_cloud_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = LanguageManager.getString("पुनर्प्राप्त", "Restore"),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showDeleteAccountDialog = true },
                            modifier = Modifier.testTag("delete_account_button")
                        ) {
                            Text(
                                text = LanguageManager.getString("खाता एवं डेटा हटाएँ", "Delete Account & Data"),
                                style = MaterialTheme.typography.labelSmall.copy(color = RahuKaalDangerColor)
                            )
                        }

                        TextButton(
                            onClick = { showSignOutDialog = true },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Text(
                                text = LanguageManager.getString("साइन आउट", "Sign Out"),
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                } else {
                    Text(
                        text = LanguageManager.getString(
                            "अपने कुण्डली प्रोफाइल क्लाउड पर सुरक्षित रखने के लिए साइन इन करें — गूगल से या ईमेल से।",
                            "Sign in — with Google or email — to back up and sync your Kundali profiles to the cloud."
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    // Was a bare Google button; with the sign-in gate gone this is
                    // one of the two ways into the auth screen, and that screen
                    // carries email sign-in as well — a phone with no Google
                    // account on it must still be able to back up.
                    GoldGlowButton(
                        text = LanguageManager.getString("साइन इन करें", "Sign in"),
                        onClick = { viewModel.openAuthScreen() },
                        modifier = Modifier.fillMaxWidth().testTag("google_sign_in_button")
                    )
                }

                if (!backupStatusMessage.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElevatedSurface.copy(alpha = 0.6f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = backupStatusMessage!!,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearBackupStatusMessage() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = LanguageManager.getString("संदेश हटाएं", "Clear Message"),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(
                    text = LanguageManager.getString("साइन आउट करें?", "Sign Out?"),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            },
            text = {
                Text(
                    text = LanguageManager.getString(
                        "क्या आप Google खाते से साइन आउट करना चाहते हैं?\n\nआपके स्थानीय रूप से सहेजे गए सभी कुण्डली प्रोफाइल इस डिवाइस पर सुरक्षित रहेंगे।",
                        "Are you sure you want to sign out of your Google account?\n\nYour locally saved Kundali profiles will remain safe on this device."
                    ),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOutFirebase()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = LanguageManager.getString("साइन आउट", "Sign Out"),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(
                        text = LanguageManager.getString("रद्द करें", "Cancel"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    if (showDeleteAccountDialog) {
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
                    text = LanguageManager.getString("खाता एवं डेटा स्थायी रूप से हटाएँ?", "Delete Account & All Data?"),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            },
            text = {
                Text(
                    text = LanguageManager.getString(
                        "क्या आप अपना Revati खाता एवं सभी डेटा स्थायी रूप से हटाना चाहते हैं?\n\n• क्लाउड में सहेजे गए सभी कुण्डली प्रोफाइल\n• इस डिवाइस पर सहेजे गए सभी प्रोफाइल एवं रिपोर्ट\n• गूगल साइन-इन खाता एवं क्रेडेंशियल्स\n\nयह प्रक्रिया पूरी तरह से स्थायी है।",
                        "Are you sure you want to permanently delete your Revati account and all associated data?\n\n• All cloud-backed-up Kundali profiles (Firestore)\n• All local saved profiles and reports on this device\n• Google sign-in account and credentials\n\nThis action cannot be undone."
                    ),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        viewModel.deleteAccountAndData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RahuKaalDangerColor)
                ) {
                    Text(
                        text = LanguageManager.getString("हां, हटाएँ", "Delete Account"),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text(
                        text = LanguageManager.getString("रद्द करें", "Cancel"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * Save stays off until the place is picked from the list; this says why, in the
 * words the Kundali form already uses for the same refusal.
 */
@Composable
private fun PickPlaceHint(visible: Boolean) {
    if (!visible) return
    Text(
        text = LanguageManager.getString(
            "⚠️ सूची में से जन्म स्थान चुनें — सही लग्न के लिए स्थान के निर्देशांक आवश्यक हैं",
            "⚠️ Pick the birth place from the list — the Ascendant needs the place's coordinates"
        ),
        style = MaterialTheme.typography.bodySmall.copy(color = RahuKaalDangerColor)
    )
}
