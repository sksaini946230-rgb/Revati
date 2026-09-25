package com.example.data

import com.example.data.local.KundaliEntity
import com.example.data.local.ProfileTransfer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Export/import round-trip and, more importantly, what happens to a file the app
 * did not write.
 *
 * An imported file is arbitrary external input — the user picks it from anywhere,
 * and it may have been edited by hand or corrupted in transit. It is the only
 * untrusted input path in the app besides the AI question box, so most of these
 * tests are about rejecting things rather than accepting them.
 *
 * Robolectric because ProfileTransfer uses org.json, which is stubbed on a plain
 * JVM.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class ProfileTransferTest {

    private fun profile(
        uuid: String = java.util.UUID.randomUUID().toString(),
        name: String = "राम",
        dob: String = "1994-08-25",
        tob: String = "14:15"
    ) = KundaliEntity(
        uuid = uuid, name = name, gender = "MALE",
        dateOfBirth = dob, timeOfBirth = tob, placeOfBirth = "Jaipur",
        latitude = 26.9124, longitude = 75.7873, notes = "note", createdAt = 1_700_000_000L
    )

    @Test
    fun `a full round trip preserves every field`() {
        val original = listOf(profile(name = "राम"), profile(name = "Sita", tob = "09:40"))
        val decoded = ProfileTransfer.decode(ProfileTransfer.encode(original))

        assertEquals(2, decoded.size)
        original.zip(decoded).forEach { (a, b) ->
            assertEquals(a.uuid, b.uuid)
            assertEquals(a.name, b.name)
            assertEquals(a.dateOfBirth, b.dateOfBirth)
            assertEquals(a.timeOfBirth, b.timeOfBirth)
            assertEquals(a.placeOfBirth, b.placeOfBirth)
            assertEquals(a.latitude, b.latitude, 1e-9)
            assertEquals(a.longitude, b.longitude, 1e-9)
            assertEquals(a.notes, b.notes)
            assertEquals(a.createdAt, b.createdAt)
        }
    }

    @Test
    fun `an empty export round trips`() {
        assertTrue(ProfileTransfer.decode(ProfileTransfer.encode(emptyList())).isEmpty())
    }

    @Test
    fun `a file that is not JSON is refused with a readable message`() {
        val e = runCatching { ProfileTransfer.decode("this is not json") }
            .exceptionOrNull() as? ProfileTransfer.TransferException
        assertTrue("must be a TransferException", e != null)
        assertTrue(e!!.messageHi.isNotBlank() && e.messageEn.isNotBlank())
    }

    @Test
    fun `valid JSON from some other app is refused`() {
        val e = runCatching { ProfileTransfer.decode("""{"hello":"world"}""") }
            .exceptionOrNull()
        assertTrue(e is ProfileTransfer.TransferException)
    }

    @Test
    fun `a file from a newer app version is refused rather than half-read`() {
        val newer = """{"format":"revati-profiles","version":99,"profiles":[]}"""
        assertTrue(
            runCatching { ProfileTransfer.decode(newer) }.exceptionOrNull()
                is ProfileTransfer.TransferException
        )
    }

    @Test
    fun `entries with bad dates, times or coordinates are dropped, not imported`() {
        val json = """
            {"format":"revati-profiles","version":1,"profiles":[
              {"uuid":"a","name":"Good","gender":"MALE","dateOfBirth":"1994-08-25","timeOfBirth":"14:15","placeOfBirth":"Jaipur","latitude":26.9,"longitude":75.7,"notes":"","createdAt":1},
              {"uuid":"b","name":"BadDate","gender":"MALE","dateOfBirth":"1994-13-45","timeOfBirth":"14:15","placeOfBirth":"X","latitude":26.9,"longitude":75.7,"notes":"","createdAt":1},
              {"uuid":"c","name":"BadTime","gender":"MALE","dateOfBirth":"1994-08-25","timeOfBirth":"99:99","placeOfBirth":"X","latitude":26.9,"longitude":75.7,"notes":"","createdAt":1},
              {"uuid":"d","name":"BadLat","gender":"MALE","dateOfBirth":"1994-08-25","timeOfBirth":"14:15","placeOfBirth":"X","latitude":999.0,"longitude":75.7,"notes":"","createdAt":1},
              {"uuid":"e","name":"","gender":"MALE","dateOfBirth":"1994-08-25","timeOfBirth":"14:15","placeOfBirth":"X","latitude":26.9,"longitude":75.7,"notes":"","createdAt":1}
            ]}
        """.trimIndent()

        val decoded = ProfileTransfer.decode(json)

        assertEquals("only the one valid entry survives", 1, decoded.size)
        assertEquals("Good", decoded.first().name)
    }

    @Test
    fun `markup in an imported name is stripped`() {
        val json = """
            {"format":"revati-profiles","version":1,"profiles":[
              {"uuid":"a","name":"<script>alert(1)</script>राहुल","gender":"MALE","dateOfBirth":"1994-08-25","timeOfBirth":"14:15","placeOfBirth":"<b>Jaipur</b>","latitude":26.9,"longitude":75.7,"notes":"","createdAt":1}
            ]}
        """.trimIndent()

        val p = ProfileTransfer.decode(json).single()

        assertEquals("राहुल", p.name)
        assertEquals("Jaipur", p.placeOfBirth)
    }

    @Test
    fun `an entry with no uuid gets a fresh one instead of colliding`() {
        val json = """
            {"format":"revati-profiles","version":1,"profiles":[
              {"name":"A","gender":"MALE","dateOfBirth":"1994-08-25","timeOfBirth":"14:15","placeOfBirth":"X","latitude":26.9,"longitude":75.7,"notes":"","createdAt":1},
              {"name":"B","gender":"MALE","dateOfBirth":"1994-08-25","timeOfBirth":"14:15","placeOfBirth":"X","latitude":26.9,"longitude":75.7,"notes":"","createdAt":1}
            ]}
        """.trimIndent()

        val decoded = ProfileTransfer.decode(json)

        assertEquals(2, decoded.size)
        assertTrue(decoded[0].uuid.isNotBlank())
        assertNotEquals(decoded[0].uuid, decoded[1].uuid)
    }

    @Test
    fun `importing the same file twice adds nothing the second time`() {
        val existing = listOf(profile(uuid = "same-uuid"))
        val incoming = listOf(profile(uuid = "same-uuid"), profile(uuid = "new-uuid"))

        val (fresh, duplicates) = ProfileTransfer.plan(incoming, existing)

        assertEquals(1, fresh.size)
        assertEquals("new-uuid", fresh.single().uuid)
        assertEquals(1, duplicates)
    }

    @Test
    fun `importing into an empty device takes everything`() {
        val incoming = listOf(profile(), profile(), profile())
        val (fresh, duplicates) = ProfileTransfer.plan(incoming, emptyList())
        assertEquals(3, fresh.size)
        assertEquals(0, duplicates)
    }

    @Test
    fun `imported rows carry no local id so Room assigns fresh ones`() {
        val decoded = ProfileTransfer.decode(ProfileTransfer.encode(listOf(profile())))
        assertEquals(0L, decoded.single().id)
    }

    private fun one(fields: String) = ProfileTransfer.decode(
        """{"format":"revati-profiles","version":1,"profiles":[{$fields}]}"""
    )

    private val base = """"latitude":26.9,"longitude":75.8"""

    @Test
    fun `a null name is no name, so the profile is skipped rather than called null`() {
        assertEquals(0, one(""""name":null,"dateOfBirth":"1994-08-25","timeOfBirth":"14:15",$base""").size)
    }

    @Test
    fun `a null uuid, notes or place is absent, not the text null`() {
        val p = one(""""name":"Ram","uuid":null,"notes":null,"placeOfBirth":null,"dateOfBirth":"1994-08-25","timeOfBirth":"14:15",$base""").single()
        assertNotEquals("null", p.uuid)
        assertTrue(p.uuid.isNotBlank())
        assertEquals("", p.notes)
        assertEquals("", p.placeOfBirth)
    }

    @Test
    fun `dates and times are stored the way the chart screen reads them`() {
        val p = one(""""name":"Ram","dateOfBirth":" \u0967\u096f\u096f\u096a-\u0966\u096e-\u0968\u096b","timeOfBirth":"9:5 ",$base""").single()
        assertEquals("1994-08-25", p.dateOfBirth)
        assertEquals("09:05", p.timeOfBirth)
        assertEquals("1994-08-05", one(""""name":"Ram","dateOfBirth":"+1994-8-5","timeOfBirth":"14:15",$base""").single().dateOfBirth)
    }
}
