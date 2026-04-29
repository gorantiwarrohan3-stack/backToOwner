package com.wpi.backtoowner.domain.repository

import com.wpi.backtoowner.domain.model.NewPost
import com.wpi.backtoowner.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {

    suspend fun getPosts(): Result<List<Post>>

    suspend fun getPost(documentId: String): Result<Post>

    fun observePosts(): Flow<Result<List<Post>>>

    suspend fun createPost(newPost: NewPost): Result<String>

    suspend fun deletePost(documentId: String): Result<Unit>

    /**
     * All rows in [AppwriteConfig.COLLECTION_POSTS_ARCHIVE] (immutable history; survives deletes on `posts`).
     */
    suspend fun getArchivedPosts(): Result<List<Post>>

    /** Persists Gemini “anchor vs candidate” score so all devices can read match badges from the backend. */
    suspend fun recordFeedMatchResult(
        anchorPostId: String,
        candidatePostId: String,
        similarity: Int,
    ): Result<Unit>

    /**
     * Map of **[candidatePostId] → similarity** for the most recently written cross-match **batch**
     * (same [anchorPostId]). Empty if unavailable or collection not configured.
     */
    suspend fun loadLatestCrossMatchScores(): Result<Map<String, Int>>

    fun observeCrossMatchScores(): Flow<Map<String, Int>>
}
