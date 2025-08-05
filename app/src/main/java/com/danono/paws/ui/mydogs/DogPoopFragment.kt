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
import androidx.recyclerview.widget.LinearLayoutManager
import com.danono.paws.R
import com.danono.paws.adapters.PoopAdapter
import com.danono.paws.databinding.FragmentDogPoopBinding
import com.danono.paws.model.DogPoop
import com.danono.paws.model.PoopColors
import com.danono.paws.utilities.ImageLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class DogPoopFragment : Fragment(R.layout.fragment_dog_poop) {

    private var _binding: FragmentDogPoopBinding? = null
    private val binding get() = _binding!!

    private lateinit var poopAdapter: PoopAdapter
    private lateinit var sharedViewModel: SharedDogsViewModel
    private val poopList = mutableListOf<DogPoop>()

    // Firebase instances
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var currentDogId: String = ""
    private var currentDogName: String = ""

    // Image handling
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            // Preview image in dialog
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

    private var currentImageView: ImageView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDogPoopBinding.bind(view)

        sharedViewModel = ViewModelProvider(requireActivity())[SharedDogsViewModel::class.java]

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
                loadPoopFromFirebase(it)
            }
        }
    }

    private fun showAddPoopDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_poop, null)
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
        colorSpinner.setText(colors[0], false) // Set default

        // Setup consistency spinner
        val consistencies = listOf("Normal", "Soft", "Hard", "Liquid", "Mucus")
        val consistencyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, consistencies)
        consistencySpinner.setAdapter(consistencyAdapter)
        consistencySpinner.setText(consistencies[0], false) // Set default

        addImageButton.setOnClickListener {
            showImagePickerOptions()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Poop Entry")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val color = colorSpinner.text.toString().trim()
                val consistency = consistencySpinner.text.toString().trim()
                val notes = notesInput.text.toString().trim()

                if (color.isNotEmpty() && consistency.isNotEmpty()) {
                    addPoop(color, consistency, notes)
                } else {
                    Toast.makeText(requireContext(), "Please select color and consistency", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditPoopDialog(poop: DogPoop) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_poop, null)
        val colorSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.poopColorSpinner)
        val consistencySpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.poopConsistencySpinner)
        val notesInput = dialogView.findViewById<TextInputEditText>(R.id.poopNotes)
        val imageView = dialogView.findViewById<ImageView>(R.id.poopImagePreview)
        val addImageButton = dialogView.findViewById<View>(R.id.addImageButton)

        currentImageView = imageView
        selectedImageUri = null

        // Setup spinners
        val colors = PoopColors.getColors().map { it.name }
        val colorAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, colors)
        colorSpinner.setAdapter(colorAdapter)
        colorSpinner.setText(poop.color, false)

        val consistencies = listOf("Normal", "Soft", "Hard", "Liquid", "Mucus")
        val consistencyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, consistencies)
        consistencySpinner.setAdapter(consistencyAdapter)
        consistencySpinner.setText(poop.consistency, false)

        notesInput.setText(poop.notes)

        // Load existing image
        if (poop.imageUrl.isNotEmpty()) {
            imageView.visibility = View.VISIBLE
            ImageLoader.getInstance().loadImage(poop.imageUrl, imageView)
        }

        addImageButton.setOnClickListener {
            showImagePickerOptions()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Poop Entry")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val color = colorSpinner.text.toString().trim()
                val consistency = consistencySpinner.text.toString().trim()
                val notes = notesInput.text.toString().trim()

                if (color.isNotEmpty() && consistency.isNotEmpty()) {
                    updatePoop(poop, color, consistency, notes)
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ ->
                deletePoop(poop)
            }
            .show()
    }

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

    private fun addPoop(color: String, consistency: String, notes: String) {
        val userId = auth.currentUser?.uid ?: return

        val poop = DogPoop(
            id = UUID.randomUUID().toString(),
            color = color,
            consistency = consistency,
            notes = notes,
            imageUrl = "", // Will be updated if image is uploaded
            createdDate = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )

        if (selectedImageUri != null) {
            uploadImageAndSavePoop(userId, poop, selectedImageUri!!)
        } else {
            savePoopToFirebase(userId, poop)
        }
    }

    private fun updatePoop(poop: DogPoop, newColor: String, newConsistency: String, newNotes: String) {
        val userId = auth.currentUser?.uid ?: return

        val updatedPoop = poop.copy(
            color = newColor,
            consistency = newConsistency,
            notes = newNotes,
            lastModified = System.currentTimeMillis()
        )

        if (selectedImageUri != null) {
            uploadImageAndSavePoop(userId, updatedPoop, selectedImageUri!!)
        } else {
            updatePoopInFirebase(userId, updatedPoop)
        }
    }

    private fun deletePoop(poop: DogPoop) {
        val userId = auth.currentUser?.uid ?: return
        deletePoopFromFirebase(userId, poop)
    }

    private fun uploadImageAndSavePoop(userId: String, poop: DogPoop, imageUri: Uri) {
        val imageRef = storage.reference.child("poop_images/${userId}/${poop.id}.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val poopWithImage = poop.copy(imageUrl = downloadUri.toString())
                    savePoopToFirebase(userId, poopWithImage)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
                savePoopToFirebase(userId, poop)
            }
    }

    private fun loadPoopFromFirebase(dogId: String) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .collection("dogs")
            .document(dogId)
            .collection("poop")
            .orderBy("lastModified", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                poopList.clear()
                for (document in documents) {
                    val poop = document.toObject(DogPoop::class.java)
                    poopList.add(poop)
                }
                poopAdapter.notifyDataSetChanged()
                updateEmptyState()
            }
            .addOnFailureListener {
                poopList.clear()
                poopAdapter.notifyDataSetChanged()
                updateEmptyState()
            }
    }

    private fun savePoopToFirebase(userId: String, poop: DogPoop) {
        firestore.collection("users")
            .document(userId)
            .collection("dogs")
            .document(currentDogId)
            .collection("poop")
            .document(poop.id)
            .set(poop)
            .addOnSuccessListener {
                poopList.add(0, poop)
                poopAdapter.notifyItemInserted(0)
                binding.poopRecyclerView.scrollToPosition(0)
                updateEmptyState()
                Toast.makeText(requireContext(), "Poop entry saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to save: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updatePoopInFirebase(userId: String, poop: DogPoop) {
        firestore.collection("users")
            .document(userId)
            .collection("dogs")
            .document(currentDogId)
            .collection("poop")
            .document(poop.id)
            .set(poop)
            .addOnSuccessListener {
                val index = poopList.indexOfFirst { it.id == poop.id }
                if (index != -1) {
                    poopList[index] = poop
                    poopAdapter.notifyItemChanged(index)
                }
                Toast.makeText(requireContext(), "Poop entry updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to update: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun deletePoopFromFirebase(userId: String, poop: DogPoop) {
        firestore.collection("users")
            .document(userId)
            .collection("dogs")
            .document(currentDogId)
            .collection("poop")
            .document(poop.id)
            .delete()
            .addOnSuccessListener {
                val index = poopList.indexOf(poop)
                if (index != -1) {
                    poopList.removeAt(index)
                    poopAdapter.notifyItemRemoved(index)
                    updateEmptyState()
                }
                Toast.makeText(requireContext(), "Poop entry deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to delete: ${exception.message}", Toast.LENGTH_LONG).show()
            }
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