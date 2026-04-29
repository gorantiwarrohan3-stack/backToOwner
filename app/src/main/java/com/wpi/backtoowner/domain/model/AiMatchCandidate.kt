package com.wpi.backtoowner.domain.model

data class AiMatchCandidate(
    /** Other post document id — unique keys for Lazy list rows */
    val candidatePostId: String,
    val label: String,
    val matchPercent: Int,
    val imageUrl: String,
)
