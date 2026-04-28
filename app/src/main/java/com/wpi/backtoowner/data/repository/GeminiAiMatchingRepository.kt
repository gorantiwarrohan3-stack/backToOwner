package com.wpi.backtoowner.data.repository

import android.graphics.Bitmap
import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.Part
import com.wpi.backtoowner.domain.repository.AiMatchingRepository
import com.wpi.backtoowner.domain.repository.MatchCandidate
import com.wpi.backtoowner.domain.repository.MatchResult
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class GeminiAiMatchingRepository @Inject constructor(
    private val client: Client
) : AiMatchingRepository {

    override suspend fun matchItem(description: String, image: Bitmap): String? = withContext(Dispatchers.IO) {
        // ... existing matchItem implementation ...
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

    override suspend fun findBestMatches(
        lostDescription: String,
        candidates: List<MatchCandidate>
    ): List<MatchResult> = withContext(Dispatchers.IO) {
        if (candidates.isEmpty()) return@withContext emptyList()

        val prompt = StringBuilder().apply {
            append("I have a lost item described as: \"$lostDescription\"\n\n")
            append("Please compare this with the following list of found items and tell me which ones match. ")
            append("For each item, provide a confidence score from 0 to 100 and a short reason.\n\n")
            append("Return the results as a JSON array of objects with fields: \"id\" (String), \"score\" (Integer), and \"reason\" (String).\n\n")
            candidates.forEachIndexed { index, candidate ->
                append("Item ID: ${candidate.id}\n")
                append("Description: ${candidate.description}\n")
                if (candidate.image != null) {
                    append("(Image attached for this item)\n")
                }
                append("---\n")
            }
        }.toString()

        try {
            val parts = mutableListOf<Part>()
            parts.add(Part.fromText(prompt))
            
            candidates.forEach { candidate ->
                candidate.image?.let {
                    val stream = ByteArrayOutputStream()
                    it.compress(Bitmap.CompressFormat.JPEG, 70, stream)
                    val imageBytes = stream.toByteArray()
                    parts.add(Part.fromBytes(imageBytes, "image/jpeg"))
                }
            }

            val content = Content.fromParts(*parts.toTypedArray())
            val response = client.models.generateContent("gemini-2.5-flash", content, null)
            val jsonText = response.text()?.removePrefix("```json")?.removeSuffix("```")?.trim() ?: "[]"
            
            val jsonArray = JSONArray(jsonText)
            val results = mutableListOf<MatchResult>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                results.add(MatchResult(
                    id = obj.getString("id"),
                    confidenceScore = obj.getInt("score"),
                    reason = obj.getString("reason")
                ))
            }
            results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
