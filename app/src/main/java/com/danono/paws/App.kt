package com.danono.paws

import android.app.Application
import com.danono.paws.utilities.ImageLoader


class App:Application() {


    override fun onCreate() {
        super.onCreate()
        ImageLoader.init(this)
    }
}