package com.example.airacerdas

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class Pilih : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pilih)
        val button1 = findViewById<Button>(R.id.button3)

        // Menetapkan OnClickListener setelah mendapatkan referensi tombol
        button1.setOnClickListener{
            val intent1 = Intent(this, Login::class.java)
            startActivity(intent1)
        }
        val button2 = findViewById<Button>(R.id.button4)

        // Menetapkan OnClickListener setelah mendapatkan referensi tombol
        button2.setOnClickListener{
            val intent1 = Intent(this, Register::class.java)
            startActivity(intent1)
        }
    }
}