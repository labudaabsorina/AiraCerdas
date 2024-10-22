package com.example.airacerdas

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class Next : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_next)
        val button1 = findViewById<Button>(R.id.button)

        // Menetapkan OnClickListener setelah mendapatkan referensi tombol
        button1.setOnClickListener{
            val intent1 = Intent(this, Pilih::class.java)
            startActivity(intent1)
        }
        val videoView = findViewById<VideoView>(R.id.videoView)

        // URL video dari internet (gunakan format yang benar untuk Google Drive)
        val videoUrl = "https://drive.google.com/uc?export=download&id=1N-7XqoX9XDRVS9JV8gpeZzuf0n14uexd"
        val uri = Uri.parse(videoUrl)

        videoView.setVideoURI(uri)

        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true // Jika ingin memutar ulang video
            mp.start()
        }

        videoView.setOnErrorListener { _, _, _ ->
            Toast.makeText(this, "Tidak dapat memutar video", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
