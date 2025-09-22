package org.interns.project.users.repo

import org.interns.project.users.model.User
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class InMemoryUserRepo : UserRepo {
    private val map = ConcurrentHashMap<Long, User>()
    private val idGen = AtomicLong(1)

    override fun nextId(): Long = idGen.getAndIncrement()

    override fun save(user: User): User {
        map[user.id] = user
        return user
    }

    override fun findByEmail(email: String): User? =
        map.values.firstOrNull { it.email.equals(email, ignoreCase = true) }

    override fun findByLogin(login: String): User? =
        map.values.firstOrNull { it.login == login }

    override fun clear() {
        map.clear()
        idGen.set(1)
    }
}
