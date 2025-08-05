package com.danono.paws.ui.mydogs

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.danono.paws.R
import com.danono.paws.adapters.ActivitiesAdapter
import com.danono.paws.databinding.FragmentDogProfileBinding
import com.danono.paws.model.Dog
import com.danono.paws.model.DogActivityCard
import com.danono.paws.model.DogActivityCards
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
    }

    private fun observeSelectedDog() {
        sharedViewModel.selectedDog.observe(viewLifecycleOwner) { dog ->
            dog?.let { bindDogData(it) }
        }
    }

    private fun bindDogData(dog: Dog) {
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
            when (card.title) {
                "Notes" -> {
                    // Navigate to Notes fragment
                    findNavController().navigate(R.id.action_dogProfileFragment_to_dogNotesFragment)
                }
                "Walks" -> {
                    // Navigate to Walks fragment
                    findNavController().navigate(R.id.action_dogProfileFragment_to_dogWalksFragment)
                }
                else -> {
                    Toast.makeText(requireContext(), "Clicked on ${card.title}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.dogProfileRECYCLERActivities.apply {
            // Use GridLayoutManager with 3 columns instead of LinearLayoutManager
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = activitiesAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}