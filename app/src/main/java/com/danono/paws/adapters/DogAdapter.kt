package com.danono.paws.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.danono.paws.R
import com.danono.paws.model.Dog
import com.danono.paws.utilities.ImageLoader

// Adapter class that connects the list of Dog objects with their IDs to the RecyclerView
class DogAdapter(
    private val dogList: List<Pair<Dog, String>>, // Pair of Dog and its Firebase document ID
    private val onDogClick: (Dog, String) -> Unit // Callback function for dog card clicks with dog and ID
) : RecyclerView.Adapter<DogAdapter.DogViewHolder>() {

    // ViewHolder class to access each view inside the item_dog_card layout
    class DogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dogImage: AppCompatImageView = itemView.findViewById(R.id.itemDog_IMG_dogImage)
        val dogName: AppCompatTextView = itemView.findViewById(R.id.itemDog_LBL_name)
        val cardView: View = itemView.findViewById(R.id.itemDog_CARD_img) // Reference to the card
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dog_card, parent, false)
        return DogViewHolder(view)
    }

    override fun onBindViewHolder(holder: DogViewHolder, position: Int) {
        val (dog, dogId) = dogList[position]
        holder.dogName.text = dog.name

        // Use ImageLoader instead of direct Glide calls
        ImageLoader.getInstance().loadDogImage(dog.imageUrl, holder.dogImage)

        // Set click listener on the entire card - pass both dog and dogId
        holder.itemView.setOnClickListener {
            onDogClick(dog, dogId)
        }
    }

    override fun getItemCount(): Int = dogList.size
}