package com.wpi.backtoowner.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.wpi.backtoowner.MainActivity
import com.wpi.backtoowner.R
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

private const val CHAT_CHANNEL_ID = "chat_messages"
private const val PREFS_CHAT_NOTIF = "chat_notifications"
private const val POLL_MS = 30_000L

@Singleton
class ChatMessageNotificationManager @Inject constructor(
    private val databases: Databases,
    private val account: Account,
    private val postRepository: PostRepository,
    private val chatThreadStore: ChatThreadStore,
    @ApplicationContext
    private val appContext: Context,
) {
    private var job: Job? = null
    private val prefs by lazy { appContext.getSharedPreferences(PREFS_CHAT_NOTIF, Context.MODE_PRIVATE) }

    fun start() {
        if (job?.isActive == true) return
        ensureNotificationChannel()
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                runCatching { pollAndNotify() }
                delay(POLL_MS)
            }
        }
    }

    private suspend fun pollAndNotify() = supervisorScope {
        val userId = account.get().id
        val trackedItemIds = trackedItemIdsFor(userId)
        if (trackedItemIds.isEmpty()) return@supervisorScope
        for (itemId in trackedItemIds) {
            val latest = latestMessage(itemId) ?: continue
            if (latest.senderUserId == userId) continue
            val key = "last_ms_$itemId"
            val seenMs = prefs.getLong(key, Long.MIN_VALUE)
            if (seenMs == Long.MIN_VALUE) {
                prefs.edit().putLong(key, latest.createdAtMs).apply()
                continue
            }
            if (latest.createdAtMs > seenMs) {
                notifyIncoming(itemId, latest.senderName, latest.body)
                prefs.edit().putLong(key, latest.createdAtMs).apply()
            }
        }
    }

    private suspend fun trackedItemIdsFor(userId: String): Set<String> {
        val fromThreads = chatThreadStore.threads.first().map { it.itemId }.filter { it.isNotBlank() }.toSet()
        val ownPosts = postRepository.getPosts().getOrElse { emptyList() }
            .filter { it.posterUserId == userId || userId in it.permissionUserIds }
            .map { it.id }
            .toSet()
        return fromThreads + ownPosts
    }

    private suspend fun latestMessage(itemId: String): LatestMessage? = withContext(Dispatchers.IO) {
        val docs = databases.listDocuments(
            databaseId = AppwriteConfig.DATABASE_ID,
            collectionId = AppwriteConfig.COLLECTION_MESSAGES,
            queries = listOf(
                Query.equal("itemId", itemId),
                Query.orderDesc("\$createdAt"),
                Query.limit(1),
            ),
        ).documents
        val doc = docs.firstOrNull() ?: return@withContext null
        @Suppress("UNCHECKED_CAST")
        val data = doc.data as? Map<String, Any?> ?: return@withContext null
        val senderUserId = data["senderUserId"]?.toString()?.trim().orEmpty()
        val senderName = data["senderName"]?.toString()?.trim().orEmpty().ifBlank { "Someone" }
        val body = data["body"]?.toString()?.trim().orEmpty()
        val createdAtMs = parseIsoMs(doc.createdAt)
        LatestMessage(itemId, senderUserId, senderName, body, createdAtMs)
    }

    private fun notifyIncoming(itemId: String, sender: String, body: String) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val openIntent = Intent(appContext, MainActivity::class.java).putExtra("chat_item_id", itemId)
        val pending = TaskStackBuilder.create(appContext).run {
            addNextIntentWithParentStack(openIntent)
            getPendingIntent(
                itemId.hashCode(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ) ?: return
        }
        val text = body.ifBlank { "You have a new message." }
        val notif = NotificationCompat.Builder(appContext, CHAT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New message from $sender")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(appContext).notify(itemId.hashCode(), notif)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHAT_CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHAT_CHANNEL_ID,
                "Chat Messages",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Alerts for new messages in your BackToOwner chats."
            },
        )
    }
}

private data class LatestMessage(
    val itemId: String,
    val senderUserId: String,
    val senderName: String,
    val body: String,
    val createdAtMs: Long,
)

private fun parseIsoMs(iso: String?): Long {
    if (iso.isNullOrBlank()) return 0L
    return try {
        Instant.parse(iso).toEpochMilli()
    } catch (_: DateTimeParseException) {
        0L
    }
}
