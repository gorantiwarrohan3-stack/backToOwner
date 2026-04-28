package com.wpi.backtoowner.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.wpi.backtoowner.domain.model.Post
import com.wpi.backtoowner.domain.model.PostType
import com.wpi.backtoowner.domain.repository.AiMatchingRepository
import com.wpi.backtoowner.domain.repository.MatchCandidate
import com.wpi.backtoowner.domain.repository.MatchResult
import com.wpi.backtoowner.domain.repository.PostRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FindMatchesUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val aiMatchingRepository: AiMatchingRepository,
    private val imageLoader: ImageLoader,
    @ApplicationContext private val context: Context
) {
    /**
     * Finds items of the OPPOSITE type that match the given [anchorPost].
     * (e.g. if anchor is LOST, it finds matching FOUND items).
     */
    suspend operator fun invoke(anchorPost: Post): List<MatchResult> {
        val oppositeType = if (anchorPost.type == PostType.LOST) PostType.FOUND else PostType.LOST
        
        // 1. Get items of the opposite type
        val candidatePosts = postRepository.getPosts().getOrElse { emptyList() }
            .filter { it.type == oppositeType && it.id != anchorPost.id }
            .take(15)

        if (candidatePosts.isEmpty()) return emptyList()

        // 2. Prepare candidates (fetch Bitmaps)
        val candidates = candidatePosts.map { post ->
            MatchCandidate(
                id = post.id,
                description = post.description,
                image = fetchBitmap(post.imageUrl)
            )
        }

        // 3. Match using AI
        return aiMatchingRepository.findBestMatches(anchorPost.description, candidates)
    }

    private suspend fun fetchBitmap(url: String): Bitmap? {
        if (url.isBlank()) return null
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()
        
        return when (val result = imageLoader.execute(request)) {
            is SuccessResult -> (result.drawable as? BitmapDrawable)?.bitmap
            else -> null
        }
    }
}
