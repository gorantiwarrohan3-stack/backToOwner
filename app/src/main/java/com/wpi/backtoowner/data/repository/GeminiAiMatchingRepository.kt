package com.wpi.backtoowner.data.repository

import android.graphics.Bitmap
import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.Part
import com.wpi.backtoowner.domain.repository.AiMatchingRepository
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class GeminiAiMatchingRepository @Inject constructor(
    private val client: Client
) : AiMatchingRepository {

    override suspend fun matchItem(description: String, image: Bitmap): String? = withContext(Dispatchers.IO) {
        val prompt = "Does this image match the following description of a lost item: '$description'? " +
                     "Please provide a match confidence percentage and a brief explanation."
        
        return@withContext try {
            val stream = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val imageBytes = stream.toByteArray()

            val content = Content.fromParts(
                Part.fromText(prompt),
                Part.fromBytes(imageBytes, "image/jpeg")
            )
            
            val response = client.models.generateContent("gemini-2.5-flash", content, null)
            response.text()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
