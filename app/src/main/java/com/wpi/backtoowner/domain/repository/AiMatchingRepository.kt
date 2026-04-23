package com.wpi.backtoowner.domain.repository

import android.graphics.Bitmap

interface AiMatchingRepository {
    suspend fun matchItem(description: String, image: Bitmap): String?
}
