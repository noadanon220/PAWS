package com.danono.paws.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.danono.paws.R
import com.danono.paws.model.DogPark
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlin.math.roundToInt

class DogParksAdapter(
    private val parks: List<DogPark>,
    private val onParkClick: (DogPark) -> Unit,
    private val onDirectionsClick: (DogPark) -> Unit,
    private val onFavoriteClick: (DogPark) -> Unit
) : RecyclerView.Adapter<DogParksAdapter.ParkViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dog_park, parent, false)
        return ParkViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParkViewHolder, position: Int) {
        val park = parks[position]
        holder.bind(park)
    }

    override fun getItemCount(): Int = parks.size

    inner class ParkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.parkCard)
        private val nameText: TextView = itemView.findViewById(R.id.parkName)
        private val addressText: TextView = itemView.findViewById(R.id.parkAddress)
        private val distanceText: TextView = itemView.findViewById(R.id.parkDistance)
        private val ratingText: TextView = itemView.findViewById(R.id.parkRating)
        private val facilitiesChipGroup: ChipGroup = itemView.findViewById(R.id.facilitiesChipGroup)
        private val favoriteButton: ImageView = itemView.findViewById(R.id.favoriteButton)
        private val directionsButton: ImageView = itemView.findViewById(R.id.directionsButton)

        fun bind(park: DogPark) {
            nameText.text = park.name
            addressText.text = park.address

            // Distance formatting
            distanceText.text = if (park.distance > 0) {
                if (park.distance < 1) {
                    "${(park.distance * 1000).roundToInt()}m"
                } else {
                    "${(park.distance * 10).roundToInt() / 10.0}km"
                }
            } else {
                ""
            }

            // Rating
            if (park.rating > 0) {
                ratingText.text = "⭐ ${park.rating}"
                ratingText.visibility = View.VISIBLE
            } else {
                ratingText.visibility = View.GONE
            }

            // Facilities chips
            facilitiesChipGroup.removeAllViews()
            park.facilities.take(3).forEach { facility -> // Show max 3 facilities
                val chip = Chip(itemView.context).apply {
                    text = facility
                    isClickable = false
                    setTextColor(ContextCompat.getColor(itemView.context, R.color.Primary_pink))
                    setChipBackgroundColorResource(R.color.Secondary_pink)
                    textSize = 10f
                }
                facilitiesChipGroup.addView(chip)
            }

            // Favorite button
            updateFavoriteButton(park.isFavorite)

            // Click listeners
            cardView.setOnClickListener {
                onParkClick(park)
            }

            favoriteButton.setOnClickListener {
                onFavoriteClick(park)
            }

            directionsButton.setOnClickListener {
                onDirectionsClick(park)
            }
        }

        private fun updateFavoriteButton(isFavorite: Boolean) {
            if (isFavorite) {
                favoriteButton.setImageResource(R.drawable.ic_favorite_filled)
                favoriteButton.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.Primary_pink)
                )
            } else {
                favoriteButton.setImageResource(R.drawable.ic_favorite_outline)
                favoriteButton.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.gray_dark)
                )
            }
        }
    }
}