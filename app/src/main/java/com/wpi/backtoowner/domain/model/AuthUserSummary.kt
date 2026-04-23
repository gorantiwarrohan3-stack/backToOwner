package com.wpi.backtoowner.domain.model

data class AuthUserSummary(
    val id: String,
    val name: String,
    val email: String,
    /** E.164 or empty from Appwrite account. */
    val phone: String = "",
)
