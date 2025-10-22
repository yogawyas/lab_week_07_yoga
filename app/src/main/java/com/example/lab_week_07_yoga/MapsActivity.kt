package com.example.lab_week_07_yoga

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.example.lab_week_07_yoga.databinding.ActivityMapsBinding

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var currentMarker: Marker? = null

    // Fused Location Provider (lokasi pengguna)
    private val fusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Register permission launcher
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) getLastLocation()
                else showPermissionRationale {
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Jika sudah punya izin lokasi → tampilkan posisi user
        when {
            hasLocationPermission() -> getLastLocation()
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showPermissionRationale {
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            else -> requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // 🌍 Tambahkan marker default di kampus atau lokasi tetap
        val kampus = LatLng(-6.364953, 106.828533)
        mMap.addMarker(MarkerOptions().position(kampus).title("Politeknik Negeri Jakarta"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kampus, 15f))

        // 🖱️ Event: klik di map → tambahkan marker baru
        mMap.setOnMapClickListener { latLng ->
            addCustomMarker(latLng)
        }
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun showPermissionRationale(positiveAction: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Izin Lokasi Diperlukan")
            .setMessage("Aplikasi membutuhkan akses lokasi agar dapat menampilkan posisi Anda.")
            .setPositiveButton("OK") { _, _ -> positiveAction() }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .create().show()
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (hasLocationPermission()) {
            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val userLatLng = LatLng(it.latitude, it.longitude)
                        updateMapLocation(userLatLng)
                        addCustomMarker(userLatLng, "You are here")
                    } ?: Log.w("MapsActivity", "Location is null")
                }
        }
    }

    private fun updateMapLocation(location: LatLng) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16f))
    }

    // 📍 Tambahkan marker baru di lokasi yang diklik
    private fun addCustomMarker(location: LatLng, title: String = "Custom Marker") {
        currentMarker?.remove() // hapus marker lama jika ada
        currentMarker = mMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(title)
        )
        updateMapLocation(location)
    }
}
