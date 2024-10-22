package com.example.airacerdas

import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)

        val emailEditText = findViewById<EditText>(R.id.emailnya)
        val passwordEditText = findViewById<EditText>(R.id.passET)
        val jabatanSpinner = findViewById<Spinner>(R.id.spinner_jabatan)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val signUpButton = findViewById<Button>(R.id.button7)
        val rememberMeCheckbox = findViewById<CheckBox>(R.id.rememberMeCheckbox)
        val forgotPasswordTextView = findViewById<TextView>(R.id.lupapw)

        val rememberedEmail = sharedPreferences.getString("email", "")
        val rememberedPassword = sharedPreferences.getString("password", "")

        if (rememberedEmail != null && rememberedEmail.isNotEmpty() &&
            rememberedPassword != null && rememberedPassword.isNotEmpty()) {
            emailEditText.setText(rememberedEmail)
            passwordEditText.setText(rememberedPassword)
            rememberMeCheckbox.isChecked = true
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val jabatan = jabatanSpinner.selectedItem.toString()

            if (email.isEmpty()) {
                emailEditText.error = "Email is required"
                emailEditText.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordEditText.error = "Password is required"
                passwordEditText.requestFocus()
                return@setOnClickListener
            }

            if (jabatan == "Pilih Jabatan") {
                Toast.makeText(this, "Please select a valid job position", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password, jabatan, rememberMeCheckbox.isChecked)
        }

        signUpButton.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        val signup = findViewById<TextView>(R.id.textView6)
        signup.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        forgotPasswordTextView.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isEmpty()) {
                emailEditText.error = "Email is required to reset password"
                emailEditText.requestFocus()
                return@setOnClickListener
            }
            sendPasswordResetEmail(email)
        }
    }

    private fun loginUser(email: String, password: String, selectedJabatan: String, rememberMe: Boolean) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid

                    userId?.let { uid ->
                        db.collection("users").document(uid)
                            .get()
                            .addOnSuccessListener { document ->
                                val storedJabatan = document?.getString("jabatan")

                                if (storedJabatan != null) {
                                    if (storedJabatan == selectedJabatan) {
                                        if (rememberMe) {
                                            val editor = sharedPreferences.edit()
                                            editor.putString("email", email)
                                            editor.putString("password", password)
                                            editor.apply()
                                        } else {
                                            sharedPreferences.edit().clear().apply()
                                        }

                                        when (storedJabatan) {
                                            "Manajemen FTI" -> {
                                                val intent = Intent(this, ManajemenFTI::class.java)
                                                startActivity(intent)
                                                finish()
                                            }
                                            "OB FTI" -> {
                                                val intent = Intent(this, OB::class.java)
                                                startActivity(intent)
                                                finish()
                                            }
                                            else -> {
                                                Toast.makeText(this, "Invalid job position", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(this, "Selected job position does not match with the registered job position", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(this, "Job position is null", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(this, "Error getting document: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(baseContext, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val newPasswordDialog = AlertDialog.Builder(this)
                    newPasswordDialog.setTitle("Enter New Password")
                    val input = EditText(this)
                    newPasswordDialog.setView(input)

                    newPasswordDialog.setPositiveButton("OK") { dialog, which ->
                        val newPassword = input.text.toString()
                        auth.signInWithEmailAndPassword(email, newPassword).addOnCompleteListener { signInTask ->
                            if (signInTask.isSuccessful) {
                                val user = auth.currentUser
                                user?.updatePassword(newPassword)
                                    ?.addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val userId = user.uid
                                            val userData = hashMapOf(
                                                "password" to newPassword
                                            )

                                            db.collection("users").document(userId)
                                                .update(userData as Map<String, Any>)
                                                .addOnSuccessListener {
                                                    Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_SHORT).show()
                                                }
                                                .addOnFailureListener { exception ->
                                                    Toast.makeText(this, "Failed to update password in Firestore: ${exception.message}", Toast.LENGTH_SHORT).show()
                                                }
                                        } else {
                                            Toast.makeText(this, "Failed to update password in Firebase Authentication: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(this, "Failed to sign in with new password: ${signInTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    newPasswordDialog.setNegativeButton("Cancel") { dialog, which ->
                        dialog.cancel()
                    }

                    newPasswordDialog.show()
                } else {
                    Toast.makeText(this, "Failed to send password reset email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }


    }
}
