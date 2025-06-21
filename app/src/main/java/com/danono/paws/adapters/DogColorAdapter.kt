package com.danono.paws.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.danono.paws.R

class DogColorAdapter(
    private val colors: List<Int>,                     // List of color resource IDs
    private val onColorSelected: (Int) -> Unit         // Callback when a color is toggled (selected or unselected)
) : RecyclerView.Adapter<DogColorAdapter.ColorViewHolder>() {

    private val selectedColors = mutableSetOf<Int>()   // Holds the selected colors

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dog_color_circle, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val color = colors[position]
        val isSelected = selectedColors.contains(color)
        holder.bind(color, isSelected)

        holder.itemView.setOnClickListener {
            val realPosition = holder.adapterPosition
            if (realPosition != RecyclerView.NO_POSITION) {
                val clickedColor = colors[realPosition]

                if (selectedColors.contains(clickedColor)) {
                    selectedColors.remove(clickedColor)
                } else {
                    selectedColors.add(clickedColor)
                }

                notifyItemChanged(realPosition)
                onColorSelected(clickedColor)
            }
        }
    }

    override fun getItemCount(): Int = colors.size

    fun getSelectedColors(): Set<Int> = selectedColors.toSet()

    fun clearSelection() {
        val previous = selectedColors.toList()
        selectedColors.clear()
        previous.forEach { color ->
            val index = colors.indexOf(color)
            if (index != -1) notifyItemChanged(index)
        }
    }

    inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorCircle: View = itemView.findViewById(R.id.color_circle)

        fun bind(color: Int, isSelected: Boolean) {
            colorCircle.backgroundTintList =
                ContextCompat.getColorStateList(itemView.context, color)

            // Always show black border background
            itemView.background = ContextCompat.getDrawable(
                itemView.context,
                R.drawable.bg_circle_unselected
            )

            // Shadow and scale on selection
            colorCircle.elevation = 6f
            colorCircle.scaleX = if (isSelected) 0.8f else 1.0f
            colorCircle.scaleY = if (isSelected) 0.8f else 1.0f
        }
    }
}
