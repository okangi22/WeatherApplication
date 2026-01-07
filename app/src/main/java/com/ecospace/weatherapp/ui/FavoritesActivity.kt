package com.ecospace.weatherapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ecospace.weatherapp.R
import com.ecospace.weatherapp.adapter.FavoritesAdapter
import com.ecospace.weatherapp.model.FavoriteCity
import com.ecospace.weatherapp.util.PreferencesHelper
import com.google.android.material.appbar.MaterialToolbar

/**
 * Activity for displaying favorite cities
 * Written in Kotlin
 */
class FavoritesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: LinearLayout
    private lateinit var adapter: FavoritesAdapter
    private lateinit var preferencesHelper: PreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        preferencesHelper = PreferencesHelper(this)

        setupToolbar()
        setupRecyclerView()
        loadFavorites()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = FavoritesAdapter(
            onItemClick = { city ->
                // Return to main activity with selected city
                val resultIntent = Intent()
                resultIntent.putExtra("city_name", city.name)
                setResult(RESULT_OK, resultIntent)

                // Navigate to main and search
                preferencesHelper.saveLastCity(city.name)
                finish()
            },
            onDeleteClick = { city ->
                preferencesHelper.removeFavorite(city)
                loadFavorites()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadFavorites() {
        val favorites = preferencesHelper.getFavorites()

        if (favorites.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
            adapter.submitList(favorites)
        }
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }
}