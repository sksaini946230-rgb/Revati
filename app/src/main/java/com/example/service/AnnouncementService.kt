package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Messages to everyone, written in Firebase Console → Messaging (the admin
 * panel's Message tab explains how). While the app is in the background the
 * SDK shows them itself on [CHANNEL_ID]; this only covers the foreground, where
 * the SDK would otherwise drop them. No token is stored anywhere of ours — the
 * Console targets the app, not a list.
 */
class AnnouncementService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val note = message.notification ?: return
        val open = PendingIntent.getActivity(
            this,
            REQUEST_CODE,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(app.revati.jyotish.R.mipmap.ic_launcher)
            .setContentTitle(note.title)
            .setContentText(note.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(note.body))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        runCatching {
            getSystemService(NotificationManager::class.java).notify(message.messageId.hashCode(), notification)
        }
    }

    // Nothing to do: tokens are never sent anywhere of ours.
    override fun onNewToken(token: String) = Unit

    companion object {
        const val CHANNEL_ID = "revati_announcements"
        private const val REQUEST_CODE = 2001

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(app.revati.jyotish.R.string.notif_channel_announcements_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }
}
