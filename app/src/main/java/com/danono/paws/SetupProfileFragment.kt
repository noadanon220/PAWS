package com.danono.paws

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.danono.paws.R
import com.danono.paws.databinding.FragmentSetupProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

/**
 * Fragment allowing a newly registered user to complete their profile.
 * This implementation matches the layout you provided (which includes username and save button).
 */
class SetupProfileFragment : Fragment() {
    private var _binding: FragmentSetupProfileBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using FragmentSetupProfileBinding (matches fragment_setup_profile.xml)
        _binding = FragmentSetupProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = auth.currentUser

        // Pre-populate the username field with the current display name, if available
        // שים לב לשם השדה: login_EDT_email -> loginEDTEmail
        binding.loginEDTEmail.setText(currentUser?.displayName ?: "")

        // Handle save profile button
        // כפתור profile_btn_save הופך ל־profileBtnSave במחלקת binding
        binding.profileBtnSave.setOnClickListener {
            val newName = binding.loginEDTEmail.text.toString().trim()
            if (newName.isNotEmpty()) {
                // Update the Firebase user profile with the chosen display name
                currentUser?.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build()
                )?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Navigate back to the home screen
                        findNavController().navigate(R.id.action_setupProfileFragment_to_navigation_home)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to update profile: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Please enter a username", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
