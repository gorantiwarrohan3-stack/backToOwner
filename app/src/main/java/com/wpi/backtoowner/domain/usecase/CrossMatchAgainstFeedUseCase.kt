package com.wpi.backtoowner.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.wpi.backtoowner.BuildConfig
import com.wpi.backtoowner.data.local.FeedMatchScoresStore
import com.wpi.backtoowner.domain.repository.AiMatchingRepository
import com.wpi.backtoowner.domain.repository.PostRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * After a successful post, compares that report (“anchor”) to every other item in [PostRepository]
 * and persists scores via [FeedMatchScoresStore] (in-memory sync) plus [PostRepository.recordFeedMatchResult].
 */
class CrossMatchAgainstFeedUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val aiMatchingRepository: AiMatchingRepository,
    private val feedMatchScoresStore: FeedMatchScoresStore,
    private val imageLoader: ImageLoader,
    @ApplicationContext private val context: Context,
) {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default

    /**
     * Runs in the background — call from a VM scope without blocking navigation.
     */
    suspend operator fun invoke(
        newPostDocumentId: String,
        anchorTitle: String,
        anchorDescription: String,
        anchorImage: Bitmap?,
    ) {
        val key = BuildConfig.GEMINI_API_KEY.trim()
        if (key.isBlank()) return

        val all = postRepository.getPosts().getOrNull() ?: return
        val candidates = all.filterNot { it.id == newPostDocumentId }
        feedMatchScoresStore.beginNewAnchoringBatch()
        if (candidates.isEmpty()) return

        withContext(ioDispatcher) {
            candidates.forEachIndexed { idx, cand ->
                val candidateBitmap =
                    cand.imageUrl.takeIf { it.isNotBlank() }?.let { fetchBitmap(it) }
                val score = aiMatchingRepository.scoreAgainstFeedListing(
                    anchorTitle = anchorTitle,
                    anchorDescription = anchorDescription,
                    anchorImage = anchorImage,
                    candidateTitle = cand.title,
                    candidateDescription = cand.description,
                    candidateImage = candidateBitmap,
                )
                if (score != null) {
                    feedMatchScoresStore.putScore(cand.id, score)
                    postRepository.recordFeedMatchResult(newPostDocumentId, cand.id, score)
                }
                if (idx < candidates.lastIndex) delay(INTER_CALL_DELAY_MS)
            }
        }
    }

    private companion object {
        const val INTER_CALL_DELAY_MS = 120L
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
}
