package com.wpi.backtoowner.ui.screens.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.wpi.backtoowner.domain.model.Post
import com.wpi.backtoowner.domain.model.PostType
import com.wpi.backtoowner.ui.theme.WpiFoundPin
import com.wpi.backtoowner.ui.theme.WpiLostPin
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private fun pinDrawable(context: Context, colorArgb: Int): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val size = (28 * d).toInt().coerceAtLeast(24)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorArgb }
    val r = size * 0.36f
    canvas.drawCircle(size / 2f, size / 2f, r, fill)
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * d
    }
    canvas.drawCircle(size / 2f, size / 2f, r, stroke)
    return BitmapDrawable(context.resources, bmp)
}

private fun zoomMapToPosts(mapView: MapView, posts: List<Post>) {
    when (posts.size) {
        0 -> {
            mapView.controller.setZoom(14.5)
            mapView.controller.setCenter(GeoPoint(42.2742, -71.8064))
        }
        1 -> {
            val p = posts.first()
            mapView.controller.setZoom(15.5)
            mapView.controller.setCenter(GeoPoint(p.latitude, p.longitude))
        }
        else -> {
            val pts = ArrayList(posts.map { GeoPoint(it.latitude, it.longitude) })
            runCatching {
                val box = BoundingBox.fromGeoPoints(pts)
                mapView.zoomToBoundingBox(box, true, 96)
            }.onFailure {
                val midLat = posts.map { it.latitude }.average()
                val midLon = posts.map { it.longitude }.average()
                mapView.controller.setZoom(14.0)
                mapView.controller.setCenter(GeoPoint(midLat, midLon))
            }
        }
    }
}

private fun syncPostMarkers(mapView: MapView, posts: List<Post>, context: Context) {
    runCatching {
        val markers = mapView.overlays.filterIsInstance<Marker>()
        mapView.overlays.removeAll(markers.toSet())
        val lostArgb = WpiLostPin.toArgb()
        val foundArgb = WpiFoundPin.toArgb()
        for (post in posts) {
            val m = Marker(mapView).apply {
                position = GeoPoint(post.latitude, post.longitude)
                title = post.title
                snippet = if (post.type == PostType.LOST) "Lost" else "Found"
                icon = pinDrawable(
                    context,
                    if (post.type == PostType.LOST) lostArgb else foundArgb,
                )
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
            mapView.overlays.add(m)
        }
        zoomMapToPosts(mapView, posts)
        mapView.invalidate()
    }
}

/**
 * OpenStreetMap via osmdroid. [mapHolder] holds the current [MapView] for the parent (e.g. recenter).
 *
 * Uses [MapView.setDestroyMode] `false` so [MapView.onDetach] is not run automatically on window detach;
 * we pair it with explicit [MapView.onPause]/[MapView.onDetach] in [DisposableEffect] to avoid double-detach
 * crashes when [AndroidView] also releases the view.
 */
@Composable
fun OsmPostMap(
    posts: List<Post>,
    mapHolder: Array<MapView?>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    /** Only re-run marker + zoom when feed data actually changes (stable order so polling does not reshuffle). */
    var lastSyncedPostKey by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner, mapHolder) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapHolder[0]?.let { runCatching { it.onResume() } }
                Lifecycle.Event.ON_PAUSE -> mapHolder[0]?.let { runCatching { it.onPause() } }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapHolder[0]?.let { mv ->
                runCatching { mv.onPause() }
                runCatching { mv.onDetach() }
            }
            mapHolder[0] = null
        }
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setDestroyMode(false)
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                minZoomLevel = 4.0
                maxZoomLevel = 19.0
                controller.setZoom(14.5)
                controller.setCenter(GeoPoint(42.2742, -71.8064))
            }
        },
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        onRelease = {
            // Do not clear [mapHolder] or call [MapView.onDetach] here: [onRelease] runs before
            // [DisposableEffect] onDispose; cleanup is handled there to avoid a double-detach.
        },
        update = { mapView ->
            mapHolder[0] = mapView
            if (mapView.tag != true) {
                mapView.tag = true
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    runCatching { mapView.onResume() }
                }
            }
            val postKey = posts
                .sortedBy { it.id }
                .joinToString(separator = "|") { p ->
                    "${p.id},${p.latitude},${p.longitude},${p.type},${p.title}"
                }
            if (postKey != lastSyncedPostKey) {
                lastSyncedPostKey = postKey
                syncPostMarkers(mapView, posts, context)
            }
        },
    )
}
