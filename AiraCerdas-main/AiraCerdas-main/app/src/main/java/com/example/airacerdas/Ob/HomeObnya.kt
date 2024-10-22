package com.example.airacerdas.Ob

import com.example.airacerdas.Ob.CircularProgressBar
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.airacerdas.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import java.text.SimpleDateFormat
import java.util.*


class HomeObnya : Fragment() {

    private lateinit var nameTextView: TextView
    private lateinit var positionTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var qrCodeImageView: ImageView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var dateTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var circularProgressBar: CircularProgressBar
    private lateinit var databaseReference: DatabaseReference
    private var lastRuangTuWeight = 0.0
    private var lastRuangDosenWeight = 0.0
    private lateinit var circularProgressBar1: CircularProgressBar
    private lateinit var dateTextView1: TextView
    private lateinit var timeTextView1: TextView
    private lateinit var qrCodeImageView1: ImageView
    private lateinit var calendarView: CalendarView
    private val eventsMap = mutableMapOf<String, Int>() // Map to store event dates and colors
    private val handler = Handler(Looper.getMainLooper())
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateTime()
            handler.postDelayed(this, 1000) // Schedule next update in 1 second
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_obnya, container, false)
        nameTextView = view.findViewById(R.id.nama)
        positionTextView = view.findViewById(R.id.jabatannya)
        profileImageView = view.findViewById(R.id.fotoProfile)
        qrCodeImageView = view.findViewById(R.id.qrcode)
        qrCodeImageView1 = view.findViewById(R.id.left_qrcode)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()
        dateTextView = view.findViewById(R.id.tanggal)
        timeTextView = view.findViewById(R.id.waktu)
        dateTextView1 = view.findViewById(R.id.left_tanggal)
        timeTextView1 = view.findViewById(R.id.left_waktu)
        calendarView = view.findViewById(R.id.calendarView)
        loadProfileData()
        setCurrentDateTime()
        setupCalendarView()
        loadCalendarEvents()


        circularProgressBar = view.findViewById(R.id.ruangtu)
        circularProgressBar1 = view.findViewById(R.id.ruangdosen)


        // Mendapatkan referensi ke Firebase Realtime Database
        val database = FirebaseDatabase.getInstance()
        val ruangTuRef = database.getReference("Ruang_Tu/berat")
        val ruangDosenRef = database.getReference("Ruang_dosen/berat")

        ruangTuRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val beratGalon = snapshot.getValue(Double::class.java)
                val maxCapacity = 19.0
                val persentase = ((beratGalon ?: 0.0) / maxCapacity * 100).toInt()
                val thresholdPercentage = 100 // Ubah kondisi menjadi persentase 100%
                val beratSaatIni = beratGalon ?: 0.0

                circularProgressBar.setMax(100)
                circularProgressBar.setProgress(persentase)

                // Periksa jika persentase mencapai atau melebihi 100%
                if (persentase >= thresholdPercentage && lastRuangTuWeight < maxCapacity) {
                    catatPergantianGalon("Ruang TU")
                }

                lastRuangTuWeight = beratSaatIni
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching data: $error")
            }
        })


        ruangDosenRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val beratGalon = snapshot.getValue(Double::class.java)
                val maxCapacity = 19.0
                val persentase = ((beratGalon ?: 0.0) / maxCapacity * 100).toInt()
                val thresholdWeight = 100
                val beratSaatIni = beratGalon ?: 0.0

                circularProgressBar1.setMax(100)
                circularProgressBar1.setProgress(persentase)

                if (beratSaatIni <= thresholdWeight && lastRuangDosenWeight > thresholdWeight) {
                    catatPergantianGalon("Ruang Dosen")
                }

                lastRuangDosenWeight = beratSaatIni
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching data: $error")
            }
        })



        qrCodeImageView.setOnClickListener {
            val integrator = IntentIntegrator.forSupportFragment(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            integrator.setPrompt("Scan a QR code")
            integrator.setCameraId(0) // Use a specific camera of the device
            integrator.setBeepEnabled(true)
            integrator.setBarcodeImageEnabled(true)
            integrator.initiateScan()
        }

        qrCodeImageView1.setOnClickListener {
            val integrator = IntentIntegrator.forSupportFragment(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            integrator.setPrompt("Scan a QR code")
            integrator.setCameraId(0) // Use a specific camera of the device
            integrator.setBeepEnabled(true)
            integrator.setBarcodeImageEnabled(true)
            integrator.initiateScan()
        }
        startUpdatingTime()

        return view
    }
    private fun updateTime() {
        val calendar = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = timeFormat.format(calendar.time)
        timeTextView.text = currentTime
        timeTextView1.text = currentTime
    }

    private fun startUpdatingTime() {
        handler.post(updateTimeRunnable)
    }
    private fun loadProfileData() {
        val userId = firebaseAuth.currentUser?.uid
        userId?.let { uid ->
            firebaseFirestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val jabatan = document.getString("jabatan")
                        val nama = document.getString("nama")
                        positionTextView.text = jabatan
                        nameTextView.text = nama
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ProfileMenu", "Error getting user data", exception)
                }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ensure fragment is attached to the activity
        val context = context ?: return  // Use context directly if not null

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val storageRef = FirebaseStorage.getInstance().reference
        val profilePictureRef = storageRef.child("profile_pictures").child("$userId.jpg")

        profilePictureRef.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(context)
                .load(uri)
                .placeholder(R.drawable.ic_profile_placeholder)
                .into(profileImageView)
        }.addOnFailureListener { e ->
            Log.e("ProfileMenu", "Error loading profile picture from Firebase Storage", e)
        }
    }



    private fun setCurrentDateTime() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val currentDate = dateFormat.format(calendar.time)
        val currentTime = timeFormat.format(calendar.time)

        dateTextView.text = currentDate

        dateTextView1.text = currentDate

    }





    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(context, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                // Process the scan result here
                val scanResult = result.contents.trim()
                when {
                    scanResult.equals("Ruang TU FTI Yarsi", ignoreCase = true) -> {
                        // Show left_date_time_section and hide others
                        showSection(R.id.left_date_time_section)
                        hideSection(R.id.date_time_section)
                        showScanResultDialog(result.contents)
                    }
                    scanResult.equals("Ruang Dosen FTI Yarsi", ignoreCase = true) -> {
                        // Show date_time_section and hide others
                        hideSection(R.id.left_date_time_section)
                        showSection(R.id.date_time_section)
                        showScanResultDialog(result.contents)

                    }
                    else -> {
                        Toast.makeText(context, "Arduino di lokasi " + scanResult + " belum di buat", Toast.LENGTH_SHORT).show()
                        resetViews()
                    }
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun showSection(sectionId: Int) {
        val sectionView = view?.findViewById<View>(sectionId)
        sectionView?.visibility = View.VISIBLE
    }

    private fun hideSection(sectionId: Int) {
        val sectionView = view?.findViewById<View>(sectionId)
        sectionView?.visibility = View.GONE
    }
    private fun resetViews() {
        showSection(R.id.left_date_time_section)
        showSection(R.id.date_time_section)

    }

    private fun showScanResultDialog(contents: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hasil Scan")
            .setMessage("Isi Barcode: $contents")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    private fun catatPergantianGalon(room: String) {
        val userId = firebaseAuth.currentUser?.uid
        userId?.let { uid ->
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            val catatan = hashMapOf(
                "tanggal" to currentDate,
                "waktu" to currentTime,
                "ruangan" to room
            )

            firebaseFirestore.collection("pergantian_galon").document(uid)
                .collection("catatan").add(catatan)
                .addOnSuccessListener {
                    Log.d("Firestore", "Catatan pergantian galon berhasil ditambahkan")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error adding document", e)
                }
        }
    }

    private fun setupCalendarView() {
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, dayOfMonth)
            val selectedDate = selectedCalendar.time
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selectedDateString = dateFormat.format(selectedDate)
            showEventDetails(selectedDateString)
        }
    }

    private fun loadCalendarEvents() {
        val userId = firebaseAuth.currentUser?.uid
        userId?.let { uid ->
            firebaseFirestore.collection("pergantian_galon").document(uid)
                .collection("catatan")
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val tanggalString = document.getString("tanggal")
                        tanggalString?.let {
                            eventsMap[it] = getColorForEvent(document.id) // Store date and color in eventsMap
                        }
                    }
                    decorateCalendar()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error fetching calendar events", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
        }
    }

    private fun getColorForEvent(eventId: String): Int {
        // Example logic to determine color based on room or event ID
        return when (eventId) {
            "Ruang_Tu" -> Color.RED
            "Ruang_Dosen" -> Color.YELLOW
            else -> Color.GREEN
        }
    }

    private fun decorateCalendar() {
        // Loop through each day in the calendar
        for (i in 0 until calendarView.childCount) {
            val dayView = calendarView.getChildAt(i)
            if (dayView is ViewGroup) {
                val dayTextView = dayView.getChildAt(0) as? TextView // Get the TextView for the day
                dayTextView?.let {
                    val dateString = it.text.toString()
                    if (eventsMap.containsKey(dateString)) {
                        val color = eventsMap[dateString] ?: Color.WHITE
                        it.setTextColor(color) // Set text color of the day based on eventsMap
                    }
                }
            }
        }
    }

    private fun showEventDetails(selectedDateString: String) {
        val userId = firebaseAuth.currentUser?.uid
        userId?.let { uid ->
            firebaseFirestore.collection("pergantian_galon").document(uid)
                .collection("catatan")
                .whereEqualTo("tanggal", selectedDateString)
                .get()
                .addOnSuccessListener { documents ->
                    val sb = StringBuilder()
                    for (document in documents) {
                        val waktu = document.getString("waktu")
                        val ruangan = document.getString("ruangan")
                        sb.append("- Galon air di $ruangan telah diganti pada pukul $waktu\n")
                    }
                    if (sb.isEmpty()) {
                        sb.append("Tidak ada catatan untuk tanggal ini")
                    }
                    showEventDetailsDialog(sb.toString())
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error fetching event details", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
        }
    }

    private fun showEventDetailsDialog(details: String) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Catatan Pergantian Galon")
            .setMessage(details)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }
}
