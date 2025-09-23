package org.interns.project.users.repo

import org.interns.project.users.model.User

interface UserRepo {
    fun nextId(): Long
    fun save(user: User): User
    fun findByEmail(email: String): User?
    fun findByLogin(login: String): User?
    fun clear()
}
