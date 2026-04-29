package com.wpi.backtoowner.data.repository

import com.wpi.backtoowner.config.AppwriteConfig
import com.wpi.backtoowner.domain.model.NewPost
import com.wpi.backtoowner.domain.model.Post
import com.wpi.backtoowner.domain.model.PostType
import com.wpi.backtoowner.domain.repository.PostRepository
import io.appwrite.ID
import io.appwrite.Permission
import io.appwrite.Query
import io.appwrite.Role
import io.appwrite.models.Document
import io.appwrite.services.Account
import io.appwrite.services.Databases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

private const val FIELD_TITLE = "title"
private val fieldDescription: String get() = AppwriteConfig.POST_DESCRIPTION_ATTR
private const val FIELD_IMAGE_URL = "imageURL"
private const val FIELD_LATITUDE = "latitude"
private const val FIELD_LONGITUDE = "longitude"
private const val FIELD_TYPE = "type"
private const val FIELD_MATCH_PERCENT = "matchPercent"
private const val FIELD_POSTER_USER_ID = "posterUserId"
private const val FIELD_POSTER_DISPLAY_NAME = "posterDisplayName"
private const val FIELD_RESOLVED = "resolved"
private const val FIELD_ANCHOR_POST_ID = "anchorPostId"
private const val FIELD_CANDIDATE_POST_ID = "candidatePostId"
private const val FIELD_SIMILARITY = "similarity"

@Singleton
class AppwritePostRepository @Inject constructor(
    private val databases: Databases,
    private val account: Account,
) : PostRepository {

    private fun requireDb(): String {
        val id = AppwriteConfig.DATABASE_ID.trim()
        require(id.isNotEmpty()) {
            "Set AppwriteConfig.DATABASE_ID to your Appwrite database ID (Console → Databases)."
        }
        return id
    }

    override suspend fun getPosts(): Result<List<Post>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = databases.listDocuments(
                databaseId = requireDb(),
                collectionId = AppwriteConfig.COLLECTION_POSTS,
                queries = listOf(
                    Query.orderDesc("\$createdAt"),
                    Query.limit(100),
                ),
            )
            response.documents.mapNotNull { it.toPost() }
        }
    }

    override suspend fun getPost(documentId: String): Result<Post> = withContext(Dispatchers.IO) {
        runCatching {
            val doc = databases.getDocument(
                databaseId = requireDb(),
                collectionId = AppwriteConfig.COLLECTION_POSTS,
                documentId = documentId,
            )
            doc.toPost() ?: error("Invalid post document")
        }
    }

    override fun observePosts(): Flow<Result<List<Post>>> = flow {
        while (coroutineContext.isActive) {
            emit(getPosts())
            delay(4_000L)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun createPost(newPost: NewPost): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val poster = account.get()
            val posterLabel = poster.name.trim().ifBlank {
                poster.email.substringBefore("@").ifBlank { "WPI user" }
            }
            val userId = poster.id
            val permissions = listOf(
                Permission.read(Role.any()),
                Permission.update(Role.user(userId)),
                Permission.delete(Role.user(userId)),
            )
            val baseData = buildMap<String, Any> {
                put(FIELD_TITLE, newPost.title)
                put(fieldDescription, newPost.description)
                put(FIELD_IMAGE_URL, newPost.imageUrl)
                put(FIELD_LATITUDE, newPost.latitude)
                put(FIELD_LONGITUDE, newPost.longitude)
                put(FIELD_TYPE, newPost.type.name)
                newPost.matchPercent?.let { put(FIELD_MATCH_PERCENT, it) }
            }
            val withPoster = baseData.toMutableMap().apply {
                put(FIELD_POSTER_USER_ID, poster.id)
                put(FIELD_POSTER_DISPLAY_NAME, posterLabel)
            }
            val docId = ID.unique()
            val payloadForArchive: Map<String, Any> = try {
                databases.createDocument(
                    databaseId = requireDb(),
                    collectionId = AppwriteConfig.COLLECTION_POSTS,
                    documentId = docId,
                    data = withPoster,
                    permissions = permissions,
                )
                withPoster
            } catch (e: Exception) {
                if (!isUnknownPosterAttributeError(e)) throw e
                databases.createDocument(
                    databaseId = requireDb(),
                    collectionId = AppwriteConfig.COLLECTION_POSTS,
                    documentId = docId,
                    data = baseData,
                    permissions = permissions,
                )
                baseData
            }
            try {
                databases.createDocument(
                    databaseId = requireDb(),
                    collectionId = AppwriteConfig.COLLECTION_POSTS_ARCHIVE,
                    documentId = docId,
                    data = payloadForArchive,
                    permissions = permissions,
                )
            } catch (e: Exception) {
                runCatching {
                    databases.deleteDocument(
                        databaseId = requireDb(),
                        collectionId = AppwriteConfig.COLLECTION_POSTS,
                        documentId = docId,
                    )
                }
                throw e
            }
            docId
        }
    }

    override suspend fun deletePost(documentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Only remove from live feed; [posts_archive] keeps the historical row for analytics.
            databases.deleteDocument(
                databaseId = requireDb(),
                collectionId = AppwriteConfig.COLLECTION_POSTS,
                documentId = documentId,
            )
            Unit
        }
    }

    override suspend fun getArchivedPosts(): Result<List<Post>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = databases.listDocuments(
                databaseId = requireDb(),
                collectionId = AppwriteConfig.COLLECTION_POSTS_ARCHIVE,
                queries = listOf(
                    Query.orderDesc("\$createdAt"),
                    Query.limit(500),
                ),
            )
            response.documents.mapNotNull { it.toPost() }
        }
    }

    override suspend fun recordFeedMatchResult(
        anchorPostId: String,
        candidatePostId: String,
        similarity: Int,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = account.get().id
            databases.createDocument(
                databaseId = requireDb(),
                collectionId = AppwriteConfig.COLLECTION_FEED_MATCHES,
                documentId = ID.unique(),
                data = mapOf(
                    FIELD_ANCHOR_POST_ID to anchorPostId,
                    FIELD_CANDIDATE_POST_ID to candidatePostId,
                    FIELD_SIMILARITY to similarity.coerceIn(0, 100),
                ),
                permissions = listOf(
                    Permission.read(Role.any()),
                    Permission.update(Role.user(uid)),
                    Permission.delete(Role.user(uid)),
                ),
            )
            Unit
        }
    }

    override suspend fun loadLatestCrossMatchScores(): Result<Map<String, Int>> = withContext(Dispatchers.IO) {
        runCatching {
            val latestResp = databases.listDocuments(
                databaseId = requireDb(),
                collectionId = AppwriteConfig.COLLECTION_FEED_MATCHES,
                queries = listOf(
                    Query.orderDesc("\$createdAt"),
                    Query.limit(1),
                ),
            )
            val first = latestResp.documents.firstOrNull()
            val anchor =
                ((first?.data as? Map<*, *>)?.get(FIELD_ANCHOR_POST_ID))?.toString()?.trim().orEmpty()
            if (anchor.isEmpty()) return@runCatching emptyMap()

            val batch = databases.listDocuments(
                databaseId = requireDb(),
                collectionId = AppwriteConfig.COLLECTION_FEED_MATCHES,
                queries = listOf(
                    Query.equal(FIELD_ANCHOR_POST_ID, anchor),
                    Query.limit(100),
                ),
            )
            batch.documents.mapNotNull { doc ->
                val map = doc.data as? Map<String, Any?> ?: return@mapNotNull null
                val cid = map[FIELD_CANDIDATE_POST_ID]?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val pct = when (val v = map[FIELD_SIMILARITY]) {
                    is Number -> v.toInt().coerceIn(0, 100)
                    else -> null
                } ?: return@mapNotNull null
                cid to pct
            }.toMap()
        }
    }

    override fun observeCrossMatchScores(): Flow<Map<String, Int>> = flow {
        while (coroutineContext.isActive) {
            emit(loadLatestCrossMatchScores().getOrElse { emptyMap() })
            delay(4_000L)
        }
    }.flowOn(Dispatchers.IO)
}

private fun parsePostType(raw: String): PostType {
    val normalized = raw.trim().uppercase()
    return when (normalized) {
        PostType.FOUND.name -> PostType.FOUND
        PostType.LOST.name -> PostType.LOST
        else -> runCatching { PostType.valueOf(raw.trim()) }.getOrElse { PostType.LOST }
    }
}

/** Matches `user:` role inside permission strings like `update("user:64ab…")`. */
private val USER_ID_IN_PERMISSION = Regex("""user:([^"/\)\s]+)""")

private fun userIdsFromPermissions(permissions: List<String>): Set<String> {
    val out = mutableSetOf<String>()
    for (perm in permissions) {
        USER_ID_IN_PERMISSION.findAll(perm).forEach { m ->
            val id = m.groupValues[1].substringBefore('/').trim()
            if (id.isNotEmpty()) out.add(id)
        }
    }
    return out
}

private fun normalizeImageUrl(raw: String?): String {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) return ""
    if (value.startsWith("http://") || value.startsWith("https://")) return value
    // Path-only view URLs (some clients store `/v1/storage/...`).
    if (value.startsWith("/v1/")) {
        val base = AppwriteConfig.ENDPOINT.trimEnd('/')
        return base + value
    }
    // Backward compatibility: some earlier rows may have stored only the Appwrite fileId.
    val base = AppwriteConfig.ENDPOINT.trimEnd('/')
    val bucket = AppwriteConfig.STORAGE_BUCKET_ID.trim()
    val project = AppwriteConfig.PROJECT_ID.trim()
    if (bucket.isBlank() || project.isBlank()) return ""
    return "${base}/storage/buckets/${bucket}/files/${value}/view?project=${project}"
}

/** Appwrite returns this until `posterUserId` / `posterDisplayName` exist on the collection. */
private fun isUnknownPosterAttributeError(e: Throwable): Boolean {
    val msg = buildString {
        append(e.message.orEmpty())
        var c: Throwable? = e.cause
        while (c != null) {
            append(' ')
            append(c.message.orEmpty())
            c = c.cause
        }
    }
    if (!msg.contains("Unknown attribute", ignoreCase = true)) return false
    return msg.contains(FIELD_POSTER_USER_ID, ignoreCase = true) ||
        msg.contains(FIELD_POSTER_DISPLAY_NAME, ignoreCase = true)
}

@Suppress("UNCHECKED_CAST")
private fun Document<*>.toPost(): Post? {
    val map = data as? Map<String, Any?> ?: return null
    fun str(key: String) = map[key]?.toString()?.takeIf { it.isNotBlank() }
    fun dbl(key: String): Double? = when (val v = map[key]) {
        is Number -> v.toDouble()
        is String -> v.trim().replace(",", ".").toDoubleOrNull()
        else -> null
    }
    fun intOrNull(key: String) = (map[key] as? Number)?.toInt()
    fun boolResolved(): Boolean = when (val v = map[FIELD_RESOLVED]) {
        null -> false
        is Boolean -> v
        is Number -> v.toInt() != 0
        is String -> v.equals("true", ignoreCase = true) || v == "1"
        else -> false
    }

    val title = str(FIELD_TITLE) ?: return null
    val description = (str(fieldDescription) ?: str("description") ?: str("decription")).orEmpty()
    val imageUrl = normalizeImageUrl(
        str(FIELD_IMAGE_URL)
            ?: str("imageUrl")
            ?: str("image_url")
            ?: str("imageId")
            ?: str("fileId")
            ?: str("photoUrl")
            ?: str("photoURL")
            ?: str("image"),
    )
    val latitude = dbl(FIELD_LATITUDE) ?: return null
    val longitude = dbl(FIELD_LONGITUDE) ?: return null
    val typeRaw = str(FIELD_TYPE) ?: return null
    val type = parsePostType(typeRaw)
    val matchPercent = intOrNull(FIELD_MATCH_PERCENT)
    val createdMs = parseCreatedAt(createdAt)

    return Post(
        id = id,
        title = title,
        description = description,
        imageUrl = imageUrl,
        latitude = latitude,
        longitude = longitude,
        type = type,
        matchPercent = matchPercent,
        createdAtEpochMs = createdMs,
        posterUserId = str(FIELD_POSTER_USER_ID),
        posterDisplayName = str(FIELD_POSTER_DISPLAY_NAME),
        resolved = boolResolved(),
        permissionUserIds = userIdsFromPermissions(permissions),
    )
}

private fun parseCreatedAt(iso: String?): Long {
    if (iso.isNullOrBlank()) return 0L
    return try {
        Instant.parse(iso).toEpochMilli()
    } catch (_: DateTimeParseException) {
        0L
    }
}
