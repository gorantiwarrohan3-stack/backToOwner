package com.wpi.backtoowner.domain.repository

import com.wpi.backtoowner.domain.model.NewPost
import com.wpi.backtoowner.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {

    suspend fun getPosts(): Result<List<Post>>

    suspend fun getPost(documentId: String): Result<Post>

    fun observePosts(): Flow<Result<List<Post>>>

    suspend fun createPost(newPost: NewPost): Result<String>
}
