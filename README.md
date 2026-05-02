# BackToOwner

Android app for the WPI community to report **lost** and **found** items, browse a unified feed, chat about listings, and see optional AI-assisted match hints. Built as a coursework project around mobile, maps, notifications, and cloud-backed data.

---

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **MVVM** with **Hilt** dependency injection
- **Appwrite**: authentication, database, storage, (optional) server functions
- **Google Maps** (Compose) for campus-focused map views
- **ML Kit Image Labeling** for on-device tagging suggestions after a photo is taken
- **Google GenAI (Gemini)** for similarity scoring between listings (text + optional images)

---

## Features (high level)

- **Auth**: Email/password via Appwrite; UI expects **@wpi.edu** addresses (enforce in Appwrite as well for production).
- **Feed**: Lost / found listings with search and filters.
- **Create post**: Photo capture or gallery upload, category/description, GPS location, ML Kit label chips.
- **Item detail**: Suggested matches powered by Gemini (titles + descriptions; photos included when URLs load).
- **Map**: Lost (red) and found (green) markers; safe-zone–style geofence notifications (WPI areas).
- **Chats**: Threads keyed by listing; notifications integration.
- **Insights** (archive): Dashboard over **`posts_archive`** with filters and simple charts.
- **Profile** and **notifications** screens.

---

## Prerequisites

- **Android Studio** (Koala or newer recommended) with Android SDK **35**
- **JDK 17**
- An **Appwrite** project (or access to the team’s existing project)
- Optional but recommended for full functionality:
  - **Google Maps API key** (Maps SDK for Android)
  - **Gemini API key** (for match scoring; app degrades gracefully if missing)

---

## Configuration

### 1. `appwrite.properties` (project root)

Copy from `appwrite.properties.example` if you are forking, then fill in:

| Key | Purpose |
|-----|--------|
| `appwrite.endpoint` | Your Appwrite API endpoint (region-specific) |
| `appwrite.projectId` | Project ID |
| `appwrite.databaseId` | Database ID |
| `appwrite.storageBucketId` | Storage bucket for listing images |

Gradle injects these into **`BuildConfig`** at compile time.

### 2. `secrets.properties` (project root, **not** committed)

This file is listed in `.gitignore`. Create it next to `appwrite.properties`:

```properties
maps.apiKey=YOUR_MAPS_SDK_KEY
gemini.apiKey=YOUR_GEMINI_API_KEY
```

- If `maps.apiKey` is empty, map-related features may be limited or disabled depending on build flags.
- If `gemini.apiKey` is empty, cross-listing match scoring and suggested matches are skipped (no crash).

---

## Appwrite backend (expected shape)

Per `AppwriteConfig` and repository code, plan for at least:

| Collection | Role |
|------------|------|
| `posts` | Live listings |
| `posts_archive` | Same attributes as `posts`; each new post is also written here with the **same document ID**; deletes only remove from `posts` so history remains for Insights |
| `messages` | Chat messages (`itemId`, sender fields, `body`, etc.) |
| `user_profiles` | Optional profile data |
| `feed_matches` | Optional: persisted Gemini scores (`anchorPostId`, `candidatePostId`, `similarity` 0–100) for consistent badges across devices |

**Storage**: A bucket for post images; documents should store a **file ID** or URL the app can load (see your existing attribute naming in Appwrite).

**Auth**: Configure email/password (and OAuth if you use Appwrite’s callback activity). Restrict registration to `@wpi.edu` in the console if required for your class policy.

---

## Build and run

1. Clone the repository.
2. Add `appwrite.properties` (and `secrets.properties` as above).
3. Open the project in Android Studio.
4. Sync Gradle, then **Run** on an emulator or device (**minSdk 26**).

```text
./gradlew :app:assembleDebug
```

---

## AI matching note

Listing comparison uses **Gemini** with **titles, descriptions, and optional images** for both sides. If an image is missing, scoring still runs on text. The model name is configured in `GeminiAiMatchingRepository` (e.g. `gemini-2.5-flash-lite`). You need a valid API key and model access for your Google AI project.

---

## Permissions

The app may request, depending on features used:

- `INTERNET`
- `CAMERA`
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`
- `POST_NOTIFICATIONS` (Android 13+)

---

## License and academic use

This repository is intended for **educational / coursework** use. Add a license file if you open-source it beyond class; coordinate with instructors on AI keys and Appwrite project ownership.

---

## Contributing (team forks)

Use feature branches and pull requests into `main`. **Never commit** real `secrets.properties` or production API keys.
