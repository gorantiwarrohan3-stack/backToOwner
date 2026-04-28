package com.wpi.backtoowner.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpi.backtoowner.domain.model.AiMatchCandidate
import com.wpi.backtoowner.domain.model.Post
import com.wpi.backtoowner.domain.model.PostType
import com.wpi.backtoowner.data.local.ChatThreadStore
import com.wpi.backtoowner.domain.repository.PostRepository
import com.wpi.backtoowner.domain.usecase.FindMatchesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val STOPWORDS = setOf(
    "the", "and", "for", "was", "with", "this", "that", "from", "have", "has", "are",
    "lost", "found", "item", "wpi", "campus", "near", "region", "area", "just",
    "into", "onto", "room", "building", "center", "centre", "desk", "table",
)

private fun tokenize(text: String): Set<String> =
    Regex("[a-zA-Z0-9]+").findAll(text.lowercase())
        .map { it.value }
        .filter { it.length > 2 && it !in STOPWORDS }
        .toSet()

/** Text overlap 0–99; boosts title hits and opposite lost/found pairs. */
private fun matchScore(anchor: Post, candidate: Post): Int {
    val a = tokenize(anchor.title + " " + anchor.description)
    val b = tokenize(candidate.title + " " + candidate.description)
    if (a.isEmpty() || b.isEmpty()) return 0
    val inter = a.intersect(b).size
    val unionSize = a.union(b).size
    if (unionSize == 0) return 0
    var score = ((100.0 * inter) / unionSize).toInt()
    val titleHit = tokenize(anchor.title).intersect(tokenize(candidate.title)).isNotEmpty()
    if (titleHit) score = (score + 18).coerceAtMost(96)
    if (anchor.type != candidate.type) score = (score + 8).coerceAtMost(98)
    return score.coerceIn(0, 99)
}

private fun typeLabel(type: PostType): String = when (type) {
    PostType.LOST -> "Lost"
    PostType.FOUND -> "Found"
}

private fun buildSuggestedMatches(anchor: Post, others: List<Post>): List<AiMatchCandidate> =
    others
        .asSequence()
        .filter { it.id != anchor.id }
        .map { other -> other to matchScore(anchor, other) }
        .filter { it.second >= 34 }
        .sortedByDescending { it.second }
        .take(6)
        .map { (other, pct) ->
            AiMatchCandidate(
                id = other.id,
                label = "${typeLabel(other.type)}: ${other.title}",
                matchPercent = pct,
                imageUrl = other.imageUrl,
            )
        }
        .toList()

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val chatThreadStore: ChatThreadStore,
    private val findMatchesUseCase: FindMatchesUseCase
) : ViewModel() {

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()

    private val _matches = MutableStateFlow<List<AiMatchCandidate>>(emptyList())
    val matches: StateFlow<List<AiMatchCandidate>> = _matches.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load(itemId: String) {
        viewModelScope.launch {
            _error.value = null
            postRepository.getPost(itemId).fold(
                onSuccess = { p ->
                    _post.value = p
                    
                    // Use AI matching for both Lost and Found posts
                    val aiMatches = findMatchesUseCase(p)
                    val allPosts = postRepository.getPosts().getOrElse { emptyList() }
                    val suggestedMatches = aiMatches.mapNotNull { result ->
                        allPosts.find { it.id == result.id }?.let { other ->
                            AiMatchCandidate(
                                id = other.id,
                                label = if (other.type == PostType.LOST) "Lost: ${other.title}" else "Found: ${other.title}",
                                matchPercent = result.confidenceScore,
                                imageUrl = other.imageUrl,
                            )
                        }
                    }.sortedByDescending { it.matchPercent }

                    // Combine with text matching if AI results are few (optional fallback)
                    if (suggestedMatches.size < 3) {
                        val textMatches = buildSuggestedMatches(p, allPosts)
                        _matches.value = (suggestedMatches + textMatches).distinctBy { it.id }.take(10)
                    } else {
                        _matches.value = suggestedMatches
                    }
                },
                onFailure = { e ->
                    _post.value = null
                    _error.value = e.message ?: "Could not load item."
                },
            )
        }
    }

    fun registerChatThreadForInbox() {
        val p = _post.value ?: return
        chatThreadStore.touch(
            itemId = p.id,
            postTitle = p.title,
            posterDisplayName = p.posterDisplayName,
            listingType = p.type,
        )
    }
}
