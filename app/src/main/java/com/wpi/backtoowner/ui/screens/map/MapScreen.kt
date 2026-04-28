package com.wpi.backtoowner.ui.screens.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wpi.backtoowner.ui.components.BrandAppHeaderTitleRow
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
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    var search by remember { mutableStateOf("") }
    val draftAddress by viewModel.foundLocationDraft.address.collectAsStateWithLifecycle()
    var addressInput by remember { mutableStateOf("") }
    LaunchedEffect(draftAddress) {
        addressInput = draftAddress
    }

    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val filteredPosts = remember(search, posts) {
        val q = search.trim().lowercase()
        if (q.isEmpty()) posts
        else {
            posts.filter { p ->
                p.title.lowercase().contains(q) || p.description.lowercase().contains(q)
            }
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
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.White)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                "Found an item? Add where you saw it",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = addressInput,
                onValueChange = { addressInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Gordon Library, 2nd floor study pods") },
                label = { Text("Address / building / room") },
                minLines = 2,
                shape = RoundedCornerShape(12.dp),
                colors = mapSearchFieldColors(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.foundLocationDraft.setAddress(addressInput.trim()) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WpiHeaderMaroon, contentColor = Color.White),
            ) {
                Text("Save for Found report")
            }
            if (draftAddress.isNotBlank()) {
                Text(
                    "Saved: $draftAddress",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 88.dp),
        ) {
            GooglePostMap(
                posts = filteredPosts,
                cameraPositionState = cameraPositionState,
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
                Text("Map Legend", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LegendRow(color = WpiLostPin, label = "Lost")
                LegendRow(color = WpiFoundPin, label = "Found")
                LegendRow(color = Color(0xFFFFC107), label = "Safe Zone: Library Desk")
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
