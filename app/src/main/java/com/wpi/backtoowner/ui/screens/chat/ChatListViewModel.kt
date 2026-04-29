package com.wpi.backtoowner.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpi.backtoowner.config.AppwriteConfig
import com.wpi.backtoowner.data.local.ChatThreadStore
import com.wpi.backtoowner.data.local.ChatThreadSummary
import com.wpi.backtoowner.domain.repository.ChatMessagingRepository
import com.wpi.backtoowner.domain.repository.PostRepository
import com.wpi.backtoowner.notifications.ChatMessageNotificationManager
import com.wpi.backtoowner.notifications.ChatReadTracker
import com.wpi.backtoowner.notifications.InAppNotificationStore
import dagger.hilt.android.lifecycle.HiltViewModel
import io.appwrite.Query
import io.appwrite.models.Document
import io.appwrite.services.Databases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatThreadStore: ChatThreadStore,
    private val databases: Databases,
    private val postRepository: PostRepository,
    private val chatReadTracker: ChatReadTracker,
    private val chatMessagingRepository: ChatMessagingRepository,
    private val chatMessageNotificationManager: ChatMessageNotificationManager,
    private val inAppNotificationStore: InAppNotificationStore,
) : ViewModel() {

    val threads: StateFlow<List<ChatThreadSummary>> = chatThreadStore.threads
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    fun clearDeleteError() {
        _deleteError.value = null
    }

    fun deleteConversation(itemId: String) {
        if (itemId.isBlank()) return
        if (_isDeleting.value) return
        viewModelScope.launch {
            _isDeleting.value = true
            _deleteError.value = null
            try {
                val remote = withContext(Dispatchers.IO) {
                    chatMessagingRepository.deleteAllMessagesForItem(itemId)
                }
                remote.onFailure { e ->
                    _deleteError.value =
                        e.message ?: "Some messages may still exist on the server (permissions)."
                }
                inAppNotificationStore.removeAllChatNotificationsForItem(itemId)
                chatMessageNotificationManager.clearThreadNotificationState(itemId)
                chatReadTracker.clearThreadRead(itemId)
                chatThreadStore.hideThreadPermanently(itemId)
                chatReadTracker.requestRefresh()
            } finally {
                _isDeleting.value = false
            }
        }
    }

    /**
     * Rebuilds the Chats tab from the Appwrite `messages` collection (distinct `itemId`).
     * Local SharedPreferences cache alone can show stale threads after you delete rows in Appwrite.
     */
    fun refreshThreadsFromMessages() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                withContext(Dispatchers.IO) {
                    runCatching {
                        val response = databases.listDocuments(
                            databaseId = AppwriteConfig.DATABASE_ID,
                            collectionId = AppwriteConfig.COLLECTION_MESSAGES,
                            queries = listOf(
                                Query.orderDesc("\$createdAt"),
                                Query.limit(500),
                            ),
                        )
                        val latestDocByItemId = LinkedHashMap<String, Document<*>>()
                        for (doc in response.documents) {
                            val itemId = doc.itemIdFromMessage() ?: continue
                            if (!latestDocByItemId.containsKey(itemId)) {
                                latestDocByItemId[itemId] = doc
                            }
                        }
                        val summaries = latestDocByItemId.mapNotNull { (itemId, doc) ->
                            val post = postRepository.getPost(itemId).getOrNull() ?: return@mapNotNull null
                            val lastMs = parseCreatedAtIso(doc.createdAt)
                            ChatThreadSummary(
                                itemId = itemId,
                                postTitle = post.title.ifBlank { "Listing" },
                                posterDisplayName = post.posterDisplayName?.takeIf { it.isNotBlank() },
                                listingType = post.type.name,
                                lastTouchedEpochMs = lastMs,
                            )
                        }
                        chatThreadStore.replaceAll(summaries)
                    }
                }
            } finally {
                _isRefreshing.value = false
                chatReadTracker.requestRefresh()
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun Document<*>.itemIdFromMessage(): String? {
    val map = data as? Map<String, Any?> ?: return null
    return map["itemId"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
}

private fun parseCreatedAtIso(iso: String?): Long {
    if (iso.isNullOrBlank()) return 0L
    return try {
        Instant.parse(iso).toEpochMilli()
    } catch (_: DateTimeParseException) {
        0L
    }
}
