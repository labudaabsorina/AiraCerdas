package com.example.airacerdas.Ob

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import com.example.airacerdas.R
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class NotifOb : Fragment() {

    private lateinit var database: FirebaseDatabase
    private lateinit var ruangTuRef: DatabaseReference
    private lateinit var ruangDosenRef: DatabaseReference
    private lateinit var ruangtu: TextView
    private lateinit var keteranganTU: TextView
    private lateinit var ruangDosen: TextView
    private lateinit var keteranganDosen: TextView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notif_ob, container, false)
        ruangtu = view.findViewById(R.id.lokasi)
        keteranganTU = view.findViewById(R.id.keterangan)
        ruangDosen = view.findViewById(R.id.lokasi1)
        keteranganDosen = view.findViewById(R.id.keterangan1)

        // Inisialisasi Firebase Database
        database = FirebaseDatabase.getInstance()

        // Referensi ke node Ruang_Tu dan Ruang_dosen
        ruangTuRef = database.getReference("Ruang_Tu")
        ruangDosenRef = database.getReference("Ruang_dosen")

        // Membuat notification channels
        createNotificationChannels()

        // Mengambil data dari node Ruang_Tu
        ruangTuRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val berat = snapshot.child("berat").getValue(Double::class.java)
                    val status = snapshot.child("status").getValue(String::class.java)
                    Log.d("NotifOb", "Ruang_Tu - berat: $berat, status: $status")
                    keteranganTU.text = status ?: "Data tidak tersedia"
                    if (status == "Air Galon Habis!" || status == "Air Galon Hampir Habis!") {
                        sendFCMNotification("Ruang_Tu", status)
                    }
                    // Tampilkan notifikasi berdasarkan status
                    when (status) {
                        "Air Galon Habis!" -> {
                            showNotification(CHANNEL_ID_1, "Notifikasi Ruang TU", status, "alarm")
                            startStatusCheckTimer("Ruang_Tu")
                        }
                        "Air Galon Hampir Habis!" -> {
                            showNotification(CHANNEL_ID_1, "Notifikasi Ruang TU", status, "alarm")
                            startStatusCheckTimer("Ruang_Tu")
                        }
                        "Yeay air galon sudah terisii :)" -> {
                            // Cancel the timer if the status indicates the gallon has been replaced
                            showNotification(CHANNEL_ID_1, "Notifikasi Ruang TU", status, "yeay")
                            Log.d("NotifOb", "Ruang_Tu - Timer cancelled, gallon replaced")
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NotifOb", "Failed to read value from Ruang_Tu.", error.toException())
            }
        })

        // Mengambil data dari node Ruang_dosen
        ruangDosenRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val berat = snapshot.child("berat").getValue(Double::class.java)
                    val status = snapshot.child("status").getValue(String::class.java)
                    Log.d("NotifOb", "Ruang_dosen - berat: $berat, status: $status")
                    keteranganDosen.text = status ?: "Data tidak tersedia"
                    if (status == "Air Galon Habis!" || status == "Air Galon Hampir Habis!") {
                        sendFCMNotification("Ruang_dosen", status)
                    }
                    // Tampilkan notifikasi berdasarkan status
                    when (status) {
                        "Air Galon Habis!" -> {
                            showNotification(CHANNEL_ID_2, "Notifikasi Ruang Dosen", status, "alarm")
                            startStatusCheckTimer("Ruang_dosen")
                        }
                        "Air Galon Hampir Habis!" -> {
                            showNotification(CHANNEL_ID_2, "Notifikasi Ruang Dosen", status, "alarm")
                            startStatusCheckTimer("Ruang_dosen")
                        }
                        "Yeay air galon sudah terisii :)" -> {
                            // Cancel the timer if the status indicates the gallon has been replaced
                            showNotification(CHANNEL_ID_1, "Notifikasi Ruang TU", status, "yeay")
                            Log.d("NotifOb", "Ruang_dosen - Timer cancelled, gallon replaced")
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NotifOb", "Failed to read value from Ruang_dosen.", error.toException())
            }
        })

        return view
    }
    private fun sendFCMNotification(location: String, status: String) {
        // Membuat data payload untuk pesan FCM
        val data = mapOf(
            "location" to location,
            "status" to status
        )

        FirebaseMessaging.getInstance().send(
            RemoteMessage.Builder("404351547973@fcm.googleapis.com")
                .setData(data)
                .build()
        )
    }

    private fun startStatusCheckTimer(node: String) {
        handler.postDelayed({
            recheckStatus(node)
        }, 30 * 60 * 1000) // 30 minutes in milliseconds
    }

    private fun recheckStatus(node: String) {
        val ref = database.getReference(node)
        ref.child("status").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                if (status == "Air Galon Habis!" || status == "Air Galon Hampir Habis!") {
                    showNotification(CHANNEL_ID_1, "Reminder", "Please replace the water gallon in $node.", "alarm")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("NotifOb", "Failed to recheck status for $node.", error.toException())
            }
        })
    }

    private fun showNotification(channelId: String, title: String, message: String, soundType: String) {
        // Safely get context
        val context = context ?: return

        val soundUri: Uri = when (soundType) {
            "alarm" -> Uri.parse("android.resource://${context.packageName}/raw/sound_effect_alarm")
            "yeay" -> Uri.parse("android.resource://${context.packageName}/raw/sound_effect_yeay")
            else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        Log.d("NotifOb", "Notification sound URI: $soundUri")

        // Coba memutar suara secara manual untuk memastikan suara dapat diakses
        try {
            val ringtone: Ringtone = RingtoneManager.getRingtone(context, soundUri)
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification_icon) // Pastikan ini adalah nama file ikon yang benar di folder drawable
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Menggunakan ID unik untuk notifikasi
        val notificationId = Random.nextInt()
        Log.d("NotifOb", "Showing notification with ID: $notificationId")
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
                requireContext().getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel1)
            notificationManager.createNotificationChannel(channel2)
        }
    }

    companion object {
        private const val CHANNEL_ID_1 = "channel_1"
        private const val CHANNEL_ID_2 = "channel_2"
    }
}
