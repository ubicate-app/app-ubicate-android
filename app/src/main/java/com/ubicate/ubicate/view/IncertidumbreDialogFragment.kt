package com.ubicate.ubicate.view

import android.content.Context
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
    private lateinit var radioGroupPregunta4: RadioGroup

    private lateinit var submitButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.dialog_incertidumbre, container, false)

        radioGroupPregunta1 = view.findViewById(R.id.radioGroupPregunta1)
        radioGroupPregunta2 = view.findViewById(R.id.radioGroupPregunta2)
        radioGroupPregunta3 = view.findViewById(R.id.radioGroupPregunta3)
        radioGroupPregunta4 = view.findViewById(R.id.radioGroupPregunta4)

        submitButton = view.findViewById(R.id.submitButton)

        submitButton.setOnClickListener {
            val respuesta1 = getRatingForQuestion(radioGroupPregunta1)
            val respuesta2 = getRatingForQuestion(radioGroupPregunta2)
            val respuesta3 = getRatingForQuestion(radioGroupPregunta3)
            val respuesta4 = getRatingForQuestion(radioGroupPregunta4)

            if (respuesta1 != -1 && respuesta2 != -1 && respuesta3 != -1 && respuesta4 != -1) {

                val formId = UUID.randomUUID().toString()

                val formulario = FormularioIncertidumbre(
                    formId = formId,
                    pregunta1 = respuesta1,
                    pregunta2 = respuesta2,
                    pregunta3 = respuesta3,
                    fechaEnvio = System.currentTimeMillis()
                )

                IncertidumbreRepository().saveIncertidumbreForm(formulario)

                val preferences = requireContext().getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE)
                preferences.edit().putBoolean("showIncertidumbre", false).apply()

                (activity as? MainActivity)?.markIncertidumbreAsSubmitted()

                Toast.makeText(requireContext(), "¡Gracias por tu evaluación!", Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Por favor, selecciona todas las puntuaciones.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun getRatingForQuestion(radioGroup: RadioGroup): Int {
        val selectedRadioButtonId = radioGroup.checkedRadioButtonId
        if (selectedRadioButtonId == -1) return -1

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
