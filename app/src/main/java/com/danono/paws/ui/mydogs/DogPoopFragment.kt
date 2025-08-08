package com.danono.paws.ui.mydogs

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.danono.paws.R
import com.danono.paws.adapters.PoopAdapter
import com.danono.paws.databinding.FragmentDogPoopBinding
import com.danono.paws.model.DogPoop
import com.danono.paws.model.PoopColors
import com.danono.paws.utilities.ImageLoader
import com.danono.paws.utilities.FirebaseDataManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.*

class DogPoopFragment : Fragment(R.layout.fragment_dog_poop) {

    private var _binding: FragmentDogPoopBinding? = null
    private val binding get() = _binding!!

    private lateinit var poopAdapter: PoopAdapter
    private lateinit var sharedViewModel: SharedDogsViewModel
    private lateinit var firebaseRepository: FirebaseDataManager

    private val poopList = mutableListOf<DogPoop>()
    private var currentDogId: String = ""
    private var currentDogName: String = ""

    // Image handling
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private var currentImageView: ImageView? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            currentImageView?.let { imageView ->
                imageView.visibility = View.VISIBLE
                ImageLoader.getInstance().loadImage(it, imageView)
            }
        }
    }

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            selectedImageUri = cameraImageUri
            currentImageView?.let { imageView ->
                imageView.visibility = View.VISIBLE
                ImageLoader.getInstance().loadImage(cameraImageUri!!, imageView)
            }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDogPoopBinding.bind(view)

        // Initialize
        sharedViewModel = ViewModelProvider(requireActivity())[SharedDogsViewModel::class.java]
        firebaseRepository = FirebaseDataManager.getInstance()

        setupRecyclerView()
        setupFab()
        observeSelectedDog()
    }

    private fun setupRecyclerView() {
        poopAdapter = PoopAdapter(poopList) { poop ->
            showEditPoopDialog(poop)
        }

        binding.poopRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = poopAdapter
        }
    }

    private fun setupFab() {
        binding.fabAddPoop.setOnClickListener {
            if (currentDogId.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a dog first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showAddPoopDialog()
        }
    }

    private fun observeSelectedDog() {
        sharedViewModel.selectedDog.observe(viewLifecycleOwner) { dog ->
            dog?.let {
                currentDogName = dog.name
                binding.dogNameTitle.text = "${dog.name}'s Poop Log"
            }
        }

        sharedViewModel.selectedDogId.observe(viewLifecycleOwner) { dogId ->
            dogId?.let {
                currentDogId = it
                loadPoopLogs(it)
            }
        }
    }

    private fun loadPoopLogs(dogId: String) {
        lifecycleScope.launch {
            try {
                val result = firebaseRepository.getPoopEntries(dogId)
                result.onSuccess { poopEntries ->
                    poopList.clear()
                    poopList.addAll(poopEntries)
                    poopAdapter.notifyDataSetChanged()
                    updateEmptyState()
                }.onFailure { exception ->
                    Toast.makeText(
                        requireContext(),
                        "Failed to load poop logs: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateEmptyState()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Failed to load poop logs: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                updateEmptyState()
            }
        }
    }

    private fun showAddPoopDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_poop, null)
        setupPoopDialog(dialogView, null) { color, consistency, notes ->
            addPoopLog(color, consistency, notes)
        }
    }

    private fun showEditPoopDialog(poop: DogPoop) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_poop, null)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Poop Entry")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val colorSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.poopColorSpinner)
                val consistencySpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.poopConsistencySpinner)
                val notesInput = dialogView.findViewById<TextInputEditText>(R.id.poopNotes)

                val color = colorSpinner.text.toString().trim()
                val consistency = consistencySpinner.text.toString().trim()
                val notes = notesInput.text.toString().trim()

                if (color.isNotEmpty() && consistency.isNotEmpty()) {
                    updatePoopLog(poop, color, consistency, notes)
                } else {
                    Toast.makeText(requireContext(), "Please select color and consistency", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ ->
                deletePoopLog(poop)
            }
            .show()

        // Setup the dialog after showing it
        setupPoopDialog(dialogView, poop) { _, _, _ -> /* handled by positive button */ }
    }

    private fun setupPoopDialog(
        dialogView: View,
        existingPoop: DogPoop?,
        onSave: (String, String, String) -> Unit
    ) {
        val colorSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.poopColorSpinner)
        val consistencySpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.poopConsistencySpinner)
        val notesInput = dialogView.findViewById<TextInputEditText>(R.id.poopNotes)
        val imageView = dialogView.findViewById<ImageView>(R.id.poopImagePreview)
        val addImageButton = dialogView.findViewById<View>(R.id.addImageButton)

        currentImageView = imageView
        selectedImageUri = null

        // Setup color spinner
        val colors = PoopColors.getColors().map { it.name }
        val colorAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, colors)
        colorSpinner.setAdapter(colorAdapter)
        colorSpinner.setText(existingPoop?.color ?: colors[0], false)

        // Setup consistency spinner
        val consistencies = listOf("Normal", "Soft", "Hard", "Liquid", "Mucus")
        val consistencyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, consistencies)
        consistencySpinner.setAdapter(consistencyAdapter)
        consistencySpinner.setText(existingPoop?.consistency ?: consistencies[0], false)

        // Setup notes
        notesInput.setText(existingPoop?.notes ?: "")

        // Load existing image
        if (!existingPoop?.imageUrl.isNullOrEmpty()) {
            imageView.visibility = View.VISIBLE
            ImageLoader.getInstance().loadImage(existingPoop!!.imageUrl, imageView)
        }

        addImageButton.setOnClickListener {
            showImagePickerOptions()
        }

        // For add dialog, show it here
        if (existingPoop == null) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Poop Entry")
                .setView(dialogView)
                .setPositiveButton("Save") { _, _ ->
                    val color = colorSpinner.text.toString().trim()
                    val consistency = consistencySpinner.text.toString().trim()
                    val notes = notesInput.text.toString().trim()

                    if (color.isNotEmpty() && consistency.isNotEmpty()) {
                        onSave(color, consistency, notes)
                    } else {
                        Toast.makeText(requireContext(), "Please select color and consistency", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun addPoopLog(color: String, consistency: String, notes: String) {
        val poop = DogPoop(
            id = UUID.randomUUID().toString(),
            color = color,
            consistency = consistency,
            notes = notes,
            imageUrl = "", // Will be updated if image is uploaded
            createdDate = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            try {
                // Upload image if selected
                val imageUrl = if (selectedImageUri != null) {
                    val result = firebaseRepository.uploadImage(currentDogId, selectedImageUri!!, "poop_images")
                    result.getOrElse { "" }
                } else {
                    ""
                }

                val poopWithImage = poop.copy(imageUrl = imageUrl)
                val result = firebaseRepository.addPoop(currentDogId, poopWithImage)

                result.onSuccess {
                    poopList.add(0, poopWithImage)
                    poopAdapter.notifyItemInserted(0)
                    binding.poopRecyclerView.scrollToPosition(0)
                    updateEmptyState()
                    Toast.makeText(requireContext(), "Poop entry saved successfully", Toast.LENGTH_SHORT).show()
                }.onFailure { exception ->
                    Toast.makeText(requireContext(), "Failed to save poop entry: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePoopLog(poop: DogPoop, newColor: String, newConsistency: String, newNotes: String) {
        lifecycleScope.launch {
            try {
                // Upload new image if selected
                val imageUrl = if (selectedImageUri != null) {
                    val result = firebaseRepository.uploadImage(currentDogId, selectedImageUri!!, "poop_images")
                    result.getOrElse { poop.imageUrl }
                } else {
                    poop.imageUrl
                }

                val updatedPoop = poop.copy(
                    color = newColor,
                    consistency = newConsistency,
                    notes = newNotes,
                    imageUrl = imageUrl,
                    lastModified = System.currentTimeMillis()
                )

                val result = firebaseRepository.updatePoop(currentDogId, updatedPoop)

                result.onSuccess {
                    val index = poopList.indexOfFirst { it.id == poop.id }
                    if (index != -1) {
                        poopList[index] = updatedPoop
                        poopAdapter.notifyItemChanged(index)
                    }
                    Toast.makeText(requireContext(), "Poop entry updated successfully", Toast.LENGTH_SHORT).show()
                }.onFailure { exception ->
                    Toast.makeText(requireContext(), "Failed to update poop entry: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deletePoopLog(poop: DogPoop) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Poop Entry")
            .setMessage("Are you sure you want to delete this entry?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        // Delete image if it exists
                        if (poop.imageUrl.isNotEmpty()) {
                            firebaseRepository.deleteImage(poop.imageUrl)
                        }

                        val result = firebaseRepository.deletePoop(currentDogId, poop.id)
                        result.onSuccess {
                            val index = poopList.indexOf(poop)
                            if (index != -1) {
                                poopList.removeAt(index)
                                poopAdapter.notifyItemRemoved(index)
                                updateEmptyState()
                            }
                            Toast.makeText(requireContext(), "Poop entry deleted successfully", Toast.LENGTH_SHORT).show()
                        }.onFailure { exception ->
                            Toast.makeText(requireContext(), "Failed to delete poop entry: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Image picker methods
    private fun showImagePickerOptions() {
        val options = arrayOf("Choose from phone", "Take a photo")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose image source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageLauncher.launch("image/*")
                    1 -> requestCameraPermission()
                }
            }
            .show()
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        cameraImageUri = createImageUri()
        takePhotoLauncher.launch(cameraImageUri)
    }

    private fun createImageUri(): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "poop_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        return requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )!!
    }

    private fun updateEmptyState() {
        if (poopList.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.poopRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.poopRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        currentImageView = null
    }
}