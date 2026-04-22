package com.wpi.backtoowner.domain.analysis

import android.graphics.Bitmap

interface ImageLabelingAnalyzer {

    suspend fun analyzeLabels(bitmap: Bitmap): ImageLabelAnalysis
}
