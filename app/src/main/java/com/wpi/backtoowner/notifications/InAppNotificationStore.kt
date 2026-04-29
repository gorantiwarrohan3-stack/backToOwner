package com.wpi.backtoowner.notifications

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS = "backtoowner_in_app_notifications"
private const val KEY_JSON = "items_json"
private const val MAX_ITEMS = 150

enum class InAppNotificationKind { CHAT, GEOFENCE }

data class InAppNotification(
    val id: String,
    val kind: InAppNotificationKind,
    val title: String,
    val body: String,
    val createdEpochMs: Long,
    /** Chat thread / listing id when [kind] is [InAppNotificationKind.CHAT]. */
    val itemId: String? = null,
)

@Singleton
class InAppNotificationStore @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val lock = Any()
    private val _items = MutableStateFlow<List<InAppNotification>>(emptyList())

    val items: StateFlow<List<InAppNotification>> = _items.asStateFlow()

    init {
        _items.value = readLocked()
    }

    fun appendChat(itemId: String, title: String, body: String, createdEpochMs: Long = System.currentTimeMillis()) {
        append(
            InAppNotification(
                id = UUID.randomUUID().toString(),
                kind = InAppNotificationKind.CHAT,
                title = title,
                body = body,
                createdEpochMs = createdEpochMs,
                itemId = itemId.takeIf { it.isNotBlank() },
            ),
        )
    }

    /**
     * In-app row for a chat message, keyed by Appwrite message document id so the same message
     * is never duplicated (poller + in-chat fetch can both run).
     */
    fun appendChatMessageIfAbsent(
        messageDocumentId: String,
        itemId: String,
        title: String,
        body: String,
        createdEpochMs: Long,
    ) {
        if (messageDocumentId.isBlank()) return
        synchronized(lock) {
            if (_items.value.any { it.id == messageDocumentId }) return
            val entry = InAppNotification(
                id = messageDocumentId,
                kind = InAppNotificationKind.CHAT,
                title = title,
                body = body,
                createdEpochMs = createdEpochMs,
                itemId = itemId.takeIf { it.isNotBlank() },
            )
            val merged = listOf(entry) + _items.value
            val trimmed = merged.take(MAX_ITEMS)
            writeLocked(trimmed)
            _items.value = trimmed
        }
    }

    fun appendGeofence(zoneName: String, createdEpochMs: Long = System.currentTimeMillis()) {
        append(
            InAppNotification(
                id = UUID.randomUUID().toString(),
                kind = InAppNotificationKind.GEOFENCE,
                title = "Safe zone",
                body = "You entered $zoneName",
                createdEpochMs = createdEpochMs,
                itemId = null,
            ),
        )
    }

    fun clearAll() {
        synchronized(lock) {
            prefs.edit().remove(KEY_JSON).apply()
            _items.value = emptyList()
        }
    }

    fun remove(id: String) {
        synchronized(lock) {
            val next = _items.value.filterNot { it.id == id }
            writeLocked(next)
            _items.value = next
        }
    }

    /** Removes in-app chat rows tied to a listing / thread [itemId] (e.g. after deleting a conversation). */
    fun removeAllChatNotificationsForItem(itemId: String) {
        if (itemId.isBlank()) return
        synchronized(lock) {
            val next = _items.value.filterNot {
                it.kind == InAppNotificationKind.CHAT && it.itemId == itemId
            }
            writeLocked(next)
            _items.value = next
        }
    }

    private fun append(entry: InAppNotification) {
        synchronized(lock) {
            val merged = listOf(entry) + _items.value.filter { it.id != entry.id }
            val trimmed = merged.take(MAX_ITEMS)
            writeLocked(trimmed)
            _items.value = trimmed
        }
    }

    private fun readLocked(): List<InAppNotification> {
        val raw = prefs.getString(KEY_JSON, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val id = o.optString("id", "")
                    if (id.isBlank()) continue
                    val kind = when (o.optString("kind", "")) {
                        "GEOFENCE" -> InAppNotificationKind.GEOFENCE
                        else -> InAppNotificationKind.CHAT
                    }
                    add(
                        InAppNotification(
                            id = id,
                            kind = kind,
                            title = o.optString("title", ""),
                            body = o.optString("body", ""),
                            createdEpochMs = o.optLong("createdEpochMs", 0L),
                            itemId = o.optString("itemId", "").takeIf { it.isNotBlank() },
                        ),
                    )
                }
            }.sortedByDescending { it.createdEpochMs }
        }.getOrDefault(emptyList())
    }

    private fun writeLocked(list: List<InAppNotification>) {
        val arr = JSONArray()
        for (n in list) {
            arr.put(
                JSONObject().apply {
                    put("id", n.id)
                    put("kind", n.kind.name)
                    put("title", n.title)
                    put("body", n.body)
                    put("createdEpochMs", n.createdEpochMs)
                    put("itemId", n.itemId ?: "")
                },
            )
        }
        prefs.edit().putString(KEY_JSON, arr.toString()).apply()
    }
}
