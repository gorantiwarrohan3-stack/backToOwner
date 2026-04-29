package com.wpi.backtoowner.ui.screens.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.wpi.backtoowner.domain.model.Post
import com.wpi.backtoowner.domain.model.PostType
import com.wpi.backtoowner.ui.theme.WpiFoundPin
import com.wpi.backtoowner.ui.theme.WpiLostPin
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun markerPositions(posts: List<Post>): List<Pair<Post, LatLng>> {
    val byKey = posts.groupBy { "${it.latitude},${it.longitude}" }
    return posts.map { post ->
        val group = byKey["${post.latitude},${post.longitude}"]!!
        if (group.size <= 1) {
            return@map post to LatLng(post.latitude, post.longitude)
        }
        val idx = group.indexOf(post).coerceAtLeast(0)
        val radius = 0.00015
        val angle = 2 * PI * idx / group.size
        post to LatLng(post.latitude + radius * sin(angle), post.longitude + radius * cos(angle))
    }
}

private fun pinDescriptor(context: Context, colorArgb: Int): BitmapDescriptor {
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
    return BitmapDescriptorFactory.fromBitmap(bmp)
}

private suspend fun zoomCameraToPosts(cameraPositionState: CameraPositionState, posts: List<Post>) {
    runCatching {
        when (posts.size) {
            0 -> cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(LatLng(42.2742, -71.8064), 14.5f),
                ),
            )
            1 -> {
                val p = posts.first()
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.fromLatLngZoom(LatLng(p.latitude, p.longitude), 15.5f),
                    ),
                )
            }
            else -> {
                val distinctKeys = posts.map { "${it.latitude},${it.longitude}" }.distinct()
                if (distinctKeys.size == 1) {
                    val p = posts.first()
                    cameraPositionState.animate(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(LatLng(p.latitude, p.longitude), 15.5f),
                        ),
                    )
                } else {
                    val bounds = LatLngBounds.builder().apply {
                        posts.forEach { include(LatLng(it.latitude, it.longitude)) }
                    }.build()
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 96))
                }
            }
        }
    }
}

@Composable
fun GooglePostMap(
    posts: List<Post>,
    cameraPositionState: CameraPositionState,
    onOpenPost: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val markerItems = remember(posts) { markerPositions(posts) }

    LaunchedEffect(posts) {
        zoomCameraToPosts(cameraPositionState, posts)
    }

    GoogleMap(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = false,
        ),
    ) {
        // BitmapDescriptorFactory requires Maps SDK to be initialized — must be called
        // inside the GoogleMap content lambda, never during outer composition.
        val lostIcon = remember(context) { pinDescriptor(context, WpiLostPin.toArgb()) }
        val foundIcon = remember(context) { pinDescriptor(context, WpiFoundPin.toArgb()) }

        markerItems.forEach { (post, latLng) ->
            Marker(
                state = MarkerState(position = latLng),
                title = post.title,
                snippet = if (post.type == PostType.LOST) "Lost · Tap info to open" else "Found · Tap info to open",
                icon = if (post.type == PostType.LOST) lostIcon else foundIcon,
                onInfoWindowClick = { onOpenPost(post.id) },
            )
        }
    }
}
