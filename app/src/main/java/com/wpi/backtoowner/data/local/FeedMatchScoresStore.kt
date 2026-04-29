package com.wpi.backtoowner.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory Gemini scores for the latest post-create cross-match (anchor vs candidates).
 * Keys are candidate [Post.id] values; values are 0–100. The home feed no longer displays these.
 */
@Singleton
class FeedMatchScoresStore @Inject constructor() {

    private val _scores = MutableStateFlow<Map<String, Int>>(emptyMap())
    val scores: StateFlow<Map<String, Int>> = _scores.asStateFlow()

    fun beginNewAnchoringBatch() {
        _scores.value = emptyMap()
    }

    fun putScore(candidatePostId: String, percent: Int) {
        _scores.update { it + (candidatePostId to percent.coerceIn(0, 100)) }
    }
}
