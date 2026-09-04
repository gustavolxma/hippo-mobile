package br.com.hippo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform