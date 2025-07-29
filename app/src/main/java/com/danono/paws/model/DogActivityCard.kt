package com.danono.paws.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.danono.paws.R

data class DogActivityCard(
    val title: String,
    @DrawableRes val iconRes: Int,
    @ColorRes val backgroundColor: Int,
    @ColorRes val foregroundColor: Int
)

object DogActivityCards {
    fun getDefaultCards() = listOf(
        DogActivityCard("Notes", R.drawable.ic_notes, R.color.Secondary_pink, R.color.Primary_pink),
        DogActivityCard("Food", R.drawable.ic_food, R.color.Secondary_green, R.color.lima_700),
        DogActivityCard("Walks", R.drawable.ic_weight, R.color.Secondary_blue, R.color.malibu_600),
        DogActivityCard("Medicine", R.drawable.ic_training, R.color.Secondary_yellow, R.color.Primary_yellow),
        DogActivityCard("Poop", R.drawable.ic_poop, R.color.Secondary_orange, R.color.orange_600),
        DogActivityCard("Training", R.drawable.ic_training, R.color.Primary_pink, R.color.black)
    )
}