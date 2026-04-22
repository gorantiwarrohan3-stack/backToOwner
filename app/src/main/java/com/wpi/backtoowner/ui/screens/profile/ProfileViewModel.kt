package com.wpi.backtoowner.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpi.backtoowner.domain.model.AuthUserSummary
import com.wpi.backtoowner.domain.model.Post
import com.wpi.backtoowner.domain.repository.AuthRepository
import com.wpi.backtoowner.domain.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Ready(
        val user: AuthUserSummary,
        val myActivePosts: List<Post>,
        val myResolvedPosts: List<Post>,
        val postsLoadFailed: Boolean,
    ) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val postRepository: PostRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val userResult = authRepository.getCurrentUser()
            userResult.fold(
                onSuccess = { user ->
                    val postsResult = postRepository.getPosts()
                    val posts = postsResult.getOrElse { emptyList() }
                    val postsFailed = postsResult.isFailure
                    val mine = posts.filter { it.posterUserId != null && it.posterUserId == user.id }
                    val active = mine.filter { !it.resolved }.sortedByDescending { it.createdAtEpochMs }
                    val resolved = mine.filter { it.resolved }.sortedByDescending { it.createdAtEpochMs }
                    _uiState.value = ProfileUiState.Ready(
                        user = user,
                        myActivePosts = active,
                        myResolvedPosts = resolved,
                        postsLoadFailed = postsFailed,
                    )
                },
                onFailure = { e ->
                    _uiState.value = ProfileUiState.Error(e.message ?: "Could not load profile.")
                },
            )
        }
    }
}
