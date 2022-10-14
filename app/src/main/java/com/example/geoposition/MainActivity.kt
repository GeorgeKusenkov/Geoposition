package com.example.geoposition

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.geoposition.databinding.ActivityMainBinding
import com.example.geoposition.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class MainActivity : AppCompatActivity() {

    private var locationListener: LocationSource.OnLocationChangedListener? = null
    private var map: GoogleMap? = null
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedClient: FusedLocationProviderClient
    private var needAnimateCamera = false
    private var needMoveCamera = true
    private val handler = Handler(Looper.getMainLooper())
    private val cameraMovedRunnable = Runnable{
        needMoveCamera = true
    }

    private val launcher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        if(map.values.isNotEmpty() && map.values.all {it}) {
            startLocation()
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            p0.lastLocation?.let { location ->
                val onLocationChanged = locationListener?.onLocationChanged(location)

                binding.speed.text = getString(R.string.speed, location.speed)

                    val cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude),
                        18f
                    )
                if(needMoveCamera) {
                    if (needAnimateCamera)
                        map?.animateCamera(cameraUpdate)
                    else {
                        needAnimateCamera = true
                        map?.moveCamera(cameraUpdate)
                    }
                }
            }
//            binding.message.text = p0.lastLocation.toString()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocation() {
        map?.isMyLocationEnabled = true
            val request = LocationRequest.create()
                .setInterval(1000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)

        fusedClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    override fun onStart() {
        super.onStart()
        checkPermissions()
    }

    override fun onStop() {
        super.onStop()
        fusedClient.removeLocationUpdates(locationCallback)
        needAnimateCamera = false
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mapOverlay.setOnTouchListener{ _, _ ->
            handler.removeCallbacks(cameraMovedRunnable)
            needMoveCamera = false
            handler.postDelayed(cameraMovedRunnable, 5000)
            false
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync{ googleMap ->
            map = googleMap
            checkPermissions()
            with(googleMap.uiSettings) {
                this.isZoomControlsEnabled = true
                isMyLocationButtonEnabled = true
            }

            googleMap.setLocationSource(object: LocationSource {
                override fun activate(p0: LocationSource.OnLocationChangedListener) {
                    locationListener = p0
                }

                override fun deactivate() {
                    locationListener = null
                }
            })
        }


        fusedClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun checkPermissions() {
        if(REQUIRED_PERMISSIONS.all { permission ->
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            }) {
            startLocation()
        } else {
            launcher.launch(REQUIRED_PERMISSIONS)
        }
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}