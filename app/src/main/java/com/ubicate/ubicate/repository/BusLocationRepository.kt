package com.ubicate.ubicate.repository

import android.util.Log
import com.google.firebase.database.*
import com.ubicate.ubicate.model.BusLocation

class BusLocationRepository {

    private val database = FirebaseDatabase.getInstance()
    private val busesRef = database.getReference("buses") // Ruta en tu BD

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

    fun stopListening(listener: ValueEventListener) {
        busesRef.removeEventListener(listener)
    }
}
