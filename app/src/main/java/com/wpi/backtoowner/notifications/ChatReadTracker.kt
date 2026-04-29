package com.wpi.backtoowner.notifications

import android.content.Context
import com.wpi.backtoowner.config.AppwriteConfig
import com.wpi.backtoowner.data.local.ChatThreadStore
import com.wpi.backtoowner.domain.repository.PostRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.appwrite.Query
import io.appwrite.services.Account
import io.appwrite.services.Databases
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PREFS = "backtoowner_chat_read_cursors"

/**
 * Tracks per-thread read cursors and exposes a total unread count for the Chats tab badge.
 * Unread = threads where the latest message is from someone else and newer than the read cursor.
 */
@Singleton
class ChatReadTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val databases: Databases,
    private val account: Account,
    private val postRepository: PostRepository,
    private val chatThreadStore: ChatThreadStore,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _unreadTotal = MutableStateFlow(0)
    val unreadTotal: StateFlow<Int> = _unreadTotal.asStateFlow()

    /** Current stored read cursor for [itemId], or [Long.MIN_VALUE] if never opened. */
    fun peekReadEpochMs(itemId: String): Long {
        if (itemId.isBlank()) return Long.MIN_VALUE
        return prefs.getLong(key(itemId), Long.MIN_VALUE)
    }

    fun clearThreadRead(itemId: String) {
        if (itemId.isBlank()) return
        prefs.edit().remove(key(itemId)).apply()
        requestRefresh()
    }

    fun markThreadRead(itemId: String, lastSeenMessageEpochMs: Long) {
        if (itemId.isBlank() || lastSeenMessageEpochMs <= 0L) return
        val k = key(itemId)
        val prev = prefs.getLong(k, Long.MIN_VALUE)
        val next = if (prev == Long.MIN_VALUE) {
            lastSeenMessageEpochMs
        } else {
            maxOf(prev, lastSeenMessageEpochMs)
        }
        prefs.edit().putLong(k, next).apply()
        requestRefresh()
    }

    fun requestRefresh() {
        scope.launch {
            refreshUnreadCount()
        }
    }

    suspend fun refreshUnreadCount() = withContext(Dispatchers.IO) {
        val userId = runCatching { account.get().id }.getOrNull() ?: return@withContext
        val tracked = trackedItemIdsFor(userId)
        if (tracked.isEmpty()) {
            _unreadTotal.value = 0
            return@withContext
        }
        var total = 0
        for (itemId in tracked) {
            val latest = latestMessage(itemId) ?: continue
            val read = prefs.getLong(key(itemId), Long.MIN_VALUE)
            if (read == Long.MIN_VALUE) {
                // Never opened this thread: only treat as "caught up" if I sent the latest message.
                if (latest.senderUserId == userId) {
                    prefs.edit().putLong(key(itemId), latest.createdAtMs).apply()
                } else {
                    total++
                }
                continue
            }
            if (latest.senderUserId != userId && latest.createdAtMs > read) {
                total++
            }
        }
        _unreadTotal.value = total
    }

    private suspend fun trackedItemIdsFor(userId: String): Set<String> {
        val fromThreads = chatThreadStore.threads.first().map { it.itemId }.filter { it.isNotBlank() }.toSet()
        val ownPosts = postRepository.getPosts().getOrElse { emptyList() }
            .filter { it.posterUserId == userId || userId in it.permissionUserIds }
            .map { it.id }
            .toSet()
        return fromThreads + ownPosts
    }

    private suspend fun latestMessage(itemId: String): LatestChat? {
        val docs = runCatching {
            databases.listDocuments(
                databaseId = AppwriteConfig.DATABASE_ID,
                collectionId = AppwriteConfig.COLLECTION_MESSAGES,
                queries = listOf(
                    Query.equal("itemId", itemId),
                    Query.orderDesc("\$createdAt"),
                    Query.limit(1),
                ),
            ).documents
        }.getOrNull() ?: return null
        val doc = docs.firstOrNull() ?: return null
        @Suppress("UNCHECKED_CAST")
        val data = doc.data as? Map<String, Any?> ?: return null
        val senderUserId = data["senderUserId"]?.toString()?.trim().orEmpty()
        val createdAtMs = parseIsoMs(doc.createdAt)
        return LatestChat(senderUserId, createdAtMs)
    }

    private fun key(itemId: String) = "read_$itemId"

    private data class LatestChat(val senderUserId: String, val createdAtMs: Long)
}

internal fun parseAppwriteCreatedAtToMs(iso: String?): Long = parseIsoMs(iso)

private fun parseIsoMs(iso: String?): Long {
    if (iso.isNullOrBlank()) return 0L
    return try {
        Instant.parse(iso).toEpochMilli()
    } catch (_: DateTimeParseException) {
        0L
    }
}
