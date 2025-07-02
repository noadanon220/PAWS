package com.danono.paws.ui.home

import android.os.Bundle
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

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var sharedViewModel: SharedDogsViewModel
    private lateinit var adapter: DogAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root


        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addDog.setOnClickListener(){
            findNavController().navigate(R.id.action_navigation_home_to_addDogFragment)
        }

        sharedViewModel = ViewModelProvider(requireActivity())[SharedDogsViewModel::class.java]

        adapter = DogAdapter(emptyList())
        binding.homeRVDogs.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.homeRVDogs.adapter = adapter

        sharedViewModel.dogs.observe(viewLifecycleOwner) { dogs ->
            adapter = DogAdapter(dogs)
            binding.homeRVDogs.adapter = adapter
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}