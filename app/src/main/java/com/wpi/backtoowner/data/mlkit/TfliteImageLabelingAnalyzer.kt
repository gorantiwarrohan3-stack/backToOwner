package com.wpi.backtoowner.data.mlkit

import android.content.Context
import android.graphics.Bitmap
import com.wpi.backtoowner.domain.analysis.ImageLabelAnalysis
import com.wpi.backtoowner.domain.analysis.ImageLabelingAnalyzer
import com.wpi.backtoowner.domain.analysis.WhitelistLabelMatch
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TfliteImageLabelingAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context,
) : ImageLabelingAnalyzer {

    companion object {
        private const val INPUT_SIZE = 224
        private const val MODEL_FILE = "model.tflite"
        private const val LABELS_FILE = "labels.txt"
    }

    private val interpreter: Interpreter by lazy {
        val bytes = context.assets.open(MODEL_FILE).use { it.readBytes() }
        val buffer = ByteBuffer.allocateDirect(bytes.size).apply {
            order(ByteOrder.nativeOrder())
            put(bytes)
            rewind()
        }
        Interpreter(buffer)
    }

    private val labels: List<String> by lazy {
        context.assets.open(LABELS_FILE).bufferedReader().readLines().filter { it.isNotBlank() }
    }

    override suspend fun analyzeLabels(bitmap: Bitmap): ImageLabelAnalysis = withContext(Dispatchers.Default) {
        runCatching {
            val input = preprocessBitmap(bitmap)
            val output = Array(1) { FloatArray(labels.size) }
            interpreter.run(input, output)

            val scores = output[0]
            val matches = labels.mapIndexed { i, label ->
                WhitelistLabelMatch(category = label, confidence = scores[i], rawMlLabel = label)
            }.sortedByDescending { it.confidence }

            val topMatches = matches
                .filter { it.confidence >= 0.10f }
                .take(ImageLabelAnalysis.MAX_CHIPS)

            val autoMatch = matches.firstOrNull {
                it.confidence >= ImageLabelAnalysis.AUTO_CATEGORY_CONFIDENCE
            }

            ImageLabelAnalysis(topMatches = topMatches, autoCategoryMatch = autoMatch)
        }.getOrElse {
            ImageLabelAnalysis(emptyList(), null)
        }
    }

    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val buffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            // MobileNetV2 preprocessing: normalize to [-1, 1]
            buffer.putFloat(((pixel shr 16 and 0xFF) / 127.5f) - 1f)
            buffer.putFloat(((pixel shr 8 and 0xFF) / 127.5f) - 1f)
            buffer.putFloat(((pixel and 0xFF) / 127.5f) - 1f)
        }
        buffer.rewind()
        return buffer
    }
}
