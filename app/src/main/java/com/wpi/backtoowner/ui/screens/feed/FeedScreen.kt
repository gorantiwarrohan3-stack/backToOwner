package com.wpi.backtoowner.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.wpi.backtoowner.domain.model.Post
import com.wpi.backtoowner.domain.model.PostType
import com.wpi.backtoowner.ui.theme.WpiCrimson
import com.wpi.backtoowner.ui.theme.WpiHeaderMaroon
import com.wpi.backtoowner.ui.theme.WpiOnCrimson
import com.wpi.backtoowner.ui.util.TimeFormatter

@Composable
fun FeedScreen(
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val search by viewModel.searchQuery.collectAsStateWithLifecycle()

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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("W", color = WpiHeaderMaroon, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "WPI BackToOwner",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FeedFilterChip(
                    label = "All",
                    selected = filter == FeedFilter.ALL,
                    onClick = { viewModel.setFilter(FeedFilter.ALL) },
                    modifier = Modifier.weight(1f),
                )
                FeedFilterChip(
                    label = "Lost",
                    selected = filter == FeedFilter.LOST,
                    onClick = { viewModel.setFilter(FeedFilter.LOST) },
                    modifier = Modifier.weight(1f),
                )
                FeedFilterChip(
                    label = "Found",
                    selected = filter == FeedFilter.FOUND,
                    onClick = { viewModel.setFilter(FeedFilter.FOUND) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = search,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search for items...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF2F2F2),
                    unfocusedContainerColor = Color(0xFFF2F2F2),
                ),
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (posts.isEmpty()) {
                item {
                    Text(
                        text = "No items match your filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else {
                items(posts, key = { it.id }) { post ->
                    DashboardPostCard(post = post, onClick = { onNavigateToDetail(post.id) })
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}

@Composable
private fun FeedFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = if (selected) WpiCrimson else Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 1.dp,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                color = if (selected) WpiOnCrimson else Color(0xFF333333),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun DashboardPostCard(
    post: Post,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val prefix = when (post.type) {
        PostType.LOST -> "Lost"
        PostType.FOUND -> "Found"
    }
    val titleLine = "$prefix: ${post.title}"
    val time = TimeFormatter.relative(post.createdAtEpochMs)
    val locationLine = String.format(
        Locale.US,
        "· %.4f, %.4f",
        post.latitude,
        post.longitude,
    )
    val match = post.matchPercent

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (post.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = post.title,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE0E0E0)),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 96.dp),
                ) {
                    Text(
                        text = titleLine,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111111),
                    )
                    Text(
                        text = time,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        text = locationLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 4.dp, end = 4.dp),
            ) {
                MatchBadge(matchPercent = match)
            }
        }
    }
}

@Composable
private fun MatchBadge(matchPercent: Int?) {
    val (bg, text) = when {
        matchPercent == null || matchPercent <= 0 -> Color(0xFF9E9E9E) to "No Match"
        matchPercent >= 70 -> WpiCrimson to "${matchPercent}% Match"
        else -> WpiCrimson to "${matchPercent}% Match"
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(20.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
