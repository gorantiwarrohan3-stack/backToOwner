package com.wpi.backtoowner.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wpi.backtoowner.ui.components.MainHeaderTrailingIcons
import com.wpi.backtoowner.ui.components.NetworkImageWithLoader
import com.wpi.backtoowner.ui.util.categoryIconForItemTitle
import com.wpi.backtoowner.domain.model.AiMatchCandidate
import com.wpi.backtoowner.domain.model.Post
import com.wpi.backtoowner.domain.model.PostType
import com.wpi.backtoowner.ui.theme.WpiHeaderMaroon
import com.wpi.backtoowner.ui.theme.WpiMatchGreen
import com.wpi.backtoowner.ui.theme.WpiMessageBlue
import com.wpi.backtoowner.ui.util.TimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    itemId: String,
    onBack: () -> Unit,
    onMessageFounder: () -> Unit,
    onOpenSuggestedMatch: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = hiltViewModel(key = itemId),
) {
    val post by viewModel.post.collectAsStateWithLifecycle()
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val suggestedMatchesLoading by viewModel.suggestedMatchesLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var showClaimHelp by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        viewModel.load(itemId)
    }

    if (showClaimHelp && post != null) {
        val p = post
        AlertDialog(
            onDismissRequest = { showClaimHelp = false },
            title = { Text("Claim this item") },
            text = {
                Text(
                    if (p?.type == PostType.FOUND) {
                        "You are viewing a found listing. Tap this when you believe the item is yours. " +
                            "Message the finder to verify details (where you lost it, photos, serial numbers) and arrange pickup."
                    } else {
                        "This shortcut is for found listings. For a lost post, use Message to reach the person who reported it."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { showClaimHelp = false }) {
                    Text("OK")
                }
            },
            dismissButton = if (p?.type == PostType.FOUND) {
                {
                    TextButton(
                        onClick = {
                            showClaimHelp = false
                            viewModel.registerChatThreadForInbox()
                            onMessageFounder()
                        },
                    ) {
                        Text("Open messages")
                    }
                }
            } else {
                null
            },
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color(0xFF0E0E0E),
        topBar = {
            TopAppBar(
                title = { Text("Item details", color = Color.White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    MainHeaderTrailingIcons()
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WpiHeaderMaroon,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            if (post != null) {
                val listing = post!!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (listing.type == PostType.FOUND) {
                        Button(
                            onClick = { showClaimHelp = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WpiMatchGreen),
                        ) {
                            Text("This is mine!", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Button(
                        onClick = {
                            viewModel.registerChatThreadForInbox()
                            onMessageFounder()
                        },
                        modifier = Modifier
                            .then(
                                if (listing.type == PostType.FOUND) {
                                    Modifier.weight(1f)
                                } else {
                                    Modifier.fillMaxWidth()
                                },
                            )
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WpiMessageBlue),
                    ) {
                        Text(
                            text = if (listing.type == PostType.LOST) {
                                "Message owner"
                            } else {
                                "Message Founder"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        when {
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(error!!, color = Color(0xFFFFB4AB))
                }
            }

            post == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                val p = post!!
                DetailBody(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                    post = p,
                    matches = matches,
                    matchesLoading = suggestedMatchesLoading,
                    onSuggestedMatchClick = onOpenSuggestedMatch,
                )
            }
        }
    }
}

@Composable
private fun DetailBody(
    post: Post,
    matches: List<AiMatchCandidate>,
    matchesLoading: Boolean,
    onSuggestedMatchClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color(0xFF0E0E0E))
            .padding(bottom = 8.dp),
    ) {
        val url = post.imageUrl.takeIf { it.isNotBlank() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (url != null) {
                NetworkImageWithLoader(
                    model = url,
                    contentDescription = null,
                    compositionKey = post.id,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = categoryIconForItemTitle(post.title),
                        contentDescription = post.title,
                        modifier = Modifier.size(88.dp),
                        tint = WpiHeaderMaroon.copy(alpha = 0.9f),
                    )
                }
            }
        }
        Text(
            text = "Item Details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        val timeLabel = when (post.type) {
            PostType.LOST -> "Reported"
            PostType.FOUND -> "Posted"
        }
        DetailRow(
            icon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFB0B0B0)) },
            text = "$timeLabel: ${TimeFormatter.relative(post.createdAtEpochMs)}",
        )
        DetailRow(
            icon = { Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFB0B0B0)) },
            text = "Location: WPI area (${String.format(Locale.US, "%.4f, %.4f", post.latitude, post.longitude)})",
        )
        DetailRow(
            icon = { Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFFB0B0B0)) },
            text = "Description: ${post.description.ifBlank { post.title }}",
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Suggested matches",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Text(
            text = when {
                matchesLoading -> "Finding similar listings…"
                matches.isEmpty() -> "No similar listings found yet."
                else -> "${matches.size} listing(s) with overlapping details:"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFB8B8B8),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        when {
            matchesLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = WpiMatchGreen,
                        trackColor = Color(0xFF2A2A2A),
                    )
                }
            }
            matches.isNotEmpty() -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    itemsIndexed(
                        matches,
                        key = { index, m -> "${m.candidatePostId}_$index" },
                    ) { _, m ->
                        MatchCard(
                            match = m,
                            onClick = { onSuggestedMatchClick(m.candidatePostId) },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun DetailRow(
    icon: @Composable () -> Unit,
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.Center) { icon() }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE0E0E0))
    }
}

@Composable
private fun MatchCard(
    match: AiMatchCandidate,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1C1C1C))
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        if (match.imageUrl.isNotBlank()) {
            NetworkImageWithLoader(
                model = match.imageUrl,
                contentDescription = match.label,
                compositionKey = match.candidatePostId,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = categoryIconForItemTitle(match.label),
                    contentDescription = match.label,
                    modifier = Modifier.size(40.dp),
                    tint = Color(0xFFB8B8B8),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${match.label} (${match.matchPercent}%)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Color.White,
        )
    }
}
