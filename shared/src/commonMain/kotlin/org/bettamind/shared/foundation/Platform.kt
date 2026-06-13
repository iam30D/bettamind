package org.bettamind.shared.foundation

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
