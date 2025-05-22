package com.ubicate.ubicate.model

data class FormularioSatisfaccion(
    val formId: String = "", // Un identificador único para el formulario
    val puntuacion: Int = 0,  // Puntuación del 1 al 5
    val fechaEnvio: Long = 0L  // Fecha y hora en milisegundos
)
