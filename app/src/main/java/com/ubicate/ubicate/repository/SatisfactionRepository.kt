package com.ubicate.ubicate.repository

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.ubicate.ubicate.model.FormularioSatisfaccion

class SatisfactionRepository {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    fun saveSatisfactionForm(formulario: FormularioSatisfaccion) {
        val key = formulario.formId  // Usamos el formId generado como clave
        database.child("satisfaction_forms").child(key).setValue(formulario)
    }
}
