package com.wpi.backtoowner.data.local

data class ChatThreadSummary(
    val itemId: String,
    val postTitle: String,
    val posterDisplayName: String?,
    val listingType: String,
    val lastTouchedEpochMs: Long,
)
