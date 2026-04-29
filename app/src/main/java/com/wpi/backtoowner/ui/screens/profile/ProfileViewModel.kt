package com.wpi.backtoowner.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpi.backtoowner.domain.model.AuthUserSummary
import com.wpi.backtoowner.domain.model.Post
import com.wpi.backtoowner.domain.repository.AuthRepository
import com.wpi.backtoowner.domain.repository.PostRepository
import com.wpi.backtoowner.geofence.GeofencingSupport
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Ready(
        val user: AuthUserSummary,
        /** All listings authored by this user (newest first). */
        val myPosts: List<Post>,
        val postsLoadFailed: Boolean,
    ) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val postRepository: PostRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private val _geofencingEnabled = MutableStateFlow(GeofencingSupport.isEnabled(appContext))
    val geofencingEnabled: StateFlow<Boolean> = _geofencingEnabled.asStateFlow()

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
                    val mine = posts.filter { post ->
                        when {
                            post.posterUserId == user.id -> true
                            user.id in post.permissionUserIds -> true
                            post.posterUserId == null &&
                                post.posterDisplayName != null &&
                                post.posterDisplayName.trim().equals(user.name.trim(), ignoreCase = true) -> true
                            else -> false
                        }
                    }
                    val myPosts = mine.sortedByDescending { it.createdAtEpochMs }
                    _uiState.value = ProfileUiState.Ready(
                        user = user,
                        myPosts = myPosts,
                        postsLoadFailed = postsFailed,
                    )
                },
                onFailure = { e ->
                    _uiState.value = ProfileUiState.Error(e.message ?: "Could not load profile.")
                },
            )
        }
    }

    fun saveProfile(
        name: String,
        email: String,
        phone: String,
        password: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        viewModelScope.launch {
            val res = authRepository.updateProfile(
                name = name,
                email = email,
                phone = phone,
                password = password.ifBlank { null },
            )
            if (res.isSuccess) {
                authRepository.getCurrentUser().fold(
                    onSuccess = { u ->
                        val cur = _uiState.value
                        if (cur is ProfileUiState.Ready) {
                            _uiState.value = cur.copy(user = u)
                        }
                    },
                    onFailure = { },
                )
            }
            onResult(res)
        }
    }

    fun deletePost(postId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = postRepository.deletePost(postId)
            if (res.isSuccess) {
                val cur = _uiState.value
                if (cur is ProfileUiState.Ready) {
                    _uiState.value = cur.copy(myPosts = cur.myPosts.filterNot { it.id == postId })
                }
            }
            onResult(res)
        }
    }

    fun setGeofencingEnabled(enabled: Boolean) {
        _geofencingEnabled.value = enabled
        GeofencingSupport.setEnabled(appContext, enabled)
    }
}
