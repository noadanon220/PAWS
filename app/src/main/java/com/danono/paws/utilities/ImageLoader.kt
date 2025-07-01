package com.danono.paws.utilities

import android.content.Context
import java.lang.ref.WeakReference
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.danono.paws.R


class ImageLoader private constructor(context: Context) {
    private val contextRef = WeakReference(context)

    fun loadImage(
        source: Drawable,
        imageView: ImageView,
        placeholder: Int = R.drawable.unavailable_photo
    ) {
        contextRef.get()?.let { context ->
            Glide
                .with(context)
                .load(source)
                .centerCrop()
                .placeholder(placeholder)
                .into(imageView);
        }
    }

    fun loadImage(
        source: String,
        imageView: ImageView,
        placeholder: Int = R.drawable.unavailable_photo
    ) {
        contextRef.get()?.let { context ->
            Glide
                .with(context)
                .load(source)
                .centerCrop()
                .placeholder(placeholder)
                .into(imageView);
        }
    }

    companion object {
        @Volatile
        private var instance: ImageLoader? = null

        fun init(context: Context): ImageLoader {
            return instance ?: synchronized(this) {
                instance ?: ImageLoader(context).also { instance = it }
            }
        }

        fun getInstance(): ImageLoader {
            return instance ?: throw IllegalStateException(
                "ImageLoader must be initialized by calling init(context) before use."
            )
        }
    }

}