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

        // Camera button (placeholder)
        binding.profileBtnCamera.setOnClickListener {
            Toast.makeText(requireContext(), "Change photo feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // Back button navigates up
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        Log.d("DogProfileFragment", "Fragment created and observing selected dog")

        binding.profileRecyclerActivities.isNestedScrollingEnabled = false

    }

    override fun onResume() {
        super.onResume()
        val selectedDog = sharedViewModel.selectedDog.value
        val selectedDogId = sharedViewModel.selectedDogId.value

        if (selectedDog != null) {
            bindDogData(selectedDog)
        } else {
            findNavController().navigateUp()
        }
    }

    private fun observeSelectedDog() {
        sharedViewModel.selectedDog.observe(viewLifecycleOwner) { dog ->
            dog?.let { bindDogData(it) }
        }
        sharedViewModel.selectedDogId.observe(viewLifecycleOwner) { /* no-op */ }
    }

    private fun bindDogData(dog: Dog) {
        try {
            binding.profileDogName.text = dog.name
            val age = calculateAge(dog.birthDate)
            binding.profileDogBreedAge.text = "${dog.breedName} • $age years old"

            ImageLoader.getInstance().loadDogImage(dog.imageUrl, binding.profileImgHeader)

            binding.profileValueBreed.text = dog.breedName
            binding.profileValueAge.text = "$age years"
            binding.profileValueWeight.text = "${dog.weight} kg"

            val birthDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(dog.birthDate))
            binding.profileValueBirthday.text = birthDate

            if (dog.color.isNotEmpty()) {
                val colorName = dog.color.first()
                    .replace("dog_color_", "")
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                binding.profileValueColor.text = colorName
            } else {
                binding.profileValueColor.text = "Unknown"
            }

            setupPersonalityTags(dog.tags)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error loading dog profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateAge(birthDateMillis: Long): Int {
        val birth = Calendar.getInstance().apply { timeInMillis = birthDateMillis }
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--
        return maxOf(age, 0)
    }

    private fun setupPersonalityTags(tags: List<String>) {
        val group = binding.profileChipTags
        group.removeAllViews()
        for (tag in tags) {
            val chip = Chip(requireContext()).apply {
                text = tag
                isCheckable = false
                isClickable = false
                setChipBackgroundColorResource(R.color.bg_grey)
                setTextColor(resources.getColor(R.color.black, null))
            }
            group.addView(chip)
        }
    }

    private fun setupActivityCards() {
        val activities = listOf(
            DogActivityCard("Notes", R.drawable.ic_notes, R.color.Secondary_pink, R.color.Primary_pink),
            DogActivityCard("Walks", R.drawable.ic_walk, R.color.Secondary_blue, R.color.malibu_600),
            DogActivityCard("Weight", R.drawable.ic_weight, R.color.Secondary_yellow, R.color.Primary_yellow),
            DogActivityCard("Poop", R.drawable.ic_poop, R.color.Secondary_orange, R.color.orange_600),
        )

        activitiesAdapter = ActivitiesAdapter(activities) { card -> handleActivityCardClick(card) }
        binding.profileRecyclerActivities.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.profileRecyclerActivities.adapter = activitiesAdapter
    }

    private fun handleActivityCardClick(card: DogActivityCard) {
        val dog = sharedViewModel.selectedDog.value ?: return
        val dogId = sharedViewModel.selectedDogId.value ?: return

        when (card.title) {
            "Notes" -> findNavController().navigate(R.id.action_dogProfileFragment_to_dogNotesFragment)
            "Walks" -> findNavController().navigate(R.id.action_dogProfileFragment_to_dogWalksFragment)
            "Poop"  -> findNavController().navigate(R.id.action_dogProfileFragment_to_dogPoopFragment)
            else    -> Toast.makeText(requireContext(), "Feature coming soon: ${card.title}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
