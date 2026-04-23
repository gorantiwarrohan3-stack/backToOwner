package com.wpi.backtoowner.config

import com.wpi.backtoowner.BuildConfig

/**
 * Appwrite settings. Fill in root **`appwrite.properties`** (copy from **`appwrite.properties.example`**).
 * That file is gitignored; never commit real IDs or keys.
 *
 * **Collections:** `posts`, `messages`, `user_profiles` — attributes and permissions per your Appwrite Console setup.
 */
object AppwriteConfig {

    /** Posts body attribute id; must match Console exactly. */
    const val POST_DESCRIPTION_ATTR: String = "description"

    val ENDPOINT: String = BuildConfig.APPWRITE_ENDPOINT
    val PROJECT_ID: String = BuildConfig.APPWRITE_PROJECT_ID
    val DATABASE_ID: String = BuildConfig.APPWRITE_DATABASE_ID

    const val COLLECTION_POSTS: String = "posts"
    const val COLLECTION_MESSAGES: String = "messages"
    const val COLLECTION_USER_PROFILES: String = "user_profiles"

    val STORAGE_BUCKET_ID: String = BuildConfig.APPWRITE_STORAGE_BUCKET_ID
}
