package com.wpi.backtoowner.ui.screens.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wpi.backtoowner.ui.components.BrandAppHeaderTitleRow
import com.wpi.backtoowner.BuildConfig
import com.wpi.backtoowner.domain.model.PostType
import com.wpi.backtoowner.ui.theme.WpiFoundPin
import com.wpi.backtoowner.ui.theme.WpiHeaderMaroon
import com.wpi.backtoowner.ui.theme.WpiLostPin
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

private val MapFieldOnLight = Color(0xFF1A1A1A)
private val MapFieldMuted = Color(0xFF5C5C5C)

private enum class MapPostFilter { ALL, LOST, FOUND }

@Composable
private fun mapSearchFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MapFieldOnLight,
    unfocusedTextColor = MapFieldOnLight,
    focusedLabelColor = MapFieldMuted,
    unfocusedLabelColor = MapFieldMuted,
    focusedPlaceholderColor = MapFieldMuted,
    unfocusedPlaceholderColor = MapFieldMuted,
    cursorColor = WpiHeaderMaroon,
    focusedLeadingIconColor = MapFieldMuted,
    unfocusedLeadingIconColor = MapFieldMuted,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedBorderColor = WpiHeaderMaroon,
    unfocusedBorderColor = Color(0xFFCCCCCC),
)

@Composable
fun MapScreen(
    onOpenPost: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    var search by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf(MapPostFilter.ALL) }
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    var legendCollapsed by rememberSaveable { mutableStateOf(screenHeightDp < 760) }

    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val filteredPosts = remember(search, posts, typeFilter) {
        val q = search.trim().lowercase()
        posts.filter { p ->
            val matchesType = when (typeFilter) {
                MapPostFilter.ALL -> true
                MapPostFilter.LOST -> p.type == PostType.LOST
                MapPostFilter.FOUND -> p.type == PostType.FOUND
            }
            val matchesSearch = q.isEmpty() ||
                p.title.lowercase().contains(q) ||
                p.description.lowercase().contains(q)
            matchesType && matchesSearch
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(42.2742, -71.8064), 14.5f)
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = WpiHeaderMaroon,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandAppHeaderTitleRow(modifier = Modifier.fillMaxWidth())
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search lost or found items...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = mapSearchFieldColors(),
                )
                Surface(
                    color = WpiHeaderMaroon,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    IconButton(
                        onClick = {
                            typeFilter = when (typeFilter) {
                                MapPostFilter.ALL -> MapPostFilter.LOST
                                MapPostFilter.LOST -> MapPostFilter.FOUND
                                MapPostFilter.FOUND -> MapPostFilter.ALL
                            }
                        },
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.White)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapFilterChip(
                    label = "All",
                    selected = typeFilter == MapPostFilter.ALL,
                    onClick = { typeFilter = MapPostFilter.ALL },
                    modifier = Modifier.weight(1f),
                )
                MapFilterChip(
                    label = "Lost",
                    selected = typeFilter == MapPostFilter.LOST,
                    onClick = { typeFilter = MapPostFilter.LOST },
                    modifier = Modifier.weight(1f),
                )
                MapFilterChip(
                    label = "Found",
                    selected = typeFilter == MapPostFilter.FOUND,
                    onClick = { typeFilter = MapPostFilter.FOUND },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (!BuildConfig.HAS_MAPS_API_KEY) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = Color(0xFFFFF3CD),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    "Maps SDK key missing. Add maps.apiKey in project-root secrets.properties " +
                        "(Google Cloud Console → APIs & Services → Credentials; enable Maps SDK for Android " +
                        "and restrict key to package com.wpi.backtoowner), then rebuild.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF664D03),
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
        ) {
            GooglePostMap(
                posts = filteredPosts,
                cameraPositionState = cameraPositionState,
                onOpenPost = onOpenPost,
                modifier = Modifier.fillMaxSize(),
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp,
            ) {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Layers, contentDescription = "Layers")
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp,
            ) {
                IconButton(
                    onClick = {
                        val fine = Manifest.permission.ACCESS_FINE_LOCATION
                        when {
                            ContextCompat.checkSelfPermission(context, fine) ==
                                PackageManager.PERMISSION_GRANTED -> {
                                scope.launch {
                                    val loc = viewModel.lastKnownLocation() ?: return@launch
                                    runCatching {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLng(LatLng(loc.first, loc.second)),
                                        )
                                    }
                                }
                            }
                            else -> locationPermissionLauncher.launch(fine)
                        }
                    },
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Locate me", tint = WpiHeaderMaroon)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
                    .padding(12.dp),
            ) {
                Text(
                    "Map Legend · ${filteredPosts.size} with location",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(
                    onClick = { legendCollapsed = !legendCollapsed },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Icon(
                        imageVector = if (legendCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = if (legendCollapsed) "Expand legend" else "Collapse legend",
                        tint = Color(0xFF333333),
                    )
                }
                Spacer(Modifier.height(8.dp))
                LegendRow(color = WpiLostPin, label = "Lost")
                LegendRow(color = WpiFoundPin, label = "Found")
                LegendRow(color = Color(0xFFFFC107), label = "Safe Zone: Library Desk")
                if (!legendCollapsed) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Listings on map",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF222222),
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = if (screenHeightDp < 760) 120.dp else 170.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(top = 4.dp),
                    ) {
                        if (filteredPosts.isEmpty()) {
                            Text(
                                "No mapped listings for current search.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF666666),
                            )
                        } else {
                            filteredPosts.forEach { post ->
                                LegendPostRow(
                                    title = post.title,
                                    typeLabel = if (post.type.name == "LOST") "Lost" else "Found",
                                    lat = post.latitude,
                                    lng = post.longitude,
                                    color = if (post.type.name == "LOST") WpiLostPin else WpiFoundPin,
                                    onOpen = { onOpenPost(post.id) },
                                )
                            }
                        }
                    }
                }
                Text(
                    "© Google Maps",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF222222))
    }
}

@Composable
private fun MapFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) WpiHeaderMaroon else Color.White,
        shadowElevation = 1.dp,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = if (selected) Color.White else Color(0xFF2D2D2D),
            )
        }
    }
}

@Composable
private fun LegendPostRow(
    title: String,
    typeLabel: String,
    lat: Double,
    lng: Double,
    color: Color,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "$typeLabel: $title",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF1D1D1D),
                fontWeight = FontWeight.Medium,
            )
            Text(
                String.format("Lat %.4f, Lng %.4f", lat, lng),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF666666),
            )
        }
        AssistChip(
            onClick = onOpen,
            label = {
                Text(
                    "Open",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                labelColor = WpiHeaderMaroon,
                containerColor = Color(0xFFF5F0F1),
            ),
        )
    }
}
