package com.danono.paws.utilities

import android.content.Context
import java.lang.ref.WeakReference
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.danono.paws.R
import android.net.Uri
import android.util.Log

class ImageLoader private constructor(context: Context) {
    private val contextRef = WeakReference(context)

    fun loadImage(
        source: Drawable,
        imageView: ImageView,
        placeholder: Int = R.drawable.default_dog_img
    ) {
        contextRef.get()?.let { context ->
            Glide
                .with(context)
                .load(source)
                .centerCrop()
                .placeholder(placeholder)
                .error(placeholder)
                .into(imageView)
        }
    }

    fun loadImage(
        source: String,
        imageView: ImageView,
        placeholder: Int = R.drawable.default_dog_img
    ) {
        contextRef.get()?.let { context ->
            if (source.isEmpty()) {
                // If no URL provided, show placeholder
                imageView.setImageResource(placeholder)
                return
            }

            Glide
                .with(context)
                .load(source)
                .centerCrop()
                .placeholder(placeholder)
                .error(placeholder)
                .listener(object : com.bumptech.glide.request.RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("ImageLoader", "Failed to load image: $source", e)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: com.bumptech.glide.request.target.Target<Drawable>,
                        dataSource: com.bumptech.glide.load.DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("ImageLoader", "Successfully loaded image: $source")
                        return false
                    }
                })
                .into(imageView)
        }
    }

    // New function for handling URI
    fun loadImage(
        source: Uri,
        imageView: ImageView,
        placeholder: Int = R.drawable.default_dog_img
    ) {
        contextRef.get()?.let { context ->
            Glide
                .with(context)
                .load(source)
                .centerCrop()
                .placeholder(placeholder)
                .error(placeholder)
                .listener(object : com.bumptech.glide.request.RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("ImageLoader", "Failed to load image URI: $source", e)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: com.bumptech.glide.request.target.Target<Drawable>,
                        dataSource: com.bumptech.glide.load.DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("ImageLoader", "Successfully loaded image URI: $source")
                        return false
                    }
                })
                .into(imageView)
        }
    }

    // Helper function for loading dog images specifically
    fun loadDogImage(
        imageUrl: String,
        imageView: ImageView
    ) {
        loadImage(imageUrl, imageView, R.drawable.default_dog_img)
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