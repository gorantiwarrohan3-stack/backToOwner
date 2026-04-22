package com.wpi.backtoowner.data.repository

import com.wpi.backtoowner.domain.model.AuthUserSummary
import com.wpi.backtoowner.domain.repository.AuthRepository
import io.appwrite.ID
import io.appwrite.services.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppwriteAuthRepository @Inject constructor(
    private val account: Account,
) : AuthRepository {

    override suspend fun hasActiveSession(): Boolean = withContext(Dispatchers.IO) {
        runCatching { account.get(); true }.getOrDefault(false)
    }

    override suspend fun login(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireWpiEmail(email)
            account.createEmailPasswordSession(email, password)
            Unit
        }
    }

    override suspend fun signup(name: String, email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireWpiEmail(email)
                require(name.isNotBlank()) { "Enter your full name." }
                require(password.length >= 8) { "Password must be at least 8 characters." }
                account.create(
                    userId = ID.unique(),
                    email = email.trim().lowercase(),
                    password = password,
                    name = name.trim(),
                )
                account.createEmailPasswordSession(email.trim().lowercase(), password)
                Unit
            }
        }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            account.deleteSession("current")
            Unit
        }
    }

    override suspend fun getCurrentUser(): Result<AuthUserSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val user = account.get()
            AuthUserSummary(
                id = user.id,
                name = user.name.ifBlank { user.email.substringBefore("@") },
                email = user.email,
            )
        }
    }

    private fun requireWpiEmail(email: String) {
        val trimmed = email.trim().lowercase()
        require(WPI_EMAIL.matches(trimmed)) {
            "Use your WPI email address (must end with @wpi.edu)."
        }
    }

    companion object {
        private val WPI_EMAIL = Regex("""^[a-zA-Z0-9._%+-]+@wpi\.edu$""")
    }
}
