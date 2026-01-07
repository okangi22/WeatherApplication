package com.ecospace.weatherapp.util

import android.content.Context
import android.content.SharedPreferences
import com.ecospace.weatherapp.model.FavoriteCity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Helper class for managing favorite cities in SharedPreferences
 * Written in Kotlin
 */
class PreferencesHelper(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val PREFS_NAME = "WeatherAppPrefs"
        private const val KEY_FAVORITES = "favorite_cities"
        private const val KEY_LAST_CITY = "last_searched_city"
        private const val KEY_UNIT = "temperature_unit"
    }
    
    // Save favorite cities
    fun saveFavorites(favorites: List<FavoriteCity>) {
        val json = gson.toJson(favorites)
        sharedPreferences.edit().putString(KEY_FAVORITES, json).apply()
    }
    
    // Get favorite cities
    fun getFavorites(): List<FavoriteCity> {
        val json = sharedPreferences.getString(KEY_FAVORITES, null) ?: return emptyList()
        val type = object : TypeToken<List<FavoriteCity>>() {}.type
        return gson.fromJson(json, type)
    }
    
    // Add a favorite city
    fun addFavorite(city: FavoriteCity): Boolean {
        val favorites = getFavorites().toMutableList()
        
        // Check if already exists
        if (favorites.any { it.name.equals(city.name, ignoreCase = true) && 
                           it.country.equals(city.country, ignoreCase = true) }) {
            return false
        }
        
        favorites.add(city)
        saveFavorites(favorites)
        return true
    }
    
    // Remove a favorite city
    fun removeFavorite(city: FavoriteCity) {
        val favorites = getFavorites().toMutableList()
        favorites.removeAll { it.name.equals(city.name, ignoreCase = true) && 
                             it.country.equals(city.country, ignoreCase = true) }
        saveFavorites(favorites)
    }
    
    // Check if city is favorite
    fun isFavorite(cityName: String, country: String): Boolean {
        return getFavorites().any { 
            it.name.equals(cityName, ignoreCase = true) && 
            it.country.equals(country, ignoreCase = true) 
        }
    }
    
    // Save last searched city
    fun saveLastCity(cityName: String) {
        sharedPreferences.edit().putString(KEY_LAST_CITY, cityName).apply()
    }
    
    // Get last searched city
    fun getLastCity(): String? {
        return sharedPreferences.getString(KEY_LAST_CITY, null)
    }
    
    // Save temperature unit (metric/imperial)
    fun saveUnit(unit: String) {
        sharedPreferences.edit().putString(KEY_UNIT, unit).apply()
    }
    
    // Get temperature unit
    fun getUnit(): String {
        return sharedPreferences.getString(KEY_UNIT, "metric") ?: "metric"
    }
    
    // Toggle unit
    fun toggleUnit(): String {
        val currentUnit = getUnit()
        val newUnit = if (currentUnit == "metric") "imperial" else "metric"
        saveUnit(newUnit)
        return newUnit
    }
}
