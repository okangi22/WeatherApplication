package com.ecospace.weatherapp.model

/**
 * Data class for storing favorite cities
 */
data class FavoriteCity(
    val name: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun getDisplayName(): String {
        return "$name, $country"
    }
}
