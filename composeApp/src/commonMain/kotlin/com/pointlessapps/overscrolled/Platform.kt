package com.pointlessapps.overscrolled

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform