package com.example.airacerdas.Ob

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.airacerdas.Login
import com.example.airacerdas.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream


class ProfileOB : Fragment() {

    private lateinit var emailTextView: TextView
    private lateinit var passwordTextView: TextView
    private lateinit var jabatanTextView: TextView
    private lateinit var namaEditText: EditText
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseFirestore: FirebaseFirestore
    private var isPasswordVisible = false
    private lateinit var profilePictureImageView: ImageView
    private val GALLERY_REQUEST_CODE = 123
    private val CAMERA_REQUEST_CODE = 456

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile_o_b, container, false)

        emailTextView = view.findViewById(R.id.email)
        passwordTextView = view.findViewById(R.id.password)
        jabatanTextView = view.findViewById(R.id.jabatan)
        namaEditText = view.findViewById(R.id.NamaUser)
        profilePictureImageView = view.findViewById(R.id.profilePicture)
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()

        val userId = firebaseAuth.currentUser?.uid
        userId?.let { uid ->
            firebaseFirestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val email = document.getString("email")
                        val password = document.getString("password")
                        val jabatan = document.getString("jabatan")
                        val nama = document.getString("nama")

                        emailTextView.text = email
                        passwordTextView.text = password
                        jabatanTextView.text = jabatan
                        namaEditText.setText(nama)
                        updatePasswordVisibility()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ProfileMenu", "Error getting user data", exception)
                }
        }

        view.findViewById<View>(R.id.passwordToggle).setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            updatePasswordVisibility()
        }

        view.findViewById<View>(R.id.logout).setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(requireActivity(), Login::class.java))
            requireActivity().finish()
        }

        view.findViewById<View>(R.id.editProfilePicture).setOnClickListener {
            showProfilePictureOptions()
        }

        view.findViewById<View>(R.id.editNama).setOnClickListener {
            val newName = namaEditText.text.toString().trim()
            if (newName.isNotEmpty()) {
                updateUserName(newName)
            } else {
                Toast.makeText(requireContext(), "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun updateUserName(newName: String) {
        val userId = firebaseAuth.currentUser?.uid ?: return

        firebaseFirestore.collection("users").document(userId)
            .update("nama", newName)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Nama berhasil diubah", Toast.LENGTH_SHORT).show()
                namaEditText.setText(newName)
            }
            .addOnFailureListener { e ->
                Log.e("ProfileMenu", "Error updating name", e)
                Toast.makeText(requireContext(), "Gagal mengubah nama", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showProfilePictureOptions() {
        val options = arrayOf("Choose from Gallery", "Take Photo")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Update Profile Picture")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openGallery()
                1 -> openCamera()
            }
        }
        builder.create().show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    private fun uploadImageToStorage(imageUri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val storageRef = FirebaseStorage.getInstance().reference
        val profilePictureRef = storageRef.child("profile_pictures").child("$userId.jpg")

        profilePictureRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                profilePictureRef.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
                }.addOnFailureListener { e ->
                    onFailure(e)
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    private fun updateProfilePictureInFirestore(imageUrl: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.e("ProfileMenu", "User is not authenticated")
            return
        }

        val userRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)

        userRef.update(mapOf("imageUri" to imageUrl))
            .addOnSuccessListener {
                Log.d("ProfileMenu", "Profile picture URL updated in Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("ProfileMenu", "Error updating profile picture URL in Firestore", e)
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {
                    val selectedImageUri = data?.data
                    if (selectedImageUri != null) {
                        profilePictureImageView.setImageURI(selectedImageUri)
                        uploadImageToStorage(selectedImageUri, { imageUrl ->
                            updateProfilePictureInFirestore(imageUrl)
                        }, { e ->
                            Log.e("ProfileMenu", "Error uploading profile picture from gallery", e)
                        })
                    }
                }
                CAMERA_REQUEST_CODE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    profilePictureImageView.setImageBitmap(imageBitmap)
                    val imageUri = getImageUriFromBitmap(requireContext(), imageBitmap)
                    if (imageUri != null) {
                        uploadImageToStorage(imageUri, { imageUrl ->
                            updateProfilePictureInFirestore(imageUrl)
                        }, { e ->
                            Log.e("ProfileMenu", "Error uploading profile picture from camera", e)
                        })
                    }
                }
            }
        }
    }

    private fun getImageUriFromBitmap(context: Context, bitmap: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "TempImage", null)
        return Uri.parse(path)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val storageRef = FirebaseStorage.getInstance().reference
            val profilePictureRef = storageRef.child("profile_pictures").child("$userId.jpg")

            profilePictureRef.downloadUrl.addOnSuccessListener { uri ->
                if (isAdded) {
                    Glide.with(requireContext())
                        .load(uri)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .into(profilePictureImageView)
                }
            }.addOnFailureListener { e ->
                if (isAdded) {
                    Log.e("ProfileMenu", "Error loading profile picture from Firebase Storage", e)
                }
            }
        }
    }


    private fun updatePasswordVisibility() {
        passwordTextView.transformationMethod = if (isPasswordVisible) {
            null
        } else {
            android.text.method.PasswordTransformationMethod.getInstance()
        }
    }
}
