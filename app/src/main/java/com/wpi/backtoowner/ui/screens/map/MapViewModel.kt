package com.wpi.backtoowner.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.wpi.backtoowner.data.local.FoundLocationDraft
import com.wpi.backtoowner.domain.model.Post
import com.wpi.backtoowner.domain.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    val foundLocationDraft: FoundLocationDraft,
    private val postRepository: PostRepository,
    private val fusedLocationClient: FusedLocationProviderClient,
) : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    init {
        viewModelScope.launch {
            postRepository.observePosts().collect { result ->
                result.onSuccess { _posts.value = it }
            }
        }
    }

    suspend fun lastKnownLocation(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        runCatching {
            fusedLocationClient.lastLocation.await()?.let { it.latitude to it.longitude }
        }.getOrNull()
    }
}
