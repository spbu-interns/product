package org.interns.project

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform