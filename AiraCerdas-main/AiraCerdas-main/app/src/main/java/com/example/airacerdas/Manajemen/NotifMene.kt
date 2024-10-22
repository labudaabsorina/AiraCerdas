package com.example.airacerdas.Manajemen

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
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
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.airacerdas.R
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage

class NotifMene : Fragment() {

    private lateinit var database: FirebaseDatabase
    private lateinit var ruangTuRef: DatabaseReference
    private lateinit var ruangDosenRef: DatabaseReference
    private lateinit var ruangtu: TextView
    private lateinit var keteranganTU: TextView
    private lateinit var ruangDosen: TextView
    private lateinit var keteranganDosen: TextView
    private lateinit var editWaktuButton: Button

    private val emptyGallonTimerHandler = Handler(Looper.getMainLooper())
    private var maxWaitTimeMillis: Long = 30 * 60 * 1000 // Default 30 minutes

    private val REQUEST_NOTIFICATION_PERMISSION = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notif_mene, container, false)
        ruangtu = view.findViewById(R.id.lokasi)
        keteranganTU = view.findViewById(R.id.keterangan)
        ruangDosen = view.findViewById(R.id.lokasi1)
        keteranganDosen = view.findViewById(R.id.keterangan1)
        editWaktuButton = view.findViewById(R.id.editwaktu)

        // Inisialisasi Firebase Database
        database = FirebaseDatabase.getInstance()

        // Referensi ke node Ruang_Tu dan Ruang_dosen
        ruangTuRef = database.getReference("Ruang_Tu")
        ruangDosenRef = database.getReference("Ruang_dosen")

        // Check and request notification permission
        checkAndRequestNotificationPermission()

        // Mengambil data dari node Ruang_Tu
        ruangTuRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val berat = snapshot.child("berat").getValue(Double::class.java)
                    val status = snapshot.child("status").getValue(String::class.java)
                    Log.d("NotifMene", "Ruang_Tu - berat: $berat, status: $status")
                    keteranganTU.text = status ?: "Data tidak tersedia"
                    if (status == "Air Galon Habis!" || status == "Air Galon Hampir Habis!") {
                        sendFCMNotification("Ruang_Tu", status)
                    }
                    // Tampilkan notifikasi berdasarkan status
                    when (status) {
                        "Air Galon Habis!" -> {
                            showNotification(CHANNEL_ID_1, "Notifikasi Ruang TU", status, "alarm")
                        }
                        "Air Galon Hampir Habis!" -> {
                            showNotification(CHANNEL_ID_1, "Notifikasi Ruang TU", status, "alarm")
                        }
                        "Yeay air galon sudah terisii :)" -> {
                            showNotification(CHANNEL_ID_1, "Notifikasi Ruang TU", status, "yeay")
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NotifMene", "Failed to read value from Ruang_Tu.", error.toException())
            }
        })

        // Mengambil data dari node Ruang_dosen
        ruangDosenRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val berat = snapshot.child("berat").getValue(Double::class.java)
                    val status = snapshot.child("status").getValue(String::class.java)
                    Log.d("NotifMene", "Ruang_dosen - berat: $berat, status: $status")
                    keteranganDosen.text = status ?: "Data tidak tersedia"
                    if (status == "Air Galon Habis!" || status == "Air Galon Hampir Habis!") {
                        sendFCMNotification("Ruang_dosen", status)
                    }
                    // Tampilkan notifikasi berdasarkan status
                    when (status) {
                        "Air Galon Habis!" -> {
                            showNotification(CHANNEL_ID_2, "Notifikasi Ruang Dosen", status, "alarm")
                        }
                        "Air Galon Hampir Habis!" -> {
                            showNotification(CHANNEL_ID_2, "Notifikasi Ruang Dosen", status, "alarm")
                        }
                        "Yeay air galon sudah terisii :)" -> {
                            showNotification(CHANNEL_ID_2, "Notifikasi Ruang Dosen", status, "yeay")
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NotifMene", "Failed to read value from Ruang_dosen.", error.toException())
            }
        })

        // Mengatur OnClickListener untuk frame dan frame1
        view.findViewById<LinearLayout>(R.id.frame).setOnClickListener {
            showEditNotificationDialog(it.id)
        }
        view.findViewById<LinearLayout>(R.id.frame1).setOnClickListener {
            showEditNotificationDialog(it.id)
        }

        // Mengatur OnClickListener untuk tombol edit waktu
        editWaktuButton.setOnClickListener {
            showEditWaktuDialog()
        }

        // Memulai pengecekan galon kosong secara periodik
        emptyGallonTimerHandler.postDelayed(emptyGallonCheckRunnable, maxWaitTimeMillis)
        createNotificationChannels()
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
    private val emptyGallonCheckRunnable = object : Runnable {
        override fun run() {
            checkEmptyGallonStatus()
            // Ulangi pengecekan setelah waktu yang ditentukan
            emptyGallonTimerHandler.postDelayed(this, maxWaitTimeMillis)
        }
    }

    private fun checkEmptyGallonStatus() {
        ruangTuRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val statusTU = snapshot.child("status").getValue(String::class.java)
                val timeStampTU = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                ruangDosenRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val statusDosen = snapshot.child("status").getValue(String::class.java)
                        val timeStampDosen = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                        val currentTime = System.currentTimeMillis()

                        if (statusTU == "Air Galon Habis!" && (currentTime - timeStampTU > maxWaitTimeMillis)) {
                            showNotification(CHANNEL_ID_1, "Ruang TU", "Air galon di Ruang TU belum diganti selama lebih dari ${maxWaitTimeMillis / (60 * 1000)} menit", "alarm")
                        }

                        if (statusDosen == "Air Galon Habis!" && (currentTime - timeStampDosen > maxWaitTimeMillis)) {
                            showNotification(CHANNEL_ID_2, "Ruang Dosen", "Air galon di Ruang Dosen belum diganti selama lebih dari ${maxWaitTimeMillis / (60 * 1000)} menit", "alarm")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("NotifMene", "Failed to read value from Ruang_dosen.", error.toException())
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NotifMene", "Failed to read value from Ruang_Tu.", error.toException())
            }
        })
    }

    private fun showNotification(channelId: String, title: String, message: String, soundType: String) {
        val context = context ?: return

        val soundUri: Uri = when (soundType) {
            "alarm" -> Uri.parse("android.resource://${requireContext().packageName}/raw/sound_effect_alarm")
            "yeay" -> Uri.parse("android.resource://${requireContext().packageName}/raw/sound_effect_yeay")
            else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        Log.d("NotifMene", "Notification sound URI: $soundUri")

        try {
            val ringtone: Ringtone = RingtoneManager.getRingtone(context, soundUri)
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(title)
            .setContentText(message)
            .setSound(null) // Supaya suara tidak terduplikasi
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel1 = NotificationChannel(
                CHANNEL_ID_1,
                "Channel 1", NotificationManager.IMPORTANCE_HIGH
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
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(channel1)
            notificationManager.createNotificationChannel(channel2)
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission was granted
                Log.d("NotifMene", "Notification permission granted")
            } else {
                // Permission denied
                Toast.makeText(requireContext(), "Permission denied for notifications", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        emptyGallonTimerHandler.removeCallbacks(emptyGallonCheckRunnable)
    }

    private fun showEditWaktuDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        builder.setTitle("Edit Durasi Waktu")

        val dialogLayout = inflater.inflate(R.layout.dialog_edit_waktu, null)
        val editTextWaktu = dialogLayout.findViewById<EditText>(R.id.editTextWaktu)

        builder.setView(dialogLayout)
        builder.setPositiveButton("OK") { _, _ ->
            val newTime = editTextWaktu.text.toString().toLongOrNull()
            if (newTime != null) {
                maxWaitTimeMillis = newTime * 60 * 1000
                Toast.makeText(requireContext(), "Durasi waktu tunggu maksimal diubah menjadi $newTime menit", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Durasi waktu tidak valid", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun showEditNotificationDialog(viewId: Int) {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        builder.setTitle("Edit Pesan Notifikasi")

        val dialogLayout = inflater.inflate(R.layout.dialog_edit_notification, null)
        val editTextMessage = dialogLayout.findViewById<EditText>(R.id.editTextMessage)

        if (viewId == R.id.frame) {
            editTextMessage.setText(keteranganTU.text)
        } else if (viewId == R.id.frame1) {
            editTextMessage.setText(keteranganDosen.text)
        }

        builder.setView(dialogLayout)
        builder.setPositiveButton("Send") { _, _ ->
            val newMessage = editTextMessage.text.toString().trim()
            if (viewId == R.id.frame) {
                keteranganTU.text = newMessage
                ruangTuRef.child("status").setValue(newMessage)
                ruangTuRef.child("meneMessage").setValue(newMessage)  // Update meneMessage in Firebase
                showNotification(CHANNEL_ID_1, "Notifikasi Ruang TU", newMessage, "alarm")
            } else if (viewId == R.id.frame1) {
                keteranganDosen.text = newMessage
                ruangDosenRef.child("status").setValue(newMessage)
                ruangDosenRef.child("meneMessage").setValue(newMessage)  // Update meneMessage in Firebase
                showNotification(CHANNEL_ID_2, "Notifikasi Ruang Dosen", newMessage, "alarm")
            }
            Toast.makeText(requireContext(), "Pesan notifikasi diubah", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }


    companion object {
        private const val CHANNEL_ID_1 = "channel1"
        private const val CHANNEL_ID_2 = "channel2"
    }
}
