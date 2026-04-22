package com.wpi.backtoowner.domain.model

data class AiMatchCandidate(
    val label: String,
    val matchPercent: Int,
    val imageUrl: String,
)
