package com.example.data.local

import android.content.Context

/**
 * Singleton dependency provider for initializing and accessing Room Database instance,
 * DAOs, and repositories across the application.
 */
object DatabaseProvider {

    @Volatile
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: AppDatabase.getDatabase(context).also { instance = it }
        }
    }

    fun getKundaliDao(context: Context): KundaliDao = getDatabase(context).kundaliDao()

    fun getRecentSearchDao(context: Context): RecentSearchDao = getDatabase(context).recentSearchDao()

    fun getPanchangCacheDao(context: Context): PanchangCacheDao = getDatabase(context).panchangCacheDao()
    
    // ...

    fun getRecentSearchRepository(context: Context): RecentSearchRepository {
        return RecentSearchRepository(getRecentSearchDao(context))
    }

    fun getHoroscopeCacheDao(context: Context): HoroscopeCacheDao = getDatabase(context).horoscopeCacheDao()

    fun getKundaliRepository(context: Context): KundaliRepository {
        return KundaliRepository(getKundaliDao(context))
    }

    fun getAstroCacheRepository(context: Context): AstroCacheRepository {
        return AstroCacheRepository(getPanchangCacheDao(context), getHoroscopeCacheDao(context))
    }
}
