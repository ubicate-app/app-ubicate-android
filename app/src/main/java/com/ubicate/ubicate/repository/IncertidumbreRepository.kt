package com.ubicate.ubicate.repository

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.ubicate.ubicate.model.FormularioIncertidumbre

class IncertidumbreRepository {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    // Método para guardar el formulario de incertidumbre en la base de datos
    fun saveIncertidumbreForm(formulario: FormularioIncertidumbre) {
        val key = formulario.formId  // Usamos el formId generado como clave
        database.child("incertidumbre_forms").child(key).setValue(formulario)  // Guardamos bajo la ruta 'incertidumbre_forms'
    }
}
