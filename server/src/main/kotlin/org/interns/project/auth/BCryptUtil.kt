package org.interns.project.auth


import at.favre.lib.crypto.bcrypt.BCrypt

fun main() {
    val password = System.getenv("HASH_PASSWORD") ?: run {
        print("Password (stdin): ")
        System.`in`.bufferedReader().readLine()
    } ?: ""
    val cost = 12
    val hash = BCrypt.withDefaults().hashToString(cost, password.toCharArray())
    println(hash)
}