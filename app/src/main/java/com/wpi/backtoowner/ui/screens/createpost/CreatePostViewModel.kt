package com.wpi.backtoowner.ui.screens.createpost

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.wpi.backtoowner.domain.analysis.ImageLabelingAnalyzer
import com.wpi.backtoowner.domain.analysis.WhitelistLabelMatch
import com.wpi.backtoowner.domain.model.NewPost
import com.wpi.backtoowner.domain.model.PostType
import com.wpi.backtoowner.data.local.FoundLocationDraft
import com.wpi.backtoowner.domain.repository.PostImageRepository
import com.wpi.backtoowner.domain.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

private const val WPI_CAMPUS_LAT = 42.2742
private const val WPI_CAMPUS_LNG = -71.8064

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val imageLabelingAnalyzer: ImageLabelingAnalyzer,
    private val postRepository: PostRepository,
    private val postImageRepository: PostImageRepository,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val foundLocationDraft: FoundLocationDraft,
) : ViewModel() {

    /** Address typed on the Map tab; prepended to Found posts when non-blank. */
    val mapFoundAddressHint: StateFlow<String> = foundLocationDraft.address

    private val _category = MutableStateFlow("")
    val category: StateFlow<String> = _category.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _suggestedLabels = MutableStateFlow<List<String>>(emptyList())
    val suggestedLabels: StateFlow<List<String>> = _suggestedLabels.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val _smartTag = MutableStateFlow<WhitelistLabelMatch?>(null)
    val smartTag: StateFlow<WhitelistLabelMatch?> = _smartTag.asStateFlow()

    private val _postType = MutableStateFlow(PostType.LOST)
    val postType: StateFlow<PostType> = _postType.asStateFlow()

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    private val _postError = MutableStateFlow<String?>(null)
    val postError: StateFlow<String?> = _postError.asStateFlow()

    private val _pickedLat = MutableStateFlow<Double?>(null)
    private val _pickedLng = MutableStateFlow<Double?>(null)

    private val _locationHint = MutableStateFlow("WPI area (default). Tap Set Location to use GPS.")
    val locationHint: StateFlow<String> = _locationHint.asStateFlow()

    /** Building / room / address typed on Create Post (optional, both Lost and Found). */
    private val _manualPlaceNote = MutableStateFlow("")
    val manualPlaceNote: StateFlow<String> = _manualPlaceNote.asStateFlow()

    fun refreshDeviceLocation() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    val loc = fusedLocationClient.lastLocation.await()
                    if (loc != null) {
                        _pickedLat.value = loc.latitude
                        _pickedLng.value = loc.longitude
                        _locationHint.value = String.format(
                            Locale.US,
                            "GPS: %.5f, %.5f",
                            loc.latitude,
                            loc.longitude,
                        )
                    } else {
                        _locationHint.value = "No recent GPS fix. Move outdoors or try again."
                    }
                }.onFailure {
                    _locationHint.value = "Could not read location (${it.message ?: "error"})."
                }
            }
        }
    }

    fun onPhotoCaptured(bitmap: Bitmap) {
        _previewBitmap.value = bitmap
        _postError.value = null
        viewModelScope.launch {
            _isAnalyzing.value = true
            _smartTag.value = null
            val analysis = imageLabelingAnalyzer.analyzeLabels(bitmap)
            _suggestedLabels.value = analysis.topMatches.map { it.category }
            analysis.autoCategoryMatch?.let { match ->
                _category.value = match.category
                _smartTag.value = match
            }
            _isAnalyzing.value = false
        }
    }

    fun onSuggestedLabelSelected(label: String) {
        _smartTag.value = null
        _category.value = label
        val others = _suggestedLabels.value.filterNot { it.equals(label, ignoreCase = true) }
        _description.value = if (others.isEmpty()) {
            "Detected in photo: $label."
        } else {
            "Detected in photo: $label. Other possible matches: ${others.joinToString(", ")}."
        }
    }

    fun onCategoryChange(value: String) {
        _category.value = value
        _smartTag.value?.let { tag ->
            if (!value.equals(tag.category, ignoreCase = true)) {
                _smartTag.value = null
            }
        }
    }

    fun onDescriptionChange(value: String) {
        _description.value = value
    }

    fun onManualPlaceNoteChange(value: String) {
        _manualPlaceNote.value = value
    }

    fun onPostTypeChange(type: PostType) {
        _postType.value = type
    }

    fun clearPostError() {
        _postError.value = null
    }

    fun submitPost(onSuccess: () -> Unit) {
        if (_isPosting.value) return
        val type = _postType.value
        val bitmap = _previewBitmap.value
        val category = _category.value.trim()
        val descRaw = _description.value.trim()

        if (type == PostType.LOST) {
            if (category.isBlank() || descRaw.isBlank()) {
                _postError.value = "Add a category and description for your lost item."
                return
            }
        } else {
            if (bitmap == null) {
                _postError.value = "Take a photo of the found item before posting."
                return
            }
            if (category.isBlank() || descRaw.isBlank()) {
                _postError.value = "Add a category and short description for the found item."
                return
            }
        }

        val title = category.ifBlank { "Item" }
        var desc = descRaw
        val manual = _manualPlaceNote.value.trim()
        val mapNote = foundLocationDraft.current()
        val placeLines = buildList {
            if (manual.isNotBlank()) add("Place details: $manual")
            if (type == PostType.FOUND && mapNote.isNotBlank()) {
                add("Found near (from Map tab): $mapNote")
            }
        }
        if (placeLines.isNotEmpty()) {
            desc = placeLines.joinToString("\n") + "\n\n" + desc
        }

        viewModelScope.launch {
            _isPosting.value = true
            _postError.value = null
            val imageUrl = if (bitmap != null) {
                postImageRepository.uploadPostImage(bitmap).getOrElse { e ->
                    _isPosting.value = false
                    _postError.value = e.message ?: "Could not upload image. Check Appwrite Storage bucket permissions."
                    return@launch
                }
            } else {
                ""
            }
            val matchPercent = _smartTag.value?.let { (it.confidence * 100f).toInt().coerceIn(1, 99) }
            val lat = _pickedLat.value ?: WPI_CAMPUS_LAT
            val lng = _pickedLng.value ?: WPI_CAMPUS_LNG
            val result = postRepository.createPost(
                NewPost(
                    title = title,
                    description = desc,
                    imageUrl = imageUrl,
                    latitude = lat,
                    longitude = lng,
                    type = type,
                    matchPercent = matchPercent,
                ),
            )
            _isPosting.value = false
            result.fold(
                onSuccess = {
                    _manualPlaceNote.value = ""
                    if (type == PostType.FOUND) foundLocationDraft.clear()
                    onSuccess()
                },
                onFailure = { e ->
                    _postError.value = e.message ?: "Could not save post."
                },
            )
        }
    }
}
