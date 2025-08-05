package com.danono.paws.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.danono.paws.R
import com.danono.paws.model.DogPoop
import com.danono.paws.utilities.ImageLoader
import java.text.SimpleDateFormat
import java.util.*

class PoopAdapter(
    private val poops: List<DogPoop>,
    private val onPoopClick: (DogPoop) -> Unit
) : RecyclerView.Adapter<PoopAdapter.PoopViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PoopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_poop_card, parent, false)
        return PoopViewHolder(view)
    }

    override fun onBindViewHolder(holder: PoopViewHolder, position: Int) {
        val poop = poops[position]
        holder.bind(poop)

        holder.itemView.setOnClickListener {
            onPoopClick(poop)
        }
    }

    override fun getItemCount(): Int = poops.size

    class PoopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorIndicator: View = itemView.findViewById(R.id.poopColorIndicator)
        private val consistencyText: TextView = itemView.findViewById(R.id.poopConsistency)
        private val notesText: TextView = itemView.findViewById(R.id.poopNotes)
        private val dateText: TextView = itemView.findViewById(R.id.poopDate)
        private val poopImage: ImageView = itemView.findViewById(R.id.poopImage)

        fun bind(poop: DogPoop) {
            consistencyText.text = poop.consistency.ifEmpty { "Normal" }
            notesText.text = poop.notes.ifEmpty { "No notes" }

            // Format date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            dateText.text = dateFormat.format(Date(poop.lastModified))

            // Set color indicator based on poop color
            when (poop.color) {
                "Normal Brown" -> colorIndicator.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.poop_color_normal)
                )
                "Dark Brown" -> colorIndicator.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.poop_color_dark)
                )
                "Light Brown" -> colorIndicator.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.poop_color_light)
                )
                "Yellow" -> colorIndicator.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.poop_color_yellow)
                )
                "Green" -> colorIndicator.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.poop_color_green)
                )
                "Red" -> colorIndicator.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.poop_color_red)
                )
                "Black" -> colorIndicator.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.poop_color_black)
                )
                "White" -> colorIndicator.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.poop_color_white)
                )
                else -> colorIndicator.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.poop_color_normal)
                )
            }

            // Load image if available
            if (poop.imageUrl.isNotEmpty()) {
                poopImage.visibility = View.VISIBLE
                ImageLoader.getInstance().loadImage(poop.imageUrl, poopImage)
            } else {
                poopImage.visibility = View.GONE
            }
        }
    }
}