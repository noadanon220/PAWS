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

/**
 * Profile setup screen with image picker/camera.
 * Falls back to user_default_img if the user has no photo.
 */
class SetupProfileFragment : Fragment() {

    private var _binding: FragmentSetupProfileBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Selected/captured image URIs
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    // Gallery picker
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            // Update preview with the selected image
            binding.profileImgAvatar.setImageURI(it)
        }
    }

    // Camera capture
    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            selectedImageUri = cameraImageUri
            // Update preview with the captured image
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

        // Prefill display name if exists
        binding.loginEDTEmail.setText(currentUser?.displayName ?: "")

        // Ensure initial photo state (existing photo or default)
        applyInitialPhoto(currentUser?.photoUrl)

        // Save profile (display name + optional photo uri)
        binding.profileBtnSave.setOnClickListener {
            val newName = binding.loginEDTEmail.text.toString().trim()
            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val builder = UserProfileChangeRequest.Builder()
                .setDisplayName(newName)

            // If user picked or captured an image, set it on the auth profile.
            // Note: For persistence across devices, upload to Firebase Storage and use the download URL.
            selectedImageUri?.let { builder.setPhotoUri(it) }

            currentUser?.updateProfile(builder.build())
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        findNavController().navigate(R.id.action_setupProfileFragment_to_navigation_home)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to update profile: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        // Open image source options
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

    // Request camera permission if needed
    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Launch camera capture flow
    private fun launchCamera() {
        cameraImageUri = createImageUri()
        takePhotoLauncher.launch(cameraImageUri)
    }

    // Create output Uri for the captured image
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

    // Initialize avatar with selected/existing/default image
    private fun applyInitialPhoto(existingPhotoUri: Uri?) {
        when {
            selectedImageUri != null -> binding.profileImgAvatar.setImageURI(selectedImageUri)
            existingPhotoUri != null   -> binding.profileImgAvatar.setImageURI(existingPhotoUri)
            else                       -> binding.profileImgAvatar.setImageResource(R.drawable.user_default_img)
        }
    }
}
