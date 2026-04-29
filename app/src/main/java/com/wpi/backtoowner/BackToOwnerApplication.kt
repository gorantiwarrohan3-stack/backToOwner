package com.wpi.backtoowner

import android.app.Application
import android.util.Log
import com.wpi.backtoowner.geofence.GeofencingSupport
import com.wpi.backtoowner.notifications.ChatMessageNotificationManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BackToOwnerApplication : Application() {
    @Inject
    lateinit var chatMessageNotificationManager: ChatMessageNotificationManager

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.APPWRITE_PROJECT_ID.isBlank()) {
            Log.e(
                "BackToOwner",
                "Appwrite project id is empty. Add appwrite.properties in the project root " +
                    "(see committed team file or appwrite.properties.example), set appwrite.projectId, rebuild.",
            )
        }
        GeofencingSupport.ensureRegisteredIfPermitted(this)
        chatMessageNotificationManager.start()
    }
}
