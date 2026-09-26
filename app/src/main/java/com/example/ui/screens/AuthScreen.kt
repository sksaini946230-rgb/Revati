package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.GoldGlowButton
import com.example.ui.theme.RahuKaalDangerColor
import com.example.ui.theme.ShubhSuccessColor
import com.example.util.LanguageManager

/**
 * Sign-in. This used to be a gate in front of the whole app; it is now an
 * optional screen opened from Saved Profiles or Settings, because an account
 * buys the user cloud backup and nothing else. Somebody who came to read
 * today's Tithi never has to see it.
 *
 * `onDismiss` is what makes it optional — when it is non-null the screen shows
 * a close control, Back closes it instead of leaving the app, and a successful
 * sign-in closes it too.
 *
 * Email is not decoration. A phone with no Google account on it — common enough
 * for the audience this app is written for — would otherwise have no way to
 * back anything up.
 */
@Composable
fun AuthScreen(viewModel: MainViewModel, onDismiss: (() -> Unit)? = null) {
    val context = LocalContext.current
    val isBusy by viewModel.isAuthInProgress.collectAsState()
    val error by viewModel.authError.collectAsState()
    val notice by viewModel.authNotice.collectAsState()
    val needsCode by viewModel.needsSignInCode.collectAsState()

    var isSignUp by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // A message from the previous mode is stale the moment the user switches.
    LaunchedEffect(isSignUp) { viewModel.clearAuthMessages() }

    // Once the user is signed in there is nothing left on this screen to do.
    val user by viewModel.currentUser.collectAsState()
    LaunchedEffect(user) { if (user != null) onDismiss?.invoke() }

    // Back closes the screen when it is optional; when it is not (no dismiss
    // handler), it leaves the app rather than falling through to whatever was
    // underneath.
    val activity = context as? android.app.Activity
    androidx.activity.compose.BackHandler(enabled = true) {
        when {
            needsCode -> viewModel.clearAuthMessages()
            isSignUp -> isSignUp = false
            onDismiss != null -> onDismiss()
            else -> activity?.finish()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Without this the keyboard covered the password field and the
                // page would not scroll out from under it.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = app.revati.jyotish.R.drawable.revati_logo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Revati",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = LanguageManager.getString(
                    "खाता बनाएं और अपनी कुण्डली व प्रोफाइल क्लाउड पर सुरक्षित रखें — बाकी ऐप बिना खाते के भी चलता है",
                    "An account backs your charts and profiles up to the cloud. The rest of the app works without one."
                ),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                ),
                modifier = Modifier.widthIn(max = 320.dp)
            )

            Spacer(modifier = Modifier.height(22.dp))

            if (needsCode) SignInCodeCard(viewModel, isBusy, error) else GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    // Sign in / Sign up switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        AuthModeTab(
                            label = LanguageManager.getString("साइन इन", "Sign in"),
                            selected = !isSignUp,
                            modifier = Modifier.weight(1f)
                        ) { isSignUp = false }
                        AuthModeTab(
                            label = LanguageManager.getString("नया खाता", "Sign up"),
                            selected = isSignUp,
                            modifier = Modifier.weight(1f)
                        ) { isSignUp = true }
                    }

                    AnimatedVisibility(visible = isSignUp) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(LanguageManager.getString("नाम", "Name")) },
                            singleLine = true,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(LanguageManager.getString("ईमेल", "Email")) },
                        singleLine = true,
                        enabled = !isBusy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(LanguageManager.getString("पासवर्ड", "Password")) },
                        singleLine = true,
                        enabled = !isBusy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation =
                            if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = LanguageManager.getString(
                                        if (showPassword) "पासवर्ड छिपाएं" else "पासवर्ड दिखाएं",
                                        if (showPassword) "Hide password" else "Show password"
                                    ),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isSignUp) {
                        Text(
                            text = LanguageManager.getString(
                                "पासवर्ड कम से कम 6 अक्षर का रखें।",
                                "Use at least 6 characters."
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }

                    error?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = RahuKaalDangerColor,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        )
                    }
                    notice?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ShubhSuccessColor,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        )
                    }

                    if (isBusy) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 46.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.5.dp
                            )
                        }
                    } else {
                        GoldGlowButton(
                            text = if (isSignUp)
                                LanguageManager.getString("खाता बनाएं", "Create account")
                            else
                                LanguageManager.getString("साइन इन करें", "Sign in"),
                            onClick = {
                                if (isSignUp) viewModel.signUpWithEmail(email, password, name)
                                else viewModel.signInWithEmail(email, password)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = if (isSignUp) "auth_sign_up" else "auth_sign_in"
                        )
                    }

                    if (!isSignUp) {
                        Text(
                            text = LanguageManager.getString("पासवर्ड भूल गए?", "Forgot password?"),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = !isBusy) { viewModel.sendPasswordReset(email) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Text(
                    text = LanguageManager.getString("या", "or"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = LanguageManager.getString("Google से जारी रखें", "Continue with Google"),
                style = MaterialTheme.typography.titleSmall.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                    .clickable(enabled = !isBusy) { viewModel.signInWithGoogleGate(context) }
                    .padding(vertical = 14.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = LanguageManager.getString(
                    "जारी रखने पर आप हमारी गोपनीयता नीति और सेवा की शर्तों से सहमत होते हैं।",
                    "By continuing you agree to our Privacy Policy and Terms of Service."
                ),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                ),
                modifier = Modifier.widthIn(max = 320.dp)
            )
        }

        // After the Column, not before it: the Column fills the Box and would
        // otherwise sit on top of this and swallow every tap on it.
        if (onDismiss != null) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .testTag("auth_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = LanguageManager.getString("बंद करें", "Close"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AuthModeTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        ),
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 9.dp)
    )
}

/**
 * The second step for an account with an authenticator. Only admins enrol one,
 * so this is English only and nobody else ever sees it.
 */
@Composable
private fun SignInCodeCard(viewModel: MainViewModel, isBusy: Boolean, error: String?) {
    var code by remember { mutableStateOf("") }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Enter the 6-digit code from your authenticator app.",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
            )
            OutlinedTextField(
                value = code,
                onValueChange = { typed -> code = typed.filter(Char::isDigit).take(6) },
                label = { Text("Code") },
                singleLine = true,
                enabled = !isBusy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("auth_code_field")
            )
            error?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall.copy(color = RahuKaalDangerColor))
            }
            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
            } else {
                GoldGlowButton(
                    text = "Verify",
                    onClick = { viewModel.submitSignInCode(code) },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "auth_code_verify"
                )
            }
        }
    }
}
