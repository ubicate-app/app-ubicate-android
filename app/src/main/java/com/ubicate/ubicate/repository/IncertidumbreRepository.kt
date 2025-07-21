package com.ubicate.ubicate.repository

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.ubicate.ubicate.model.FormularioIncertidumbre

class IncertidumbreRepository {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    fun saveIncertidumbreForm(formulario: FormularioIncertidumbre) {
        val key = formulario.formId
        database.child("incertidumbre_forms").child(key).setValue(formulario)
    }
}
