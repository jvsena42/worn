package com.github.worn

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform