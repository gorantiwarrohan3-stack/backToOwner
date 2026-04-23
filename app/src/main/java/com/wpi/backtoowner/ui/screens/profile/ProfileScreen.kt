package com.wpi.backtoowner.ui.screens.profile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.wpi.backtoowner.domain.model.AuthUserSummary
import com.wpi.backtoowner.domain.model.Post
import com.wpi.backtoowner.domain.model.PostType
import com.wpi.backtoowner.ui.theme.WpiCrimson
import com.wpi.backtoowner.ui.theme.WpiHeaderMaroon
import com.wpi.backtoowner.ui.theme.WpiOnCrimson
import com.wpi.backtoowner.ui.util.TimeFormatter
import com.wpi.backtoowner.ui.util.categoryIconForItemTitle

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
    var showProfileEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Post?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }

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
            verticalAlignment = Alignment.Top,
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
                        Text(
                            user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        if (user.phone.isNotBlank()) {
                            Text(
                                user.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF666666),
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        TextButton(
                            onClick = { showProfileEditor = true },
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            Text("Edit profile details", color = WpiCrimson, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        if (profileState is ProfileUiState.Ready) {
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WpiHeaderMaroon,
                    contentColor = WpiOnCrimson,
                ),
            ) {
                Text("Log out", fontWeight = FontWeight.SemiBold)
            }
        }

        if (showProfileEditor && profileState is ProfileUiState.Ready) {
            ProfileEditDialog(
                user = (profileState as ProfileUiState.Ready).user,
                viewModel = viewModel,
                onDismiss = { showProfileEditor = false },
            )
        }

        pendingDelete?.let { post ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("Delete this listing?") },
                text = {
                    Text(
                        "Remove \"${post.title}\" from the feed? This cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deletePost(post.id) { result ->
                                pendingDelete = null
                                deleteError = result.exceptionOrNull()?.message
                            }
                        },
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text("Cancel")
                    }
                },
            )
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
                Text(
                    "One listing per card. Swipe sideways when you have more than one. Delete when the item is returned.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
                deleteError?.let { err ->
                    Text(
                        err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
                ProfileMyPostsList(
                    posts = s.myPosts,
                    emptyMessage = "No listings yet. Create one from the Report tab.",
                    onOpenPost = onOpenPost,
                    onRequestDelete = { post ->
                        deleteError = null
                        pendingDelete = post
                    },
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileMyPostsList(
    posts: List<Post>,
    emptyMessage: String,
    onOpenPost: (String) -> Unit,
    onRequestDelete: (Post) -> Unit,
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
    LaunchedEffect(posts.map { it.id }.joinToString()) {
        if (posts.isNotEmpty() && pagerState.currentPage >= posts.size) {
            pagerState.scrollToPage((posts.size - 1).coerceAtLeast(0))
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 12.dp,
        ) { page ->
            val post = posts[page]
            ProfileMyPostRow(
                post = post,
                onOpen = { onOpenPost(post.id) },
                onDelete = { onRequestDelete(post) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (pageCount > 1) {
            Text(
                text = "${pagerState.currentPage + 1} of $pageCount",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(pageCount) { i ->
                    Box(
                        modifier = Modifier
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
private fun ProfileMyPostRow(
    post: Post,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val url = post.imageUrl.takeIf { it.isNotBlank() }
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = post.title,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = categoryIconForItemTitle(post.title),
                        contentDescription = post.title,
                        modifier = Modifier.size(40.dp),
                        tint = WpiHeaderMaroon.copy(alpha = 0.88f),
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    post.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111111),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (post.type == PostType.LOST) "Lost" else "Found",
                        style = MaterialTheme.typography.labelMedium,
                        color = WpiHeaderMaroon,
                    )
                    if (post.resolved) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFE8F5E9),
                        ) {
                            Text(
                                "Returned",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32),
                            )
                        }
                    }
                }
                Text(
                    TimeFormatter.relative(post.createdAtEpochMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                TextButton(onClick = onOpen) {
                    Text("View", color = WpiHeaderMaroon, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ProfileEditDialog(
    user: AuthUserSummary,
    viewModel: ProfileViewModel,
    onDismiss: () -> Unit,
) {
    var name by remember(user.id) { mutableStateOf(user.name) }
    var email by remember(user.id) { mutableStateOf(user.email) }
    var phone by remember(user.id) { mutableStateOf(user.phone) }
    var password by remember(user.id) { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val fieldScroll = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(fieldScroll),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorText = null },
                    label = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WpiHeaderMaroon,
                        focusedLabelColor = WpiHeaderMaroon,
                        cursorColor = WpiHeaderMaroon,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorText = null },
                    label = { Text("Email (@wpi.edu)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WpiHeaderMaroon,
                        focusedLabelColor = WpiHeaderMaroon,
                        cursorColor = WpiHeaderMaroon,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorText = null },
                    label = { Text("Mobile") },
                    placeholder = { Text("E.164 e.g. +15085551234") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WpiHeaderMaroon,
                        focusedLabelColor = WpiHeaderMaroon,
                        cursorColor = WpiHeaderMaroon,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorText = null },
                    label = { Text("Current password") },
                    placeholder = { Text("Required if you change email or phone") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WpiHeaderMaroon,
                        focusedLabelColor = WpiHeaderMaroon,
                        cursorColor = WpiHeaderMaroon,
                    ),
                )
                Text(
                    "Display name can be updated without a password. Email must stay a @wpi.edu address. Phone updates use your Appwrite account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(top = 8.dp),
                )
                errorText?.let { msg ->
                    Text(
                        msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    errorText = null
                    viewModel.saveProfile(name, email, phone, password) { result ->
                        result.fold(
                            onSuccess = { onDismiss() },
                            onFailure = { e ->
                                errorText = e.message ?: "Could not update profile."
                            },
                        )
                    }
                },
            ) {
                Text("Save", color = WpiCrimson, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
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
