package com.hou.courierdriver

import android.Manifest
import android.annotation.SuppressLint
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.hou.courierdriver.models.ParcelMarker
import com.hou.courierdriver.util.*
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.view_details.view.*
import java.io.IOException

const val TAG = "Firebase"
const val MARKERS_FIREBASE_PATH = "markers"

class MapsActivity : BaseActivity(), OnMapReadyCallback {

    override val layoutResourceId: Int = R.layout.activity_maps

    private lateinit var database: DatabaseReference
    private lateinit var mMap: GoogleMap
    private lateinit var geoCoder: Geocoder
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val defaultLocation = LatLng(37.975299, 23.736831)
    private val defaultZoom = 15.0f

    private var lastKnownLocation: Location? = null
    private var markers: MutableList<ParcelMarker> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize GoogleMaps Services
        initializeGoogleMapsServices()
    }

    override fun onStart() {
        super.onStart()
        setupSearch()
    }

    private fun initializeGoogleMapsServices() {
        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        geoCoder = Geocoder(this)

        // Initialize Google Maps
        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setupMap()
        requestUserLocation()
        loadDatabaseMarkers()
    }

    private fun setupMap() = mMap.run {
        mapType = GoogleMap.MAP_TYPE_NORMAL
        setMinZoomPreference(6.0f)
        setMaxZoomPreference(20.0f)
        uiSettings.run {
            isCompassEnabled = true
            isRotateGesturesEnabled = true
            isZoomControlsEnabled = true
            isZoomGesturesEnabled = true
        }
        setOnMarkerClickListener { marker ->
            if (marker.isInfoWindowShown) marker.hideInfoWindow()
            else showInfoDetails(marker)
            false
        }
        setOnInfoWindowCloseListener { showAddMarkerButton() }
    }

    private fun showInfoDetails(marker: Marker) {
        marker.showInfoWindow()
        addMarker.hide()
        val parcelMarker = marker.tag as ParcelMarker
        markerDetails.run {
            render(parcelMarker)
            onSaveButtonClicked {
                marker.remove()
                updateMarker(parcelMarker)
            }
            onRemoveButtonClicked {
                marker.remove()
                removeMarker(parcelMarker)
            }
        }
    }

    private fun showAddMarkerButton() {
        markerDetails.hide()
        addMarker.show()
    }

    private fun requestUserLocation() {
        PermissionManager.requestPermission(
            this,
            onGranted = { showUserLocation() },
            onDenied = { isPermanentlyDenied ->
                if (isPermanentlyDenied) showToast(R.string.Location_Permission_Message)
                else showToast(R.string.Location_Permission_Denied)
            },
            permission = Manifest.permission.ACCESS_FINE_LOCATION,
            rationaleString = R.string.Location_Permission_Message
        )
    }

    @SuppressLint("MissingPermission")
    private fun showUserLocation() {
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        getDeviceLocation()
        setupAddMarkerButton()
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        val locationResult = fusedLocationProviderClient.lastLocation
        locationResult.addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                lastKnownLocation = task.result
                if (lastKnownLocation != null)
                    moveCameraTo(lastKnownLocation!!.latitude, lastKnownLocation!!.longitude)
            } else {
                moveCameraTo(defaultLocation.latitude, defaultLocation.longitude)
                mMap.uiSettings?.isMyLocationButtonEnabled = false
            }
        }
    }

    private fun setupAddMarkerButton() = addMarker.run {
        setOnClickListener {
            if (lastKnownLocation != null) {
                addCurrentLocationMarker()
                hide()
            }
        }
        show()
    }

    private fun addCurrentLocationMarker() {
        val position = LatLng(lastKnownLocation!!.latitude, lastKnownLocation!!.longitude)
        val marker =
            mMap.addMarker(MarkerOptions().position(position).title(getString(R.string.new_marker)))
        marker.tag = ParcelMarker(
            null,
            "",
            "",
            false,
            lastKnownLocation!!.latitude,
            lastKnownLocation!!.longitude
        )
        showInfoDetails(marker)
    }

    private fun loadDatabaseMarkers() {
        database = Firebase.database.reference

        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                clearMarkersList()
                val data = dataSnapshot.child(MARKERS_FIREBASE_PATH).value
                if (data != null) {
                    Log.e("Firebase", "data $data")
                    (data as HashMap<*,*>).keys.forEach {
                        parseMarkerSafely(data[it] as HashMap<String, Object>)
                    }
                    Log.e("Firebase", "markers $markers")
                    populateMap()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "loadPost: onCancelled ${databaseError.toException()}")
            }
        }
        database.addValueEventListener(listener)

        // Test packages
//        database.setValue(
//            hashMapOf(
//                "markers" to
//                        listOf(
//                            ParcelMarker(
//                                "0",
//                                "test",
//                                "information",
//                                false,
//                                47.078086,
//                                8.293203
//                            ),
//                            ParcelMarker(
//                                "1",
//                                "test2",
//                                "information2",
//                                false,
//                                47.070445,
//                                8.292316
//                            )
//                        )
//            )
//        )
    }

    private fun parseMarkerSafely(marker: HashMap<String, Object>) {
        try {
            val parcelMarker = marker.toParcelMarker()
            markers.add(parcelMarker)
        } catch (exception: Exception) {
            Log.e(TAG, "catch exception $exception")
        }
    }

    private fun clearMarkersList() {
        markers = mutableListOf()
    }

    private fun populateMap() {
        mMap.clear()
        if (markers.isNotEmpty()) {
            markers.forEach {
                val marker = mMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(it.latitude!!, it.longitude!!))
                        .title(it.title)
                        .snippet(it.information)
                        .alpha(if (it.delivered!!) 0.5f else 1.0f)
                )
                marker.tag = it
            }
        }
    }

    private fun removeMarker(parcelMarker: ParcelMarker) {
        if (parcelMarker.uid != null) {
            database.child(MARKERS_FIREBASE_PATH).child(parcelMarker.uid!!).removeValue()
        }
    }

    private fun updateMarker(parcelMarker: ParcelMarker) {
        if (parcelMarker.uid == null) {
            parcelMarker.uid =
                database.child(MARKERS_FIREBASE_PATH).push().key ?: markers.size.toString()
        }
        parcelMarker.title = markerDetails.title.text.toString()
        parcelMarker.information = markerDetails.information.text.toString()
        parcelMarker.delivered = markerDetails.status.isChecked
        database.child(MARKERS_FIREBASE_PATH).child(parcelMarker.uid!!).setValue(parcelMarker)
    }

    private fun moveCameraTo(latitude: Double, longitude: Double) = mMap.moveCamera(
        CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), defaultZoom)
    )

    private fun showToast(message: Int) {
        Toast.makeText(this, getString(message), Toast.LENGTH_SHORT).show()
    }

    private fun setupSearch() {
        search.onImeSearch {
            try {
                if (Geocoder.isPresent()) {
                    val addressList: List<Address> = geoCoder.getFromLocationName(it, 5)
                    if (addressList.isNullOrEmpty()) showToast(R.string.no_address_found)
                    else moveCameraTo(addressList[0].latitude, addressList[0].longitude)
                }
            } catch (exception: IOException) {
                Log.e(TAG, " exception: ${exception.toString()}")
                showToast(R.string.error_on_searching_address)
            }
            search.closeKeyboard(this)
        }
    }
}