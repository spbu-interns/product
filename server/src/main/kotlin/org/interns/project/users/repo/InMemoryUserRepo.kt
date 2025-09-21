package org.interns.project.users.repo

import org.interns.project.users.model.User
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class InMemoryUserRepo {
    private val map = ConcurrentHashMap<Long, User>()
    private val idGen = AtomicLong(1)

    fun nextId(): Long = idGen.getAndIncrement()

    fun save(user: User): User {
        map[user.id] = user
        return user
    }

    fun findByEmail(email: String): User? =
        map.values.firstOrNull { it.email.equals(email, ignoreCase = true) }

    fun findByLogin(login: String): User? =
        map.values.firstOrNull { it.login == login }

    fun clear() {
        map.clear()
        idGen.set(1)
    }
}
