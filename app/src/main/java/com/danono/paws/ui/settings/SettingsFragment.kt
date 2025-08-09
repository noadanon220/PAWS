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
import android.content.Intent

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


        // Back button
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // Logout
        // Add a click listener to the logout row. When the user taps this option we
        // sign out from FirebaseAuth and return to the login screen. We also clear
        // the back stack so the user cannot navigate back into the app after
        // logging out.
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
