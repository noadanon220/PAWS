package com.danono.paws.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.danono.paws.R
import com.danono.paws.model.WalkTime
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox

class WalkTimeAdapter(
    private val walkTimes: List<WalkTime>,
    private val onWalkClick: (WalkTime) -> Unit,
    private val onEditClick: (WalkTime) -> Unit,
    private val onDeleteClick: (WalkTime) -> Unit
) : RecyclerView.Adapter<WalkTimeAdapter.WalkTimeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalkTimeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_walk_time, parent, false)
        return WalkTimeViewHolder(view)
    }

    override fun onBindViewHolder(holder: WalkTimeViewHolder, position: Int) {
        val walkTime = walkTimes[position]
        holder.bind(walkTime)
    }

    override fun getItemCount(): Int = walkTimes.size

    inner class WalkTimeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.walkTimeCard)
        private val emojiText: TextView = itemView.findViewById(R.id.walkEmoji)
        private val nameText: TextView = itemView.findViewById(R.id.walkName)
        private val timeText: TextView = itemView.findViewById(R.id.walkTime)
        private val checkBox: MaterialCheckBox = itemView.findViewById(R.id.walkCompleted)
        private val statusText: TextView = itemView.findViewById(R.id.walkStatus)
        private val editButton: MaterialButton = itemView.findViewById(R.id.editButton)
        private val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)

        fun bind(walkTime: WalkTime) {
            emojiText.text = walkTime.emoji
            nameText.text = walkTime.name
            timeText.text = walkTime.time
            checkBox.isChecked = walkTime.isCompleted

            // Update status text and colors based on completion
            if (walkTime.isCompleted) {
                statusText.text = "✅ Walk Completed"
                statusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.lima_600))
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.Secondary_green))
            } else {
                statusText.text = "⏳ Pending"
                statusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray_dark))
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
            }

            // Set click listeners
            checkBox.setOnCheckedChangeListener(null) // Clear previous listener
            checkBox.setOnCheckedChangeListener { _, _ ->
                onWalkClick(walkTime)
            }

            editButton.setOnClickListener {
                onEditClick(walkTime)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(walkTime)
            }

            cardView.setOnClickListener {
                onWalkClick(walkTime)
            }
        }
    }
}