package com.wpi.backtoowner.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.wpi.backtoowner.BuildConfig
import com.wpi.backtoowner.domain.model.AiMatchCandidate
import com.wpi.backtoowner.domain.model.Post
import com.wpi.backtoowner.domain.model.PostType
import com.wpi.backtoowner.domain.repository.AiMatchingRepository
import com.wpi.backtoowner.domain.repository.PostRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Builds “Suggested matches” on the item detail screen using [AiMatchingRepository] (Gemini),
 * same pairing model as [CrossMatchAgainstFeedUseCase] but without persisting to the feed store.
 */
class SuggestedGeminiMatchesUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val aiMatchingRepository: AiMatchingRepository,
    private val imageLoader: ImageLoader,
    @ApplicationContext private val context: Context,
) {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default

    suspend operator fun invoke(anchor: Post): List<AiMatchCandidate> {
        val key = BuildConfig.GEMINI_API_KEY.trim()
        if (key.isBlank()) return emptyList()

        val all = postRepository.getPosts().getOrNull() ?: return emptyList()
        val candidates = all
            .asSequence()
            .filter { it.id != anchor.id }
            .sortedByDescending { it.createdAtEpochMs }
            .take(MAX_CANDIDATES)
            .toList()
        if (candidates.isEmpty()) return emptyList()

        val anchorBitmap = anchor.imageUrl.takeIf { it.isNotBlank() }?.let { fetchBitmap(it) }

        return withContext(ioDispatcher) {
            val scored = mutableListOf<Pair<Post, Int>>()
            candidates.forEachIndexed { idx, cand ->
                val candidateBitmap =
                    cand.imageUrl.takeIf { it.isNotBlank() }?.let { fetchBitmap(it) }
                val score = aiMatchingRepository.scoreAgainstFeedListing(
                    anchorTitle = anchor.title,
                    anchorDescription = anchor.description,
                    anchorImage = anchorBitmap,
                    candidateTitle = cand.title,
                    candidateDescription = cand.description,
                    candidateImage = candidateBitmap,
                )
                if (score != null && score >= MIN_SHOW_PERCENT) {
                    scored += cand to score
                }
                if (idx < candidates.lastIndex) delay(INTER_CALL_DELAY_MS)
            }
            scored
                .sortedByDescending { it.second }
                .take(MAX_SUGGESTIONS)
                .map { (cand, pct) ->
                    AiMatchCandidate(
                        candidatePostId = cand.id,
                        label = "${typeLabel(cand.type)}: ${cand.title}",
                        matchPercent = pct,
                        imageUrl = cand.imageUrl,
                    )
                }
        }
    }

    private suspend fun fetchBitmap(url: String): Bitmap? =
        withContext(ioDispatcher) {
            val req = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()
            when (val r = imageLoader.execute(req)) {
                is SuccessResult -> (r.drawable as? BitmapDrawable)?.bitmap
                else -> null
            }
        }

    private companion object {
        const val MIN_SHOW_PERCENT = 30
        const val MAX_CANDIDATES = 40
        const val MAX_SUGGESTIONS = 6
        const val INTER_CALL_DELAY_MS = 120L
    }
}

private fun typeLabel(type: PostType): String = when (type) {
    PostType.LOST -> "Lost"
    PostType.FOUND -> "Found"
}
