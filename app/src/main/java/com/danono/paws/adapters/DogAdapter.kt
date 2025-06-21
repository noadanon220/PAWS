package com.danono.paws.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.danono.paws.R
import com.danono.paws.model.Dog

// Adapter class that connects the list of Dog objects to the RecyclerView
class DogAdapter(private val dogList: List<Dog>) :
    RecyclerView.Adapter<DogAdapter.DogViewHolder>() {

    // This is to get access to each detail inside the dog item
    class DogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dog_img: AppCompatImageView = itemView.findViewById(R.id.dog_img)
        val dog_name: AppCompatTextView = itemView.findViewById(R.id.dog_name)
    }

    // This is getting called only when a new ViewHolder needs to be created
    // inflate: turn the dog_item.xml to a View
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DogViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_home_dog, parent, false)
        return DogViewHolder(view)
    }

    // Binds data from a Dog object to a ViewHolder (fills the item layout with content)
    override fun onBindViewHolder(holder: DogViewHolder, position: Int) {
        val dog = dogList[position]
        holder.dog_name.text = dog.name
        holder.dog_img.setImageResource(dog.image)
    }

    // Returns the total number of items to display in the RecyclerView
    override fun getItemCount(): Int = dogList.size


} // DogAdapter