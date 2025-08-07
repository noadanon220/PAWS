package com.danono.paws.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.danono.paws.R
import com.danono.paws.model.Reminder
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class RemindersAdapter(
    private val reminders: List<Reminder>,
    private val onReminderClick: (Reminder) -> Unit
) : RecyclerView.Adapter<RemindersAdapter.ReminderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder_card, parent, false)
        return ReminderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = reminders[position]
        holder.bind(reminder)

        holder.itemView.setOnClickListener {
            onReminderClick(reminder)
        }
    }

    override fun getItemCount(): Int = reminders.size

    class ReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.reminderCard)
        private val titleText: TextView = itemView.findViewById(R.id.reminderTitle)
        private val dogNameText: TextView = itemView.findViewById(R.id.reminderDogName)
        private val notesText: TextView = itemView.findViewById(R.id.reminderNotes)
        private val timeText: TextView = itemView.findViewById(R.id.reminderTime)
        private val dateText: TextView = itemView.findViewById(R.id.reminderDate)
        private val typeIcon: ImageView = itemView.findViewById(R.id.reminderTypeIcon)
        private val statusIndicator: View = itemView.findViewById(R.id.statusIndicator)

        fun bind(reminder: Reminder) {
            titleText.text = "${reminder.reminderType.emoji} ${reminder.title}"

            // Show dog name
            if (reminder.dogName.isNotEmpty()) {
                dogNameText.text = "🐕 ${reminder.dogName}"
                dogNameText.visibility = View.VISIBLE
            } else {
                dogNameText.visibility = View.GONE
            }

            // Show notes
            if (reminder.notes.isNotEmpty()) {
                notesText.text = reminder.notes
                notesText.visibility = View.VISIBLE
            } else {
                notesText.visibility = View.GONE
            }

            // Format date and time
            val date = Date(reminder.dateTime)
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

            timeText.text = timeFormat.format(date)
            dateText.text = dateFormat.format(date)

            // Set type icon
            val iconRes = when (reminder.reminderType) {
                com.danono.paws.model.ReminderType.VET_APPOINTMENT -> R.drawable.ic_training
                com.danono.paws.model.ReminderType.VACCINATION -> R.drawable.ic_notes
                com.danono.paws.model.ReminderType.GROOMING -> R.drawable.ic_training
                com.danono.paws.model.ReminderType.MEDICATION -> R.drawable.ic_notes
                com.danono.paws.model.ReminderType.TRAINING -> R.drawable.ic_training
                com.danono.paws.model.ReminderType.WALKING -> R.drawable.ic_weight
                com.danono.paws.model.ReminderType.FEEDING -> R.drawable.ic_food
                com.danono.paws.model.ReminderType.BATH -> R.drawable.ic_notes
                com.danono.paws.model.ReminderType.NAIL_CLIPPING -> R.drawable.ic_training
                com.danono.paws.model.ReminderType.DEWORMING -> R.drawable.ic_notes
                com.danono.paws.model.ReminderType.CHECKUP -> R.drawable.ic_training
                else -> R.drawable.calendar_ic
            }
            typeIcon.setImageResource(iconRes)

            // Set status indicator color
            val statusColor = if (reminder.isCompleted) {
                R.color.lima_600
            } else {
                val currentTime = System.currentTimeMillis()
                if (reminder.dateTime < currentTime) {
                    R.color.orange_600 // Overdue
                } else {
                    R.color.Primary_pink // Upcoming
                }
            }
            statusIndicator.backgroundTintList =
                ContextCompat.getColorStateList(itemView.context, statusColor)

            // Update card appearance based on status
            if (reminder.isCompleted) {
                cardView.alpha = 0.7f
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.Secondary_green)
                )
            } else {
                cardView.alpha = 1.0f
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.white)
                )
            }
        }
    }
}