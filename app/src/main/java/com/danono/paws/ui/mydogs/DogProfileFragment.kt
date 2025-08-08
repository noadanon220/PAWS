package com.danono.paws.ui.mydogs

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.danono.paws.R
import com.danono.paws.adapters.ActivitiesAdapter
import com.danono.paws.databinding.FragmentDogProfileBinding
import com.danono.paws.model.Dog
import com.danono.paws.model.DogActivityCard
import com.danono.paws.utilities.ImageLoader
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

class DogProfileFragment : Fragment(R.layout.fragment_dog_profile) {

    private var _binding: FragmentDogProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var activitiesAdapter: ActivitiesAdapter
    private lateinit var sharedViewModel: SharedDogsViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDogProfileBinding.bind(view)

        sharedViewModel = ViewModelProvider(requireActivity())[SharedDogsViewModel::class.java]

        setupActivityCards()
        observeSelectedDog()

        Log.d("DogProfileFragment", "Fragment created and observing selected dog")
    }

    override fun onResume() {
        super.onResume()
        // Re-observe the selected dog when returning to this fragment
        Log.d("DogProfileFragment", "Fragment resumed - checking selected dog")

        val selectedDog = sharedViewModel.selectedDog.value
        val selectedDogId = sharedViewModel.selectedDogId.value

        Log.d("DogProfileFragment", "Current selected dog: ${selectedDog?.name}, ID: $selectedDogId")

        if (selectedDog != null) {
            bindDogData(selectedDog)
        } else {
            Log.w("DogProfileFragment", "No selected dog found - navigating back")
            findNavController().navigateUp()
        }
    }

    private fun observeSelectedDog() {
        sharedViewModel.selectedDog.observe(viewLifecycleOwner) { dog ->
            Log.d("DogProfileFragment", "Selected dog changed: ${dog?.name}")
            dog?.let {
                bindDogData(it)
            } ?: run {
                Log.w("DogProfileFragment", "Selected dog is null")
            }
        }

        sharedViewModel.selectedDogId.observe(viewLifecycleOwner) { dogId ->
            Log.d("DogProfileFragment", "Selected dog ID changed: $dogId")
        }
    }

    private fun bindDogData(dog: Dog) {
        Log.d("DogProfileFragment", "Binding data for dog: ${dog.name}")

        try {
            // Set dog name
            binding.dogProfileLBLName.text = dog.name

            // Set breed and age
            val age = calculateAge(dog.birthDate)
            binding.dogProfileLBLBreedAndAge.text = "${dog.breedName} • $age years old"

            // Load dog image using ImageLoader
            ImageLoader.getInstance().loadDogImage(dog.imageUrl, binding.dogProfileIMGHeader)

            // Set color info
            if (dog.color.isNotEmpty()) {
                val colorName = dog.color.first().replace("dog_color_", "").replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
                binding.dogProfileLBLColor.text = "Color\n$colorName"
            } else {
                binding.dogProfileLBLColor.text = "Color\nUnknown"
            }

            // Set weight
            binding.dogProfileLBLWeight.text = "Weight\n${dog.weight}kg"

            // Set birthday
            val birthDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(dog.birthDate))
            binding.dogProfileLBLBirthday.text = "Birthday\n$birthDate"

            // Add personality tags
            setupPersonalityTags(dog.tags)

            Log.d("DogProfileFragment", "Successfully bound data for dog: ${dog.name}")
        } catch (e: Exception) {
            Log.e("DogProfileFragment", "Error binding dog data: ${e.message}")
            Toast.makeText(requireContext(), "Error loading dog profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateAge(birthDateMillis: Long): Int {
        val birthDate = Calendar.getInstance().apply { timeInMillis = birthDateMillis }
        val today = Calendar.getInstance()

        var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)

        if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        return maxOf(age, 0)
    }

    private fun setupPersonalityTags(tags: List<String>) {
        val chipGroup = binding.dogProfileCHIPSTags
        chipGroup.removeAllViews()

        for (tag in tags) {
            val chip = Chip(requireContext()).apply {
                text = tag
                isCheckable = false
                isClickable = false
                setChipBackgroundColorResource(R.color.bg_grey)
                setTextColor(resources.getColor(R.color.black, null))
            }
            chipGroup.addView(chip)
        }
    }

    private fun setupActivityCards() {
        val activityCards = listOf(
            DogActivityCard("Notes", R.drawable.ic_notes, R.color.Secondary_pink, R.color.Primary_pink),
            DogActivityCard("Food", R.drawable.ic_food, R.color.Secondary_green, R.color.lima_700),
            DogActivityCard("Walks", R.drawable.ic_weight, R.color.Secondary_blue, R.color.malibu_600),
            DogActivityCard("Medicine", R.drawable.ic_training, R.color.Secondary_yellow, R.color.Primary_yellow),
            DogActivityCard("Poop", R.drawable.ic_poop, R.color.Secondary_orange, R.color.orange_600),
            DogActivityCard("Training", R.drawable.ic_training, R.color.purple_100, R.color.purple_600)
        )

        activitiesAdapter = ActivitiesAdapter(activityCards) { card ->
            handleActivityCardClick(card)
        }

        binding.dogProfileRECYCLERActivities.apply {
            // Use GridLayoutManager with 3 columns instead of LinearLayoutManager
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = activitiesAdapter
        }
    }

    private fun handleActivityCardClick(card: DogActivityCard) {
        // Verify we have a selected dog before navigating
        val selectedDog = sharedViewModel.selectedDog.value
        val selectedDogId = sharedViewModel.selectedDogId.value

        if (selectedDog == null || selectedDogId == null) {
            Toast.makeText(requireContext(), "No dog selected", Toast.LENGTH_SHORT).show()
            Log.w("DogProfileFragment", "Attempted to navigate with no selected dog")
            return
        }

        Log.d("DogProfileFragment", "Navigating to ${card.title} for dog: ${selectedDog.name}")

        try {
            when (card.title) {
                "Notes" -> {
                    Log.d("DogProfileFragment", "Navigating to Notes fragment")
                    findNavController().navigate(R.id.action_dogProfileFragment_to_dogNotesFragment)
                }
                "Walks" -> {
                    Log.d("DogProfileFragment", "Navigating to Walks fragment")
                    findNavController().navigate(R.id.action_dogProfileFragment_to_dogWalksFragment)
                }
                "Poop" -> {
                    Log.d("DogProfileFragment", "Navigating to Poop fragment")
                    findNavController().navigate(R.id.action_dogProfileFragment_to_dogPoopFragment)
                }
                else -> {
                    Toast.makeText(requireContext(), "Feature coming soon: ${card.title}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("DogProfileFragment", "Navigation error: ${e.message}")
            Toast.makeText(requireContext(), "Navigation error", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("DogProfileFragment", "Fragment view destroyed")
        _binding = null
    }
}