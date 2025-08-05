package com.danono.paws.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.danono.paws.R
import com.danono.paws.model.DayItem
import com.google.android.material.card.MaterialCardView

class DayAdapter(
    private val days: List<DayItem>,
    private val onDayClick: (DayItem) -> Unit
) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day_selector, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        holder.bind(day, position)
    }

    override fun getItemCount(): Int = days.size

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.dayCard)
        private val dayNameText: TextView = itemView.findViewById(R.id.dayName)
        private val dayNumberText: TextView = itemView.findViewById(R.id.dayNumber)

        fun bind(day: DayItem, position: Int) {
            dayNameText.text = day.dayName
            dayNumberText.text = day.dayNumber.toString()

            // Highlight today or selected day
            if (day.isToday || position == selectedPosition) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.Primary_pink))
                cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.Primary_pink)
                dayNameText.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                dayNumberText.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))

                if (day.isToday) {
                    selectedPosition = position
                }
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.bg_grey)
                dayNameText.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                dayNumberText.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray_dark))
            }

            cardView.strokeWidth = 2

            cardView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onDayClick(day)
            }
        }
    }
}