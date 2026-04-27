package com.wpi.backtoowner.data.repository

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.wpi.backtoowner.domain.repository.AiMatchingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiAiMatchingRepository @Inject constructor(
    private val generativeModel: GenerativeModel
) : AiMatchingRepository {

    override suspend fun matchItem(description: String, image: Bitmap): String? {
        val prompt = "Does this image match the following description of a lost item: '$description'? " +
                     "Please provide a match confidence percentage and a brief explanation."
        
        return try {
            val inputContent = content {
                image(image)
                text(prompt)
            }
            val response = generativeModel.generateContent(inputContent)
            response.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
