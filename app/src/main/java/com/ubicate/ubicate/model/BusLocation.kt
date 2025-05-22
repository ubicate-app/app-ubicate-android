package com.ubicate.ubicate.model

data class BusLocation(
    val busId: String = "",
    val location: Location = Location(),
    val timestamp: Long = 0L
)