package com.danono.paws.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.danono.paws.R
import com.danono.paws.model.QuickAction
import com.google.android.material.card.MaterialCardView

/**
 * Adapter for displaying a row of quick action cards on the home screen.
 * Each quick action consists of a colored square with an icon and a title. When the
 * card is tapped, the provided callback is executed allowing the fragment to
 * navigate to the appropriate destination.
 */
class QuickActionsAdapter(
    private val actions: List<QuickAction>,
    private val onClick: (QuickAction) -> Unit
) : RecyclerView.Adapter<QuickActionsAdapter.QuickActionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickActionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_activity, parent, false)
        return QuickActionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuickActionViewHolder, position: Int) {
        val action = actions[position]
        holder.bind(action)
        holder.itemView.setOnClickListener { onClick(action) }
    }

    override fun getItemCount(): Int = actions.size

    class QuickActionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.activity_card)
        private val iconView: ImageView = itemView.findViewById(R.id.card_IMG_icon)
        private val titleView: TextView = itemView.findViewById(R.id.card_LBL_title)

        fun bind(action: QuickAction) {
            titleView.text = action.title
            iconView.setImageResource(action.iconRes)

            // Set tint and background colors based on the provided resources
            val context = itemView.context
            iconView.imageTintList = ContextCompat.getColorStateList(context, action.iconTintRes)
            cardView.setCardBackgroundColor(
                ContextCompat.getColor(context, action.backgroundColorRes)
            )
        }
    }
}
