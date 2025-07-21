package com.ubicate.ubicate.repository

import android.util.Log
import com.google.firebase.database.*
import com.ubicate.ubicate.model.BusLocation
import com.ubicate.ubicate.model.Location

class BusLocationRepository {

    private val database = FirebaseDatabase.getInstance()
    private val busesRef = database.getReference("buses")


    fun stopListening(listener: ValueEventListener) {
        busesRef.removeEventListener(listener)
    }

    fun updateBusLocation(busId: String, lat: Double, lng: Double) {
        val busLocation = BusLocation(busId, Location(lat, lng), System.currentTimeMillis())
        busesRef.child(busId).setValue(busLocation)
            .addOnSuccessListener {
                Log.d("BusLocationRepository", "Ubicación del bus actualizada")
            }
            .addOnFailureListener { e ->
                Log.e("BusLocationRepository", "Error al actualizar la ubicación del bus", e)
            }
    }

    fun listenBusLocations(
        onUpdate: (List<BusLocation>) -> Unit,
        onError: (DatabaseError) -> Unit
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val busList = mutableListOf<BusLocation>()
                for (busSnapshot in snapshot.children) {
                    val busLocation = busSnapshot.getValue(BusLocation::class.java)
                    if (busLocation != null) {
                        busList.add(busLocation)
                    }
                }
                Log.d("BusLocationRepository", "Datos recibidos: ${busList.size} buses")
                onUpdate(busList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BusLocationRepository", "Error al leer datos", error.toException())
                onError(error)
            }
        }
        busesRef.addValueEventListener(listener)
        return listener
    }

}
