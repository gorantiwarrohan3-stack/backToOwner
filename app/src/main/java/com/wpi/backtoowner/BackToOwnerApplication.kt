package com.wpi.backtoowner

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

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
    }
}
