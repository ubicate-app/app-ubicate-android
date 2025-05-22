package com.ubicate.ubicate.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RatingBar
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.ubicate.ubicate.R
import com.ubicate.ubicate.model.FormularioSatisfaccion
import com.ubicate.ubicate.repository.SatisfactionRepository
import java.util.UUID


class SatisfactionDialogFragment : DialogFragment() {

    private lateinit var ratingBar: RatingBar
    private lateinit var submitButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflar el layout
        val view = inflater.inflate(R.layout.dialog_satisfaction, container, false)

        // Referencias a los elementos del layout
        ratingBar = view.findViewById(R.id.ratingBar)
        submitButton = view.findViewById(R.id.submitButton)

        // Configurar el comportamiento del botón
        submitButton.setOnClickListener {
            val rating = ratingBar.rating.toInt()

            // Verifica si el usuario ha dado una puntuación
            if (rating > 0) {
                // Generar un ID único para cada formulario
                val formId = UUID.randomUUID().toString()

                // Crear el objeto del formulario
                val formulario = FormularioSatisfaccion(
                    formId = formId,  // Usamos un ID único para cada formulario
                    puntuacion = rating,
                    fechaEnvio = System.currentTimeMillis()
                )

                // Guardar en el repositorio
                SatisfactionRepository().saveSatisfactionForm(formulario)

                // Mensaje de éxito
                Toast.makeText(requireContext(), "¡Gracias por tu evaluación!", Toast.LENGTH_SHORT).show()
                dismiss()  // Cierra el diálogo

                // Cierra la actividad después de enviar el formulario
                activity?.finish()  // Esto cerrará la actividad y la aplicación

            } else {
                Toast.makeText(requireContext(), "Por favor, selecciona una puntuación.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}

