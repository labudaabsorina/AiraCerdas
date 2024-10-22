package com.example.airacerdas

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.*
import kotlin.random.Random

class FirebaseBackgroundService : Service() {

    private lateinit var database: FirebaseDatabase
    private lateinit var ruangTuRef: DatabaseReference
    private lateinit var ruangDosenRef: DatabaseReference

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        database = FirebaseDatabase.getInstance()
        ruangTuRef = database.getReference("Ruang_Tu")
        ruangDosenRef = database.getReference("Ruang_dosen")

        ruangTuRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val status = snapshot.child("status").getValue(String::class.java)
                    handleStatusChange("Ruang_Tu", status)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseService", "Failed to read value from Ruang_Tu.", error.toException())
            }
        })

        ruangDosenRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val status = snapshot.child("status").getValue(String::class.java)
                    handleStatusChange("Ruang_dosen", status)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseService", "Failed to read value from Ruang_dosen.", error.toException())
            }
        })
    }

    private fun handleStatusChange(room: String, status: String?) {
        if (status == "Air Galon Habis!" || status == "Air Galon Hampir Habis!" || status == "Yeay air galon sudah terisii :)") {
            val soundType = when (status) {
                "Air Galon Habis!", "Air Galon Hampir Habis!" -> "alarm"
                "Yeay air galon sudah terisii :)" -> "yeay"
                else -> "default"
            }
            showNotification("Notification $room", status ?: "No status available", soundType)
        }
    }

    private fun showNotification(title: String, message: String, soundType: String) {
        val soundUri: Uri = when (soundType) {
            "alarm" -> Uri.parse("android.resource://${packageName}/raw/sound_effect_alarm")
            "yeay" -> Uri.parse("android.resource://${packageName}/raw/sound_effect_yeay")
            else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        Log.d("FirebaseService", "Notification sound URI: $soundUri")

        // Coba memutar suara secara manual untuk memastikan suara dapat diakses
        try {
            val ringtone: Ringtone = RingtoneManager.getRingtone(applicationContext, soundUri)
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification_icon) // Pastikan ini adalah nama file ikon yang benar di folder drawable
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = Random.nextInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Default Channel"
            val descriptionText = "This is the default notification channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val CHANNEL_ID = "channel_id"
    }
}
