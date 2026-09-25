package com.example.data.local

import com.example.util.SecurityUtils
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/**
 * Moving saved birth profiles off the device, and back on again.
 *
 * WHY THIS EXISTS: the Room database is deliberately excluded from Android's
 * Auto Backup and from device transfer, because it holds the most personal thing
 * this app stores — name, exact date and time of birth, place and coordinates.
 * That is the right call. But it left a hole: a user who does not sign in had
 * **no way at all** to move their profiles to a new phone. Lose the phone, lose
 * the kundalis — and an exact birth time is not something most people can look
 * up again.
 *
 * Signing in was never the right answer to that. An account should buy cloud
 * backup for people who want it, not be the price of not losing your data.
 *
 * The file is plain JSON on purpose: the user can open it, read it, and see
 * exactly what they are about to send through WhatsApp or Drive. Nothing is
 * hidden from them about what is in their own birth data.
 */
object ProfileTransfer {

    const val FORMAT = "revati-profiles"
    const val VERSION = 1

    /** What an import did, so the UI can say something true about it. */
    data class ImportResult(
        val imported: Int,
        val skippedDuplicates: Int,
        val rejected: Int
    )

    /** Thrown when a file is not something this app wrote. Carries user-facing text. */
    class TransferException(val messageHi: String, val messageEn: String) :
        IllegalArgumentException(messageEn)

    fun encode(profiles: List<KundaliEntity>): String {
        val arr = JSONArray()
        profiles.forEach { p ->
            arr.put(
                JSONObject().apply {
                    put("uuid", p.uuid)
                    put("name", p.name)
                    put("gender", p.gender)
                    put("dateOfBirth", p.dateOfBirth)
                    put("timeOfBirth", p.timeOfBirth)
                    put("placeOfBirth", p.placeOfBirth)
                    put("latitude", p.latitude)
                    put("longitude", p.longitude)
                    put("notes", p.notes)
                    put("createdAt", p.createdAt)
                }
            )
        }
        return JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("profiles", arr)
        }.toString(2)
    }

    /**
     * Reads an export file.
     *
     * Everything in here is untrusted — the user may have picked any file, or
     * edited one by hand. Each entry is validated and sanitised the same way
     * typed input is, and a bad entry is dropped rather than taken on faith or
     * allowed to fail the whole import.
     */
    fun decode(text: String): List<KundaliEntity> {
        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw TransferException(
                "यह फ़ाइल पढ़ी नहीं जा सकी। क्या आपने Revati की एक्सपोर्ट फ़ाइल चुनी है?",
                "That file could not be read. Is it a Revati export file?"
            )
        }

        if (root.optString("format") != FORMAT) {
            throw TransferException(
                "यह Revati की प्रोफ़ाइल फ़ाइल नहीं है।",
                "That is not a Revati profiles file."
            )
        }
        if (root.optInt("version", 0) > VERSION) {
            throw TransferException(
                "यह फ़ाइल Revati के नए संस्करण से बनी है। पहले ऐप अपडेट करें।",
                "That file was made by a newer version of Revati. Update the app first."
            )
        }

        val arr = root.optJSONArray("profiles") ?: JSONArray()
        val out = ArrayList<KundaliEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val entity = readEntity(o) ?: continue
            out.add(entity)
        }
        return out
    }

    /**
     * A field as text, with JSON `null` read as absent. `optString` returns the
     * four letters "null" for it, which imported a profile named "null", with
     * notes "null" and a uuid "null" that every such profile then shared.
     */
    private fun text(o: JSONObject, key: String): String = if (o.isNull(key)) "" else o.optString(key)

    /**
     * The date as the rest of the app reads it: ASCII digits, zero-padded.
     * The validity check accepts Devanagari digits ("१९९४-०८-२५"), a stray
     * space or a "+", and they used to be stored as written — so the import
     * succeeded and the chart screen then refused the profile.
     */
    private fun canonicalDate(raw: String): String? {
        if (!SecurityUtils.isValidDate(raw)) return null
        val (y, m, d) = raw.trim().split("-").map { it.toInt() }
        return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
    }

    /** The time the same way: "९:५" and " 9:05 " both become "09:05". */
    private fun canonicalTime(raw: String): String? {
        if (!SecurityUtils.isValidTime(raw)) return null
        val (h, m) = raw.trim().split(":").map { it.toInt() }
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    /** Returns null for anything that does not survive validation. */
    private fun readEntity(o: JSONObject): KundaliEntity? {
        val name = SecurityUtils.sanitizeTextInput(text(o, "name"))
        if (name.isBlank()) return null

        val dob = canonicalDate(text(o, "dateOfBirth")) ?: return null
        val tob = canonicalTime(text(o, "timeOfBirth")) ?: return null

        val lat = o.optDouble("latitude", Double.NaN)
        val lon = o.optDouble("longitude", Double.NaN)
        if (lat.isNaN() || lon.isNaN()) return null
        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) return null

        val gender = text(o, "gender").takeIf { it == "MALE" || it == "FEMALE" } ?: "MALE"

        // A file written by an older build, or edited by hand, may have no uuid.
        // Minting one here is correct: it is a profile this device has not seen.
        val uuid = text(o, "uuid").takeIf { it.isNotBlank() }
            ?: java.util.UUID.randomUUID().toString()

        val createdAt = o.optLong("createdAt", 0L).takeIf { it > 0L }
            ?: System.currentTimeMillis()

        return KundaliEntity(
            id = 0,
            uuid = uuid,
            name = name,
            gender = gender,
            dateOfBirth = dob,
            timeOfBirth = tob,
            placeOfBirth = SecurityUtils.sanitizeTextInput(text(o, "placeOfBirth")),
            latitude = lat,
            longitude = lon,
            notes = SecurityUtils.sanitizeTextInput(text(o, "notes")),
            createdAt = createdAt
        )
    }

    /**
     * Works out what an import should actually write, given what is already here.
     *
     * Matching is on uuid, the same rule the cloud sync uses — see [ProfileMerge].
     */
    fun plan(incoming: List<KundaliEntity>, existing: List<KundaliEntity>): Pair<List<KundaliEntity>, Int> {
        val have = existing.mapTo(HashSet()) { it.uuid }
        val fresh = incoming.filter { it.uuid !in have }
        return fresh to (incoming.size - fresh.size)
    }
}
