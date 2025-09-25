package org.interns.project.users

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.interns.project.users.utils.Validation

class ValidationTest {
    @Test
    fun emailRegex_acceptsValidEmails() {
        val valids = listOf(
            "a@b.com",
            "user.name+tag+sorting@example.com",
            "user_name@example.co.uk",
            "USER123@example.org",
            "x@y.z"
        )
        for (e in valids) {
            assertTrue(Validation.isValidEmail(e), "Should accept $e")
        }
    }

    @Test
    fun emailRegex_rejectsInvalidEmails() {
        val invalids = listOf(
            "plainaddress",
            "@no-local-part.com",
            "Outlook Contact <outlook-contact@domain.com>",
            "no-at.domain.com",
            "no-tld@domain",
            "user@.com",
            "user@domain..com"
        )
        for (e in invalids) {
            assertFalse(Validation.isValidEmail(e), "Should reject $e")
        }
    }
}
