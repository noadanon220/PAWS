package com.danono.paws

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.danono.paws.databinding.FragmentSetupProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage

/**
 * Profile setup screen with image picker/camera.
 * Uploads the chosen image to Firebase Storage and updates FirebaseAuth (displayName + photoUrl).
 */
class SetupProfileFragment : Fragment() {

    private var _binding: FragmentSetupProfileBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    // Selected/captured image URIs
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    // Gallery picker
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.profileImgAvatar.setImageURI(it)
        }
    }

    // Camera capture
    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            selectedImageUri = cameraImageUri
            binding.profileImgAvatar.setImageURI(cameraImageUri)
        } else {
            Toast.makeText(requireContext(), "Failed to capture image", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera permission
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = auth.currentUser
        binding.loginEDTEmail.setText(currentUser?.displayName ?: "")
        applyInitialPhoto(currentUser?.photoUrl)

        binding.profileBtnSave.setOnClickListener {
            val user = auth.currentUser
            val newName = binding.loginEDTEmail.text.toString().trim()

            if (user == null) {
                Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // If image selected -> upload, then update Auth profile with download URL + name
            val localUri = selectedImageUri
            if (localUri != null) {
                val ref = storage.reference.child("users/${user.uid}/profile/${System.currentTimeMillis()}.jpg")
                ref.putFile(localUri)
                    .continueWithTask { task ->
                        if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                        ref.downloadUrl
                    }
                    .addOnSuccessListener { downloadUri ->
                        val updates = UserProfileChangeRequest.Builder()
                            .setDisplayName(newName)
                            .setPhotoUri(downloadUri)
                            .build()
                        user.updateProfile(updates).addOnCompleteListener { t ->
                            if (t.isSuccessful) {
                                goToHome()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to update profile: ${t.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // No image selected: update name only
                val updates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build()
                user.updateProfile(updates).addOnCompleteListener { t ->
                    if (t.isSuccessful) {
                        goToHome()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to update profile: ${t.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        binding.profileBtnCamera.setOnClickListener { showImagePickerOptions() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Presents gallery/camera options
    private fun showImagePickerOptions() {
        val options = arrayOf("Choose from phone", "Take a photo")
        AlertDialog.Builder(requireContext())
            .setTitle("Choose image source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageLauncher.launch("image/*")
                    1 -> requestCameraPermission()
                }
            }
            .show()
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        cameraImageUri = createImageUri()
        takePhotoLauncher.launch(cameraImageUri)
    }

    private fun createImageUri(): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "profile_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        return requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )!!
    }

    private fun applyInitialPhoto(existingPhotoUri: Uri?) {
        when {
            selectedImageUri != null -> binding.profileImgAvatar.setImageURI(selectedImageUri)
            existingPhotoUri != null -> binding.profileImgAvatar.setImageURI(existingPhotoUri)
            else -> binding.profileImgAvatar.setImageResource(R.drawable.user_default_img)
        }
    }

    private fun goToHome() {
        findNavController().navigate(R.id.action_setupProfileFragment_to_navigation_home)
    }
}
