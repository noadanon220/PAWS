package com.danono.paws.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.danono.paws.R
import com.danono.paws.model.CalendarDay
import com.google.android.material.card.MaterialCardView

class CalendarDayAdapter(
    private val days: List<CalendarDay>,
    private val onDayClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarDayAdapter.CalendarDayViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarDayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calender_day, parent, false)
        return CalendarDayViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarDayViewHolder, position: Int) {
        val day = days[position]
        holder.bind(day)
    }

    override fun getItemCount(): Int = days.size

    inner class CalendarDayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.calendarDayCard)
        private val dayAbbreviation: TextView = itemView.findViewById(R.id.dayAbbreviation)
        private val dayNumber: TextView = itemView.findViewById(R.id.dayNumber)
        private val morningIndicator: View = itemView.findViewById(R.id.morningIndicator)
        private val afternoonIndicator: View = itemView.findViewById(R.id.afternoonIndicator)
        private val eveningIndicator: View = itemView.findViewById(R.id.eveningIndicator)

        fun bind(day: CalendarDay) {
            dayAbbreviation.text = day.dayAbbreviation
            dayNumber.text = day.dayNumber.toString()

            // Update card appearance based on state
            when {
                day.isSelected -> {
                    // Selected day styling
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.Primary_pink))
                    cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.Primary_pink)
                    dayAbbreviation.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                    dayNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                }
                day.isToday -> {
                    // Today styling
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.Secondary_pink))
                    cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.Primary_pink)
                    dayAbbreviation.setTextColor(ContextCompat.getColor(itemView.context, R.color.Primary_pink))
                    dayNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.Primary_pink))
                }
                else -> {
                    // Default styling
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                    cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.bg_grey)
                    dayAbbreviation.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray_dark))
                    dayNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                }
            }

            // Update walk completion indicators
            updateIndicator(morningIndicator, day.walkData.morningCompleted)
            updateIndicator(afternoonIndicator, day.walkData.afternoonCompleted)
            updateIndicator(eveningIndicator, day.walkData.eveningCompleted)

            // Handle click
            cardView.setOnClickListener {
                onDayClick(day)
            }
        }

        /**
         * Update individual walk indicator
         */
        private fun updateIndicator(indicator: View, completed: Boolean) {
            val colorRes = if (completed) {
                R.color.lima_600  // Green for completed
            } else {
                R.color.bg_grey   // Gray for not completed
            }
            indicator.backgroundTintList = ContextCompat.getColorStateList(itemView.context, colorRes)
        }
    }
}