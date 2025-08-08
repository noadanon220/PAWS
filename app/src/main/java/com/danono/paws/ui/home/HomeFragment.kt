package com.danono.paws.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.danono.paws.R
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.danono.paws.adapters.DogAdapter
import com.danono.paws.databinding.FragmentHomeBinding
import com.danono.paws.ui.mydogs.SharedDogsViewModel
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedViewModel: SharedDogsViewModel
    private lateinit var adapter: DogAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize SharedViewModel
        sharedViewModel = ViewModelProvider(requireActivity())[SharedDogsViewModel::class.java]

        setupClickListeners()
        setupRecyclerView()
        loadDogs()
        observeViewModel()

        Log.d("HomeFragment", "Fragment initialized")
    }

    override fun onResume() {
        super.onResume()
        Log.d("HomeFragment", "Fragment resumed - refreshing dogs")
        // Refresh dogs when returning to home
        sharedViewModel.refreshDogs()
    }

    private fun setupClickListeners() {
        binding.addDog.setOnClickListener {
            Log.d("HomeFragment", "Add dog button clicked")
            findNavController().navigate(R.id.action_navigation_home_to_addDogFragment)
        }

        binding.homeFabSearch.setOnClickListener {
            Log.d("HomeFragment", "Reminders button clicked")
            findNavController().navigate(R.id.navigation_reminders)
        }
    }

    private fun setupRecyclerView() {
        // Initialize adapter with empty list initially
        adapter = DogAdapter(emptyList()) { dog, dogId ->
            handleDogClick(dog, dogId)
        }

        binding.homeRVDogs.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = this@HomeFragment.adapter
        }
    }

    private fun loadDogs() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            Log.d("HomeFragment", "Loading dogs for user: $userId")
            sharedViewModel.loadDogsFromFirestore()
        } else {
            Log.w("HomeFragment", "No user logged in")
        }
    }

    private fun observeViewModel() {
        // Observe dogs list
        sharedViewModel.dogs.observe(viewLifecycleOwner) { dogsWithIds ->
            Log.d("HomeFragment", "Dogs list updated: ${dogsWithIds.size} dogs")

            adapter = DogAdapter(dogsWithIds) { dog, dogId ->
                handleDogClick(dog, dogId)
            }
            binding.homeRVDogs.adapter = adapter
        }

        // Observe loading state
        sharedViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("HomeFragment", "Loading state: $isLoading")
            // You can show/hide a progress bar here if needed
            // binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe error state
        sharedViewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Log.e("HomeFragment", "Error: $error")
                // You can show an error message here if needed
                // Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleDogClick(dog: com.danono.paws.model.Dog, dogId: String) {
        Log.d("HomeFragment", "Dog clicked: ${dog.name} with ID: $dogId")

        // Select the dog in the shared ViewModel
        sharedViewModel.selectDog(dog, dogId)

        // Navigate to dog profile
        try {
            findNavController().navigate(R.id.action_navigation_home_to_dogProfileFragment)
            Log.d("HomeFragment", "Navigation to dog profile successful")
        } catch (e: Exception) {
            Log.e("HomeFragment", "Navigation error: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("HomeFragment", "Fragment view destroyed")
        _binding = null
    }
}