package com.danono.paws.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.danono.paws.R
import com.danono.paws.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth

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

    private fun setupUserInfo() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            binding.userName.text = user.displayName ?: "User"
            binding.userEmail.text = user.email ?: ""
        }
    }

    private fun setupClickListeners() {
        // Personal Info
        binding.personalInfoLayout.setOnClickListener {
            // Navigate to personal info screen
        }

        // Language
        binding.languageLayout.setOnClickListener {
            // Navigate to language selection
        }

        // Notifications
        binding.notificationLayout.setOnClickListener {
            // Navigate to notification settings
        }

        // Dark Mode Toggle
        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Handle dark mode toggle
            // You can save this preference and apply theme
        }

        // Help
        binding.helpLayout.setOnClickListener {
            // Navigate to help screen
        }

        // Back button
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}