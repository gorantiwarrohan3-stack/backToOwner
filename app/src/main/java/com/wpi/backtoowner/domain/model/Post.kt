package com.wpi.backtoowner.domain.model

data class Post(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val latitude: Double,
    val longitude: Double,
    val type: PostType,
    /** 0–100 for feed match badge; null shows "No Match". */
    val matchPercent: Int? = null,
    val createdAtEpochMs: Long = 0L,
    /** Appwrite account id of whoever created the post; null for older documents. */
    val posterUserId: String? = null,
    /** Display name from account at post time; null for older documents. */
    val posterDisplayName: String? = null,
    /** When true, treat as closed / returned (optional Appwrite `resolved` attribute). */
    val resolved: Boolean = false,
)
