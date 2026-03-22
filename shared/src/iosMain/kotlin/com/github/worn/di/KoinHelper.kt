package com.github.worn.di

import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(sharedModule, iosModule)
    }
}
