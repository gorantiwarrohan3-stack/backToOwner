package com.wpi.backtoowner.geofence

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

private const val TAG = "GeofencingSupport"
const val GEOFENCE_CHANNEL_ID = "safe_zone_alerts"
private const val PREFS_GEOFENCE = "geofence_settings"
private const val KEY_GEOFENCE_ENABLED = "geofence_enabled"

private data class SafeZone(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Float,
)

private val SAFE_ZONES = listOf(
    SafeZone("gordon_library", "Gordon Library", 42.2742, -71.8064, 120f),
    SafeZone("campus_police", "Campus Police", 42.2750, -71.8058, 120f),
)

object GeofencingSupport {

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_GEOFENCE, Context.MODE_PRIVATE)
            .getBoolean(KEY_GEOFENCE_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_GEOFENCE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_GEOFENCE_ENABLED, enabled)
            .apply()
        if (enabled) {
            ensureRegisteredIfPermitted(context)
        } else {
            unregister(context)
        }
    }

    fun ensureRegisteredIfPermitted(context: Context) {
        ensureNotificationChannel(context)
        if (!isEnabled(context)) {
            unregister(context)
            return
        }
        if (!hasFineLocationPermission(context)) return
        val geofencingClient = LocationServices.getGeofencingClient(context)
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(
                SAFE_ZONES.map { zone ->
                    Geofence.Builder()
                        .setRequestId(zone.id)
                        .setCircularRegion(zone.lat, zone.lng, zone.radiusMeters)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                        .setLoiteringDelay(15_000)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .build()
                },
            )
            .build()
        geofencingClient
            .removeGeofences(geofencePendingIntent(context))
            .addOnCompleteListener {
                geofencingClient
                    .addGeofences(request, geofencePendingIntent(context))
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Could not register geofences: ${e.message}")
                    }
            }
    }

    fun unregister(context: Context) {
        LocationServices.getGeofencingClient(context)
            .removeGeofences(geofencePendingIntent(context))
            .addOnFailureListener { e ->
                Log.w(TAG, "Could not remove geofences: ${e.message}")
            }
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(GEOFENCE_CHANNEL_ID)
        if (existing != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                GEOFENCE_CHANNEL_ID,
                "Safe Zone Alerts",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Alerts when you enter WPI safe zones."
            },
        )
    }

    internal fun zoneNameById(id: String): String? = SAFE_ZONES.firstOrNull { it.id == id }?.name
}

private fun hasFineLocationPermission(context: Context): Boolean =
    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

private fun geofencePendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    return PendingIntent.getBroadcast(context, 7091, intent, flags)
}
