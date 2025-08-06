package com.danono.paws.ui.dog_parks

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.danono.paws.R
import com.danono.paws.adapters.DogParksAdapter
import com.danono.paws.databinding.FragmentDogParksBinding
import com.danono.paws.model.DogPark
import com.danono.paws.utilities.LocationHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DogParksFragment : Fragment(R.layout.fragment_dog_parks), OnMapReadyCallback {

    private var _binding: FragmentDogParksBinding? = null
    private val binding get() = _binding!!

    private lateinit var dogParksViewModel: DogParksViewModel
    private lateinit var parksAdapter: DogParksAdapter
    private lateinit var locationHelper: LocationHelper

    private var googleMap: GoogleMap? = null
    private var mapFragment: SupportMapFragment? = null
    private var userLocation: Location? = null
    private val parksList = mutableListOf<DogPark>()
    private val mapMarkers = mutableMapOf<Marker, DogPark>()
    private var selectedPark: DogPark? = null

    // UI State
    private var isMapView = true

    // Location permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            getCurrentLocation()
        } else {
            showLocationPermissionRationale()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDogParksBinding.bind(view)

        dogParksViewModel = ViewModelProvider(this)[DogParksViewModel::class.java]
        locationHelper = LocationHelper(requireContext())

        setupMap()
        setupRecyclerView()
        setupSearchView()
        setupButtons()
        setupViewModel()

        // Check location permission and load parks
        checkLocationPermissionAndLoad()
    }

    private fun setupMap() {
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Configure map
        googleMap?.apply {
            uiSettings.isZoomControlsEnabled = false
            uiSettings.isMyLocationButtonEnabled = false
            uiSettings.isCompassEnabled = false

            // Set map click listeners
            setOnMarkerClickListener { marker ->
                mapMarkers[marker]?.let { park ->
                    showParkDetails(park)
                    true
                } ?: false
            }

            setOnMapClickListener {
                hideParkDetails()
            }
        }

        // Enable location if permission granted
        if (hasLocationPermission()) {
            enableMapLocation()
        }
    }

    private fun setupRecyclerView() {
        parksAdapter = DogParksAdapter(
            parks = parksList,
            onParkClick = { park ->
                showParkDetails(park)
                // Animate to park on map
                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(park.latitude, park.longitude),
                        15f
                    )
                )
            },
            onDirectionsClick = { park ->
                openDirections(park)
            },
            onFavoriteClick = { park ->
                toggleFavorite(park)
            }
        )

        binding.parksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = parksAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchParks(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterParks(newText ?: "")
                return true
            }
        })
    }

    private fun setupButtons() {
        // Location button
        binding.locationButton.setOnClickListener {
            if (hasLocationPermission()) {
                getCurrentLocation()
            } else {
                requestLocationPermission()
            }
        }

        // Toggle view buttons
        binding.mapViewButton.setOnClickListener {
            showMapView()
        }

        binding.listViewButton.setOnClickListener {
            showListView()
        }

        // Bottom sheet buttons
        binding.directionsButton.setOnClickListener {
            selectedPark?.let { openDirections(it) }
        }

        binding.favoriteButton.setOnClickListener {
            selectedPark?.let { toggleFavorite(it) }
        }

        binding.shareButton.setOnClickListener {
            selectedPark?.let { sharePark(it) }
        }

        binding.closeBottomSheet.setOnClickListener {
            hideParkDetails()
        }
    }

    private fun setupViewModel() {
        dogParksViewModel.parks.observe(viewLifecycleOwner) { parks ->
            updateParksList(parks)
        }

        dogParksViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        dogParksViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showMapView() {
        isMapView = true
        mapFragment?.view?.visibility = View.VISIBLE
        binding.parksRecyclerView.visibility = View.GONE

        // Update button states
        binding.mapViewButton.apply {
            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.Primary_pink))
        }
        binding.listViewButton.apply {
            setTextColor(ContextCompat.getColor(requireContext(), R.color.Primary_pink))
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
        }
    }

    private fun showListView() {
        isMapView = false
        mapFragment?.view?.visibility = View.GONE
        binding.parksRecyclerView.visibility = View.VISIBLE
        hideParkDetails()

        // Update button states
        binding.listViewButton.apply {
            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.Primary_pink))
        }
        binding.mapViewButton.apply {
            setTextColor(ContextCompat.getColor(requireContext(), R.color.Primary_pink))
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
        }
    }

    private fun showParkDetails(park: DogPark) {
        selectedPark = park

        binding.selectedParkName.text = park.name
        binding.selectedParkRating.text = if (park.rating > 0) "⭐ ${park.rating}" else ""
        binding.selectedParkReviews.text = "(124 reviews)" // Placeholder
        binding.selectedParkDistance.text = if (park.distance > 0) {
            if (park.distance < 1) {
                "${(park.distance * 1000).toInt()}m away"
            } else {
                "${"%.1f".format(park.distance)}km away"
            }
        } else ""

        // Update status chips
        binding.selectedParkStatusChips.removeAllViews()
        park.facilities.take(3).forEach { facility ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = facility
                isClickable = false
                when (facility.lowercase()) {
                    "fenced", "fenced area" -> {
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.Primary_pink))
                        setChipBackgroundColorResource(R.color.Secondary_pink)
                    }

                    "water station", "water available", "water fountain" -> {
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.malibu_600))
                        setChipBackgroundColorResource(R.color.Secondary_blue)
                    }

                    else -> {
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.lima_700))
                        setChipBackgroundColorResource(R.color.Secondary_green)
                    }
                }
            }
            binding.selectedParkStatusChips.addView(chip)
        }

        binding.parkDetailsBottomSheet.visibility = View.VISIBLE
    }

    private fun hideParkDetails() {
        binding.parkDetailsBottomSheet.visibility = View.GONE
        selectedPark = null
    }

    private fun checkLocationPermissionAndLoad() {
        if (hasLocationPermission()) {
            getCurrentLocation()
        } else {
            loadSampleParks()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun enableMapLocation() {
        if (hasLocationPermission()) {
            try {
                googleMap?.isMyLocationEnabled = true
            } catch (e: SecurityException) {
                // Handle security exception
            }
        }
    }

    private fun getCurrentLocation() {
        if (!hasLocationPermission()) return

        binding.progressOverlay.visibility = View.VISIBLE

        locationHelper.getCurrentLocation(object : LocationHelper.LocationCallback {
            override fun onLocationReceived(location: Location) {
                userLocation = location
                binding.progressOverlay.visibility = View.GONE

                // Move camera to user location
                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude),
                        12f
                    )
                )

                // Load parks near user location
                dogParksViewModel.loadParksNearLocation(location.latitude, location.longitude)

                // Update location button icon
                binding.locationButton.setImageResource(R.drawable.ic_location_on)

                enableMapLocation()
            }

            override fun onLocationError(error: String) {
                binding.progressOverlay.visibility = View.GONE
                Toast.makeText(requireContext(), "Location error: $error", Toast.LENGTH_SHORT)
                    .show()
                loadSampleParks()
            }
        })
    }

    private fun loadSampleParks() {
        dogParksViewModel.loadSampleParks()
    }

    private fun searchParks(query: String) {
        userLocation?.let { location ->
            dogParksViewModel.searchParks(query, location.latitude, location.longitude)
        } ?: run {
            filterParks(query)
        }
    }

    private fun filterParks(query: String) {
        val filteredParks = if (query.isEmpty()) {
            dogParksViewModel.parks.value ?: emptyList()
        } else {
            dogParksViewModel.parks.value?.filter { park ->
                park.name.contains(query, ignoreCase = true) ||
                        park.address.contains(query, ignoreCase = true) ||
                        park.facilities.any { it.contains(query, ignoreCase = true) }
            } ?: emptyList()
        }
        updateParksList(filteredParks)
    }

    private fun updateParksList(parks: List<DogPark>) {
        // Calculate distances if we have user location
        val parksWithDistance = if (userLocation != null) {
            parks.map { park ->
                val distance = LocationHelper.calculateDistance(
                    userLocation!!.latitude, userLocation!!.longitude,
                    park.latitude, park.longitude
                )
                park.copy(distance = distance)
            }.sortedBy { it.distance }
        } else {
            parks
        }

        parksList.clear()
        parksList.addAll(parksWithDistance)
        parksAdapter.notifyDataSetChanged()

        // Update map markers
        updateMapMarkers(parksWithDistance)
        updateEmptyState()
    }

    private fun updateMapMarkers(parks: List<DogPark>) {
        googleMap?.let { map ->
            // Clear existing markers
            map.clear()
            mapMarkers.clear()

            // Add new markers
            parks.forEach { park ->
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(LatLng(park.latitude, park.longitude))
                        .title(park.name)
                        .snippet(park.address)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                )
                marker?.let { mapMarkers[it] = park }
            }

            // Fit all markers in view if we have parks
            if (parks.isNotEmpty()) {
                val boundsBuilder = com.google.android.gms.maps.model.LatLngBounds.Builder()
                parks.forEach { park ->
                    boundsBuilder.include(LatLng(park.latitude, park.longitude))
                }

                // Include user location if available
                userLocation?.let { location ->
                    boundsBuilder.include(LatLng(location.latitude, location.longitude))
                }

                try {
                    val bounds = boundsBuilder.build()
                    val padding = 100 // padding in pixels
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                } catch (e: Exception) {
                    // Handle case where bounds are invalid
                }
            }
        }
    }

    private fun openDirections(park: DogPark) {
        val uri =
            Uri.parse("geo:${park.latitude},${park.longitude}?q=${park.latitude},${park.longitude}(${park.name})")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            // Fallback to browser
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=${park.latitude},${park.longitude}")
            )
            startActivity(browserIntent)
        }
    }

    private fun toggleFavorite(park: DogPark) {
        val updatedPark = park.copy(isFavorite = !park.isFavorite)
        val index = parksList.indexOf(park)
        if (index != -1) {
            parksList[index] = updatedPark
            parksAdapter.notifyItemChanged(index)

            // Update selected park if it's the same
            if (selectedPark?.id == park.id) {
                selectedPark = updatedPark
                updateFavoriteButton(updatedPark.isFavorite)
            }
        }

        val message = if (updatedPark.isFavorite) {
            "Added to favorites ❤️"
        } else {
            "Removed from favorites"
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun updateFavoriteButton(isFavorite: Boolean) {
        if (isFavorite) {
            binding.favoriteButton.setIconResource(R.drawable.ic_favorite_filled)
            binding.favoriteButton.setIconTintResource(R.color.Primary_pink)
        } else {
            binding.favoriteButton.setIconResource(R.drawable.ic_favorite_outline)
            binding.favoriteButton.setIconTintResource(R.color.Primary_pink)
        }
    }

    private fun sharePark(park: DogPark) {
        val shareText = buildString {
            append("Check out this dog park: ${park.name}\n")
            append("📍 ${park.address}\n")
            if (park.rating > 0) {
                append("⭐ Rating: ${park.rating}\n")
            }
            if (park.facilities.isNotEmpty()) {
                append("Facilities: ${park.facilities.joinToString(", ")}\n")
            }
            append("https://www.google.com/maps/search/?api=1&query=${park.latitude},${park.longitude}")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Dog Park: ${park.name}")
        }

        startActivity(Intent.createChooser(shareIntent, "Share Dog Park"))
    }

    private fun showLocationPermissionRationale() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Location Permission")
            .setMessage("To find dog parks near you and show them on the map, we need access to your location. You can still browse sample parks without this permission.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestLocationPermission()
            }
            .setNegativeButton("Browse Sample Parks") { _, _ ->
                loadSampleParks()
            }
            .show()
    }

    private fun updateEmptyState() {
        val isEmpty = parksList.isEmpty()
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE

        if (isMapView) {
            mapFragment?.view?.visibility = if (isEmpty) View.GONE else View.VISIBLE
        } else {
            binding.parksRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}