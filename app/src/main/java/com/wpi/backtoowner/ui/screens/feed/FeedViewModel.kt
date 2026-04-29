package com.wpi.backtoowner.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpi.backtoowner.domain.model.Post
import com.wpi.backtoowner.domain.model.PostType
import com.wpi.backtoowner.domain.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class FeedFilter { ALL, LOST, FOUND }

@HiltViewModel
class FeedViewModel @Inject constructor(
    postRepository: PostRepository,
) : ViewModel() {

    private val postsFlow = postRepository.observePosts()
        .map { result -> result.getOrElse { emptyList() } }

    private val _filter = MutableStateFlow(FeedFilter.ALL)
    val filter: StateFlow<FeedFilter> = _filter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val posts: StateFlow<List<Post>> = combine(
        postsFlow,
        _filter,
        _searchQuery,
    ) { list, filter, query ->
        list
            .asSequence()
            .filter { post ->
                when (filter) {
                    FeedFilter.ALL -> true
                    FeedFilter.LOST -> post.type == PostType.LOST
                    FeedFilter.FOUND -> post.type == PostType.FOUND
                }
            }
            .filter { post ->
                if (query.isBlank()) return@filter true
                val q = query.trim()
                post.title.contains(q, ignoreCase = true) ||
                    post.description.contains(q, ignoreCase = true)
            }
            .toList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    fun setFilter(filter: FeedFilter) {
        _filter.value = filter
    }

    fun setSearchQuery(value: String) {
        _searchQuery.value = value
    }
}
