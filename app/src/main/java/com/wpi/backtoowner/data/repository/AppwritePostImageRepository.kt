package com.wpi.backtoowner.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.wpi.backtoowner.config.AppwriteConfig
import com.wpi.backtoowner.domain.repository.PostImageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.appwrite.ID
import io.appwrite.Permission
import io.appwrite.Role
import io.appwrite.models.InputFile
import io.appwrite.services.Account
import io.appwrite.services.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppwritePostImageRepository @Inject constructor(
    private val storage: Storage,
    private val account: Account,
    @ApplicationContext private val context: Context,
) : PostImageRepository {

    override suspend fun uploadPostImage(bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(AppwriteConfig.STORAGE_BUCKET_ID.isNotBlank()) {
                "Set STORAGE_BUCKET_ID in AppwriteConfig (Appwrite Console → Storage → create a bucket, then paste its ID)."
            }
            val temp = File.createTempFile("post", ".jpg", context.cacheDir)
            try {
                temp.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
                val userId = account.get().id
                val uploaded = storage.createFile(
                    bucketId = AppwriteConfig.STORAGE_BUCKET_ID,
                    fileId = ID.unique(),
                    file = InputFile.fromPath(temp.absolutePath),
                    permissions = listOf(
                        Permission.read(Role.any()),
                        Permission.update(Role.user(userId)),
                        Permission.delete(Role.user(userId)),
                    ),
                )
                fileViewUrl(uploaded.id)
            } finally {
                temp.delete()
            }
        }
    }

    private fun fileViewUrl(fileId: String): String {
        val base = AppwriteConfig.ENDPOINT.trimEnd('/')
        val bucket = AppwriteConfig.STORAGE_BUCKET_ID
        val project = AppwriteConfig.PROJECT_ID
        return "${base}/storage/buckets/${bucket}/files/${fileId}/view?project=${project}"
    }
}
