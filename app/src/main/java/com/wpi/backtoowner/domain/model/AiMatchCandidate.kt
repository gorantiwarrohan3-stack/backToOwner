package com.wpi.backtoowner.domain.model

data class AiMatchCandidate(
    val id: String = "",
    val label: String,
    val matchPercent: Int,
    val imageUrl: String,
)
