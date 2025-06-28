package com.danono.paws.ui.dog_parks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.danono.paws.databinding.FragmentDogParksBinding

class DogParksFragment : Fragment() {

    private var _binding: FragmentDogParksBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dogParksViewModel =
            ViewModelProvider(this).get(DogParksViewModel::class.java)

        _binding = FragmentDogParksBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textDogParks
        dogParksViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}