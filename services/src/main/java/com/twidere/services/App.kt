package com.twidere.services

import android.app.Application
import android.content.Context

open class App: Application(){

    companion object{
       lateinit var app: Context
    }
    override fun onCreate() {
        super.onCreate()
        app = this
    }
}