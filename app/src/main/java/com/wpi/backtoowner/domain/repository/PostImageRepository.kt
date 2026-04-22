package com.wpi.backtoowner.domain.repository

import android.graphics.Bitmap

fun interface PostImageRepository {

    suspend fun uploadPostImage(bitmap: Bitmap): Result<String>
}
