package com.danono.paws.ui.home

import android.os.Bundle
import android.util.Log
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
 * HomeFragment displays a header with the app name and an Add Dog button,
 * a horizontal list of dogs, and separate cards for nearby dog parks and
 * upcoming reminders. Whenever the fragment is resumed, it refreshes
 * the dogs list and upcoming reminders.
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

        // Initialize shared ViewModel for dogs
        sharedViewModel = ViewModelProvider(requireActivity())[SharedDogsViewModel::class.java]

        setupClickListeners()
        setupDogsRecyclerView()
        setupRemindersRecyclerView()
        loadDogs()
        loadUpcomingReminders()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Refresh dogs and reminders whenever returning to the home screen
        sharedViewModel.refreshDogs()
        loadUpcomingReminders()
    }

    /**
     * Set up click listeners for header actions:
     * - Add Dog button
     * - View All Parks link
     * - Add Reminder button
     */
    private fun setupClickListeners() {
        // Navigate to Add Dog screen
        binding.addDog.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_addDogFragment)
        }
        // Navigate to Dog Parks screen
        binding.homeBtnViewAllParks.setOnClickListener {
            findNavController().navigate(R.id.navigation_dog_parks)
        }
        // Navigate to Reminders screen
        binding.homeFabAddReminder.setOnClickListener {
            findNavController().navigate(R.id.navigation_reminders)
        }
    }

    /**
     * Initialize horizontal RecyclerView for user's dogs.
     */
    private fun setupDogsRecyclerView() {
        dogAdapter = DogAdapter(emptyList()) { dog, dogId ->
            handleDogClick(dog, dogId)
        }
        binding.homeRVDogs.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = dogAdapter
        }
    }

    /**
     * Initialize vertical RecyclerView for upcoming reminders.
     */
    private fun setupRemindersRecyclerView() {
        remindersAdapter = RemindersAdapter(emptyList()) {
            findNavController().navigate(R.id.navigation_reminders)
        }
        binding.homeRVReminders.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = remindersAdapter
        }
    }

    /**
     * Load dogs for the current user from Firestore.
     */
    private fun loadDogs() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            sharedViewModel.loadDogsFromFirestore()
        } else {
            Log.w("HomeFragment", "No user logged in")
        }
    }

    /**
     * Fetch upcoming reminders from the database, filter out any reminders
     * whose date/time has already passed, sort them by date and time,
     * limit to the next five reminders, and update the adapter.
     */
    private fun loadUpcomingReminders() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = FirebaseDataManager.getInstance().getAllReminders()
            if (result.isSuccess) {
                val now = System.currentTimeMillis()
                val reminders = result.getOrNull() ?: emptyList()
                // Filter for future reminders only, sort and take up to five
                val upcoming = reminders
                    .filter { it.dateTime >= now }
                    .sortedBy { it.dateTime }
                    .take(5)

                remindersAdapter = RemindersAdapter(upcoming) {
                    findNavController().navigate(R.id.navigation_reminders)
                }
                binding.homeRVReminders.adapter = remindersAdapter
            } else {
                Toast.makeText(
                    requireContext(),
                    "Failed to load reminders",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Observe changes in the dogs list from the ViewModel
     * and update the RecyclerView accordingly.
     */
    private fun observeViewModel() {
        sharedViewModel.dogs.observe(viewLifecycleOwner) { dogsWithIds ->
            dogAdapter = DogAdapter(dogsWithIds) { dog, dogId ->
                handleDogClick(dog, dogId)
            }
            binding.homeRVDogs.adapter = dogAdapter
        }
    }

    /**
     * Handle clicking on a dog card: select the dog and navigate to its profile.
     */
    private fun handleDogClick(dog: com.danono.paws.model.Dog, dogId: String) {
        sharedViewModel.selectDog(dog, dogId)
        findNavController().navigate(R.id.action_navigation_home_to_dogProfileFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
