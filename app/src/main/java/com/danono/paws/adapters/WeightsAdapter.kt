package com.danono.paws.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.danono.paws.R
import com.danono.paws.model.DogWeight
import java.text.SimpleDateFormat
import java.util.*


class WeightsAdapter(
    private val weights: List<DogWeight>,
    private val onWeightClick: (DogWeight) -> Unit
) : RecyclerView.Adapter<WeightsAdapter.WeightViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeightViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weight_entry, parent, false)
        return WeightViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeightViewHolder, position: Int) {
        val weightEntry = weights[position]
        holder.bind(weightEntry)

        holder.itemView.setOnClickListener {
            onWeightClick(weightEntry)
        }
    }

    override fun getItemCount(): Int = weights.size

    class WeightViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val weightText: TextView = itemView.findViewById(R.id.weightValue)
        private val dateText: TextView = itemView.findViewById(R.id.weightDate)

        fun bind(entry: DogWeight) {
            // Display weight with units
            weightText.text = "${entry.weight} kg"

            // Format the last modified date nicely
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dateText.text = dateFormat.format(Date(entry.lastModified))
        }
    }
}