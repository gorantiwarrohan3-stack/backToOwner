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
        val prompt =
            "Does this image match the following description of a lost item: '$description'? " +
                "Please provide a match confidence percentage and a brief explanation."

        return@withContext try {
            val content = Content.fromParts(
                Part.fromText(prompt),
                Part.fromBytes(jpegBytes(image), "image/jpeg"),
            )
            client.models.generateContent(MODEL_NAME, content, null).text()
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun scoreAgainstFeedListing(
        anchorTitle: String,
        anchorDescription: String,
        anchorImage: Bitmap?,
        candidateTitle: String,
        candidateDescription: String,
        candidateImage: Bitmap?,
    ): Int? = withContext(Dispatchers.IO) {
        try {
            val header = """
                You compare Item A (a report just filed) vs Item B (an existing listing) on campus.
                Decide how likely BOTH refer to the SAME physical lost/found item (appearance, descriptors, clues).
                If information is unrelated, similarity should be near 0.
                Respond ONLY with strict JSON — no Markdown, no code fences — on one line, shape:
                {"similarity":<integer 0-100>}
            """.trimIndent()

            val body = """
                Item A (new report)
                Title: $anchorTitle
                Description: ${anchorDescription.lines().take(12).joinToString(" ")}

                Item B (existing feed listing)
                Title: $candidateTitle
                Description: ${candidateDescription.lines().take(12).joinToString(" ")}
                ${if (anchorImage != null) "\nAttachment order: Item A photo first (if shown), Item B photo second.\n" else ""}
            """.trimIndent()

            val parts = mutableListOf<Part>()
            parts += Part.fromText("$header\n\n$body")
            anchorImage?.let { parts += Part.fromText("Photo for Item A:") ; parts += Part.fromBytes(jpegBytes(it), "image/jpeg") }
            candidateImage?.let { parts += Part.fromText("Photo for Item B:") ; parts += Part.fromBytes(jpegBytes(it), "image/jpeg") }

            val content = Content.fromParts(*parts.toTypedArray())
            val raw = client.models.generateContent(MODEL_NAME, content, null).text()
            similarityFromModelText(raw)
        } catch (_: Exception) {
            null
        }
    }

    private fun jpegBytes(bitmap: Bitmap): ByteArray =
        ByteArrayOutputStream().apply { bitmap.compress(Bitmap.CompressFormat.JPEG, BUNDLE_JPEG_Q, this) }.toByteArray()

    private companion object {
        const val MODEL_NAME = "gemini-2.5-flash"
        const val BUNDLE_JPEG_Q = 82
    }
}

private fun similarityFromModelText(raw: String?): Int? {
    if (raw.isNullOrBlank()) return null
    Regex("\"similarity\"\\s*:\\s*(\\d{1,3})").find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull()
        ?.takeIf { it in 0..100 }
        ?.let { return it }
    Regex("(?i)similarity[^\\d]{0,8}(\\d{1,3})").find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull()
        ?.takeIf { it in 0..100 }
        ?.let { return it }
    return Regex("(\\d{1,3})\\s*%").find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..100 }
}
