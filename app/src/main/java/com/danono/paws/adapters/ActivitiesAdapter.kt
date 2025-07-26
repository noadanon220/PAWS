package com.danono.paws.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.danono.paws.R

class DogActivityAdapter(private val activities: List<String>) :
    RecyclerView.Adapter<DogActivityAdapter.ActivityViewHolder>() {

    private val backgroundColors = listOf(
        R.color.Secondary_pink,
        R.color.Secondary_yellow,
        R.color.Secondary_orange,
        R.color.Secondary_green,
        R.color.Secondary_blue
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val activity = activities[position]
        holder.title.text = activity

        val colorRes = backgroundColors[position % backgroundColors.size]
        holder.itemView.setBackgroundColor(
            ContextCompat.getColor(holder.itemView.context, colorRes)
        )
    }

    override fun getItemCount(): Int = activities.size

    class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.card_LBL_title)
    }
}
