package com.danono.paws.ui.mydogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.danono.paws.R
import com.danono.paws.adapters.DogColorAdapter
import com.danono.paws.databinding.FragmentAddDogBinding

class AddDogFragment : Fragment() {

    private var _binding: FragmentAddDogBinding? = null
    private val binding get() = _binding!!

    private lateinit var colorAdapter: DogColorAdapter
    private val selectedColors = mutableSetOf<Int>() // Multi-select set

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddDogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupColorList()
    }

    private fun setupColorList() {
        val colorList = listOf(
            R.color.dog_color_brown,
            R.color.dog_color_white,
            R.color.dog_color_black,
            R.color.dog_color_golden,
            R.color.dog_color_grey,
            R.color.dog_color_reddish,
            R.color.dog_color_beige
        )

        colorAdapter = DogColorAdapter(colorList) { color ->
            if (selectedColors.contains(color)) {
                selectedColors.remove(color)
                Toast.makeText(requireContext(), "Color removed!", Toast.LENGTH_SHORT).show()
            } else {
                selectedColors.add(color)
                Toast.makeText(requireContext(), "Color added!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.addDogLSTColors.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = colorAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
