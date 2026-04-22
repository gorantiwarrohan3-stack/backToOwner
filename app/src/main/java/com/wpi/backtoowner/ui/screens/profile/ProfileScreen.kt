package com.wpi.backtoowner.ui.screens.profile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.wpi.backtoowner.domain.model.Post
import com.wpi.backtoowner.domain.model.PostType
import com.wpi.backtoowner.ui.theme.WpiCrimson
import com.wpi.backtoowner.ui.theme.WpiHeaderMaroon
import com.wpi.backtoowner.ui.util.TimeFormatter

private fun initialsFromName(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (parts.isEmpty()) return "?"
    return parts.take(2).map { it.first().uppercaseChar() }.joinToString("")
}

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onOpenPost: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profileState by viewModel.uiState.collectAsStateWithLifecycle()
    var geo by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF5F5F5)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "WPI",
                color = WpiCrimson,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = " | ",
                color = Color.LightGray,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Row {
                Text("BackTo", color = WpiCrimson, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("Owner", color = Color(0xFF555555), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (val s = profileState) {
                is ProfileUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = WpiHeaderMaroon)
                    }
                }
                is ProfileUiState.Error -> {
                    Column(Modifier.fillMaxWidth()) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text("Retry", color = WpiCrimson)
                        }
                    }
                }
                is ProfileUiState.Ready -> {
                    val user = s.user
                    val initials = initialsFromName(user.name)
                    Box {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDDDDDD)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                initials,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color(0xFF333333),
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = WpiCrimson,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(22.dp)
                                .background(Color.White, CircleShape),
                        )
                    }
                    Spacer(Modifier.size(16.dp))
                    Column {
                        Text(
                            user.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111111),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                user.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF666666),
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        when (val s = profileState) {
            is ProfileUiState.Ready -> {
                if (s.postsLoadFailed) {
                    Text(
                        text = "Could not refresh listings; showing profile only. Pull to retry from feed later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB00020),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
                ProfileSectionHeader(
                    icon = { Icon(Icons.Default.Description, null, tint = WpiHeaderMaroon) },
                    title = "My Posts",
                )
                ProfilePostCarousel(
                    posts = s.myActivePosts,
                    emptyMessage = "No active listings. Create one from the Report tab.",
                    onOpenPost = onOpenPost,
                )

                Spacer(Modifier.height(8.dp))
                ProfileSectionHeader(
                    icon = { Icon(Icons.Default.CheckCircle, null, tint = WpiHeaderMaroon) },
                    title = "Resolved",
                )
                ProfilePostCarousel(
                    posts = s.myResolvedPosts,
                    emptyMessage = "No resolved listings yet. When an item is returned to its owner, mark that post as resolved in your project database so it appears in this carousel.",
                    onOpenPost = onOpenPost,
                )
            }
            else -> Unit
        }

        Spacer(Modifier.height(16.dp))
        ProfileSectionHeader(icon = { Icon(Icons.Default.Settings, null, tint = WpiHeaderMaroon) }, title = "Settings")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("GeoFencing Alerts:", fontWeight = FontWeight.Bold)
                Switch(
                    checked = geo,
                    onCheckedChange = { geo = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = WpiHeaderMaroon,
                    ),
                )
            }
            Text(
                "Alert me if a lost item is found near my current location.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        TextButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Text("Log out", color = WpiCrimson, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfilePostCarousel(
    posts: List<Post>,
    emptyMessage: String,
    onOpenPost: (String) -> Unit,
) {
    if (posts.isEmpty()) {
        Text(
            text = emptyMessage,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF666666),
        )
        return
    }
    val pageCount = posts.size
    val pagerState = rememberPagerState(pageCount = { pageCount }, initialPage = 0)
    Column(Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 40.dp),
            pageSpacing = 16.dp,
        ) { page ->
            ProfilePostCard(post = posts[page], onClick = { onOpenPost(posts[page].id) })
        }
        if (pageCount > 1) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(pageCount) { i ->
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (pagerState.currentPage == i) 9.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (pagerState.currentPage == i) WpiHeaderMaroon else Color(0xFFCCCCCC)),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfilePostCard(
    post: Post,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.padding(12.dp)) {
            val url = post.imageUrl.takeIf { it.isNotBlank() }
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = post.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.2f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.2f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No photo", color = Color(0xFF888888), style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                post.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111111),
            )
            Text(
                if (post.type == PostType.LOST) "Lost" else "Found",
                style = MaterialTheme.typography.labelMedium,
                color = WpiHeaderMaroon,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                TimeFormatter.relative(post.createdAtEpochMs),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun ProfileSectionHeader(
    icon: @Composable () -> Unit,
    title: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        Spacer(Modifier.height(4.dp))
        Text(title, fontWeight = FontWeight.Bold, color = WpiHeaderMaroon)
    }
}
