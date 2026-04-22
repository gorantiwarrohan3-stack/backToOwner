package com.wpi.backtoowner.domain.analysis

data class WhitelistLabelMatch(
    val category: String,
    val confidence: Float,
    val rawMlLabel: String,
)

data class ImageLabelAnalysis(
    /** Up to three best whitelist matches for chips. */
    val topMatches: List<WhitelistLabelMatch>,
    /** Highest-confidence match if above [AUTO_CATEGORY_CONFIDENCE]; drives category + Smart Tag. */
    val autoCategoryMatch: WhitelistLabelMatch?,
) {
    companion object {
        /** Slightly lower so on-device labels can still drive category when confident. */
        const val AUTO_CATEGORY_CONFIDENCE: Float = 0.58f
        const val MAX_CHIPS: Int = 3
    }
}
