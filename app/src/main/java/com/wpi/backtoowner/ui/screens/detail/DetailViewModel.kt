package com.wpi.backtoowner.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpi.backtoowner.domain.model.Post
import com.wpi.backtoowner.data.local.ChatThreadStore
import com.wpi.backtoowner.domain.model.AiMatchCandidate
import com.wpi.backtoowner.domain.repository.PostRepository
import com.wpi.backtoowner.domain.usecase.SuggestedGeminiMatchesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val chatThreadStore: ChatThreadStore,
    private val suggestedGeminiMatchesUseCase: SuggestedGeminiMatchesUseCase,
) : ViewModel() {

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()

    private val _matches = MutableStateFlow<List<AiMatchCandidate>>(emptyList())
    val matches: StateFlow<List<AiMatchCandidate>> = _matches.asStateFlow()

    /** True while Gemini suggested-match scoring runs after the item has loaded. */
    private val _suggestedMatchesLoading = MutableStateFlow(false)
    val suggestedMatchesLoading: StateFlow<Boolean> = _suggestedMatchesLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var suggestedMatchesJob: Job? = null

    fun load(itemId: String) {
        suggestedMatchesJob?.cancel()
        suggestedMatchesJob = viewModelScope.launch {
            _error.value = null
            _matches.value = emptyList()
            _suggestedMatchesLoading.value = false
            postRepository.getPost(itemId).fold(
                onSuccess = { p ->
                    _post.value = p
                    _suggestedMatchesLoading.value = true
                    try {
                        _matches.value = runCatching { suggestedGeminiMatchesUseCase(p) }
                            .getOrElse { emptyList() }
                    } finally {
                        _suggestedMatchesLoading.value = false
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
