package com.wpi.backtoowner.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpi.backtoowner.config.AppwriteConfig
import com.wpi.backtoowner.data.local.ChatThreadStore
import com.wpi.backtoowner.domain.model.Post
import com.wpi.backtoowner.domain.model.PostType
import com.wpi.backtoowner.domain.repository.AuthRepository
import com.wpi.backtoowner.domain.repository.PostRepository
import com.wpi.backtoowner.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import io.appwrite.ID
import io.appwrite.Permission
import io.appwrite.Query
import io.appwrite.Role
import io.appwrite.models.Document
import io.appwrite.services.Databases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

data class ChatBubbleUi(
    val id: String,
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
    private val databases: Databases,
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

    private val _sendError = MutableStateFlow<String?>(null)
    val sendError: StateFlow<String?> = _sendError.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            if (itemId.isBlank()) {
                _loadError.value = "Missing item id."
                return@launch
            }
            val user = authRepository.getCurrentUser().getOrNull()
            currentUserId = user?.id
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
                    loadMessages()
                },
                onFailure = { e ->
                    _loadError.value = e.message ?: "Could not load listing."
                },
            )
        }
    }

    private fun selfRole(post: Post, viewerId: String?): String {
        val viewerIsPoster = viewerId != null && viewerId == post.posterUserId
        return if (viewerIsPoster) {
            if (post.type == PostType.LOST) "Owner" else "Founder"
        } else {
            if (post.type == PostType.LOST) "Founder" else "Owner"
        }
    }

    private suspend fun loadMessages() {
        val uid = currentUserId
        val post = _post.value ?: return
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val response = databases.listDocuments(
                    databaseId = AppwriteConfig.DATABASE_ID,
                    collectionId = AppwriteConfig.COLLECTION_MESSAGES,
                    queries = listOf(
                        Query.equal("itemId", itemId),
                        Query.orderAsc("\$createdAt"),
                        Query.limit(200),
                    ),
                )
                response.documents.mapNotNull { it.toChatBubble(uid) }
            }
        }
        result.fold(
            onSuccess = { _messages.value = it },
            onFailure = { e ->
                _loadError.value = e.message ?: "Could not load messages."
            },
        )
        // Keep local thread list warm so Chats tab has entries.
        chatThreadStore.touch(
            itemId = post.id,
            postTitle = post.title,
            posterDisplayName = post.posterDisplayName,
            listingType = post.type,
        )
    }

    fun sendMessage(body: String) {
        val text = body.trim()
        if (text.isBlank()) return
        if (_isSending.value) return
        val uid = currentUserId ?: run {
            _sendError.value = "You must be signed in to send messages."
            return
        }
        val name = _currentUserLabel.value ?: "WPI user"
        val post = _post.value ?: run {
            _sendError.value = "Could not resolve listing for this chat."
            return
        }
        val role = selfRole(post, uid)
        viewModelScope.launch {
            _isSending.value = true
            _sendError.value = null
            val created = withContext(Dispatchers.IO) {
                runCatching {
                    databases.createDocument(
                        databaseId = AppwriteConfig.DATABASE_ID,
                        collectionId = AppwriteConfig.COLLECTION_MESSAGES,
                        documentId = ID.unique(),
                        data = mapOf(
                            "itemId" to itemId,
                            "senderUserId" to uid,
                            "senderName" to name,
                            "senderRole" to role,
                            "body" to text,
                        ),
                        permissions = listOf(
                            Permission.read(Role.any()),
                            Permission.update(Role.user(uid)),
                            Permission.delete(Role.user(uid)),
                        ),
                    )
                }
            }
            _isSending.value = false
            created.fold(
                onSuccess = { loadMessages() },
                onFailure = { e ->
                    _sendError.value = e.message ?: "Could not send message."
                },
            )
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun Document<*>.toChatBubble(currentUserId: String?): ChatBubbleUi? {
    val map = data as? Map<String, Any?> ?: return null
    fun str(key: String) = map[key]?.toString()?.takeIf { it.isNotBlank() }
    val senderId = str("senderUserId")
    val senderName = str("senderName") ?: return null
    val role = str("senderRole") ?: "Member"
    val body = str("body") ?: return null
    val self = currentUserId != null && senderId == currentUserId
    return ChatBubbleUi(
        id = id,
        displayName = senderName,
        roleLabel = role,
        body = body,
        isSelf = self,
    )
}
