package com.wpi.backtoowner.domain.repository

import android.graphics.Bitmap

data class MatchCandidate(
    val id: String,
    val description: String,
    val image: Bitmap?
)

data class MatchResult(
    val id: String,
    val confidenceScore: Int, // 0-100
    val reason: String
)

interface AiMatchingRepository {
    suspend fun matchItem(description: String, image: Bitmap): String?
    
    suspend fun findBestMatches(
        lostDescription: String,
        candidates: List<MatchCandidate>
    ): List<MatchResult>
}
