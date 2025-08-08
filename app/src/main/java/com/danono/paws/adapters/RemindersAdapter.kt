package com.danono.paws.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.danono.paws.R
import com.danono.paws.model.Reminder
import com.danono.paws.model.ReminderType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Adapter that renders reminder cards with type, dog, date/time, location and notes.
class RemindersAdapter(
    private var items: List<Reminder>,
    private val onItemClick: (Reminder) -> Unit
) : RecyclerView.Adapter<RemindersAdapter.ReminderVH>() {

    private val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder_card, parent, false)
        return ReminderVH(view)
    }

    override fun onBindViewHolder(holder: ReminderVH, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<Reminder>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class ReminderVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.reminderIcon)
        private val title: TextView = itemView.findViewById(R.id.reminderTitle)
        private val dogAndTime: TextView = itemView.findViewById(R.id.reminderDogAndTime)
        private val locationRow: View = itemView.findViewById(R.id.locationRow)
        private val locationText: TextView = itemView.findViewById(R.id.reminderLocationText)
        private val notesText: TextView = itemView.findViewById(R.id.reminderNotesText)

        fun bind(reminder: Reminder) {
            val type = reminder.reminderType
            title.text = "${type.emoji}  ${type.displayName}"

            icon.setImageResource(iconForType(type))

            val d = Date(reminder.dateTime)
            val datePart = dateFormat.format(d)
            val timePart = timeFormat.format(d)
            dogAndTime.text = "${reminder.dogName} • $datePart • $timePart"

            if (reminder.location.isBlank()) {
                locationRow.visibility = View.GONE
            } else {
                locationRow.visibility = View.VISIBLE
                locationText.text = reminder.location
            }

            if (reminder.notes.isBlank()) {
                notesText.visibility = View.GONE
            } else {
                notesText.visibility = View.VISIBLE
                notesText.text = reminder.notes
            }
        }
    }

    private fun iconForType(type: ReminderType): Int {
        return when (type) {
            ReminderType.TRAINING,
            ReminderType.GROOMING,
            ReminderType.WALKING,
            ReminderType.BATH,
            ReminderType.NAIL_CLIPPING -> R.drawable.ic_training
            ReminderType.MEDICATION,
            ReminderType.VACCINATION,
            ReminderType.DEWORMING,
            ReminderType.CHECKUP,
            ReminderType.VET_APPOINTMENT,
            ReminderType.FEEDING,
            ReminderType.OTHER -> R.drawable.ic_notes
        }
    }
}
