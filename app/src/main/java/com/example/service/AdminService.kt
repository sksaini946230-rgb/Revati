package com.example.service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.TotpMultiFactorGenerator
import com.google.firebase.auth.TotpSecret
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * The admin panel's reads and writes.
 *
 * Revati is on the Spark plan, so there is no server code: `firebase/firestore.rules`
 * is the server, and every check that matters — the email allowlist, the
 * authenticator code, the audit entry beside each ban — is enforced there.
 * Nothing here decides anything; it only asks, and a refusal comes back as an
 * exception.
 */
class AdminService {

    data class DirectoryUser(val uid: String, val email: String, val name: String, val lastSeen: Date?)
    data class Ban(val uid: String, val reason: String, val at: Date?)
    data class AuditEntry(
        val action: String,
        val actorEmail: String,
        val targetEmail: String,
        val detail: String,
        val at: Date?
    )
    data class Page<T>(val items: List<T>, val next: DocumentSnapshot?)

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /**
     * Whether the rules let this sign-in near the panel: a verified address on
     * the allowlist. The probed document never exists; being allowed to ask is
     * the answer. Asked of the server, because the cache would answer anybody.
     */
    suspend fun isAllowlisted(): Boolean {
        if (auth.currentUser == null) return false
        return runCatching { db.document("admin_gate/probe").get(Source.SERVER).await() }.isSuccess
    }

    fun hasAuthenticator(): Boolean =
        auth.currentUser?.multiFactor?.enrolledFactors.orEmpty()
            .any { it.factorId == TotpMultiFactorGenerator.FACTOR_ID }

    /** Whether this sign-in was completed with the authenticator code — what the rules ask for. */
    suspend fun signedInWithCode(): Boolean {
        val user = auth.currentUser ?: return false
        return user.getIdToken(true).await().signInSecondFactor == TotpMultiFactorGenerator.FACTOR_ID
    }

    suspend fun startEnrolment(): TotpSecret {
        val user = requireNotNull(auth.currentUser)
        return TotpMultiFactorGenerator.generateSecret(user.multiFactor.session.await()).await()
    }

    suspend fun finishEnrolment(secret: TotpSecret, code: String) {
        val user = requireNotNull(auth.currentUser)
        user.multiFactor.enroll(
            TotpMultiFactorGenerator.getAssertionForEnrollment(secret, code),
            "Authenticator"
        ).await()
    }

    /**
     * Newest first; with a search, by email from its start (Firestore has no
     * substring match, and an admin looking somebody up knows how the address
     * begins).
     */
    suspend fun users(search: String, after: DocumentSnapshot?): Page<DirectoryUser> {
        val prefix = search.trim().lowercase()
        var query: Query = if (prefix.isEmpty()) {
            db.collection("directory").orderBy("lastSeenAt", Query.Direction.DESCENDING)
        } else {
            db.collection("directory").orderBy("email").startAt(prefix).endAt(prefix + "")
        }
        if (after != null) query = query.startAfter(after)
        val snap = query.limit(PAGE_SIZE).get(Source.SERVER).await()
        return Page(snap.documents.map(::toUser), snap.documents.takeIf { it.size.toLong() == PAGE_SIZE }?.last())
    }

    suspend fun user(uid: String): DirectoryUser? =
        db.document("directory/$uid").get(Source.SERVER).await().takeIf { it.exists() }?.let(::toUser)

    suspend fun bans(): List<Ban> =
        db.collection("bans").whereEqualTo("banned", true).get(Source.SERVER).await().documents.map {
            Ban(it.id, it.getString("reason").orEmpty(), it.getDate("at"))
        }

    suspend fun audit(after: DocumentSnapshot?): Page<AuditEntry> {
        var query = db.collection("admin_audit").orderBy("at", Query.Direction.DESCENDING)
        if (after != null) query = query.startAfter(after)
        val snap = query.limit(PAGE_SIZE).get(Source.SERVER).await()
        val items = snap.documents.map {
            AuditEntry(
                action = it.getString("action").orEmpty(),
                actorEmail = it.getString("actorEmail").orEmpty(),
                targetEmail = it.getString("targetEmail").orEmpty(),
                detail = it.getString("detail").orEmpty(),
                at = it.getDate("at")
            )
        }
        return Page(items, snap.documents.takeIf { it.size.toLong() == PAGE_SIZE }?.last())
    }

    /**
     * Bans or unbans, with the audit entry in the same batch — the rules refuse
     * a ban that arrives without one. A ban is never deleted, only switched
     * off, so undoing it is this same call with `banned = false`.
     */
    suspend fun setBanned(target: DirectoryUser, banned: Boolean, reason: String) {
        val me = requireNotNull(auth.currentUser)
        val entry = db.collection("admin_audit").document()
        db.batch()
            .set(
                entry,
                mapOf(
                    "actor" to me.uid,
                    "actorEmail" to me.email,
                    "action" to if (banned) "ban" else "unban",
                    "targetUid" to target.uid,
                    "targetEmail" to target.email,
                    "detail" to reason,
                    "at" to FieldValue.serverTimestamp()
                )
            )
            .set(
                db.document("bans/${target.uid}"),
                mapOf(
                    "banned" to banned,
                    "reason" to reason,
                    "by" to me.uid,
                    "at" to FieldValue.serverTimestamp(),
                    "auditId" to entry.id
                )
            )
            .commit()
            .await()
    }

    private fun toUser(doc: DocumentSnapshot) = DirectoryUser(
        uid = doc.id,
        email = doc.getString("email").orEmpty(),
        name = doc.getString("name").orEmpty(),
        lastSeen = doc.getDate("lastSeenAt")
    )

    private companion object {
        const val PAGE_SIZE = 25L
    }
}
