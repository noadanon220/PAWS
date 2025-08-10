package com.danono.paws.ui.dog_parks

import android.Manifest
import android.content.Context
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
import com.danono.paws.R
import com.danono.paws.databinding.FragmentDogParksBinding
import com.danono.paws.model.DogPark
import com.danono.paws.utilities.LocationHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class DogParksFragment : Fragment(R.layout.fragment_dog_parks), OnMapReadyCallback {

    private var _binding: FragmentDogParksBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DogParksViewModel
    private lateinit var locationHelper: LocationHelper

    private var googleMap: GoogleMap? = null
    private var mapFragment: SupportMapFragment? = null

    private var userLocation: Location? = null
    private val parksList = mutableListOf<DogPark>()
    private val markerToPark = mutableMapOf<Marker, DogPark>()

    private var selectedPark: DogPark? = null
    private var customSelection: LatLng? = null
    private var customMarker: Marker? = null

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fineGranted || coarseGranted) {
                getCurrentLocation()
            } else {
                showLocationPermissionRationale()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDogParksBinding.bind(view)

        viewModel = ViewModelProvider(this)[DogParksViewModel::class.java]
        locationHelper = LocationHelper(requireContext())

        setupMap()
        setupSearch()
        setupButtons()
        observeViewModel()

        if (hasLocationPermission()) getCurrentLocation() else viewModel.loadSampleParks()
    }

    private fun setupMap() {
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map.apply {
            uiSettings.isZoomControlsEnabled = false
            uiSettings.isMyLocationButtonEnabled = false
            uiSettings.isCompassEnabled = false

            setOnMarkerClickListener { marker ->
                if (marker == customMarker) {
                    selectedPark = null
                    customSelection = marker.position
                    updateSelectionHint()
                    true
                } else {
                    val park = markerToPark[marker]
                    if (park != null) {
                        selectedPark = park
                        customSelection = null
                        updateSelectionHint()
                        true
                    } else false
                }
            }

            setOnMapLongClickListener { latLng ->
                selectedPark = null
                customSelection = latLng
                customMarker?.remove()
                customMarker = addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(getString(R.string.custom_point))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )
                updateSelectionHint()
            }
        }

        if (hasLocationPermission()) enableMyLocation()
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchParks(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterParks(newText.orEmpty())
                return true
            }
        })
    }

    private fun setupButtons() {
        binding.locationButton.setOnClickListener {
            if (hasLocationPermission()) getCurrentLocation() else requestLocationPermission()
        }

        binding.addFavoriteParkButton.setOnClickListener {
            openAddFavoriteDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.parks.observe(viewLifecycleOwner) { parks ->
            updateParksOnMap(parks)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.progressOverlay.visibility = if (it) View.VISIBLE else View.GONE
        }
        viewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let { toast(it) }
        }
    }

    // ---------- Location ----------
    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun enableMyLocation() {
        try {
            if (hasLocationPermission()) googleMap?.isMyLocationEnabled = true
        } catch (_: SecurityException) { }
    }

    private fun getCurrentLocation() {
        if (!hasLocationPermission()) return
        binding.progressOverlay.visibility = View.VISIBLE
        locationHelper.getCurrentLocation(object : LocationHelper.LocationCallback {
            override fun onLocationReceived(location: Location) {
                userLocation = location
                binding.progressOverlay.visibility = View.GONE

                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude), 12f
                    )
                )
                viewModel.loadParksNearLocation(location.latitude, location.longitude)
                binding.locationButton.setImageResource(R.drawable.ic_location_on)
                enableMyLocation()
            }

            override fun onLocationError(error: String) {
                binding.progressOverlay.visibility = View.GONE
                toast("Location error: $error")
                viewModel.loadSampleParks()
            }
        })
    }

    // ---------- Map markers & filtering ----------
    private fun updateParksOnMap(parks: List<DogPark>) {
        parksList.clear()
        parksList.addAll(parks)
        googleMap?.let { map ->
            map.clear()
            markerToPark.clear()
            customMarker = null

            var hasAny = false
            val bounds = LatLngBounds.Builder()

            parks.forEach { park ->
                val m = map.addMarker(
                    MarkerOptions()
                        .position(LatLng(park.latitude, park.longitude))
                        .title(park.name)
                        .snippet(park.address)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                )
                if (m != null) {
                    markerToPark[m] = park
                    bounds.include(m.position)
                    hasAny = true
                }
            }

            userLocation?.let { bounds.include(LatLng(it.latitude, it.longitude)) }

            if (hasAny) {
                try {
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
                } catch (_: Exception) { /* bounds may be single point */ }
            }
        }
        updateSelectionHint()
    }

    private fun searchParks(query: String) {
        userLocation?.let {
            viewModel.searchParks(query, it.latitude, it.longitude)
        } ?: filterParks(query)
    }

    private fun filterParks(query: String) {
        val src = viewModel.parks.value ?: emptyList()
        val filtered =
            if (query.isBlank()) src
            else src.filter {
                it.name.contains(query, true) ||
                        it.address.contains(query, true) ||
                        it.facilities.any { f -> f.contains(query, true) }
            }
        updateParksOnMap(filtered)
    }

    // ---------- Favorite dialog ----------
    private fun openAddFavoriteDialog() {
        val selectedLatLng: LatLng? = when {
            selectedPark != null -> LatLng(selectedPark!!.latitude, selectedPark!!.longitude)
            customSelection != null -> customSelection
            else -> null
        }

        if (selectedLatLng == null) {
            toast(getString(R.string.pick_location_first))
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_favorite_park, null, false)
        val edtName = dialogView.findViewById<TextInputEditText>(R.id.edtFavoriteName)
        val coords = "${"%.5f".format(selectedLatLng.latitude)}, ${"%.5f".format(selectedLatLng.longitude)}"
        (dialogView.findViewById<View>(R.id.txtCoords) as? android.widget.TextView)?.text =
            getString(R.string.coords_fmt, coords)

        edtName.setText(selectedPark?.name ?: getString(R.string.default_favorite_name))

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.set_favorite_park)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { dialog, _ ->
                val name = edtName.text?.toString()?.trim().takeUnless { it.isNullOrEmpty() }
                    ?: getString(R.string.default_favorite_name)
                saveFavorite(selectedLatLng.latitude, selectedLatLng.longitude, name)
                toast(getString(R.string.favorite_saved))
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun saveFavorite(lat: Double, lon: Double, name: String) {
        val prefs = requireContext().getSharedPreferences("favorite_park", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("name", name)
            .putFloat("lat", lat.toFloat())
            .putFloat("lon", lon.toFloat())
            .apply()
    }

    private fun updateSelectionHint() {
        binding.searchView.queryHint = getString(R.string.search_dog_parks)
        if (selectedPark == null && customSelection == null) {
            // toast(getString(R.string.long_press_tip))
        }
    }

    // ---------- misc ----------
    private fun openDirections(park: DogPark) {
        val uri = Uri.parse("geo:${park.latitude},${park.longitude}?q=${park.latitude},${park.longitude}(${park.name})")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=${park.latitude},${park.longitude}")
                )
            )
        }
    }

    private fun showLocationPermissionRationale() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.location_permission_title)
            .setMessage(R.string.location_permission_msg)
            .setPositiveButton(R.string.grant_permission) { _, _ -> requestLocationPermission() }
            .setNegativeButton(R.string.browse_anyway) { _, _ -> viewModel.loadSampleParks() }
            .show()
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
