package com.wpi.backtoowner.data.local

import android.content.Context
import com.wpi.backtoowner.domain.model.PostType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS = "backtoowner_chat_threads"
private const val KEY_JSON = "threads_json"

@Singleton
class ChatThreadStore @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _threads = MutableStateFlow<List<ChatThreadSummary>>(emptyList())
    val threads: StateFlow<List<ChatThreadSummary>> = _threads.asStateFlow()

    init {
        _threads.value = readAll()
    }

    fun touch(
        itemId: String,
        postTitle: String,
        posterDisplayName: String?,
        listingType: PostType,
    ) {
        if (itemId.isBlank()) return
        val now = System.currentTimeMillis()
        val list = _threads.value.toMutableList()
        val idx = list.indexOfFirst { it.itemId == itemId }
        val next = ChatThreadSummary(
            itemId = itemId,
            postTitle = postTitle.ifBlank { "Listing" },
            posterDisplayName = posterDisplayName?.takeIf { it.isNotBlank() },
            listingType = listingType.name,
            lastTouchedEpochMs = now,
        )
        if (idx >= 0) {
            list[idx] = next
        } else {
            list.add(0, next)
        }
        list.sortByDescending { it.lastTouchedEpochMs }
        _threads.value = list
        writeAll(list)
    }

    private fun readAll(): List<ChatThreadSummary> {
        val raw = prefs.getString(KEY_JSON, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val id = o.optString("itemId", "")
                    if (id.isBlank()) continue
                    add(
                        ChatThreadSummary(
                            itemId = id,
                            postTitle = o.optString("postTitle", "Listing"),
                            posterDisplayName = o.optString("posterDisplayName", "").takeIf { it.isNotBlank() },
                            listingType = o.optString("listingType", PostType.LOST.name),
                            lastTouchedEpochMs = o.optLong("lastTouchedEpochMs", 0L),
                        ),
                    )
                }
            }.sortedByDescending { it.lastTouchedEpochMs }
        }.getOrDefault(emptyList())
    }

    private fun writeAll(list: List<ChatThreadSummary>) {
        val arr = JSONArray()
        for (t in list) {
            arr.put(
                JSONObject().apply {
                    put("itemId", t.itemId)
                    put("postTitle", t.postTitle)
                    put("posterDisplayName", t.posterDisplayName ?: "")
                    put("listingType", t.listingType)
                    put("lastTouchedEpochMs", t.lastTouchedEpochMs)
                },
            )
        }
        prefs.edit().putString(KEY_JSON, arr.toString()).apply()
    }
}
