package com.ubicate.ubicate.model

data class FormularioIncertidumbre(
    val formId: String = "",            // ID único para cada formulario
    val pregunta1: Int = 0,            // Respuesta a la primera pregunta (1 a 5)
    val pregunta2: Int = 0,            // Respuesta a la segunda pregunta (1 a 5)
    val pregunta3: Int = 0,            // Respuesta a la tercera pregunta (1 a 5)
    val fechaEnvio: Long = 0L          // Fecha de envío en milisegundos
)
