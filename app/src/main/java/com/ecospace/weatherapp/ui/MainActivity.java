package com.ecospace.weatherapp.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ecospace.weatherapp.BuildConfig;
import com.ecospace.weatherapp.R;
import com.ecospace.weatherapp.api.RetrofitClient;
import com.ecospace.weatherapp.api.WeatherApiService;
import com.ecospace.weatherapp.model.FavoriteCity;
import com.ecospace.weatherapp.model.WeatherResponse;
import com.ecospace.weatherapp.util.PreferencesHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import coil.ImageLoader;
import coil.request.ImageRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Main Activity for Weather App
 * Written in Java
 */
public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final String API_KEY = BuildConfig.WEATHER_API_KEY;

    private EditText etSearch;
    private ImageButton btnSearch;
    private ImageButton btnLocation;
    private ImageButton btnFavorites;
    private ImageButton btnFavorite;
    private ImageButton btnToggleUnit;
    private SwipeRefreshLayout swipeRefresh;
    
    private CardView cardWeather;
    private LinearLayout layoutError;
    private ProgressBar progressBar;
    
    private TextView tvCity;
    private TextView tvCountry;
    private TextView tvTemperature;
    private TextView tvDescription;
    private TextView tvFeelsLike;
    private TextView tvHumidity;
    private TextView tvWind;
    private TextView tvPressure;
    private TextView tvSunrise;
    private TextView tvSunset;
    private TextView tvLastUpdated;
    private ImageView ivWeatherIcon;
    
    private MapView mapView;
    
    private WeatherApiService apiService;
    private PreferencesHelper preferencesHelper;
    private FusedLocationProviderClient fusedLocationClient;
    
    private WeatherResponse currentWeather;
    private String currentUnit = "metric";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize OSMDroid configuration
        Configuration.getInstance().setUserAgentValue(getPackageName());
        
        setContentView(R.layout.activity_main);
        
        initViews();
        setupListeners();
        
        apiService = RetrofitClient.getInstance().getApiService();
        preferencesHelper = new PreferencesHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        currentUnit = preferencesHelper.getUnit();
        updateUnitButton();
        
        // Load last searched city or default
        String lastCity = preferencesHelper.getLastCity();
        if (lastCity != null && !lastCity.isEmpty()) {
            searchWeather(lastCity);
        } else {
            searchWeather("Nairobi"); // Default city
        }
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        btnLocation = findViewById(R.id.btnLocation);
        btnFavorites = findViewById(R.id.btnFavorites);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnToggleUnit = findViewById(R.id.btnToggleUnit);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        
        cardWeather = findViewById(R.id.cardWeather);
        layoutError = findViewById(R.id.layoutError);
        progressBar = findViewById(R.id.progressBar);
        
        tvCity = findViewById(R.id.tvCity);
        tvCountry = findViewById(R.id.tvCountry);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvDescription = findViewById(R.id.tvDescription);
        tvFeelsLike = findViewById(R.id.tvFeelsLike);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvWind = findViewById(R.id.tvWind);
        tvPressure = findViewById(R.id.tvPressure);
        tvSunrise = findViewById(R.id.tvSunrise);
        tvSunset = findViewById(R.id.tvSunset);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon);
        
        mapView = findViewById(R.id.mapView);
        setupMap();
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(10.0);
    }

    private void setupListeners() {
        btnSearch.setOnClickListener(v -> {
            String city = etSearch.getText().toString().trim();
            if (!city.isEmpty()) {
                hideKeyboard();
                searchWeather(city);
            }
        });
        
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String city = etSearch.getText().toString().trim();
                if (!city.isEmpty()) {
                    hideKeyboard();
                    searchWeather(city);
                }
                return true;
            }
            return false;
        });
        
        btnLocation.setOnClickListener(v -> getCurrentLocation());
        
        btnFavorites.setOnClickListener(v -> {
            startActivity(new Intent(this, FavoritesActivity.class));
        });
        
        btnFavorite.setOnClickListener(v -> toggleFavorite());
        
        btnToggleUnit.setOnClickListener(v -> {
            currentUnit = preferencesHelper.toggleUnit();
            updateUnitButton();
            if (currentWeather != null) {
                searchWeather(currentWeather.getName());
            }
        });
        
        swipeRefresh.setOnRefreshListener(() -> {
            if (currentWeather != null) {
                searchWeather(currentWeather.getName());
            } else {
                swipeRefresh.setRefreshing(false);
            }
        });
        
        swipeRefresh.setColorSchemeResources(R.color.primary_blue, R.color.accent_orange);
        
        findViewById(R.id.btnRetry).setOnClickListener(v -> {
            String lastCity = preferencesHelper.getLastCity();
            if (lastCity != null) {
                searchWeather(lastCity);
            }
        });
    }

    private void searchWeather(String city) {
        showLoading(true);
        
        apiService.getWeatherByCity(city, API_KEY, currentUnit)
            .enqueue(new Callback<WeatherResponse>() {
                @Override
                public void onResponse(@NonNull Call<WeatherResponse> call, 
                                      @NonNull Response<WeatherResponse> response) {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    
                    if (response.isSuccessful() && response.body() != null) {
                        currentWeather = response.body();
                        preferencesHelper.saveLastCity(city);
                        displayWeather(currentWeather);
                        showError(false);
                    } else {
                        showError(true);
                        Toast.makeText(MainActivity.this, 
                            "City not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<WeatherResponse> call, 
                                     @NonNull Throwable t) {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    showError(true);
                    Toast.makeText(MainActivity.this, 
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void searchWeatherByLocation(double lat, double lon) {
        showLoading(true);
        
        apiService.getWeatherByCoordinates(lat, lon, API_KEY, currentUnit)
            .enqueue(new Callback<WeatherResponse>() {
                @Override
                public void onResponse(@NonNull Call<WeatherResponse> call, 
                                      @NonNull Response<WeatherResponse> response) {
                    showLoading(false);
                    
                    if (response.isSuccessful() && response.body() != null) {
                        currentWeather = response.body();
                        preferencesHelper.saveLastCity(currentWeather.getName());
                        displayWeather(currentWeather);
                        showError(false);
                    } else {
                        showError(true);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<WeatherResponse> call, 
                                     @NonNull Throwable t) {
                    showLoading(false);
                    showError(true);
                    Toast.makeText(MainActivity.this, 
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void displayWeather(WeatherResponse weather) {
        tvCity.setText(weather.getName());
        tvCountry.setText(weather.getSys().getCountry());
        
        String unitSymbol = currentUnit.equals("metric") ? "°C" : "°F";
        String windUnit = currentUnit.equals("metric") ? "m/s" : "mph";
        
        tvTemperature.setText(String.format(Locale.getDefault(), 
            "%.0f%s", weather.getMain().getTemp(), unitSymbol));
        
        if (!weather.getWeather().isEmpty()) {
            String description = weather.getWeather().get(0).getDescription();
            tvDescription.setText(capitalizeFirst(description));
            
            // Load weather icon
            String iconUrl = weather.getWeather().get(0).getIconUrl();
            ImageLoader imageLoader = new ImageLoader.Builder(this).build();
            ImageRequest request = new ImageRequest.Builder(this)
                .target(ivWeatherIcon)
                .build();
            imageLoader.enqueue(request);
        }
        
        tvFeelsLike.setText(String.format(Locale.getDefault(), 
            "%.0f%s", weather.getMain().getFeelsLike(), unitSymbol));
        tvHumidity.setText(String.format(Locale.getDefault(), 
            "%d%%", weather.getMain().getHumidity()));
        tvWind.setText(String.format(Locale.getDefault(), 
            "%.1f %s", weather.getWind().getSpeed(), windUnit));
        tvPressure.setText(String.format(Locale.getDefault(), 
            "%d hPa", weather.getMain().getPressure()));
        
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tvSunrise.setText(timeFormat.format(new Date(weather.getSys().getSunrise() * 1000)));
        tvSunset.setText(timeFormat.format(new Date(weather.getSys().getSunset() * 1000)));
        
        SimpleDateFormat updateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        tvLastUpdated.setText("Updated: " + updateFormat.format(new Date()));
        
        // Update map
        updateMap(weather.getCoord().getLat(), weather.getCoord().getLon(), weather.getName());
        
        // Update favorite button
        updateFavoriteButton();
        
        cardWeather.setVisibility(View.VISIBLE);
    }

    private void updateMap(double lat, double lon, String cityName) {
        GeoPoint point = new GeoPoint(lat, lon);
        mapView.getController().setCenter(point);
        mapView.getController().setZoom(11.0);
        
        // Clear existing markers
        mapView.getOverlays().clear();
        
        // Add marker
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(cityName);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);
        
        mapView.invalidate();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                LOCATION_PERMISSION_REQUEST);
            return;
        }
        
        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(this, location -> {
                if (location != null) {
                    searchWeatherByLocation(location.getLatitude(), location.getLongitude());
                } else {
                    Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Location error: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
    }

    private void toggleFavorite() {
        if (currentWeather == null) return;
        
        FavoriteCity city = new FavoriteCity(
            currentWeather.getName(),
            currentWeather.getSys().getCountry(),
            currentWeather.getCoord().getLat(),
            currentWeather.getCoord().getLon(),
            System.currentTimeMillis()
        );
        
        if (preferencesHelper.isFavorite(city.getName(), city.getCountry())) {
            preferencesHelper.removeFavorite(city);
            Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
        } else {
            preferencesHelper.addFavorite(city);
            Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
        }
        
        updateFavoriteButton();
    }

    private void updateFavoriteButton() {
        if (currentWeather != null && 
            preferencesHelper.isFavorite(currentWeather.getName(), 
                                         currentWeather.getSys().getCountry())) {
            btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
        } else {
            btnFavorite.setImageResource(R.drawable.ic_favorite_border);
        }
    }

    private void updateUnitButton() {
        if (currentUnit.equals("metric")) {
            btnToggleUnit.setImageResource(R.drawable.ic_celsius);
        } else {
            btnToggleUnit.setImageResource(R.drawable.ic_fahrenheit);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            cardWeather.setVisibility(View.GONE);
            layoutError.setVisibility(View.GONE);
        }
    }

    private void showError(boolean show) {
        layoutError.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            cardWeather.setVisibility(View.GONE);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        updateFavoriteButton();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
