
package com.example.airacerdas
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            val title = it.title ?: ""
            val message = it.body ?: ""
            val channelId = it.channelId ?: CHANNEL_ID_DEFAULT // Default channel if not provided

            // Ensure notifications show properly, create channels if not already created
            createNotificationChannels()

            // Show the notification
            showNotification(channelId, title, message)
        }
    }

    private fun showNotification(channelId: String, title: String, message: String) {
        val soundUri: Uri = when {
            message.contains("Habis") -> Uri.parse("android.resource://${packageName}/raw/sound_effect_alarm")
            message.contains("Hampir Habis") -> Uri.parse("android.resource://${packageName}/raw/sound_effect_alarm")
            message.contains("Yeay") -> Uri.parse("android.resource://${packageName}/raw/sound_effect_yeay")
            else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Using unique ID for notification
        val notificationId = System.currentTimeMillis().toInt() // Unique ID
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel1 = NotificationChannel(
                CHANNEL_ID_1,
                "Channel 1",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification channel for Ruang TU"
            }

            val channel2 = NotificationChannel(
                CHANNEL_ID_2,
                "Channel 2",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification channel for Ruang Dosen"
            }

            val notificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel1)
            notificationManager.createNotificationChannel(channel2)
        }
    }

    companion object {
        private const val CHANNEL_ID_1 = "channel_1"
        private const val CHANNEL_ID_2 = "channel_2"
        private const val CHANNEL_ID_DEFAULT = "channel_default"
    }
}
