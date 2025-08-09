package com.danono.paws.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.danono.paws.R
import com.danono.paws.adapters.DogAdapter
import com.danono.paws.adapters.RemindersAdapter
import com.danono.paws.databinding.FragmentHomeBinding
import com.danono.paws.ui.mydogs.SharedDogsViewModel
import com.danono.paws.utilities.FirebaseDataManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * HomeFragment shows:
 * - Header (title + add-dog FAB)
 * - Horizontal dogs list
 * - Cards: nearby parks, upcoming reminders (next 5)
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedViewModel: SharedDogsViewModel
    private lateinit var dogAdapter: DogAdapter
    private lateinit var remindersAdapter: RemindersAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Shared ViewModel for dogs
        sharedViewModel = ViewModelProvider(requireActivity())[SharedDogsViewModel::class.java]

        setupClickListeners()
        setupDogsRecyclerView()
        setupRemindersRecyclerView()
        loadDogs()
        loadUpcomingReminders()
        observeViewModel()

        binding.homeRVDogs.isNestedScrollingEnabled = false
        binding.homeRVReminders.isNestedScrollingEnabled = false

    }

    override fun onResume() {
        super.onResume()
        // Refresh dogs and reminders whenever coming back to home
        sharedViewModel.refreshDogs()
        loadUpcomingReminders()
    }

    // -------------------- Clicks --------------------

    private fun setupClickListeners() {
        binding.addDog.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_addDogFragment)
        }
        binding.homeBtnViewAllParks.setOnClickListener {
            findNavController().navigate(R.id.navigation_dog_parks)
        }
        binding.homeFabAddReminder.setOnClickListener {
            findNavController().navigate(R.id.navigation_reminders)
        }
    }

    // -------------------- Dogs --------------------

    private fun setupDogsRecyclerView() {
        dogAdapter = DogAdapter(emptyList()) { dog, dogId ->
            handleDogClick(dog, dogId)
        }
        binding.homeRVDogs.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = dogAdapter
        }
    }

    private fun loadDogs() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            sharedViewModel.loadDogsFromFirestore()
        }
    }

    private fun observeViewModel() {
        sharedViewModel.dogs.observe(viewLifecycleOwner) { dogsWithIds ->
            dogAdapter = DogAdapter(dogsWithIds) { dog, dogId ->
                handleDogClick(dog, dogId)
            }
            binding.homeRVDogs.adapter = dogAdapter
        }
    }

    private fun handleDogClick(dog: com.danono.paws.model.Dog, dogId: String) {
        sharedViewModel.selectDog(dog, dogId)
        findNavController().navigate(R.id.action_navigation_home_to_dogProfileFragment)
    }

    // -------------------- Upcoming reminders --------------------

    private fun setupRemindersRecyclerView() {
        remindersAdapter = RemindersAdapter(emptyList()) {
            findNavController().navigate(R.id.navigation_reminders)
        }
        binding.homeRVReminders.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = remindersAdapter
        }
    }

    private fun loadUpcomingReminders() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseDataManager.getInstance().getUpcomingReminders(limit = 5)
            if (result.isSuccess) {
                val upcoming = result.getOrNull().orEmpty()
                remindersAdapter = RemindersAdapter(upcoming) {
                    findNavController().navigate(R.id.navigation_reminders)
                }
                binding.homeRVReminders.adapter = remindersAdapter
            } else {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to load reminders", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // -------------------- Lifecycle --------------------

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
