package com.example.worker

import com.example.data.model.dateLocal
import com.example.util.LanguageManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import com.example.MainActivity
import com.example.astro.ChoghadiyaCalculator
import com.example.astro.PanchangCalculator
import com.example.data.local.CityPreferences
import com.example.data.model.ChoghadiyaType
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

class MuhuratNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val WORK_NAME = "astro_muhurat_notification_work"
        const val CHANNEL_ID = "astro_muhurat_notifications"

        fun scheduleMuhuratNotification(context: Context) {
            val sharedPrefs = context.getSharedPreferences("astroveda_prefs", Context.MODE_PRIVATE)
            val isEnabled = sharedPrefs.getBoolean("muhurat_notification_enabled", true)

            if (!isEnabled) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                return
            }

            // Schedule for 7:00 AM daily (optimal morning time for daily auspicious timings)
            val hour = sharedPrefs.getInt("muhurat_notification_hour", com.example.util.NotificationDefaults.MUHURAT_HOUR)
            val minute = sharedPrefs.getInt("muhurat_notification_minute", 0)

            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            if (calendar.timeInMillis <= now) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val initialDelay = calendar.timeInMillis - now

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val dailyWorkRequest = PeriodicWorkRequestBuilder<MuhuratNotificationWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag("DAILY_MUHURAT")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                dailyWorkRequest
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val context = applicationContext
            val userCity = CityPreferences.read(context)
            val use24Hour = CityPreferences.use24Hour(context)
            val today = Date()
            val panchang = PanchangCalculator.calculatePanchang(today, userCity, use24Hour)
            val choghadiyaSlots = ChoghadiyaCalculator.getChoghadiyaSlots(
                today,
                isDaytime = true,
                lat = userCity.latitude,
                lon = userCity.longitude,
                use24Hour = use24Hour
            )

            val auspiciousSlots = choghadiyaSlots.filter {
                it.type == ChoghadiyaType.AMRIT || it.type == ChoghadiyaType.SHUBH || it.type == ChoghadiyaType.LABH
            }

            val shubhChoghadiyaSummary = if (auspiciousSlots.isNotEmpty()) {
                auspiciousSlots.take(2).joinToString(", ") { "${it.type.nameLocal} (${it.timeSlotString})" }
            } else {
                LanguageManager.getString("उपलब्ध", "available")
            }

            val title = LanguageManager.getString(
                "✨ आज के शुभ मुहूर्त एवं चौघड़िया (${userCity.cityNameHindi})",
                "✨ Today's muhurats & Choghadiya (${userCity.cityName})"
            )
            val content = LanguageManager.getString(
                "🌟 अभिजित: ${panchang.abhijitMuhurat} • 🌅 ब्रह्म: ${panchang.brahmaMuhurat}",
                "🌟 Abhijit: ${panchang.abhijitMuhurat} • 🌅 Brahma: ${panchang.brahmaMuhurat}"
            )
            
            val bigText = StringBuilder().apply {
                append(LanguageManager.getString(
                    "🕉️ Revati दैनिक शुभ मुहूर्त गाइड (${panchang.dateLocal})\n\n" +
                        "🌟 अभिजित मुहूर्त (सर्वश्रेष्ठ): ${panchang.abhijitMuhurat}\n" +
                        "🌅 ब्रह्म मुहूर्त: ${panchang.brahmaMuhurat}\n" +
                        "⚡ शुभ चौघड़िया समय: $shubhChoghadiyaSummary\n" +
                        "⚠️ राहुकाल (अशुभ समय): ${panchang.rahuKaal}\n\n" +
                        "💡 टिप: नया कार्य, पूजन या यात्रा अभिजित मुहूर्त या अमृत/शुभ चौघड़िया में आरंभ करना अत्यंत फलदायी होता है।",
                    "🕉️ Revati — today's auspicious timings (${panchang.dateString})\n\n" +
                        "🌟 Abhijit Muhurat (the best window): ${panchang.abhijitMuhurat}\n" +
                        "🌅 Brahma Muhurta: ${panchang.brahmaMuhurat}\n" +
                        "⚡ Auspicious Choghadiya: $shubhChoghadiyaSummary\n" +
                        "⚠️ Rahu Kaal (avoid): ${panchang.rahuKaal}\n\n" +
                        "💡 Tip: starting new work, a puja or a journey in Abhijit, or in the Amrit/Shubh Choghadiya, is held to be most fruitful."
                ))
            }.toString()

            showNotification(title, content, bigText)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Only a genuinely transient failure earns a retry; retry() for
            // everything turned a permanent bug into an endless backoff loop.
            if (e is java.io.IOException) Result.retry() else Result.failure()
        }
    }

    private fun showNotification(title: String, content: String, bigText: String) {
        val context = applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = try {
                context.getString(app.revati.jyotish.R.string.notif_channel_muhurat_name)
            } catch (e: Throwable) {
                "शुभ मुहूर्त अलर्ट"
            }
            val channelDesc = try {
                context.getString(app.revati.jyotish.R.string.notif_channel_muhurat_desc)
            } catch (e: Throwable) {
                "प्रतिदिन शुभ मुहूर्त, अभिजित मुहूर्त, चौघड़िया एवं ब्रह्म मुहूर्त की सूचनाएं।"
            }
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDesc
                enableLights(true)
                lightColor = android.graphics.Color.parseColor("#FFD700") // Auspicious Gold
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "muhurat")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1003,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(app.revati.jyotish.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(android.graphics.Color.parseColor("#FFD700"))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        notificationManager.notify(1003, notification)
    }
}
