package com.ubicate.ubicate.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
import com.ubicate.ubicate.model.Location
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
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val routeRepository = RouteRepository(RouteProvider())
        val routeFactory = BusRouteViewModelFactory(routeRepository)
        busRouteViewModel = ViewModelProvider(this, routeFactory).get(BusRouteViewModel::class.java)

        val busRepository = BusLocationRepository()
        val busFactory = BusLocationViewModelFactory(busRepository)
        busLocationViewModel = ViewModelProvider(this, busFactory).get(BusLocationViewModel::class.java)
        busLocationViewModel.startListening()

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_mapa, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                    userLocationMarker = googleMap.addMarker(
                        MarkerOptions()
                            .position(location)
                            .title("Tu ubicación")
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
                // Cambiar el color de la polilínea aquí con un color hexadecimal (por ejemplo, #FF5722 para un color anaranjado)
                polylineOptions.color(0xFF4CAF50.toInt())

                // Agregar la polilínea con el color configurado
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

    // Calcular la distancia entre dos puntos (usuario y bus) y devolver solo la distancia en metros
    private fun calculateDistance(userLocation: LatLng, busLocation: Location): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(userLocation.latitude, userLocation.longitude, busLocation.lat, busLocation.lng, results)
        return results[0] // La distancia en metros
    }


    // Calcular el ETA (tiempo estimado de llegada) basado en la distancia en metros
    private fun calculateETA(distanceInMeters: Float): String {
        // Calculamos el ETA usando una velocidad promedio
        val averageSpeed = 13 // velocidad promedio del bus en km/h
        val speedInMetersPerSecond = averageSpeed * 1000 / 3600
        val etaInSeconds = (distanceInMeters / speedInMetersPerSecond).toInt()
        val etaInMinutes = etaInSeconds / 60

        return "$etaInMinutes min"
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        checkLocationPermissionAndEnable()

        val uiSettings = googleMap.uiSettings
        uiSettings.isMyLocationButtonEnabled = false

        // Establecer el listener para la selección de una ubicación de espera
        googleMap.setOnMapClickListener { latLng ->
            // Eliminar el marcador previo si existe
            selectedMarker?.remove()

            // Encontrar el punto más cercano en la ruta
            val nearestPoint = findNearestPointOnRoute(latLng)

            // Guardamos el punto más cercano como la ubicación de espera
            waitingLocation = nearestPoint

            // Colocar un nuevo marcador en el punto más cercano de la ruta
            selectedMarker = googleMap.addMarker(MarkerOptions().position(nearestPoint).title("Esperar bus aquí"))

            // Ahora ya no se debe abrir el modal, solo se marca la ubicación de espera
            Toast.makeText(requireContext(), "Ubicación de espera seleccionada", Toast.LENGTH_SHORT).show()
        }

        googleMap.setOnMarkerClickListener { marker ->
            val busId = marker.title
            selectedBus = busLocationViewModel.busLocations.value?.firstOrNull { it.busId == busId }

            selectedBus?.let { bus ->
                // Verificamos si ya se seleccionó una ubicación de espera
                waitingLocation?.let { waitingPoint ->
                    // Calculamos la distancia en metros
                    val busLocation = Location(bus.location.lat, bus.location.lng)
                    val distanceInMeters = calculateDistance(waitingPoint, busLocation) // Aquí calculamos la distancia en metros

                    // Convertimos la distancia en km y m
                    val kilometers = (distanceInMeters / 1000).toInt()
                    val meters = (distanceInMeters % 1000).toInt()
                    val distanceFormatted = if (kilometers > 0) {
                        "$kilometers km $meters m"
                    } else {
                        "$meters m"
                    }

                    val eta = calculateETA(distanceInMeters)

                    showBusDetailsModal(bus, eta, distanceFormatted)
                } ?: run {
                    Toast.makeText(requireContext(), "Por favor selecciona primero una ubicación de espera.", Toast.LENGTH_SHORT).show()
                }
            }

            true
        }

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
            .setMaxUpdates(1)  // Solo queremos una actualización
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
                fusedLocationClient.removeLocationUpdates(this) // detener después de recibir 1 ubicación
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

        if (!::googleMap.isInitialized) return

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            handler.postDelayed({
                if (locationSettingsChecker.isLocationEnabled()) {
                    enableUserLocation()
                    refreshUserLocation()
                } else {
                    showLocationDisabledDialog()
                }
            }, 1500)  // Delay para dejar que sistema actualice estado GPS
        }
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

    private fun updateBusMarkers(buses: List<BusLocation>) {
        val busesIds = buses.map { it.busId }.toSet()

        val iterator = busMarkers.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key !in busesIds) {
                entry.value.remove()
                iterator.remove()
            }
        }

        for (bus in buses) {
            val position = LatLng(bus.location.lat, bus.location.lng)
            val marker = busMarkers[bus.busId]
            if (marker == null) {
                val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_bus_icon)
                val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 100, 100, false)

                val newMarker = googleMap.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(bus.busId)
                        .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap))
                )
                if (newMarker != null) {
                    busMarkers[bus.busId] = newMarker
                }
            } else {
                marker.position = position
            }
        }
    }

    private fun findNearestPointOnRoute(userLocation: LatLng): LatLng {
        // Asegúrate de que la ruta del bus está cargada
        val busRouteCoordinates = busRouteViewModel.busRoute.value?.points ?: return userLocation

        var closestDistance = Float.MAX_VALUE
        var closestPoint = busRouteCoordinates[0]

        for (i in 1 until busRouteCoordinates.size) {
            val start = busRouteCoordinates[i - 1]
            val end = busRouteCoordinates[i]

            // Encuentra el punto más cercano de la línea
            val closest = closestPointOnLine(userLocation, start, end)

            // Calcula la distancia entre la ubicación del usuario y el punto más cercano
            val distance = FloatArray(1)
            android.location.Location.distanceBetween(userLocation.latitude, userLocation.longitude, closest.latitude, closest.longitude, distance)

            // Si este punto es más cercano, lo actualizamos
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
}
