package com.danono.paws.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.danono.paws.R
import com.danono.paws.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth

/**
 * Settings screen shows display name, email, and avatar from FirebaseAuth.photoUrl.
 * If no photoUrl, shows default avatar.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()

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
        setupUserInfo()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // Refresh in case profile changed elsewhere
        setupUserInfo()
    }

    private fun setupUserInfo() {
        val user = auth.currentUser
        binding.userName.text = user?.displayName ?: "User"
        binding.userEmail.text = user?.email ?: ""

        val photoUrl = user?.photoUrl
        if (photoUrl != null) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.user_default_img)
                .error(R.drawable.user_default_img)
                .centerCrop()
                .into(binding.settingsImgAvatar)
        } else {
            binding.settingsImgAvatar.setImageResource(R.drawable.user_default_img)
        }
    }

    private fun setupClickListeners() {
        // Personal info row (optional: open edit dialog/screen)
        binding.personalInfoLayout.setOnClickListener {
            // e.g., open your EditProfileDialogFragment if you want inline editing
        }

        // Logout
        binding.logoutLayout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), com.danono.paws.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
