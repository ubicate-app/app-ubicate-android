package com.ubicate.ubicate.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ubicate.ubicate.R
import com.ubicate.ubicate.model.BusLocation
import com.ubicate.ubicate.repository.BusLocationRepository
import com.ubicate.ubicate.repository.LocationRepository
import com.ubicate.ubicate.repository.LocationSettingsChecker
import com.ubicate.ubicate.viewmodel.BusLocationViewModel
import com.ubicate.ubicate.viewmodel.BusRouteViewModel
import com.ubicate.ubicate.viewmodel.factory.BusRouteViewModelFactory
import com.ubicate.ubicate.repository.RouteRepository
import com.ubicate.ubicate.service.RouteProvider
import com.ubicate.ubicate.viewmodel.factory.BusLocationViewModelFactory
import com.ubicate.ubicate.viewmodel.MapaViewModel
import com.ubicate.ubicate.viewmodel.factory.MapaViewModelFactory
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.model.Polyline
import com.ubicate.ubicate.repository.UserRepository
import com.ubicate.ubicate.service.BusLocationService
import kotlinx.coroutines.launch

class MapaFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var viewModel: MapaViewModel
    private lateinit var busLocationViewModel: BusLocationViewModel
    private lateinit var busRouteViewModel: BusRouteViewModel
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var userLocationMarker: Marker? = null
    private val busMarkers = mutableMapOf<String, Marker>()
    private var selectedMarker: Marker? = null
    private var selectedBus: BusLocation? = null
    private var waitingLocation: LatLng? = null
    private lateinit var locationSettingsChecker: LocationSettingsChecker

    private var currentPolyline: Polyline? = null
    private lateinit var userRepository: UserRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var busLocationRepository: BusLocationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userRepository = UserRepository(requireContext())
        locationRepository = LocationRepository(requireContext())
        busLocationRepository = BusLocationRepository()

        val userId = userRepository.getUserId()

        val busFactory = BusLocationViewModelFactory(busLocationRepository)
        busLocationViewModel = ViewModelProvider(this, busFactory).get(BusLocationViewModel::class.java)

        if (userId == null) {
            val newUserId = userRepository.generateUniqueUserId()
            userRepository.saveUser(newUserId, false)
            showRoleSelectionDialog(newUserId)
        }

        val routeRepository = RouteRepository(RouteProvider())
        val routeFactory = BusRouteViewModelFactory(routeRepository)
        busRouteViewModel = ViewModelProvider(this, routeFactory).get(BusRouteViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_mapa, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        busLocationViewModel.startListening()

        locationSettingsChecker = LocationSettingsChecker(requireActivity())

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                enableUserLocation()
                viewModel.fetchUserLocation(true)
            } else {
                Toast.makeText(requireContext(), "Permiso de ubicación no concedido", Toast.LENGTH_SHORT).show()
            }
        }

        val locationRepository = LocationRepository(requireContext())
        val factory = MapaViewModelFactory(locationRepository)
        viewModel = ViewModelProvider(this, factory).get(MapaViewModel::class.java)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        viewModel.userLocation.observe(viewLifecycleOwner) { location ->
            if (location != null && ::googleMap.isInitialized) {
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(location, 15f)
                googleMap.animateCamera(cameraUpdate)

                if (userLocationMarker == null) {
                    val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_user_location)
                    val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 100, 100, false)

                    val userLocationIcon = BitmapDescriptorFactory.fromBitmap(resizedBitmap)

                    userLocationMarker = googleMap.addMarker(
                        MarkerOptions().position(location)
                            .title("Tu ubicación")
                            .icon(userLocationIcon)
                    )
                } else {
                    userLocationMarker?.position = location
                }
            }
        }

        busLocationViewModel.busLocations.observe(viewLifecycleOwner) { buses ->
            if (::googleMap.isInitialized) {
                updateBusMarkers(buses)
            }
        }

        busRouteViewModel.busRoute.observe(viewLifecycleOwner) { polylineOptions ->
            if (::googleMap.isInitialized) {
                polylineOptions.color(0xFF4CAF50.toInt())
                googleMap.addPolyline(polylineOptions)
            }
        }

        busRouteViewModel.loadSnappedRoute()

        val btnUbicacion: AppCompatImageButton = view.findViewById(R.id.btnUbicacion)
        btnUbicacion.setOnClickListener {
            val userLocation = viewModel.userLocation.value
            if (userLocation != null && ::googleMap.isInitialized) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
            } else {
                Toast.makeText(requireContext(), "No se pudo obtener la ubicación del usuario", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        startUserMode()
    }

    private fun startUserMode() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            listenToBusLocations()
        }
    }
    private fun showRoleSelectionDialog(userId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Selecciona tu rol")
            .setItems(arrayOf("Soy un bus", "Soy un usuario común")) { _, which ->
                when (which) {
                    0 -> {
                        userRepository.saveUser(userId, true)
                        saveBusId(userId)
                        startBusMode(userId)
                    }
                    1 -> {
                        userRepository.saveUser(userId, false)
                    }
                }
            }
            .show()
    }

    private fun saveBusId(busId: String) {
        val sharedPrefs = requireContext().getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("bus_id", busId).apply()
    }

    private fun getBusId(): String? {
        val sharedPrefs = requireContext().getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE)
        return sharedPrefs.getString("bus_id", null)
    }
    private fun createBusStopMarkerWithInfo(position: LatLng, walkingTime: String, walkingDistance: Float) {
        val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_bus_stop)
        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 100, 100, false)
        val stopIcon = BitmapDescriptorFactory.fromBitmap(resizedBitmap)

        selectedMarker = googleMap.addMarker(
            MarkerOptions()
                .position(position)
                .title("Esperar bus aquí")
                .icon(stopIcon)
        )

        Toast.makeText(
            requireContext(),
            "🚏 Parada seleccionada\n🚶‍♂️ Caminarás $walkingTime (${walkingDistance.toInt()}m)",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun calculateWalkingTime(distance: Float): String {
        val walkingSpeed = 1.39f
        val timeInSeconds = distance / walkingSpeed
        val timeInMinutes = timeInSeconds / 60

        return when {
            timeInMinutes < 1 -> "< 1 min"
            timeInMinutes < 60 -> "${timeInMinutes.toInt()} min"
            else -> {
                val hours = timeInMinutes.toInt() / 60
                val minutes = timeInMinutes.toInt() % 60
                "${hours}h ${minutes}m"
            }
        }
    }

    private fun listenToBusLocations() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            busLocationViewModel.busLocations.observe(viewLifecycleOwner) { buses ->
                if (::googleMap.isInitialized) {
                    updateBusMarkers(buses)
                }
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        busLocationViewModel.stopListening()
    }

    private fun startBusMode(userId: String) {
        saveBusId(userId)

        val busId = generateBusId()
        saveBusId(busId)

        if (busId != null) {
            startLocationUpdates()
        } else {
            Toast.makeText(requireContext(), "No se pudo obtener el ID del bus", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBusMarkers(buses: List<BusLocation>) {
        val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_bus_icon)
        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 75, 75, false)

        val busesIds = buses.map { it.busId }.toSet()

        val iterator = busMarkers.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key !in busesIds) {
                entry.value.remove()
                iterator.remove()
                Log.d("MapFragment", "Removed marker for bus: ${entry.key}")
            }
        }

        for (bus in buses) {
            val position = LatLng(bus.location.lat, bus.location.lng)
            Log.d("MapFragment", "Bus location for ${bus.busId}: $position")

            val marker = busMarkers[bus.busId]
            if (marker == null) {
                val newMarker = googleMap.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(bus.busId)
                        .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap))
                )
                if (newMarker != null) {
                    busMarkers[bus.busId] = newMarker
                    Log.d("MapFragment", "Added marker for bus: ${bus.busId}")
                }
            } else {
                marker.position = position
                Log.d("MapFragment", "Updated marker for bus: ${bus.busId}")
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        checkLocationPermissionAndEnable()

        val uiSettings = googleMap.uiSettings
        uiSettings.isMyLocationButtonEnabled = false

        googleMap.setOnMapClickListener { latLng ->
            selectedMarker?.remove()

            waitingLocation = latLng

            val busRouteCoordinates =
                busRouteViewModel.busRoute.value?.points ?: return@setOnMapClickListener

            val closestPoint = findNearestPointOnRoute(waitingLocation!!, busRouteCoordinates)

            val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_bus_stop)
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 100, 100, false)
            val stopIcon = BitmapDescriptorFactory.fromBitmap(resizedBitmap)

            selectedMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(closestPoint)
                    .title("Esperar bus aquí")
                    .icon(stopIcon)
            )

            currentPolyline?.remove()

            val userLocation = viewModel.userLocation.value

            if (userLocation != null) {
                lifecycleScope.launch {
                    try {
                        val routeProvider = RouteProvider()

                        val (walkingRoutePolyline, _) = routeProvider.getRouteBetweenLocations(
                            userLocation,
                            closestPoint,
                            isWalkingRoute = true
                        )

                        currentPolyline = googleMap.addPolyline(walkingRoutePolyline)

                        Toast.makeText(
                            requireContext(),
                            "Parada seleccionada. Ahora selecciona un bus para ver el tiempo de llegada",
                            Toast.LENGTH_SHORT
                        ).show()

                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            requireContext(),
                            "Error al obtener la ruta",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        googleMap.setOnMarkerClickListener { marker ->
            val busId = marker.title

            if (busId == "Esperar bus aquí") {
                if (waitingLocation != null && currentPolyline != null) {
                    val walkingDistance = calculatePolylineDistance(currentPolyline!!)
                    val walkingTime = calculateWalkingTime(walkingDistance)

                    marker.title = "🚏 Parada Seleccionada"
                    marker.snippet = "🚶‍♂️ $walkingTime • ${walkingDistance.toInt()}m hasta aquí"

                    marker.showInfoWindow()

                    Toast.makeText(
                        requireContext(),
                        "🚶‍♂️ Caminarás $walkingTime para llegar a la parada (${walkingDistance.toInt()}m)",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@setOnMarkerClickListener true
            }

            if (busId != "Esperar bus aquí" && busId != "Tu ubicación" && busId != "🚏 Parada Seleccionada") {
                selectedBus =
                    busLocationViewModel.busLocations.value?.firstOrNull { it.busId == busId }

                selectedBus?.let { bus ->
                    if (waitingLocation != null && selectedMarker != null) {
                        lifecycleScope.launch {
                            try {
                                val busLocation = LatLng(bus.location.lat, bus.location.lng)
                                val stopLocation = selectedMarker!!.position

                                val routeProvider = RouteProvider()
                                val (busToStopRoute, _) = routeProvider.getRouteBetweenLocations(
                                    busLocation,
                                    stopLocation,
                                    isWalkingRoute = false
                                )

                                val tempPolyline = googleMap.addPolyline(busToStopRoute)
                                val distance = calculatePolylineDistance(tempPolyline)
                                val eta = calculateETA(distance)

                                tempPolyline.remove()

                                showBusDetailsModal(bus, eta, "${distance.toInt()} m")

                            } catch (e: Exception) {
                                e.printStackTrace()
                                showBusDetailsModal(bus, "Calculando...", "Calculando...")
                            }
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Primero selecciona una parada en el mapa",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            true
        }
    }
    private fun calculatePolylineDistance(polyline: Polyline): Float {
        var totalDistance = 0f

        val points = polyline.points
        for (i in 0 until points.size - 1) {
            val start = points[i]
            val end = points[i + 1]
            val results = FloatArray(1)
            android.location.Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results)
            totalDistance += results[0]
        }
        return totalDistance
    }
    private fun calculateETA(distance: Float): String {
        val busSpeed = 3.5

        val timeInSeconds = distance / busSpeed

        val timeInMinutes = timeInSeconds / 60

        return "${timeInMinutes.toInt()} min"

    }

        private fun checkLocationPermissionAndEnable() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                if (locationSettingsChecker.isLocationEnabled()) {
                    enableUserLocation()
                    viewModel.fetchUserLocation(true)
                } else {
                    showLocationDisabledDialog()
                }
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(requireContext(), "Necesitamos permiso para mostrar tu ubicación", Toast.LENGTH_SHORT).show()
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun requestUserLocationUpdate() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMaxUpdates(1)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null && ::googleMap.isInitialized) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    viewModel.updateUserLocation(latLng)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                    if (userLocationMarker == null) {
                        userLocationMarker = googleMap.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title("Tu ubicación")
                        )
                    } else {
                        userLocationMarker?.position = latLng
                    }
                }
                fusedLocationClient.removeLocationUpdates(this)
            }
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    fun onLocationEnabled() {
        if (isAdded && !isDetached) {
            checkLocationPermissionAndEnable()
            refreshUserLocation()
            busLocationViewModel.startListening()
            busRouteViewModel.loadSnappedRoute()
            Toast.makeText(requireContext(), "Ubicación activada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()

        val busId = getBusId()
        if (busId != null) {
            val intent = Intent(requireContext(), BusLocationService::class.java)
            requireContext().startService(intent)
        } else {
            Toast.makeText(requireContext(), "No se pudo obtener el ID del bus", Toast.LENGTH_SHORT).show()
        }

        if (shouldShowIncertidumbreForm()) {
            val incertFormDialog = IncertidumbreDialogFragment()
            incertFormDialog.show(childFragmentManager, "IncertidumbreDialog")
        }
    }

    private fun shouldShowIncertidumbreForm(): Boolean {
        val preferences = requireContext().getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE)
        val showIncertidumbre = preferences.getBoolean("showIncertidumbre", true)

        Log.d("MapaFragment", "shouldShowIncertidumbreForm: $showIncertidumbre")
        return showIncertidumbre
    }


    private fun generateBusId(): String {
        val prefix = "AYO"
        val number = (100..999).random()
        return "$prefix-$number"
    }

    private fun showLocationDisabledDialog() {
        val existingDialog = parentFragmentManager.findFragmentByTag("location_disabled_dialog")
        if (existingDialog == null) {
            val dialog = LocationDisabledDialogFragment()
            dialog.isCancelable = false
            dialog.show(parentFragmentManager, "location_disabled_dialog")

            dialog.dialog?.setOnDismissListener {
                if (locationSettingsChecker.isLocationEnabled()) {
                    enableUserLocation()
                    viewModel.fetchUserLocation(true)
                } else {
                    requireActivity().finish()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LocationSettingsChecker.REQUEST_CHECK_SETTINGS) {
            if (resultCode == android.app.Activity.RESULT_OK) {
                Toast.makeText(requireContext(), "Ubicación activada", Toast.LENGTH_SHORT).show()
                enableUserLocation()
                viewModel.fetchUserLocation(true)
            } else {
                Toast.makeText(requireContext(), "Ubicación no activada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enableUserLocation() {
        if (!::googleMap.isInitialized) return

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun showBusDetailsModal(selectedBus: BusLocation, eta: String, distance: String) {
        val busDetailsDialog = BusDetailsDialogFragment(selectedBus, eta, distance)
        busDetailsDialog.show(childFragmentManager, "bus_details")
    }

    class BusDetailsDialogFragment(
        private val busLocation: BusLocation,
        private val eta: String,
        private val distance: String
    ) : BottomSheetDialogFragment() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.dialog_bus_details, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val busIdTextView: TextView = view.findViewById(R.id.busIdTextView)
            val etaTextView: TextView = view.findViewById(R.id.etaTextView)
            val distanceTextView: TextView = view.findViewById(R.id.distanceTextView)
            val statusTextView: TextView = view.findViewById(R.id.statusTextView)

            busIdTextView.text = busLocation.busId
            etaTextView.text = "Llega en $eta"
            distanceTextView.text = "$distance -"
            statusTextView.text = "En ruta"
        }
    }
    private fun refreshUserLocation() {
        requestUserLocationUpdate()
    }

    private fun findNearestPointOnRoute(userLocation: LatLng, busRouteCoordinates: List<LatLng>): LatLng {
        var closestDistance = Float.MAX_VALUE
        var closestPoint = busRouteCoordinates[0]

        for (i in 1 until busRouteCoordinates.size) {
            val start = busRouteCoordinates[i - 1]
            val end = busRouteCoordinates[i]

            val closest = closestPointOnLine(userLocation, start, end)

            val distance = FloatArray(1)
            android.location.Location.distanceBetween(userLocation.latitude, userLocation.longitude, closest.latitude, closest.longitude, distance)

            if (distance[0] < closestDistance) {
                closestDistance = distance[0]
                closestPoint = closest
            }
        }
        return closestPoint
    }

    private fun closestPointOnLine(point: LatLng, start: LatLng, end: LatLng): LatLng {
        val lineLength = distanceBetween(start, end)
        val t = ((point.latitude - start.latitude) * (end.latitude - start.latitude) + (point.longitude - start.longitude) * (end.longitude - start.longitude)) / lineLength
        val clampedT = t.coerceIn(0.0, 1.0)
        return LatLng(start.latitude + clampedT * (end.latitude - start.latitude), start.longitude + clampedT * (end.longitude - start.longitude))
    }

    private fun distanceBetween(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results)
        return results[0]
    }

    override fun onDestroy() {
        super.onDestroy()
        busLocationViewModel.stopListening()
    }
    private fun startLocationUpdates() {
        val handler = Handler(Looper.getMainLooper())
        val locationUpdateInterval: Long = 5000

        handler.postDelayed(object : Runnable {
            override fun run() {
                getCurrentLocationAndUpdateBusLocation()
                handler.postDelayed(this, locationUpdateInterval)
            }
        }, locationUpdateInterval)
    }


    private fun getCurrentLocationAndUpdateBusLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val lat = it.latitude
                    val lng = it.longitude
                    val busId = getBusId()
                    busId?.let {
                        busLocationRepository.updateBusLocation(busId, lat, lng)
                    }
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error al obtener la ubicación: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
