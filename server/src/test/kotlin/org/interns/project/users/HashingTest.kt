package org.interns.project.users

import kotlin.test.Test
import kotlin.test.assertTrue
import at.favre.lib.crypto.bcrypt.BCrypt

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
}
