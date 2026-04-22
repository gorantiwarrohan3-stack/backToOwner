package com.wpi.backtoowner.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpi.backtoowner.data.local.ChatThreadStore
import com.wpi.backtoowner.domain.model.Post
import com.wpi.backtoowner.domain.repository.AuthRepository
import com.wpi.backtoowner.domain.repository.PostRepository
import com.wpi.backtoowner.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatBubbleUi(
    val displayName: String,
    val roleLabel: String,
    val body: String,
    val isSelf: Boolean,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val chatThreadStore: ChatThreadStore,
) : ViewModel() {

    private val itemId: String = savedStateHandle.get<String>(Screen.Chat.ARG_ITEM_ID).orEmpty()

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _currentUserLabel = MutableStateFlow<String?>(null)
    val currentUserLabel: StateFlow<String?> = _currentUserLabel.asStateFlow()

    private val _otherPartyLabel = MutableStateFlow<String?>(null)
    val otherPartyLabel: StateFlow<String?> = _otherPartyLabel.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatBubbleUi>>(emptyList())
    val messages: StateFlow<List<ChatBubbleUi>> = _messages.asStateFlow()

    init {
        viewModelScope.launch {
            if (itemId.isBlank()) {
                _loadError.value = "Missing item id."
                return@launch
            }
            val user = authRepository.getCurrentUser().getOrNull()
            _currentUserLabel.value = user?.name

            postRepository.getPost(itemId).fold(
                onSuccess = { p ->
                    _post.value = p
                    chatThreadStore.touch(
                        itemId = p.id,
                        postTitle = p.title,
                        posterDisplayName = p.posterDisplayName,
                        listingType = p.type,
                    )
                    val poster = p.posterDisplayName?.takeIf { it.isNotBlank() }
                    val viewerId = user?.id
                    val isViewerPoster = viewerId != null && viewerId == p.posterUserId
                    _otherPartyLabel.value = when {
                        poster == null && p.posterUserId == null ->
                            "Listing poster (older posts may not show a name)"
                        isViewerPoster ->
                            "People messaging you about your listing"
                        else -> poster ?: "Listing poster"
                    }
                },
                onFailure = { e ->
                    _loadError.value = e.message ?: "Could not load listing."
                },
            )
        }
    }

    fun sendMessage(body: String) {
        val text = body.trim()
        if (text.isBlank()) return
        val label = _currentUserLabel.value ?: "You"
        _messages.value = _messages.value + ChatBubbleUi(
            displayName = label,
            roleLabel = "You",
            body = text,
            isSelf = true,
        )
    }
}
