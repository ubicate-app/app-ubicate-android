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
        val view = inflater.inflate(R.layout.dialog_satisfaction, container, false)

        ratingBar = view.findViewById(R.id.ratingBar)
        submitButton = view.findViewById(R.id.submitButton)

        submitButton.setOnClickListener {
            val rating = ratingBar.rating.toInt()

            if (rating > 0) {
                val formId = UUID.randomUUID().toString()

                val formulario = FormularioSatisfaccion(
                    formId = formId,
                    puntuacion = rating,
                    fechaEnvio = System.currentTimeMillis()
                )

                SatisfactionRepository().saveSatisfactionForm(formulario)
                Toast.makeText(requireContext(), "¡Gracias por tu evaluación!", Toast.LENGTH_SHORT).show()
                dismiss()
                activity?.finish()

            } else {
                Toast.makeText(requireContext(), "Por favor, selecciona una puntuación.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}

