package com.wpi.backtoowner.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpi.backtoowner.domain.model.Post
import com.wpi.backtoowner.domain.model.PostType
import com.wpi.backtoowner.domain.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DayCount(
    val label: String,
    val count: Int,
)

data class TitleCount(
    val title: String,
    val count: Int,
)

data class InsightsSnapshot(
    /** Rows in archive before filters (for subtitle). */
    val archiveTotalCount: Int,
    /** Rows matching current category + created date filters. */
    val filteredCount: Int,
    val lostCount: Int,
    val foundCount: Int,
    /** Newest on the right — last 7 calendar days (local). */
    val lastSevenDays: List<DayCount>,
    /** Most common listing titles in the archive. */
    val topTitles: List<TitleCount>,
)

sealed interface InsightsUiState {
    data object Loading : InsightsUiState
    data class Error(val message: String) : InsightsUiState
    data class Ready(val snapshot: InsightsSnapshot) : InsightsUiState
}

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val postRepository: PostRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<InsightsUiState>(InsightsUiState.Loading)
    val state: StateFlow<InsightsUiState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _allPosts = MutableStateFlow<List<Post>>(emptyList())

    /** `null` = all categories; otherwise exact normalized listing title (same as post "category"). */
    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()

    private val _availableCategories = MutableStateFlow<List<String>>(emptyList())
    val availableCategories: StateFlow<List<String>> = _availableCategories.asStateFlow()

    private val _fromDate = MutableStateFlow<LocalDate?>(null)
    val fromDate: StateFlow<LocalDate?> = _fromDate.asStateFlow()

    private val _toDate = MutableStateFlow<LocalDate?>(null)
    val toDate: StateFlow<LocalDate?> = _toDate.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = InsightsUiState.Loading
            fetchAndApplyState()
        }
    }

    /** Pull-to-refresh: reloads archive without clearing the screen to the initial loading state. */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                fetchAndApplyState()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun fetchAndApplyState() {
        withContext(Dispatchers.IO) {
            postRepository.getArchivedPosts()
        }.fold(
            onSuccess = { posts ->
                _allPosts.value = posts
                val categories = computeCategoriesFromPosts(posts)
                _availableCategories.value = categories
                val selected = _categoryFilter.value
                if (selected != null && categories.none { it == selected }) {
                    _categoryFilter.value = null
                }
                publishFilteredSnapshot()
            },
            onFailure = { e ->
                _state.value =
                    InsightsUiState.Error(e.message ?: "Could not load archive insights.")
            },
        )
    }

    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category?.trim()?.takeIf { it.isNotEmpty() }
        if (_state.value is InsightsUiState.Ready || _allPosts.value.isNotEmpty()) {
            publishFilteredSnapshot()
        }
    }

    fun clearCategoryFilter() {
        _categoryFilter.value = null
        if (_state.value is InsightsUiState.Ready || _allPosts.value.isNotEmpty()) {
            publishFilteredSnapshot()
        }
    }

    fun setFromDate(date: LocalDate?) {
        _fromDate.value = date
        if (_state.value is InsightsUiState.Ready || _allPosts.value.isNotEmpty()) {
            publishFilteredSnapshot()
        }
    }

    fun setToDate(date: LocalDate?) {
        _toDate.value = date
        if (_state.value is InsightsUiState.Ready || _allPosts.value.isNotEmpty()) {
            publishFilteredSnapshot()
        }
    }

    fun clearDateFilters() {
        _fromDate.value = null
        _toDate.value = null
        if (_state.value is InsightsUiState.Ready || _allPosts.value.isNotEmpty()) {
            publishFilteredSnapshot()
        }
    }

    private fun publishFilteredSnapshot() {
        var from = _fromDate.value
        var to = _toDate.value
        if (from != null && to != null && from.isAfter(to)) {
            _fromDate.value = to
            _toDate.value = from
            from = _fromDate.value
            to = _toDate.value
        }
        val all = _allPosts.value
        val filtered = applyFilters(all, from, to)
        _state.value = InsightsUiState.Ready(buildSnapshot(filteredPosts = filtered, archiveTotalCount = all.size))
    }

    private fun applyFilters(posts: List<Post>, fromOverride: LocalDate? = null, toOverride: LocalDate? = null): List<Post> {
        var r = posts
        _categoryFilter.value?.let { key ->
            r = r.filter { categoryKey(it) == key }
        }
        var from = fromOverride ?: _fromDate.value
        var to = toOverride ?: _toDate.value
        val zone = ZoneId.systemDefault()
        if (from != null) {
            val fromMs = from.atStartOfDay(zone).toInstant().toEpochMilli()
            r = r.filter { p ->
                if (p.createdAtEpochMs <= 0L) false else p.createdAtEpochMs >= fromMs
            }
        }
        if (to != null) {
            val endInclusive = to.atTime(23, 59, 59, 999_999_999).atZone(zone).toInstant().toEpochMilli()
            r = r.filter { p ->
                if (p.createdAtEpochMs <= 0L) false else p.createdAtEpochMs <= endInclusive
            }
        }
        return r
    }
}

private fun categoryKey(post: Post): String =
    post.title.trim().ifBlank { "(untitled)" }

private fun computeCategoriesFromPosts(posts: List<Post>): List<String> =
    posts
        .map { categoryKey(it) }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }
                .thenBy { it.key.lowercase() },
        )
        .take(32)
        .map { it.key }

private val dayLabelFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

private fun buildSnapshot(filteredPosts: List<Post>, archiveTotalCount: Int): InsightsSnapshot {
    val posts = filteredPosts
    val lost = posts.count { it.type == PostType.LOST }
    val found = posts.count { it.type == PostType.FOUND }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val lastSevenDays = (6 downTo 0).map { daysAgo ->
        val d = today.minusDays(daysAgo.toLong())
        val count = posts.count { p ->
            if (p.createdAtEpochMs <= 0L) return@count false
            val ld = Instant.ofEpochMilli(p.createdAtEpochMs).atZone(zone).toLocalDate()
            ld == d
        }
        DayCount(label = d.format(dayLabelFmt), count = count)
    }
    val titleGroups = posts
        .map { categoryKey(it).take(48) }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(8)
        .map { TitleCount(title = it.key, count = it.value) }
    return InsightsSnapshot(
        archiveTotalCount = archiveTotalCount,
        filteredCount = posts.size,
        lostCount = lost,
        foundCount = found,
        lastSevenDays = lastSevenDays,
        topTitles = titleGroups,
    )
}
