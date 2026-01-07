package com.ecospace.weatherapp.api;

import com.ecospace.weatherapp.model.WeatherResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit API Service for OpenWeatherMap
 * Written in Java for demonstration
 */
public interface WeatherApiService {
    
    /**
     * Get current weather by city name
     */
    @GET("weather")
    Call<WeatherResponse> getWeatherByCity(
        @Query("q") String cityName,
        @Query("appid") String apiKey,
        @Query("units") String units
    );
    
    /**
     * Get current weather by coordinates
     */
    @GET("weather")
    Call<WeatherResponse> getWeatherByCoordinates(
        @Query("lat") double latitude,
        @Query("lon") double longitude,
        @Query("appid") String apiKey,
        @Query("units") String units
    );
}
