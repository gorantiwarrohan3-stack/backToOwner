package com.wpi.backtoowner.domain.repository

import android.graphics.Bitmap

interface AiMatchingRepository {
    suspend fun matchItem(description: String, image: Bitmap): String?

    /**
     * Compare a newly reported item (“anchor”) to an existing feed listing (“candidate”).
     * Uses description text and optional photos. Returns similarity 0–100, or null on failure.
     */
    suspend fun scoreAgainstFeedListing(
        anchorTitle: String,
        anchorDescription: String,
        anchorImage: Bitmap?,
        candidateTitle: String,
        candidateDescription: String,
        candidateImage: Bitmap?,
    ): Int?
}
