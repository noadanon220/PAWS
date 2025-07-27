package com.danono.paws.ui.mydogs

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.danono.paws.R
import com.danono.paws.adapters.DogActivityAdapter
import com.danono.paws.databinding.FragmentDogProfileBinding
import com.danono.paws.model.Dog
import com.danono.paws.utilities.ImageLoader
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

class DogProfileFragment : Fragment(R.layout.fragment_dog_profile) {

    private var _binding: FragmentDogProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var activitiesAdapter: DogActivityAdapter
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
        val activityTitles = listOf("Notes", "Training", "Food")

        activitiesAdapter = DogActivityAdapter(activityTitles)

        binding.dogProfileRECYCLERActivities.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = activitiesAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}