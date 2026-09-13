package com.example.data.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The plaintext check decides whether a user's existing database is converted
 * or opened with the key, and it is the one part of the encryption path that
 * runs without the native library. A wrong answer either way is a crash on
 * launch (a plaintext file opened with a key) or a database opened bare.
 */
class DatabaseEncryptionTest {

    private fun file(bytes: ByteArray): File =
        File.createTempFile("revati-db", ".bin").apply { writeBytes(bytes); deleteOnExit() }

    private val header = "SQLite format 3".toByteArray(Charsets.US_ASCII) + byteArrayOf(0)

    @Test
    fun `a real SQLite header is plaintext`() {
        assertTrue(DatabaseEncryption.isPlaintextSqlite(file(header + ByteArray(4080))))
    }

    @Test
    fun `random bytes, a short file and no file are not`() {
        assertFalse(DatabaseEncryption.isPlaintextSqlite(file(ByteArray(4096) { (it * 31 + 7).toByte() })))
        assertFalse(DatabaseEncryption.isPlaintextSqlite(file("SQLite".toByteArray())))
        assertFalse(DatabaseEncryption.isPlaintextSqlite(File("/nonexistent/revati.db")))
    }

    @Test
    fun `the header without its terminating NUL is not`() {
        assertFalse(DatabaseEncryption.isPlaintextSqlite(file("SQLite format 3 ".toByteArray() + ByteArray(4080))))
    }
}
