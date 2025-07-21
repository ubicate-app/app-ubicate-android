package com.ubicate.ubicate.model

data class FormularioIncertidumbre(
    val formId: String = "",
    val pregunta1: Int = 0,
    val pregunta2: Int = 0,
    val pregunta3: Int = 0,
    val pregunta4: Int= 0,

    val fechaEnvio: Long = 0L
)
