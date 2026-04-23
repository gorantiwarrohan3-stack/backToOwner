package com.wpi.backtoowner.ui.screens.createpost

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wpi.backtoowner.domain.model.PostType
import com.wpi.backtoowner.ui.theme.WpiHeaderMaroon
import com.wpi.backtoowner.ui.theme.WpiOnCrimson
import com.wpi.backtoowner.ui.util.categoryIconForItemTitle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    onPostSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreatePostViewModel = hiltViewModel(),
) {
    val category by viewModel.category.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val suggestedLabels by viewModel.suggestedLabels.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val previewBitmap by viewModel.previewBitmap.collectAsStateWithLifecycle()
    val smartTag by viewModel.smartTag.collectAsStateWithLifecycle()
    val postType by viewModel.postType.collectAsStateWithLifecycle()
    val isPosting by viewModel.isPosting.collectAsStateWithLifecycle()
    val postError by viewModel.postError.collectAsStateWithLifecycle()
    val locationHint by viewModel.locationHint.collectAsStateWithLifecycle()
    val mapFoundHint by viewModel.mapFoundAddressHint.collectAsStateWithLifecycle()
    val manualPlaceNote by viewModel.manualPlaceNote.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val onSurface = MaterialTheme.colorScheme.onSurface
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = onSurface,
        unfocusedTextColor = onSurface,
        focusedLabelColor = onSurface,
        unfocusedLabelColor = onSurface.copy(alpha = 0.75f),
        focusedPlaceholderColor = onSurface.copy(alpha = 0.55f),
        unfocusedPlaceholderColor = onSurface.copy(alpha = 0.55f),
        cursorColor = WpiHeaderMaroon,
        focusedBorderColor = WpiHeaderMaroon,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    )
    val canPost = !isAnalyzing && !isPosting && when (postType) {
        PostType.LOST -> category.isNotBlank() && description.isNotBlank()
        PostType.FOUND -> previewBitmap != null && category.isNotBlank() && description.isNotBlank()
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        if (bitmap != null) viewModel.onPhotoCaptured(bitmap)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) takePictureLauncher.launch(null)
    }
    val takePhoto = {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> takePictureLauncher.launch(null)
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.refreshDeviceLocation()
    }
    val requestLocation = {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED -> viewModel.refreshDeviceLocation()
            else -> locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                    title = {
                    Text("Create Post", fontWeight = FontWeight.Bold, color = Color(0xFF111111))
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = WpiHeaderMaroon)
                            Text("Back", color = WpiHeaderMaroon)
                        }
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.clearPostError()
                            viewModel.submitPost(onPostSuccess)
                        },
                        enabled = canPost,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WpiHeaderMaroon,
                            contentColor = WpiOnCrimson,
                            disabledContainerColor = WpiHeaderMaroon.copy(alpha = 0.45f),
                            disabledContentColor = WpiOnCrimson.copy(alpha = 0.75f),
                        ),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text("Post")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF111111),
                    navigationIconContentColor = WpiHeaderMaroon,
                    actionIconContentColor = WpiHeaderMaroon,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = previewBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = categoryIconForItemTitle(category.ifBlank { "Item" }),
                        contentDescription = category.ifBlank { "Item category" },
                        modifier = Modifier.size(120.dp),
                        tint = WpiHeaderMaroon.copy(alpha = 0.45f),
                    )
                }
                IconButton(
                    onClick = takePhoto,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(WpiHeaderMaroon),
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color.White)
                }
            }
            Text(
                if (postType == PostType.LOST) {
                    "Photo optional for lost items — add a clear description."
                } else {
                    "Photo required for found items."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = onSurface.copy(alpha = 0.72f),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            if (mapFoundHint.isNotBlank()) {
                Text(
                    "Map note will be attached: $mapFoundHint",
                    style = MaterialTheme.typography.bodySmall,
                    color = WpiHeaderMaroon,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 12.dp),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = postType == PostType.LOST,
                    onClick = { viewModel.onPostTypeChange(PostType.LOST) },
                    label = { Text("Lost") },
                    enabled = !isAnalyzing && !isPosting,
                )
                FilterChip(
                    selected = postType == PostType.FOUND,
                    onClick = { viewModel.onPostTypeChange(PostType.FOUND) },
                    label = { Text("Found") },
                    enabled = !isAnalyzing && !isPosting,
                )
            }

            if (isAnalyzing) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            smartTag?.let { match ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Text(
                            "Smart tag: ${match.category} (${(match.confidence * 100).toInt()}% confidence)",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Text(
                "Suggested Tags (MLKit)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                suggestedLabels.forEach { label ->
                    val tagsEnabled = !isAnalyzing && !isPosting
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.onSuggestedLabelSelected(label) },
                        label = { Text(label) },
                        enabled = tagsEnabled,
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = tagsEnabled,
                            selected = false,
                        ),
                        colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFFF5F5F5)),
                    )
                }
            }

            Text(
                "Location Picker",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = onSurface,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE8E4DC)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("WPI Campus", color = Color(0xFF555555), style = MaterialTheme.typography.titleMedium)
                    Text(
                        locationHint,
                        color = Color(0xFF666666),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 12.dp).padding(top = 4.dp),
                    )
                }
                OutlinedButton(
                    onClick = requestLocation,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, WpiHeaderMaroon),
                ) {
                    Text("Set Location", color = WpiHeaderMaroon)
                }
            }

            Text(
                "Place in words (optional)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = onSurface,
            )
            Text(
                "Use GPS above, type a building/room here, or both. Map tab text is still merged for Found posts.",
                style = MaterialTheme.typography.bodySmall,
                color = onSurface.copy(alpha = 0.78f),
            )
            OutlinedTextField(
                value = manualPlaceNote,
                onValueChange = viewModel::onManualPlaceNoteChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Building, room, or address") },
                placeholder = { Text("e.g. Gordon Library, study room 204") },
                minLines = 2,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
            )

            Text(
                "Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = onSurface,
            )
            OutlinedTextField(
                value = category,
                onValueChange = viewModel::onCategoryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. wallet, keys") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
            )

            Text(
                "Description",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = onSurface,
            )
            OutlinedTextField(
                value = description,
                onValueChange = viewModel::onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Describe the item and where you found it...") },
                minLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
            )

            postError?.let {
                Text(
                    it,
                    color = Color(0xFFFFB4AB),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = {
                    viewModel.clearPostError()
                    viewModel.submitPost(onPostSuccess)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = canPost,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WpiHeaderMaroon,
                    contentColor = WpiOnCrimson,
                    disabledContainerColor = WpiHeaderMaroon.copy(alpha = 0.45f),
                    disabledContentColor = WpiOnCrimson.copy(alpha = 0.75f),
                ),
            ) {
                Text(if (isPosting) "Posting…" else "Post to feed")
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}
