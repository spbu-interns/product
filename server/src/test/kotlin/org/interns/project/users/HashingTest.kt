package org.interns.project.users

import kotlin.test.Test
import kotlin.test.assertTrue
import at.favre.lib.crypto.bcrypt.BCrypt
import kotlin.test.*
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class HashingTest {
    @Test
    fun bcrypt_hash_and_verify() {
        val password = "super-secret-123!"
        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val result = BCrypt.verifyer().verify(password.toCharArray(), hash)
        assertTrue(result.verified)
    }

    @Test
    fun bcrypt_differentHashesForSamePassword() {
        val password = "samepass"
        val h1 = BCrypt.withDefaults().hashToString(10, password.toCharArray())
        val h2 = BCrypt.withDefaults().hashToString(10, password.toCharArray())
        assertTrue(h1 != h2)
        assertTrue(BCrypt.verifyer().verify(password.toCharArray(), h1).verified)
        assertTrue(BCrypt.verifyer().verify(password.toCharArray(), h2).verified)
    }

    private val bcryptPattern = Regex("""^\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}$""")

    private fun extractCost(hash: String): Int {
        val m = Regex("""^\$2[aby]\$(\d{2})\$""").find(hash)
            ?: error("Не удалось распарсить cost из хеша: $hash")
        return m.groupValues[1].toInt()
    }

    @Test
    fun `verify fails on wrong password`() {
        val hash = BCrypt.withDefaults().hashToString(10, "correct".toCharArray())
        val res = BCrypt.verifyer().verify("incorrect".toCharArray(), hash)
        assertFalse(res.verified)
    }

    @Test
    fun `many salts are unique`() {
        val pwd = "salt-uniqueness"
        val hashes = (1..50).map {
            BCrypt.withDefaults().hashToString(10, pwd.toCharArray())
        }
        assertEquals(hashes.size, hashes.toSet().size, "Ожидали уникальные хеши (разные соли)")
    }

    @Test
    fun `hash format and length are correct`() {
        val hash = BCrypt.withDefaults().hashToString(12, "x".toCharArray())
        assertEquals(60, hash.length, "Стандартная длина bcrypt-строки — 60 символов")
        assertTrue(bcryptPattern.matches(hash), "Хеш не соответствует формату bcrypt: $hash")
    }

    @Test
    fun `embedded cost equals requested`() {
        val costs = listOf(6, 10, 12)
        for (c in costs) {
            val h = BCrypt.withDefaults().hashToString(c, "cost-test".toCharArray())
            assertEquals(c, extractCost(h), "Неверный cost в хеше: $h")
            assertTrue(BCrypt.verifyer().verify("cost-test".toCharArray(), h).verified)
        }
    }

    @Test
    fun `tampering hash makes verification fail (or throw)`() {
        val pwd = "dont-tamper"
        val h = BCrypt.withDefaults().hashToString(10, pwd.toCharArray())

        val tampered1 = h.dropLast(1) + if (h.last() != 'A') 'A' else 'B'
        runCatching { BCrypt.verifyer().verify(pwd.toCharArray(), tampered1) }
            .onSuccess { assertFalse(it.verified, "Подмена хеша должна приводить к провалу") }
            .onFailure { }

        val tampered2 = h.replaceFirst("$", "")
        runCatching { BCrypt.verifyer().verify(pwd.toCharArray(), tampered2) }
            .onSuccess { assertFalse(it.verified) }
            .onFailure { }
    }

    @Test
    fun `no implicit trimming - spaces matter`() {
        val h = BCrypt.withDefaults().hashToString(10, "pass".toCharArray())
        assertFalse(BCrypt.verifyer().verify(" pass".toCharArray(), h).verified)
        assertFalse(BCrypt.verifyer().verify("pass ".toCharArray(), h).verified)
        assertTrue(BCrypt.verifyer().verify("pass".toCharArray(), h).verified)
    }

    @Test
    fun `unicode passwords work`() {
        val pwd = "Пароль🔒🙂"
        val h = BCrypt.withDefaults().hashToString(10, pwd.toCharArray())
        assertTrue(BCrypt.verifyer().verify(pwd.toCharArray(), h).verified)
        assertFalse(BCrypt.verifyer().verify("Пароль🔒🙃".toCharArray(), h).verified)
    }

    @Test
    fun `long passwords still verify`() {
        val longPwd = "a".repeat(71)
        val h = BCrypt.withDefaults().hashToString(10, longPwd.toCharArray())
        assertTrue(BCrypt.verifyer().verify(longPwd.toCharArray(), h).verified)
    }

    @Test
    fun `thread-safety - parallel hashing produces unique hashes`() {
        val threads = 16
        val latch = CountDownLatch(threads)
        val hashes = Collections.synchronizedList(mutableListOf<String>())
        val pwd = "parallel"

        repeat(threads) {
            thread(start = true) {
                try {
                    val h = BCrypt.withDefaults().hashToString(10, pwd.toCharArray())
                    hashes.add(h)
                } finally {
                    latch.countDown()
                }
            }
        }
        assertTrue(latch.await(15, TimeUnit.SECONDS), "Не дождались завершения потоков")
        assertEquals(threads, hashes.size)
        assertEquals(threads, hashes.toSet().size, "Хеши должны отличаться из-за разных солей")
        hashes.forEach { assertTrue(BCrypt.verifyer().verify(pwd.toCharArray(), it).verified) }
    }
}
