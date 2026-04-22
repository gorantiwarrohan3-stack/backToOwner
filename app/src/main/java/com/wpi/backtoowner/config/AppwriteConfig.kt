package com.wpi.backtoowner.config

/**
 * Appwrite project settings (Console → Settings / Databases / Storage).
 *
 * **Create in Appwrite Console:**
 *
 * 1. **Database** (any name). Copy its **Database ID** into [DATABASE_ID].
 *
 * 2. **Collection `posts`** — attributes (all optional unless noted):
 *    - `title` (string, required)
 *    - `description` (string) — attribute key must match [POST_DESCRIPTION_ATTR] (fix typos like `decription` in Console or here).
 *    - `imageURL` (string)
 *    - `latitude` (double)
 *    - `longitude` (double)
 *    - `type` (enum string: `LOST` or `FOUND`)
 *    - `matchPercent` (integer, 0–100; optional for feed badge)
 *    - `posterUserId` (string) — Appwrite user id of the author (set by the app on create)
 *    - `posterDisplayName` (string) — name or email local-part for chat labels (set on create)
 *      If these two are missing, the app still creates posts without them; add them in Console when you want names in chat.
 *    - `resolved` (boolean, optional) — when true, listing appears under Profile → Resolved instead of My Posts.
 *
 * **Map tab:** Uses OpenStreetMap via [osmdroid](https://github.com/osmdroid/osmdroid) (no Google Maps API key).
 * Follow [OSM tile usage policy](https://operations.osmfoundation.org/policies/tiles/) for production traffic.
 *
 * 3. **Collection `messages`** (for chat):
 *    - `itemId` (string) — links to post document
 *    - `senderUserId` (string)
 *    - `senderName` (string)
 *    - `senderRole` (string) — e.g. `Founder` / `Owner`
 *    - `body` (string)
 *    - `createdAt` (datetime) or rely on `$createdAt`
 *
 * 4. **Collection `user_profiles`** (optional; for reputation / geofence if you persist them):
 *    - `userId` (string, key)
 *    - `displayName` (string)
 *    - `reputation` (double)
 *    - `memberLevel` (string)
 *    - `geoAlertsEnabled` (boolean)
 *
 * **Permissions:** For a class project, set collection read to `role:all` on `posts` if the feed is public;
 * restrict `write` to `user:xxx` when you add rules. Messages should be read/write for participants only.
 *
 * **If posting shows "No permission provided for action 'create'":** the client session is fine; Appwrite is
 * blocking **creates** at the project level. In Console:
 * - **Storage** → your bucket → **Settings** → **Permissions** → add **Create** (and **Read** as needed) for
 *   **Users** (any authenticated user), so uploads from the app succeed.
 * - **Databases** → your database → collection **`posts`** → **Settings** → **Permissions** → add **Create**
 *   for **Users**, so `createDocument` succeeds. Keep **Read** for **Any** (or **Users**) if the feed should
 *   list posts.
 *
 * **Recommended collection permissions (Console → collection → Settings → Permissions):**
 *
 * **`messages`** — treat as private to a conversation (per `itemId` / participants).
 * - Turn **Row security** ON.
 * - **Table level:** **Create** = **Users** only (no broad **Read** at table level if you want strict privacy).
 * - **Per document** (when your app calls `createDocument`): grant **read** (and optional **update** and **delete**)
 *   only to participants, e.g. `read(Role.user(ownerUserId))`, `read(Role.user(otherUserId))`, plus
 *   **update** and **delete** for `Role.user(senderUserId)` if senders may edit or delete their own messages.
 * - **Class-project shortcut (not private):** **Read** + **Create** = **Users**; accept that any signed-in
 *   user could list all messages until you implement participant ACLs in code.
 *
 * **`user_profiles`** — one document per Appwrite user; only that user should edit their row.
 * - Turn **Row security** ON.
 * - **Table level:** **Create** = **Users** (each user creates their own profile once).
 * - **Per document:** e.g. **read** = **Any** or **Users** if names/levels are public; **update** and **delete**
 *   = Role.user scoped to the profile owner’s Appwrite account id (same value as the userId attribute).
 * - Avoid giving **Update** at table scope to **Users** without row ACLs, or anyone could overwrite anyone’s
 *   profile.
 */
object AppwriteConfig {

    /**
     * Posts collection attribute id for the body text. Must match Appwrite exactly.
     * Use `decription` only if your Console attribute is misspelled that way.
     */
    const val POST_DESCRIPTION_ATTR: String = "description"

    const val ENDPOINT: String = "https://fra.cloud.appwrite.io/v1"

    const val PROJECT_ID: String = "69c6f8fe001ea120b91e"

    /** Paste Database ID from Appwrite → Databases → your database. */
    const val DATABASE_ID: String = "69e7d01f001a305411ae"

    const val COLLECTION_POSTS: String = "posts"
    const val COLLECTION_MESSAGES: String = "messages"
    const val COLLECTION_USER_PROFILES: String = "user_profiles"

    const val STORAGE_BUCKET_ID: String = "69c6fc09000e30bd5313"
}
