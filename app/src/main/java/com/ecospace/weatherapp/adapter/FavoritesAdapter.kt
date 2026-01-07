package com.ecospace.weatherapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ecospace.weatherapp.R
import com.ecospace.weatherapp.model.FavoriteCity

/**
 * RecyclerView Adapter for favorite cities list
 * Written in Kotlin
 */
class FavoritesAdapter(
    private val onItemClick: (FavoriteCity) -> Unit,
    private val onDeleteClick: (FavoriteCity) -> Unit
) : ListAdapter<FavoriteCity, FavoritesAdapter.FavoriteViewHolder>(FavoriteDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_city, parent, false)
        return FavoriteViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCityName: TextView = itemView.findViewById(R.id.tvCityName)
        private val tvCountry: TextView = itemView.findViewById(R.id.tvCountry)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        
        fun bind(city: FavoriteCity) {
            tvCityName.text = city.name
            tvCountry.text = city.country
            
            itemView.setOnClickListener { onItemClick(city) }
            btnDelete.setOnClickListener { onDeleteClick(city) }
        }
    }
    
    class FavoriteDiffCallback : DiffUtil.ItemCallback<FavoriteCity>() {
        override fun areItemsTheSame(oldItem: FavoriteCity, newItem: FavoriteCity): Boolean {
            return oldItem.name == newItem.name && oldItem.country == newItem.country
        }
        
        override fun areContentsTheSame(oldItem: FavoriteCity, newItem: FavoriteCity): Boolean {
            return oldItem == newItem
        }
    }
}
