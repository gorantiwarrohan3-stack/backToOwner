package com.wpi.backtoowner

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class BackToOwnerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        @Suppress("DEPRECATION")
        val versionLabel = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        // OSM tile policy: identify the app (https://operations.osmfoundation.org/policies/tiles/)
        Configuration.getInstance().userAgentValue = "$packageName/$versionLabel"
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
    }
}
