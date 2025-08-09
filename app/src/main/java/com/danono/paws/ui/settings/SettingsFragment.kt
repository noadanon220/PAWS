package com.danono.paws.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.danono.paws.R
import com.danono.paws.databinding.FragmentSettingsBinding
import com.danono.paws.utilities.ImageLoader
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.android.material.floatingactionbutton.FloatingActionButton


class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()

    // Holds a picked avatar while dialog is open
    private var pendingAvatarUri: Uri? = null
    private var currentDialogAvatar: ImageView? = null

    // Gallery picker
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                pendingAvatarUri = uri
                currentDialogAvatar?.setImageURI(uri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderUserHeader()
        setupClicks()
    }

    private fun renderUserHeader() {
        val user = auth.currentUser
        binding.userName.text = user?.displayName ?: "User"
        binding.userEmail.text = user?.email ?: ""

        val loader = ImageLoader.getInstance()
        val avatarView = binding.settingsIMGAvatar
        val photo = user?.photoUrl
        if (photo != null) {
            loader.loadImage(photo, avatarView, R.drawable.user_default_img)
        } else {
            avatarView.setImageResource(R.drawable.user_default_img)
        }
    }

    private fun setupClicks() {
        // Open edit dialog by tapping the row or the arrow
        binding.personalInfoLayout.setOnClickListener { showEditProfileDialog() }
        binding.settingsIMGArrow.setOnClickListener { showEditProfileDialog() }

        // Logout
        binding.logoutLayout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), com.danono.paws.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun showEditProfileDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_edit_profile, null, false)

        val imgAvatar = view.findViewById<ImageView>(R.id.edit_img_avatar)
        val btnChange = view.findViewById<FloatingActionButton>(R.id.edit_btn_change_photo)
        btnChange.setOnClickListener { pickImage.launch("image/*") }
        val edtName = view.findViewById<TextInputEditText>(R.id.edit_edt_name)

        currentDialogAvatar = imgAvatar
        pendingAvatarUri = null

        // Prefill
        val user = auth.currentUser
        edtName.setText(user?.displayName.orEmpty())
        val loader = ImageLoader.getInstance()
        user?.photoUrl?.let { loader.loadImage(it, imgAvatar, R.drawable.user_default_img) }
            ?: imgAvatar.setImageResource(R.drawable.user_default_img)

        btnChange.setOnClickListener { pickImage.launch("image/*") }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.settings_title))
            .setView(view)
            .setPositiveButton(R.string.save, null) // we override later to validate
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val positive = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            positive.setOnClickListener {
                val name = edtName.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.please_enter_display_name), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val req = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .apply { pendingAvatarUri?.let { setPhotoUri(it) } }
                    .build()

                user?.updateProfile(req)?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        renderUserHeader()
                        Toast.makeText(requireContext(), getString(R.string.profile_updated), Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            task.exception?.message ?: "Failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        currentDialogAvatar = null
        pendingAvatarUri = null
    }
}
