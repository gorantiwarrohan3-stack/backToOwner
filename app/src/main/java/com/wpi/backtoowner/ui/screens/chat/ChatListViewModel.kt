package com.wpi.backtoowner.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpi.backtoowner.config.AppwriteConfig
import com.wpi.backtoowner.data.local.ChatThreadStore
import com.wpi.backtoowner.data.local.ChatThreadSummary
import com.wpi.backtoowner.domain.repository.PostRepository
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
) : ViewModel() {

    val threads: StateFlow<List<ChatThreadSummary>> = chatThreadStore.threads
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * Rebuilds the Chats tab from the Appwrite `messages` collection (distinct `itemId`).
     * Local SharedPreferences cache alone can show stale threads after you delete rows in Appwrite.
     */
    fun refreshThreadsFromMessages() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
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
            _isRefreshing.value = false
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
