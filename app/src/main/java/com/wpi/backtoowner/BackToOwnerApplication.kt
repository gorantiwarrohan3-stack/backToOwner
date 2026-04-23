package com.wpi.backtoowner

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class BackToOwnerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.APPWRITE_PROJECT_ID.isBlank()) {
            Log.e(
                "BackToOwner",
                "Appwrite project id is empty. Add appwrite.properties in the project root " +
                    "(see committed team file or appwrite.properties.example), set appwrite.projectId, rebuild.",
            )
        }
        @Suppress("DEPRECATION")
        val versionLabel = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        // OSM tile policy: identify the app (https://operations.osmfoundation.org/policies/tiles/)
        Configuration.getInstance().userAgentValue = "$packageName/$versionLabel"
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
    }
}
