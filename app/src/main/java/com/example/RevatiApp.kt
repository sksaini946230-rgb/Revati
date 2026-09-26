package com.example

import android.app.Application

/**
 * Process-wide startup.
 *
 * WHY THIS EXISTS: there was no Application class, so everything app-wide was
 * initialised in MainActivity.onCreate. But MainActivity is only one of six ways
 * this process starts — the other five are two AppWidgetProviders and three
 * WorkManager workers, none of which go through an Activity.
 *
 * The visible cost was language. LanguageManager.init reads the saved choice from
 * SharedPreferences; without it, currentLanguage sits at its default of HINDI. So
 * the daily 6:30 AM notification, fired by WorkManager into a fresh process, and
 * both home-screen widgets after any reboot, came out in Hindi for an English
 * user — every day, until they happened to open the app. That is exactly the
 * failure the "both languages, always" rule in CLAUDE.md exists to prevent; the
 * Aug 2026 sweep fixed the strings and left the initialisation alone.
 *
 * App Check had the same shape of problem: installed only from MainActivity, so
 * any Firebase call made from a worker went unattested.
 */
class RevatiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.example.util.LanguageManager.init(this)
        com.example.service.AppCheckInitializer.install()

        // The app-open ad watches the process's foreground state, so it has to
        // be registered from here rather than from an Activity — an Activity
        // cannot see the app being backgrounded and brought back.
        com.example.service.AppOpenAdManager.register(this)
        com.example.service.AnnouncementService.createChannel(this)
    }
}
