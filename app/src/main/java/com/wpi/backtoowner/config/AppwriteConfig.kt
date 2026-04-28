package com.wpi.backtoowner.config

import com.wpi.backtoowner.BuildConfig

/**
 * Appwrite settings from root **`appwrite.properties`** (Gradle → [BuildConfig]).
 * This repo may commit that file for shared class builds; forks can use **`appwrite.properties.example`**.
 *
 * **Collections:** `posts`, `messages`, `user_profiles` — attributes and permissions per your Appwrite Console setup.
 *
 * **`messages`:** string attributes `itemId`, `senderUserId`, `senderName`, `senderRole`, `body`.
 * Add a **key/index** on `itemId` if list/query by item fails. Collection needs **Create** (and **Read**) for **Users**
 * (or rules you intend). Storage bucket must allow **Read** for image URLs to load in the app.
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
