package com.ubicate.ubicate.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.ubicate.ubicate.R
import com.ubicate.ubicate.model.FormularioIncertidumbre
import com.ubicate.ubicate.repository.IncertidumbreRepository
import java.util.UUID
import com.ubicate.ubicate.MainActivity

class IncertidumbreDialogFragment : DialogFragment() {

    private lateinit var radioGroupPregunta1: RadioGroup
    private lateinit var radioGroupPregunta2: RadioGroup
    private lateinit var radioGroupPregunta3: RadioGroup
    private lateinit var submitButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.dialog_incertidumbre, container, false)

        // Referencias a los elementos del layout
        radioGroupPregunta1 = view.findViewById(R.id.radioGroupPregunta1)
        radioGroupPregunta2 = view.findViewById(R.id.radioGroupPregunta2)
        radioGroupPregunta3 = view.findViewById(R.id.radioGroupPregunta3)
        submitButton = view.findViewById(R.id.submitButton)

        // Configurar el comportamiento del botón
        submitButton.setOnClickListener {
            // Obtener las respuestas seleccionadas
            val respuesta1 = getRatingForQuestion(radioGroupPregunta1)
            val respuesta2 = getRatingForQuestion(radioGroupPregunta2)
            val respuesta3 = getRatingForQuestion(radioGroupPregunta3)

            // Verificar si todas las preguntas tienen una respuesta seleccionada
            if (respuesta1 != -1 && respuesta2 != -1 && respuesta3 != -1) {
                // Generar un ID único para cada formulario
                val formId = UUID.randomUUID().toString()

                // Crear el objeto del formulario con las respuestas
                val formulario = FormularioIncertidumbre(
                    formId = formId,
                    pregunta1 = respuesta1,
                    pregunta2 = respuesta2,
                    pregunta3 = respuesta3,
                    fechaEnvio = System.currentTimeMillis()  // Obtener la fecha y hora actuales
                )

                // Guardar el formulario en el repositorio
                IncertidumbreRepository().saveIncertidumbreForm(formulario)

                // Marcar que el formulario fue enviado
                (activity as? MainActivity)?.markIncertidumbreAsSubmitted()

                // Mensaje de éxito
                Toast.makeText(requireContext(), "¡Gracias por tu evaluación!", Toast.LENGTH_SHORT).show()
                dismiss()  // Cierra el diálogo
            } else {
                Toast.makeText(requireContext(), "Por favor, selecciona todas las puntuaciones.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    // Método para obtener el valor de la respuesta seleccionada de un RadioGroup
    private fun getRatingForQuestion(radioGroup: RadioGroup): Int {
        val selectedRadioButtonId = radioGroup.checkedRadioButtonId
        if (selectedRadioButtonId == -1) return -1 // No se seleccionó ninguna respuesta

        val selectedRadioButton: RadioButton = view?.findViewById(selectedRadioButtonId) ?: return -1
        return when (selectedRadioButton.text.toString()) {
            "1" -> 1
            "2" -> 2
            "3" -> 3
            "4" -> 4
            "5" -> 5
            else -> -1 // Si no es ninguna opción válida
        }
    }
}
