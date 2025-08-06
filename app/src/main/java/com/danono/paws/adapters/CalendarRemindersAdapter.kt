package com.danono.paws.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.danono.paws.R
import com.danono.paws.model.CalendarDayReminder
import com.google.android.material.card.MaterialCardView

class CalendarRemindersAdapter(
    private val days: List<CalendarDayReminder>,
    private val onDayClick: (CalendarDayReminder) -> Unit
) : RecyclerView.Adapter<CalendarRemindersAdapter.CalendarDayViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarDayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day_reminder, parent, false)
        return CalendarDayViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarDayViewHolder, position: Int) {
        val day = days[position]
        holder.bind(day)
    }

    override fun getItemCount(): Int = days.size

    inner class CalendarDayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.calendarDayCard)
        private val dayName: TextView = itemView.findViewById(R.id.dayName)
        private val dayNumber: TextView = itemView.findViewById(R.id.dayNumber)
        private val reminderIndicator: View = itemView.findViewById(R.id.reminderIndicator)

        fun bind(day: CalendarDayReminder) {
            // Handle empty cells (before first day of month)
            if (day.dayNumber == 0) {
                dayName.visibility = View.GONE
                dayNumber.visibility = View.GONE
                reminderIndicator.visibility = View.GONE
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.transparent))
                cardView.strokeWidth = 0
                cardView.isClickable = false
                return
            }

            dayName.visibility = View.VISIBLE
            dayNumber.visibility = View.VISIBLE
            cardView.isClickable = true

            dayName.text = day.dayName
            dayNumber.text = day.dayNumber.toString()

            // Update card appearance based on state
            when {
                day.isSelected -> {
                    // Selected day styling
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.Primary_pink))
                    cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.Primary_pink)
                    cardView.strokeWidth = 4
                    dayName.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                    dayNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                }
                day.isToday -> {
                    // Today styling
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.Secondary_pink))
                    cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.Primary_pink)
                    cardView.strokeWidth = 2
                    dayName.setTextColor(ContextCompat.getColor(itemView.context, R.color.Primary_pink))
                    dayNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.Primary_pink))
                }
                else -> {
                    // Default styling
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                    cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.bg_grey)
                    cardView.strokeWidth = 1
                    dayName.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray_dark))
                    dayNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                }
            }

            // Show/hide reminder indicator
            if (day.hasReminders) {
                reminderIndicator.visibility = View.VISIBLE
                reminderIndicator.backgroundTintList = ContextCompat.getColorStateList(
                    itemView.context,
                    if (day.isSelected) R.color.white else R.color.Primary_pink
                )
            } else {
                reminderIndicator.visibility = View.GONE
            }

            // Handle click
            cardView.setOnClickListener {
                onDayClick(day)
            }
        }
    }
}