package com.danono.paws.ui.mydogs

import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.danono.paws.R
import com.danono.paws.adapters.DogColorAdapter
import com.danono.paws.data.remote.DogApiClient
import com.danono.paws.databinding.FragmentAddDogBinding
import com.danono.paws.model.DogTag
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.danono.paws.model.Dog

/**
 * Fragment for adding a new dog to the user's collection.
 * Handles dog information input including name, breed, birthdate, colors, and personality tags.
 */
class AddDogFragment : Fragment() {

    // ================================
    // PROPERTIES
    // ================================

    private var _binding: FragmentAddDogBinding? = null
    private val binding get() = _binding!!

    private lateinit var colorAdapter: DogColorAdapter
    private val selectedColors = mutableSetOf<Int>() // Multi-select set for dog colors

    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    private lateinit var sharedViewModel: SharedDogsViewModel


    // === GALLERY PICK ===
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            Log.d("AddDogFragment", "Gallery URI selected: $it")
            loadImageWithErrorHandling(it)
        }
    }

    // === CAMERA ===
    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        Log.d("AddDogFragment", "Camera result: $success")

        if (success && cameraImageUri != null) {
            Log.d("AddDogFragment", "Camera URI: $cameraImageUri")
            selectedImageUri = cameraImageUri

            binding.root.postDelayed({
                loadImageWithErrorHandling(cameraImageUri!!)
            }, 100)
        } else {
            Toast.makeText(
                requireContext(),
                "Failed to capture image",
                Toast.LENGTH_SHORT
            ).show()
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

    // Predefined personality tags for dogs
    private val allTags = listOf(
        DogTag("Friendly with dogs", DogTag.Category.BEHAVIOR_WITH_DOGS),
        DogTag("Aggressive with dogs", DogTag.Category.BEHAVIOR_WITH_DOGS),
        DogTag("Loves people", DogTag.Category.BEHAVIOR_WITH_HUMANS),
        DogTag("Good with kids", DogTag.Category.BEHAVIOR_WITH_HUMANS),
        DogTag("Calm", DogTag.Category.PERSONALITY),
        DogTag("Playful", DogTag.Category.PERSONALITY),
        DogTag("Energetic", DogTag.Category.ACTIVITY_LEVEL),
        DogTag("Needs space", DogTag.Category.SPECIAL_NOTES)
    )

    // ================================
    // LIFECYCLE METHODS
    // ================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddDogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize all UI components
        setupColorList()
        setupTagChips()
        setupDatePicker()
        fetchDogBreeds()

        binding.addDogBTNEditImage.setOnClickListener {
            showImagePickerOptions()
        }

        sharedViewModel = ViewModelProvider(requireActivity())[SharedDogsViewModel::class.java]

        binding.addDogBTNWoof.setOnClickListener {
            val name = binding.addDogName.text.toString()
            val breed = binding.addDogACTVBreed.text.toString()
            val birthDateStr = binding.addDogEDTBirthdate.text.toString()
            val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(birthDateStr)
            val birthDate = date?.time ?: System.currentTimeMillis()
            val tags = getSelectedTags()
            val colors = getSelectedColors().map { requireContext().resources.getResourceEntryName(it) }

            val dog = Dog.Builder()
                .name(name)
                .birthDate(birthDate)
                .breedName(breed)
                .tags(tags)
                .color(colors)
                .image(R.drawable.default_dog_img)
                .build()

            sharedViewModel.addDog(dog)
            findNavController().navigateUp()
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ================================
    // UI SETUP METHODS
    // ================================

    /**
     * Sets up the horizontal color selection RecyclerView
     */
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
            // Handle color selection/deselection
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

    /**
     * Creates and displays personality tag chips in the ChipGroup
     */
    private fun setupTagChips() {
        val chipGroup = binding.addDogCHIPGROUPTags
        chipGroup.removeAllViews()

        // Create a chip for each predefined tag
        for (tag in allTags) {
            val chip = Chip(requireContext()).apply {
                text = tag.label
                isCheckable = true
                isClickable = true
            }
            chipGroup.addView(chip)
        }
    }

    /**
     * Sets up the date picker for the dog's birthdate field
     */
    private fun setupDatePicker() {
        // Handle clicks on the date input field
        binding.addDogEDTBirthdate.setOnClickListener {
            showDatePicker()
        }

        // Handle clicks on the calendar icon
        binding.addDogLAYOUTBirthdate.setEndIconOnClickListener {
            showDatePicker()
        }
    }

    // =============================
    // IMAGE PICKER LOGIC
    // =============================

    private fun showImagePickerOptions() {
        val options = arrayOf("Choose from phone", "Take a photo")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
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
            put(MediaStore.Images.Media.DISPLAY_NAME, "dog_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        return requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )!!
    }

    private fun loadImageWithErrorHandling(uri: Uri) {
        try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                Log.d("AddDogFragment", "File is accessible")

                Glide.with(this)
                    .load(uri)
                    .placeholder(android.R.drawable.ic_menu_gallery) // אייקון מקום ברירת מחדל
                    .error(android.R.drawable.ic_menu_close_clear_cancel) // אייקון שגיאה ברירת מחדל
                    .centerCrop()
                    .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                        override fun onLoadFailed(
                            e: com.bumptech.glide.load.engine.GlideException?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.e("AddDogFragment", "Glide failed to load image", e)
                            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
                            return false
                        }

                        override fun onResourceReady(
                            resource: android.graphics.drawable.Drawable,
                            model: Any,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                            dataSource: com.bumptech.glide.load.DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.d("AddDogFragment", "Image loaded successfully")
                            return false
                        }
                    })
                    .into(binding.addDogIMGDogImage)
            }
        } catch (e: Exception) {
            Log.e("AddDogFragment", "Error accessing image file", e)
            Toast.makeText(
                requireContext(),
                "Error accessing image: ${e.message}",
                Toast.LENGTH_LONG
            ).show()

            if (uri == cameraImageUri) {
                binding.root.postDelayed({
                    retryImageLoad(uri)
                }, 500)
            }
        }
    }

    private fun retryImageLoad(uri: Uri) {
        try {
            requireContext().contentResolver.openInputStream(uri)?.use {
                Log.d("AddDogFragment", "Retry successful")
                Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(binding.addDogIMGDogImage)
            }
        } catch (e: Exception) {
            Log.e("AddDogFragment", "Retry failed", e)
            Toast.makeText(requireContext(), "Image not ready yet", Toast.LENGTH_SHORT).show()
        }
    }

    // ================================
    // DATE PICKER METHODS
    // ================================

    /**
     * Displays a date picker dialog and handles date selection
     */
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                // Format and display the selected date
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedDate.time)

                binding.addDogEDTBirthdate.setText(formattedDate)
            },
            year,
            month,
            day
        )

        // Prevent future dates (dogs can't be born in the future)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    // ================================
    // API METHODS
    // ================================

    /**
     * Fetches dog breeds from the API and sets up the breed dropdown
     */
    private fun fetchDogBreeds() {
        lifecycleScope.launch {
            try {
                val breeds = DogApiClient.dogApiService.getAllBreeds()
                val breedNames = breeds.map { it.name }

                // Create adapter for the AutoCompleteTextView
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    breedNames
                )

                binding.addDogACTVBreed.setAdapter(adapter)

                // Show dropdown when user clicks on the field
                binding.addDogACTVBreed.setOnClickListener {
                    binding.addDogACTVBreed.showDropDown()
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load breeds", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    // ================================
    // OTHER METHODS
    // ================================

    /**
     * Returns a list of selected personality tags
     * @return List of selected tag labels
     */
    private fun getSelectedTags(): List<String> {
        val selected = mutableListOf<String>()
        val chipGroup = binding.addDogCHIPGROUPTags

        // Iterate through all chips and collect selected ones
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            if (chip.isChecked) {
                selected.add(chip.text.toString())
            }
        }

        return selected
    }

    /**
     * Returns the currently selected colors
     * @return Set of selected color resource IDs
     */
    private fun getSelectedColors(): Set<Int> {
        return selectedColors.toSet()
    }
}