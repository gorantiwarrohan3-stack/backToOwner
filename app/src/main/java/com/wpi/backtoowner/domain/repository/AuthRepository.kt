package com.wpi.backtoowner.domain.repository

import com.wpi.backtoowner.domain.model.AuthUserSummary

interface AuthRepository {

    /** `true` if a user session exists. */
    suspend fun hasActiveSession(): Boolean

    suspend fun login(email: String, password: String): Result<Unit>

    suspend fun signup(name: String, email: String, password: String): Result<Unit>

    suspend fun logout(): Result<Unit>

    /** Current account from Appwrite (requires active session). */
    suspend fun getCurrentUser(): Result<AuthUserSummary>
}
