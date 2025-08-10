package com.danono.paws.ui.mydogs

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.danono.paws.R
import com.danono.paws.adapters.WeightsAdapter
import com.danono.paws.databinding.FragmentDogWeightBinding
import com.danono.paws.model.DogWeight
import com.danono.paws.utilities.FirebaseDataManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.util.*

/**
 * Fragment for displaying and managing weight entries for a selected dog.
 *
 * This fragment closely follows the implementation of DogNotesFragment. It sets up
 * a RecyclerView backed by WeightsAdapter, listens for realtime updates
 * from Firestore via FirebaseDataManager and provides dialogs to add,
 * edit or delete weight entries.
 */
class DogWeightFragment : Fragment(R.layout.fragment_dog_weight) {

    private var _binding: FragmentDogWeightBinding? = null
    private val binding get() = _binding!!

    private lateinit var weightsAdapter: WeightsAdapter
    private lateinit var sharedViewModel: SharedDogsViewModel
    private lateinit var firebaseDataManager: FirebaseDataManager
    private val weightsList = mutableListOf<DogWeight>()

    private var currentDogId: String = ""
    private var currentDogName: String = ""
    private var weightsListener: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDogWeightBinding.bind(view)

        firebaseDataManager = FirebaseDataManager.getInstance()
        sharedViewModel = ViewModelProvider(requireActivity())[SharedDogsViewModel::class.java]

        setupRecyclerView()
        setupFab()
        observeSelectedDog()
    }

    private fun setupRecyclerView() {
        weightsAdapter = WeightsAdapter(weightsList) { entry ->
            showEditWeightDialog(entry)
        }

        binding.weightsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = weightsAdapter
        }
    }

    private fun setupFab() {
        binding.fabAddWeight.setOnClickListener {
            showAddWeightDialog()
        }
    }

    private fun observeSelectedDog() {
        sharedViewModel.selectedDog.observe(viewLifecycleOwner) { dog ->
            dog?.let {
                currentDogName = dog.name
                binding.dogNameTitle.text = "${dog.name}'s Weight"
            }
        }

        sharedViewModel.selectedDogId.observe(viewLifecycleOwner) { dogId ->
            dogId?.let {
                currentDogId = it
                setupWeightsListener(it)
            }
        }
    }

    /**
     * Setup realtime listener for weights collection.
     */
    private fun setupWeightsListener(dogId: String) {
        weightsListener?.remove()
        weightsListener = firebaseDataManager.addWeightsListener(dogId) { entries ->
            weightsList.clear()
            weightsList.addAll(entries)
            weightsAdapter.notifyDataSetChanged()
            updateEmptyState()
        }

        if (weightsListener == null) {
            // fallback to fetch once if listener fails (e.g. offline)
            loadWeightsFromFirebase(dogId)
        }
    }

    /**
     * Fallback: fetch weights once without realtime updates.
     */
    private fun loadWeightsFromFirebase(dogId: String) {
        lifecycleScope.launch {
            val result = firebaseDataManager.getWeights(dogId)
            result.fold(
                onSuccess = { entries ->
                    weightsList.clear()
                    weightsList.addAll(entries)
                    weightsAdapter.notifyDataSetChanged()
                    updateEmptyState()
                },
                onFailure = {
                    weightsList.clear()
                    weightsAdapter.notifyDataSetChanged()
                    updateEmptyState()
                }
            )
        }
    }

    private fun showAddWeightDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_weight, null)
        val weightInput: EditText = dialogView.findViewById(R.id.weightInput)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.add_weight))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val weightText = weightInput.text.toString().trim()
                if (weightText.isNotEmpty()) {
                    val weightValue = weightText.toDoubleOrNull()
                    if (weightValue != null && weightValue > 0.0) {
                        addWeight(weightValue)
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.please_enter_weight), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), getString(R.string.please_enter_weight), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showEditWeightDialog(entry: DogWeight) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_weight, null)
        val weightInput: EditText = dialogView.findViewById(R.id.weightInput)
        weightInput.setText(entry.weight.toString())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.edit_weight))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.update)) { _, _ ->
                val weightText = weightInput.text.toString().trim()
                val newWeight = weightText.toDoubleOrNull()
                if (newWeight != null && newWeight > 0.0) {
                    updateWeight(entry, newWeight)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .setNeutralButton(getString(R.string.delete)) { _, _ ->
                deleteWeight(entry)
            }
            .show()
    }

    private fun addWeight(value: Double) {
        val entry = DogWeight(
            id = UUID.randomUUID().toString(),
            weight = value,
            createdDate = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )
        lifecycleScope.launch {
            val result = firebaseDataManager.addWeight(currentDogId, entry)
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), getString(R.string.weight_saved), Toast.LENGTH_SHORT).show()
                },
                onFailure = { exception ->
                    Toast.makeText(requireContext(), "Failed to save weight: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun updateWeight(entry: DogWeight, newValue: Double) {
        val updated = entry.copy(
            weight = newValue,
            lastModified = System.currentTimeMillis()
        )
        lifecycleScope.launch {
            val result = firebaseDataManager.updateWeight(currentDogId, updated)
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), getString(R.string.weight_updated), Toast.LENGTH_SHORT).show()
                },
                onFailure = { exception ->
                    Toast.makeText(requireContext(), "Failed to update weight: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun deleteWeight(entry: DogWeight) {
        lifecycleScope.launch {
            val result = firebaseDataManager.deleteWeight(currentDogId, entry.id)
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), getString(R.string.weight_deleted), Toast.LENGTH_SHORT).show()
                },
                onFailure = { exception ->
                    Toast.makeText(requireContext(), "Failed to delete weight: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun updateEmptyState() {
        if (weightsList.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.weightsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.weightsRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        weightsListener?.remove()
        _binding = null
    }
}