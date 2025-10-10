package org.interns.project.security.token

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlin.math.pow

object TokenCrypto {
    private val rnd = SecureRandom()

    fun randomDigits(len: Int): String {
        val bound = 10.0.pow(len.toDouble()).toInt()
        val n = rnd.nextInt(bound)
        return n.toString().padStart(len, '0')
    }

    fun randomToken(bytes: Int = 32): String {
        val b = ByteArray(bytes)
        rnd.nextBytes(b)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b)
    }

    fun sha256Hex(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val h = md.digest(input.toByteArray())
        val sb = StringBuilder(h.size * 2)
        for (b in h) sb.append(String.format("%02x", b))
        return sb.toString()
    }
}