package com.wpi.backtoowner.domain.repository

/**
 * Deletes chat message documents for a listing thread ([itemId]).
 * Per-document deletes may fail for messages sent by another user depending on Appwrite permissions;
 * callers should still hide the thread locally if the user asked to remove it from the inbox.
 */
interface ChatMessagingRepository {

    suspend fun deleteAllMessagesForItem(itemId: String): Result<Unit>
}
