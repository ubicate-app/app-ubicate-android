package com.ubicate.ubicate.view

import BusDetailsDialogFragment
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.ubicate.ubicate.R
import com.ubicate.ubicate.model.BusLocation
import com.ubicate.ubicate.model.Location
import com.ubicate.ubicate.repository.BusLocationRepository
import com.ubicate.ubicate.repository.LocationRepository
import com.ubicate.ubicate.viewmodel.BusLocationViewModel
import com.ubicate.ubicate.viewmodel.BusRouteViewModel
import com.ubicate.ubicate.viewmodel.factory.BusRouteViewModelFactory
import com.ubicate.ubicate.repository.RouteRepository
import com.ubicate.ubicate.service.RouteProvider

import com.ubicate.ubicate.viewmodel.factory.BusLocationViewModelFactory
import com.ubicate.ubicate.viewmodel.MapaViewModel
import com.ubicate.ubicate.viewmodel.factory.MapaViewModelFactory

class MapaFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var viewModel: MapaViewModel
    private lateinit var busLocationViewModel: BusLocationViewModel
    private lateinit var busRouteViewModel: BusRouteViewModel
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var userLocationMarker: Marker? = null
    private val busMarkers = mutableMapOf<String, Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa el BusRouteViewModel
        val routeRepository = RouteRepository(RouteProvider())
        val routeFactory = BusRouteViewModelFactory(routeRepository)
        busRouteViewModel = ViewModelProvider(this, routeFactory).get(BusRouteViewModel::class.java)

        // Inicializa BusLocationViewModel con su factory
        val busRepository = BusLocationRepository()
        val busFactory = BusLocationViewModelFactory(busRepository)
        busLocationViewModel = ViewModelProvider(this, busFactory).get(BusLocationViewModel::class.java)
        busLocationViewModel.startListening() // Comenzar a escuchar ubicaciones de buses
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_mapa, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar requestPermissionLauncher aquí
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

        // Observa la ubicación del usuario
        viewModel.userLocation.observe(viewLifecycleOwner) { location ->
            if (location != null && ::googleMap.isInitialized) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))

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

        // Observa las ubicaciones de buses y actualiza los marcadores
        busLocationViewModel.busLocations.observe(viewLifecycleOwner) { buses ->
            if (::googleMap.isInitialized) {
                updateBusMarkers(buses)
            }
        }

        // Observa la ruta del bus y dibújala en el mapa
        busRouteViewModel.busRoute.observe(viewLifecycleOwner) { polylineOptions ->
            if (::googleMap.isInitialized) {
                googleMap.addPolyline(polylineOptions)  // Dibuja la ruta
            }
        }

        // Cargar la ruta
        busRouteViewModel.loadSnappedRoute() // Cambiado para usar la ruta ajustada a las carreteras

        // Obtener el botón de ubicación
        val btnUbicacion: AppCompatImageButton = view.findViewById(R.id.btnUbicacion)

        // Configurar el listener del botón para centrar el mapa en la ubicación del usuario
        btnUbicacion.setOnClickListener {
            val userLocation = viewModel.userLocation.value
            if (userLocation != null && ::googleMap.isInitialized) {
                // Centrar la cámara en la ubicación del usuario
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
            } else {
                Toast.makeText(requireContext(), "No se pudo obtener la ubicación del usuario", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        checkLocationPermissionAndEnable()

        // Deshabilitar el botón de ubicación
        val uiSettings = googleMap.uiSettings
        uiSettings.isMyLocationButtonEnabled = false  // Esto elimina el botón de ubicación

        googleMap.setOnMarkerClickListener { marker ->
            val busId = marker.title
            val selectedBusLocation = busLocationViewModel.busLocations.value?.find { it.busId == busId }

            if (selectedBusLocation != null) {
                val userLocation = viewModel.userLocation.value
                if (userLocation != null) {
                    val eta = calculateETA(userLocation, selectedBusLocation.location)
                    val distance = calculateDistance(userLocation, selectedBusLocation.location)

                    // Mostrar el BottomSheet
                    BusDetailsDialogFragment(selectedBusLocation, eta, distance).show(childFragmentManager, "BusDetails")
                }
            }
            true
        }
    }


    private fun calculateDistance(userLocation: LatLng, busLocation: Location): String {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(userLocation.latitude, userLocation.longitude, busLocation.lat, busLocation.lng, results)
        return "${results[0].toInt()} m"
    }

    private fun calculateETA(userLocation: LatLng, busLocation: Location): String {
        val distanceInMeters = calculateDistance(userLocation, busLocation).replace(" m", "").toFloat()
        val averageSpeed = 20 // velocidad promedio del bus en km/h

        val speedInMetersPerSecond = averageSpeed * 1000 / 3600
        val etaInSeconds = (distanceInMeters / speedInMetersPerSecond).toInt()
        val etaInMinutes = etaInSeconds / 60
        return "$etaInMinutes min"
    }

    private fun checkLocationPermissionAndEnable() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                enableUserLocation()
                viewModel.fetchUserLocation(true)
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

    private fun enableUserLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
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
                // Cargar el icono original
                val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_bus_icon)

                // Redimensionar el icono
                val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 100, 100, false)  // Ajusta el tamaño (100x100 en este caso)

                val newMarker = googleMap.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(bus.busId)  // Guardar el ID del bus como título
                        .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)) // Usar el bitmap redimensionado
                )
                if (newMarker != null) {
                    busMarkers[bus.busId] = newMarker
                }
            } else {
                marker.position = position
            }
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        busLocationViewModel.stopListening()
    }
}
