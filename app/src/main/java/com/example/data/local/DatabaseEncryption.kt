package com.example.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The Room database is encrypted with SQLCipher, and this is where its key lives
 * and where an existing plaintext database is converted.
 *
 * The database holds names, exact birth times and birthplaces. Backup and device
 * transfer already exclude it, so what this protects against is someone with the
 * phone in hand and a way to read app storage — a rooted device, a forensic
 * image, a debug bridge left on.
 *
 * **The key.** A random 32-byte passphrase, hex-encoded, generated once. It is
 * stored wrapped: AES-GCM under a key held in the Android Keystore, which never
 * leaves the secure hardware, and the wrapped blob sits in its own preference
 * file. That file is excluded from backup for the same reason the database is —
 * a restored blob could not be unwrapped on another device anyway, because the
 * Keystore key is not restored with it.
 *
 * **Losing the key loses the database**, by construction. If the Keystore entry
 * is ever gone while the database is still there (a factory-reset Keystore, an
 * OEM bug), the file cannot be opened. [keyOrNull] returns null in that case
 * rather than inventing a new key over an old file, and the caller opens the
 * database without encryption so that the app at least starts; profiles backed
 * up to the cloud can be restored from there.
 */
object DatabaseEncryption {

    const val DATABASE_NAME = "astroveda_database"
    private const val PREFS = "revati_db_key"
    private const val PREF_WRAPPED = "wrapped_passphrase"
    private const val KEYSTORE_ALIAS = "revati_db_passphrase_wrap"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    /** The first 16 bytes of every plaintext SQLite file. An encrypted one is random there. */
    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    /** True when [file] exists and starts with the plaintext SQLite header. */
    fun isPlaintextSqlite(file: File): Boolean {
        if (!file.isFile || file.length() < SQLITE_HEADER.size) return false
        val head = ByteArray(SQLITE_HEADER.size)
        file.inputStream().use { input ->
            var read = 0
            while (read < head.size) {
                val n = input.read(head, read, head.size - read)
                if (n < 0) return false
                read += n
            }
        }
        return head.contentEquals(SQLITE_HEADER)
    }

    /**
     * The passphrase, creating it on first use. Null when a wrapped passphrase
     * exists but can no longer be unwrapped — see the class note.
     */
    fun keyOrNull(context: Context): ByteArray? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(PREF_WRAPPED, null)
        if (stored != null) {
            return try {
                unwrap(stored).toByteArray(Charsets.US_ASCII)
            } catch (e: Exception) {
                android.util.Log.e("DatabaseEncryption", "passphrase could not be unwrapped", e)
                null
            }
        }
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val passphrase = bytes.joinToString("") { "%02x".format(it) }
        // commit(), not apply(): the database is about to be encrypted under this
        // key, and a write that has not reached disk when the process dies would
        // leave an encrypted file with no key to open it.
        val ok = prefs.edit().putString(PREF_WRAPPED, wrap(passphrase)).commit()
        return if (ok) passphrase.toByteArray(Charsets.US_ASCII) else null
    }

    /**
     * Converts a plaintext database to an encrypted one in place, if there is a
     * plaintext database. Returns true when the file on disk is now encrypted
     * (or there was nothing to convert), false when it is still plaintext.
     *
     * The original is only removed after the encrypted copy has been opened
     * with the key and every table's row count matches. Anything short of that
     * leaves the plaintext file exactly as it was, and the next launch tries
     * again.
     */
    fun encryptExistingIfNeeded(context: Context, passphrase: ByteArray): Boolean {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        if (!dbFile.exists()) return true
        if (!isPlaintextSqlite(dbFile)) return true

        System.loadLibrary("sqlcipher")
        val temp = File(dbFile.parentFile, "$DATABASE_NAME.encrypting")
        temp.delete()
        val key = String(passphrase, Charsets.US_ASCII)

        val counts = HashMap<String, Long>()
        var version = 0
        val plain = net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
            dbFile.absolutePath, "", null,
            // CREATE_IF_NECESSARY is for the ATTACH below, not for this file: an
            // attached database inherits the connection's open flags, and without
            // it the encrypted copy "cannot open file" — which is exactly how the
            // first run on a device failed, safely, with the plaintext untouched.
            net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READWRITE or
                net.zetetic.database.sqlcipher.SQLiteDatabase.CREATE_IF_NECESSARY,
            null
        )
        try {
            // Fold the write-ahead log into the main file first, so the export
            // cannot miss rows that were still sitting in the -wal.
            plain.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
            plain.rawQuery("PRAGMA user_version", null).use { if (it.moveToFirst()) version = it.getInt(0) }
            tableNames(plain).forEach { t ->
                plain.rawQuery("SELECT COUNT(*) FROM \"$t\"", null).use { c ->
                    if (c.moveToFirst()) counts[t] = c.getLong(0)
                }
            }
            plain.execSQL("ATTACH DATABASE ? AS encrypted KEY ?", arrayOf<Any>(temp.absolutePath, key))
            plain.rawQuery("SELECT sqlcipher_export('encrypted')", null).use { it.moveToFirst() }
            plain.execSQL("PRAGMA encrypted.user_version = $version")
            plain.execSQL("DETACH DATABASE encrypted")
        } finally {
            plain.close()
        }

        val verified = try {
            val enc = net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
                temp.absolutePath, key, null,
                net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READONLY, null
            )
            try {
                var sameVersion = false
                enc.rawQuery("PRAGMA user_version", null).use { if (it.moveToFirst()) sameVersion = it.getInt(0) == version }
                sameVersion && counts.all { (t, n) ->
                    enc.rawQuery("SELECT COUNT(*) FROM \"$t\"", null).use { it.moveToFirst() && it.getLong(0) == n }
                }
            } finally {
                enc.close()
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseEncryption", "encrypted copy did not verify", e)
            false
        }

        if (!verified) {
            temp.delete()
            return false
        }

        // rename() over an existing file is atomic within one filesystem: at
        // every instant there is a whole database under the real name, the
        // plaintext one until this line and the encrypted one after it.
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
        File(dbFile.path + "-journal").delete()
        if (!temp.renameTo(dbFile)) {
            android.util.Log.e("DatabaseEncryption", "could not move the encrypted database into place")
            temp.delete()
            return false
        }
        return true
    }

    private fun tableNames(db: net.zetetic.database.sqlcipher.SQLiteDatabase): List<String> =
        db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'",
            null
        ).use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }

    private fun keystoreKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getKey(KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun wrap(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val iv = cipher.iv
        val sealed = cipher.doFinal(plain.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(sealed, Base64.NO_WRAP)
    }

    private fun unwrap(stored: String): String {
        val (ivB64, sealedB64) = stored.split(":", limit = 2)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, Base64.decode(ivB64, Base64.NO_WRAP)))
        return String(cipher.doFinal(Base64.decode(sealedB64, Base64.NO_WRAP)), Charsets.US_ASCII)
    }
}
