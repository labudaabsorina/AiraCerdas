package com.example.airacerdas

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Register : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailEditText = findViewById<EditText>(R.id.emailnya)
        val passwordEditText = findViewById<EditText>(R.id.passET)
        val confirmPasswordEditText = findViewById<EditText>(R.id.passET1)
        val spinnerJabatan = findViewById<Spinner>(R.id.spinner_jabatan)

        val button1 = findViewById<Button>(R.id.button6)
        button1.setOnClickListener {
            val intent1 = Intent(this, Login::class.java)
            startActivity(intent1)
        }

        val button2 = findViewById<Button>(R.id.btnRegister)
        button2.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()
            val jabatan = spinnerJabatan.selectedItem.toString()

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEditText.error = "Enter a valid email"
                emailEditText.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty() || password.length < 1) {
                passwordEditText.error = "Password must be at least 1 characters"
                passwordEditText.requestFocus()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                confirmPasswordEditText.error = "Passwords do not match"
                confirmPasswordEditText.requestFocus()
                return@setOnClickListener
            }

            if (jabatan == "Pilih Jabatan") {
                Toast.makeText(this, "Please select a valid position", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(email, password, jabatan)
        }

        val signin = findViewById<TextView>(R.id.textView6)
        signin.setOnClickListener {
            val intent1 = Intent(this, Login::class.java)
            startActivity(intent1)
        }
    }

    private fun registerUser(email: String, password: String, jabatan: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                val userId = user?.uid

                val userMap = hashMapOf(
                    "email" to email,
                    "password" to password,
                    "jabatan" to jabatan
                )

                if (userId != null) {
                    // Tambahkan data pengguna ke Firestore
                    db.collection("users").document(userId).set(userMap).addOnSuccessListener {
                        Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, Login::class.java)
                        startActivity(intent)
                        finish()
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save user: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
