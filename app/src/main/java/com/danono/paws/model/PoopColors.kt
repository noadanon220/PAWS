package com.danono.paws.model

import com.danono.paws.R

object PoopColors {
    fun getColors() = listOf(
        PoopColor("Normal Brown", R.color.poop_color_normal),
        PoopColor("Dark Brown", R.color.poop_color_dark),
        PoopColor("Light Brown", R.color.poop_color_light),
        PoopColor("Yellow", R.color.poop_color_yellow),
        PoopColor("Green", R.color.poop_color_green),
        PoopColor("Red", R.color.poop_color_red),
        PoopColor("Black", R.color.poop_color_black),
        PoopColor("White", R.color.poop_color_white)
    )
}

data class PoopColor(
    val name: String,
    val colorRes: Int
)