package com.github.worn

import android.app.Application
import com.github.worn.di.androidModule
import com.github.worn.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class WornApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@WornApplication)
            modules(sharedModule, androidModule)
        }
    }
}
