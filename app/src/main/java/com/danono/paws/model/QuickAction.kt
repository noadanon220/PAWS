package com.danono.paws.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

/**
 * Represents a quick action item on the home screen. Each action has a title,
 * an icon resource, a background color and an icon tint color. When tapped,
 * the action triggers navigation to the specified destination in the nav graph.
 */
data class QuickAction(
    val title: String,
    @DrawableRes val iconRes: Int,
    @ColorRes val backgroundColorRes: Int,
    @ColorRes val iconTintRes: Int,
    val destinationId: Int
)
