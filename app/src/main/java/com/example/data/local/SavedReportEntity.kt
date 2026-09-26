package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The table stays so the database keeps its v7 schema; nothing reads or writes
 * it any more. Saved reports were never saved (no caller of the old
 * `saveReport`), so the list was always empty and was removed on 26 Sep 2026.
 * Drop the table in the next schema migration.
 */
@Entity(
    tableName = "saved_astrology_reports",
    indices = [Index(value = ["reportType"]), Index(value = ["createdAt"])]
)
data class SavedReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val reportType: String, // "KUNDALI", "MATCHING", "NUMEROLOGY_AI", "HOROSCOPE"
    val profileName: String,
    val summaryText: String,
    val detailedJsonData: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
