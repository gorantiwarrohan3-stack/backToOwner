package com.wpi.backtoowner.data.mlkit

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.wpi.backtoowner.domain.analysis.ImageLabelAnalysis
import com.wpi.backtoowner.domain.analysis.ImageLabelingAnalyzer
import com.wpi.backtoowner.domain.analysis.StudentItemWhitelist
import com.wpi.backtoowner.domain.analysis.WhitelistLabelMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MlKitImageLabelingAnalyzer @Inject constructor(
    private val imageLabeler: ImageLabeler,
) : ImageLabelingAnalyzer {

    override suspend fun analyzeLabels(bitmap: Bitmap): ImageLabelAnalysis = withContext(Dispatchers.Default) {
        runCatching {
            val image = InputImage.fromBitmap(bitmap, 0)
            val labels = imageLabeler.process(image).await()

            val whitelistMerged = labels.mapNotNull { label ->
                StudentItemWhitelist.matchCategory(label.text)?.let { category ->
                    Triple(category, label.confidence, label.text)
                }
            }
                .groupBy { it.first }
                .map { (_, list) ->
                    val best = list.maxBy { it.second }
                    WhitelistLabelMatch(
                        category = best.first,
                        confidence = best.second,
                        rawMlLabel = best.third,
                    )
                }
                .sortedByDescending { it.confidence }

            val rawFallback = labels
                .asSequence()
                .sortedByDescending { it.confidence }
                .filter { it.confidence >= 0.55f }
                .filter { it.text.isNotBlank() }
                .filter { label ->
                    // When the whitelist already identified a category, suppress generic/misleading
                    // raw labels that MLKit commonly returns alongside physical objects
                    // (e.g. "Musical instrument", "Organism", "Geological phenomenon").
                    if (whitelistMerged.isNotEmpty()) {
                        val lc = label.text.lowercase()
                        !lc.contains("instrument") && !lc.contains("musical") &&
                            !lc.contains("organism") && !lc.contains("geological") &&
                            !lc.contains("astronomical") && !lc.contains("phenomenon")
                    } else true
                }
                .map { label ->
                    val raw = label.text.trim()
                    val title = raw.replaceFirstChar { ch ->
                        ch.titlecase(Locale.getDefault())
                    }
                    WhitelistLabelMatch(
                        category = title,
                        confidence = label.confidence,
                        rawMlLabel = label.text,
                    )
                }
                .distinctBy { it.category.lowercase() }
                .filter { w -> whitelistMerged.none { it.category.equals(w.category, ignoreCase = true) } }
                .take(ImageLabelAnalysis.MAX_CHIPS)

            val mergedForChips = (whitelistMerged + rawFallback.toList())
                .distinctBy { it.category.lowercase() }
                .sortedByDescending { it.confidence }
                .take(ImageLabelAnalysis.MAX_CHIPS)

            val autoCategoryMatch = whitelistMerged
                .firstOrNull { it.confidence >= ImageLabelAnalysis.AUTO_CATEGORY_CONFIDENCE }
                ?: mergedForChips.firstOrNull()
                    ?.takeIf { whitelistMerged.isEmpty() && it.confidence >= 0.68f }

            ImageLabelAnalysis(
                topMatches = mergedForChips,
                autoCategoryMatch = autoCategoryMatch,
            )
        }.getOrElse {
            ImageLabelAnalysis(emptyList(), null)
        }
    }
}
