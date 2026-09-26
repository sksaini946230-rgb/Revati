package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.AdminService
import com.example.service.AdminService.AuditEntry
import com.example.service.AdminService.Ban
import com.example.service.AdminService.DirectoryUser
import com.example.ui.MainViewModel
import com.example.ui.components.GlassBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.GoldGlowButton
import com.example.ui.components.SubTabHeader
import com.example.ui.theme.RahuKaalDangerColor
import com.example.ui.theme.ShubhSuccessColor
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.TotpSecret
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/*
 * The admin panel: users, bans with undo, the audit log, and where to send a
 * message to everyone. Used by the owner alone, so English only.
 *
 * Who gets in is decided by firebase/firestore.rules, not here: a verified
 * address on the allowlist, signed in with the authenticator code. This screen
 * walks an allowlisted account through setting the authenticator up, and
 * everything past that is a request the rules may refuse.
 */

private const val CONSOLE_MESSAGING = "https://console.firebase.google.com/project/astroveda-7126b/messaging"
private const val SEARCH_DEBOUNCE_MS = 400L

class AdminViewModel : ViewModel() {

    enum class Gate { CHECKING, ENROL, SIGN_IN_AGAIN, OPEN }

    /** What Undo puts back: the user, and the ban state and reason they had. */
    data class Undo(val user: DirectoryUser, val banned: Boolean, val reason: String)

    private val service = AdminService()

    var gate by mutableStateOf(Gate.CHECKING); private set
    var secret by mutableStateOf<TotpSecret?>(null); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    var search by mutableStateOf(""); private set
    var users by mutableStateOf(emptyList<DirectoryUser>()); private set
    var usersHaveMore by mutableStateOf(false); private set
    var bans by mutableStateOf(emptyMap<String, Ban>()); private set
    var bannedUsers by mutableStateOf(emptyList<DirectoryUser>()); private set
    var audit by mutableStateOf(emptyList<AuditEntry>()); private set
    var auditHasMore by mutableStateOf(false); private set
    var undo by mutableStateOf<Undo?>(null); private set

    private var usersNext: DocumentSnapshot? = null
    private var auditNext: DocumentSnapshot? = null
    private var searchJob: Job? = null

    fun checkGate() = run("open the panel") {
        undo = null
        secret = null
        gate = Gate.CHECKING
        resolveGate()
    }

    private suspend fun resolveGate() {
        gate = when {
            !service.hasAuthenticator() -> Gate.ENROL
            !service.signedInWithCode() -> Gate.SIGN_IN_AGAIN
            else -> {
                refreshAll()
                Gate.OPEN
            }
        }
    }

    fun startEnrolment() = run("start the setup") { secret = service.startEnrolment() }

    /** Hands the secret to the authenticator app on this phone; the key is on screen for another device. */
    fun openInAuthenticator(context: android.content.Context, email: String) {
        val s = secret ?: return
        runCatching { s.openInOtpApp(s.generateQrCodeUrl(email, "Revati")) }
            .onFailure {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(s.generateQrCodeUrl(email, "Revati"))))
                }.onFailure { error = "No authenticator app found. Type the key into one instead." }
            }
    }

    fun finishEnrolment(code: String) = run("finish the setup") {
        val s = secret ?: return@run
        service.finishEnrolment(s, code.trim())
        secret = null
        resolveGate()
    }

    fun searchUsers(query: String) {
        search = query
        searchJob?.cancel()
        searchJob = run("search") {
            delay(SEARCH_DEBOUNCE_MS)
            loadUsers(reset = true)
        }
    }

    fun moreUsers() = run("load more users") { loadUsers(reset = false) }
    fun moreAudit() = run("load more of the log") { loadAudit(reset = false) }
    fun refresh() = run("refresh") { refreshAll() }

    fun setBanned(user: DirectoryUser, banned: Boolean, reason: String) {
        val before = bans[user.uid]
        run(if (banned) "ban ${user.email}" else "unban ${user.email}") {
            service.setBanned(user, banned, reason.trim())
            undo = Undo(user, banned = before != null, reason = before?.reason.orEmpty())
            loadBans()
            loadAudit(reset = true)
        }
    }

    fun undoLast() {
        val last = undo ?: return
        setBanned(last.user, last.banned, last.reason)
        undo = null
    }

    private suspend fun refreshAll() {
        loadUsers(reset = true)
        loadBans()
        loadAudit(reset = true)
    }

    private suspend fun loadUsers(reset: Boolean) {
        val page = service.users(search, if (reset) null else usersNext)
        users = if (reset) page.items else users + page.items
        usersNext = page.next
        usersHaveMore = page.next != null
    }

    private suspend fun loadBans() {
        val list = service.bans()
        bans = list.associateBy { it.uid }
        // A deleted account keeps its ban but loses its directory row.
        bannedUsers = list.map { service.user(it.uid) ?: DirectoryUser(it.uid, "Deleted account", "", null) }
    }

    private suspend fun loadAudit(reset: Boolean) {
        val page = service.audit(if (reset) null else auditNext)
        audit = if (reset) page.items else audit + page.items
        auditNext = page.next
        auditHasMore = page.next != null
    }

    private fun run(what: String, block: suspend () -> Unit): Job = viewModelScope.launch {
        busy = true
        error = null
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = explain(e, what)
        } finally {
            busy = false
        }
    }

    private fun explain(e: Exception, what: String): String = when {
        e is FirebaseAuthInvalidCredentialsException ->
            "That code is not right. Use the one your authenticator app shows now."
        e is FirebaseAuthRecentLoginRequiredException ->
            "Sign out, sign in again, and set the authenticator up straight away."
        e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Could not $what: the server refused. Sign out and sign in again with your code."
        e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Could not $what: no connection."
        else -> {
            com.example.util.AstroAnalytics.recordNonFatal(e, "admin")
            "Could not $what."
        }
    }
}

@Composable
fun AdminScreen(mainViewModel: MainViewModel, onClose: () -> Unit) {
    val vm: AdminViewModel = viewModel()
    val user by mainViewModel.currentUser.collectAsState()
    LaunchedEffect(user?.uid) { vm.checkGate() }
    androidx.activity.compose.BackHandler(onBack = onClose)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Admin",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        vm.error?.let { ErrorLine(it) }
        when (vm.gate) {
            AdminViewModel.Gate.CHECKING -> Busy()
            AdminViewModel.Gate.ENROL -> EnrolCard(vm, email = user?.email.orEmpty())
            AdminViewModel.Gate.SIGN_IN_AGAIN -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Explain(
                    "The authenticator is set up. Sign out and sign in again: after your password " +
                        "or Google, the app asks for the code, and then the panel opens."
                )
                GoldGlowButton(
                    text = "Sign out",
                    onClick = {
                        mainViewModel.signOutFirebase()
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "admin_sign_out"
                )
            }
            AdminViewModel.Gate.OPEN -> Panel(vm)
        }
    }
}

@Composable
private fun EnrolCard(vm: AdminViewModel, email: String) {
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Explain(
            "The panel needs a code from an authenticator app (Google Authenticator or any other), " +
                "on top of your sign-in. A stolen email password alone then cannot open it."
        )
        val secret = vm.secret
        if (secret == null) {
            if (vm.busy) Busy() else GoldGlowButton(
                text = "Set up the authenticator",
                onClick = vm::startEnrolment,
                modifier = Modifier.fillMaxWidth(),
                testTag = "admin_enrol_start"
            )
        } else {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Explain("1. Add Revati to your authenticator app:")
                    TextButton(onClick = { vm.openInAuthenticator(context, email) }) {
                        Text("Open in authenticator app")
                    }
                    Explain("or type this key into it:")
                    Text(
                        text = secret.sharedSecretKey.chunked(4).joinToString(" "),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Explain("2. Enter the 6-digit code it shows:")
                    OutlinedTextField(
                        value = code,
                        onValueChange = { typed -> code = typed.filter(Char::isDigit).take(6) },
                        label = { Text("Code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (vm.busy) Busy() else GoldGlowButton(
                        text = "Confirm",
                        onClick = { if (code.length == 6) vm.finishEnrolment(code) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "admin_enrol_confirm"
                    )
                }
            }
        }
    }
}

@Composable
private fun Panel(vm: AdminViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<DirectoryUser?>(null) }

    SubTabHeader(
        selectedTab = tab,
        tabs = listOf("Users", "Banned (${vm.bans.size})", "Log", "Message"),
        onTabSelected = { tab = it }
    )
    vm.undo?.let { last ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Explain(
                text = "${if (last.banned) "Unbanned" else "Banned"} ${last.user.email}.",
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = vm::undoLast, enabled = !vm.busy) { Text("Undo") }
        }
    }

    when (tab) {
        0 -> Column {
            // Stays mounted through loading and errors: unmounting it on every
            // debounced query would dismiss the keyboard mid-word.
            OutlinedTextField(
                value = vm.search,
                onValueChange = vm::searchUsers,
                label = { Text("Search by email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            UserList(vm, vm.users, vm.usersHaveMore, vm::moreUsers) { selected = it }
        }
        1 -> UserList(vm, vm.bannedUsers, hasMore = false, onMore = {}) { selected = it }
        2 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            if (vm.audit.isEmpty() && !vm.busy) item { Explain("Nothing yet.") }
            items(vm.audit) { entry ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "${entry.action.replaceFirstChar(Char::uppercase)} ${entry.targetEmail}",
                            style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onSurface)
                        )
                        if (entry.detail.isNotBlank()) Explain(entry.detail)
                        Explain("by ${entry.actorEmail} · ${formatTime(entry.at)}")
                    }
                }
            }
            item { LoadMore(vm, vm.auditHasMore, vm::moreAudit) }
        }
        else -> MessageEveryone()
    }

    selected?.let { target ->
        BanDialog(
            user = target,
            ban = vm.bans[target.uid],
            onDismiss = { selected = null },
            onConfirm = { banned, reason ->
                vm.setBanned(target, banned, reason)
                selected = null
            }
        )
    }
}

@Composable
private fun UserList(
    vm: AdminViewModel,
    list: List<DirectoryUser>,
    hasMore: Boolean,
    onMore: () -> Unit,
    onSelect: (DirectoryUser) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        if (list.isEmpty() && !vm.busy) item { Explain("Nobody here.") }
        items(list, key = { it.uid }) { user ->
            val ban = vm.bans[user.uid]
            GlassCard(modifier = Modifier.fillMaxWidth(), onClick = { onSelect(user) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.name.ifBlank { user.email },
                            style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onSurface)
                        )
                        if (user.name.isNotBlank()) Explain(user.email)
                        Explain(if (ban != null) "Reason: ${ban.reason}" else "Last seen ${formatTime(user.lastSeen)}")
                    }
                    StatusBadge(banned = ban != null)
                }
            }
        }
        item { LoadMore(vm, hasMore, onMore) }
    }
}

@Composable
private fun BanDialog(user: DirectoryUser, ban: Ban?, onDismiss: () -> Unit, onConfirm: (Boolean, String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    val banning = ban == null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (banning) "Ban ${user.email}?" else "Unban ${user.email}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (banning) "Their cloud backup stops and the app tells them the account is restricted. " +
                        "Their profiles stay on their phone, and they can still delete the account. You can undo this."
                    else "Banned for: ${ban?.reason}"
                )
                if (banning) OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(500) },
                    label = { Text("Reason (required, goes in the log)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(banning, if (banning) reason else "") },
                enabled = !banning || reason.isNotBlank()
            ) {
                Text(if (banning) "Ban" else "Unban", color = if (banning) RahuKaalDangerColor else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MessageEveryone() {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
        Explain(
            "A notification to everyone who has the app is sent from Firebase Console, not from here: " +
                "on the free plan there is no server to send it from inside the app."
        )
        Explain(
            "1. Open Messaging below.\n" +
                "2. New campaign → Firebase Notification messages.\n" +
                "3. Title and text, then target the app com.aistudio.astroveda.kpvqzm.\n" +
                "4. Scheduling: Now → Review → Publish."
        )
        Explain(
            "It reaches phones on this version or newer where notifications are allowed. " +
                "Earlier versions cannot receive it."
        )
        GoldGlowButton(
            text = "Open Messaging",
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CONSOLE_MESSAGING))) },
            modifier = Modifier.fillMaxWidth(),
            testTag = "admin_open_messaging"
        )
    }
}

/** One look for a user's state everywhere in the panel. */
@Composable
private fun StatusBadge(banned: Boolean) {
    GlassBadge(
        text = if (banned) "Banned" else "Active",
        textColor = if (banned) RahuKaalDangerColor else ShubhSuccessColor,
        borderColor = if (banned) RahuKaalDangerColor else ShubhSuccessColor
    )
}

@Composable
private fun LoadMore(vm: AdminViewModel, hasMore: Boolean, onMore: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        when {
            vm.busy -> CircularProgressIndicator()
            hasMore -> TextButton(onClick = onMore) { Text("Load more") }
        }
    }
}

@Composable
private fun Busy() {
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(color = RahuKaalDangerColor),
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
private fun Explain(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        modifier = modifier
    )
}

private fun formatTime(date: Date?): String =
    date?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(it) } ?: "—"
