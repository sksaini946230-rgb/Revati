package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Database(
    entities = [KundaliEntity::class, PanchangCacheEntity::class, HoroscopeCacheEntity::class, SavedReportEntity::class, RecentSearchEntity::class],
    version = 7,
    // Schemas are recorded under app/schemas from here on. Without them a
    // migration cannot be written, let alone tested, which is how this database
    // ended up on a destructive fallback in the first place.
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun kundaliDao(): KundaliDao
    abstract fun panchangCacheDao(): PanchangCacheDao
    abstract fun horoscopeCacheDao(): HoroscopeCacheDao
    abstract fun recentSearchDao(): RecentSearchDao

    companion object {

        /**
         * 5 → 6.
         *
         * Three things, all of them fixing something that was actively wrong:
         *
         *  - `saved_kundali_profiles.uuid`, backfilled for every existing row. The
         *    cloud backup keyed Firestore documents on the Room autoGenerate id,
         *    which starts at 1 on every device, so two phones on one account
         *    overwrote each other's first profile. See KundaliEntity.
         *  - `panchang_cache.planetsJson`, so a cache hit stops re-running the
         *    whole ephemeris just to recover the planet list.
         *  - Indices. There were none at all, on any table, while queries filtered
         *    on cachedAtTimestamp, period and reportType and ordered by createdAt.
         *
         * Backfill runs before the unique index is created — every pre-existing row
         * would otherwise collide on the empty-string default.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_kundali_profiles ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")

                db.query("SELECT id FROM saved_kundali_profiles WHERE uuid = ''").use { cursor ->
                    val ids = ArrayList<Long>(cursor.count)
                    while (cursor.moveToNext()) ids.add(cursor.getLong(0))
                    ids.forEach { id ->
                        db.execSQL(
                            "UPDATE saved_kundali_profiles SET uuid = ? WHERE id = ?",
                            arrayOf<Any>(UUID.randomUUID().toString(), id)
                        )
                    }
                }

                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_saved_kundali_profiles_uuid " +
                        "ON saved_kundali_profiles (uuid)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_saved_kundali_profiles_createdAt " +
                        "ON saved_kundali_profiles (createdAt)"
                )

                db.execSQL("ALTER TABLE panchang_cache ADD COLUMN planetsJson TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_panchang_cache_cachedAtTimestamp " +
                        "ON panchang_cache (cachedAtTimestamp)"
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_horoscope_cache_period " +
                        "ON horoscope_cache (period)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_horoscope_cache_cachedAtTimestamp " +
                        "ON horoscope_cache (cachedAtTimestamp)"
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_saved_astrology_reports_reportType " +
                        "ON saved_astrology_reports (reportType)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_saved_astrology_reports_createdAt " +
                        "ON saved_astrology_reports (createdAt)"
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_recent_searches_createdAt " +
                        "ON recent_searches (createdAt)"
                )
            }
        }

        /**
         * 6 → 7. No schema change at all — this exists to throw away stale
         * horoscope rows.
         *
         * RashifalProvider used to derive the rating, the lucky colour and the
         * lucky stone from `(rashiIdx + house)`, in which rashiIdx cancels out,
         * so all twelve rashis were handed the same answer. That is fixed, but
         * the fix alone does not reach anybody: horoscopes are cached for seven
         * days, the cache table survives an upgrade untouched, and a cache hit
         * is returned without consulting the generator. An updated user would
         * have gone on seeing five stars out of five for every sign, for up to
         * a week, with nothing in the app to suggest anything was wrong.
         *
         * This was not caught by reasoning about it. It was caught by upgrading
         * a real phone from versionCode 5 and reading the row back out:
         *
         *     rashiId 1..12  ->  ratingStars = 5, all twelve
         *     lucky colour   ->  two distinct values across the whole zodiac
         *
         * The general rule this is an instance of: when the *content* a cache
         * holds changes meaning, the cache is stale no matter how young its
         * rows are, and clearing it belongs in the same change as the fix.
         * panchang_cache is left alone — the ephemeris did not change.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM horoscope_cache")
            }
        }

        /** Every schema change from version 5 onward adds its migration here. */
        private val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_5_6, MIGRATION_6_7)

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // The inner re-check is not decoration. Without it, two threads that
            // both passed the outer null check each built a RoomDatabase and the
            // second overwrote the first — two open handles to one file.
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DatabaseEncryption.DATABASE_NAME
                )
                    .apply { encryptionFactory(context.applicationContext)?.let { openHelperFactory(it) } }
                    // This used to be fallbackToDestructiveMigration(dropAllTables
                    // = true): every schema change silently dropped the user's
                    // saved profiles, their reports and their recent searches.
                    // Nothing warned anyone, because a wipe is not an error.
                    //
                    // Upgrades now require a real Migration in MIGRATIONS above.
                    // A missing one fails loudly in development rather than
                    // quietly destroying data in production. Downgrades — only
                    // reachable by sideloading an older build over a newer one —
                    // still fall back, since there is no forward schema to
                    // migrate from.
                    .addMigrations(*MIGRATIONS)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        /**
         * The SQLCipher open helper, or null to open the database unencrypted.
         *
         * Null only when encryption cannot be set up without risking the data:
         * the key could not be created or unwrapped, or an existing plaintext
         * database did not convert and verify. The plaintext file is then opened
         * as it always was and the conversion is tried again on the next launch.
         * See [DatabaseEncryption].
         */
        private fun encryptionFactory(context: Context): androidx.sqlite.db.SupportSQLiteOpenHelper.Factory? {
            return try {
                val dbFile = context.getDatabasePath(DatabaseEncryption.DATABASE_NAME)
                val key = DatabaseEncryption.keyOrNull(context)
                if (key == null) {
                    if (dbFile.exists() && !DatabaseEncryption.isPlaintextSqlite(dbFile)) {
                        // Encrypted under a key that no longer exists. Nothing can
                        // read it; set it aside rather than crash on every launch.
                        dbFile.renameTo(java.io.File(dbFile.path + ".unreadable"))
                        com.example.util.AstroAnalytics.recordNonFatal(
                            IllegalStateException("database key lost; encrypted database set aside"),
                            "AppDatabase.encryption"
                        )
                    }
                    return null
                }
                System.loadLibrary("sqlcipher")
                if (!DatabaseEncryption.encryptExistingIfNeeded(context, key)) {
                    com.example.util.AstroAnalytics.recordNonFatal(
                        IllegalStateException("plaintext database did not convert; opened unencrypted"),
                        "AppDatabase.encryption"
                    )
                    return null
                }
                net.zetetic.database.sqlcipher.SupportOpenHelperFactory(key)
            } catch (e: Throwable) {
                com.example.util.AstroAnalytics.recordNonFatal(e, "AppDatabase.encryption")
                // A plaintext file is still readable without a factory; an
                // encrypted one is not, and opening it bare would crash, so
                // only fall back when the file on disk really is plaintext.
                val dbFile = context.getDatabasePath(DatabaseEncryption.DATABASE_NAME)
                if (dbFile.exists() && !DatabaseEncryption.isPlaintextSqlite(dbFile)) throw e
                null
            }
        }
    }
}
