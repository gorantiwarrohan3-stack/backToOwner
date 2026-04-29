package com.wpi.backtoowner.data.repository

import com.wpi.backtoowner.config.AppwriteConfig
import com.wpi.backtoowner.domain.repository.ChatMessagingRepository
import io.appwrite.Query
import io.appwrite.services.Databases
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AppwriteChatMessagingRepository @Inject constructor(
    private val databases: Databases,
) : ChatMessagingRepository {

    override suspend fun deleteAllMessagesForItem(itemId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (itemId.isBlank()) return@withContext Result.success(Unit)
        runCatching {
            val db = AppwriteConfig.DATABASE_ID.trim()
            require(db.isNotEmpty())
            while (true) {
                val batch = databases.listDocuments(
                    databaseId = db,
                    collectionId = AppwriteConfig.COLLECTION_MESSAGES,
                    queries = listOf(
                        Query.equal("itemId", itemId),
                        Query.limit(100),
                    ),
                )
                if (batch.documents.isEmpty()) break
                for (doc in batch.documents) {
                    runCatching {
                        databases.deleteDocument(
                            databaseId = db,
                            collectionId = AppwriteConfig.COLLECTION_MESSAGES,
                            documentId = doc.id,
                        )
                    }
                }
            }
        }
    }
}
