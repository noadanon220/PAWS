package com.danono.paws.model

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

data class DogActivityCard(
    val title: String,
    @DrawableRes val iconRes: Int,
    @ColorInt val backgroundColor: Int
)
