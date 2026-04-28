package com.wpi.backtoowner.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.wpi.backtoowner.domain.repository.AiMatchingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AnalyzeMatchUseCase @Inject constructor(
    private val aiMatchingRepository: AiMatchingRepository,
    private val imageLoader: ImageLoader,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(description: String, imageUrl: String): String? {
        val bitmap = fetchBitmap(imageUrl) ?: return null
        return aiMatchingRepository.matchItem(description, bitmap)
    }

    private suspend fun fetchBitmap(url: String): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false) // Required for Gemini processing
            .build()
        
        return when (val result = imageLoader.execute(request)) {
            is SuccessResult -> (result.drawable as? BitmapDrawable)?.bitmap
            else -> null
        }
    }
}
