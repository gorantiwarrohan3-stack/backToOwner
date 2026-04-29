package com.wpi.backtoowner.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.wpi.backtoowner.MainActivity
import com.wpi.backtoowner.R
import com.wpi.backtoowner.di.AppServicesEntryPoint
import dagger.hilt.android.EntryPointAccessors

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        GeofencingSupport.ensureNotificationChannel(context)
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return
        val first = event.triggeringGeofences?.firstOrNull() ?: return
        val zoneName = GeofencingSupport.zoneNameById(first.requestId) ?: "WPI safe zone"
        runCatching {
            val app = context.applicationContext
            EntryPointAccessors.fromApplication(app, AppServicesEntryPoint::class.java)
                .inAppNotificationStore()
                .appendGeofence(zoneName)
        }
        showNotification(context, zoneName)
    }
}

private fun showNotification(context: Context, zoneName: String) {
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    val openAppIntent = Intent(context, MainActivity::class.java)
    val pending: PendingIntent = TaskStackBuilder.create(context).run {
        addNextIntentWithParentStack(openAppIntent)
        getPendingIntent(4211, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            ?: return
    }
    val notif = NotificationCompat.Builder(context, GEOFENCE_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("You entered $zoneName")
        .setContentText("Check nearby lost/found listings or message a match now.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(pending)
        .build()
    NotificationManagerCompat.from(context).notify(zoneName.hashCode(), notif)
}
