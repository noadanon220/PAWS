package com.danono.paws.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.danono.paws.R
import com.danono.paws.model.DogActivityCard
import com.google.android.material.card.MaterialCardView

class ActivitiesAdapter(
    private val activityCards: List<DogActivityCard>,
    private val onCardClick: (DogActivityCard) -> Unit = {}
) : RecyclerView.Adapter<ActivitiesAdapter.ActivityViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val activityCard = activityCards[position]
        holder.bind(activityCard)

        holder.itemView.setOnClickListener {
            onCardClick(activityCard)
        }
    }

    override fun getItemCount(): Int = activityCards.size

    class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.card_LBL_title)
        private val icon: ImageView = itemView.findViewById(R.id.card_IMG_icon)
        private val cardView: MaterialCardView = itemView as MaterialCardView

        fun bind(activityCard: DogActivityCard) {
            title.text = activityCard.title
            icon.setImageResource(activityCard.iconRes)

            // Set background color
            val backgroundColor = ContextCompat.getColor(itemView.context, activityCard.backgroundColor)
            cardView.setCardBackgroundColor(backgroundColor)

            // Set foreground color for text and icon
            val foregroundColor = ContextCompat.getColor(itemView.context, activityCard.foregroundColor)
            title.setTextColor(foregroundColor)
            icon.setColorFilter(foregroundColor)

            // Set stroke color
            cardView.strokeColor = foregroundColor
            cardView.strokeWidth = 2 // 2dp stroke width
        }
    }
}