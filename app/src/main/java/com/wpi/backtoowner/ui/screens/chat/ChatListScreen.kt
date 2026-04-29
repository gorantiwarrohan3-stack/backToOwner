package com.wpi.backtoowner.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.wpi.backtoowner.data.local.ChatThreadSummary
import com.wpi.backtoowner.ui.components.MainHeaderTrailingIcons
import com.wpi.backtoowner.ui.theme.WpiHeaderMaroon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onOpenThread: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val threads by viewModel.threads.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isDeleting by viewModel.isDeleting.collectAsStateWithLifecycle()
    val deleteError by viewModel.deleteError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeleteThread by remember { mutableStateOf<ChatThreadSummary?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshThreadsFromMessages()
    }

    LaunchedEffect(deleteError) {
        val msg = deleteError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearDeleteError()
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color(0xFF0E0E0E),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Chats", color = Color.White, fontWeight = FontWeight.SemiBold)
                },
                actions = {
                    MainHeaderTrailingIcons()
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WpiHeaderMaroon,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refreshThreadsFromMessages,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (threads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No conversations yet. Open a listing from the feed and tap the message button on the item details screen to start one.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFB8B8B8),
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(threads, key = { it.itemId }) { row ->
                            ChatThreadRow(
                                thread = row,
                                onOpen = { onOpenThread(row.itemId) },
                                onRequestDelete = { pendingDeleteThread = row },
                                deleteEnabled = !isDeleting,
                            )
                        }
                    }
                }
            }

            pendingDeleteThread?.let { thread ->
                AlertDialog(
                    onDismissRequest = { if (!isDeleting) pendingDeleteThread = null },
                    title = { Text("Delete conversation?") },
                    text = {
                        Text(
                            "Remove \"${thread.postTitle}\" from your chats. " +
                                "Messages you can delete on the server will be removed; " +
                                "this chat will stay hidden here until you message that listing again.",
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteConversation(thread.itemId)
                                pendingDeleteThread = null
                            },
                            enabled = !isDeleting,
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { pendingDeleteThread = null },
                            enabled = !isDeleting,
                        ) {
                            Text("Cancel")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ChatThreadRow(
    thread: ChatThreadSummary,
    onOpen: () -> Unit,
    onRequestDelete: () -> Unit,
    deleteEnabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1C1C1C)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpen)
                .padding(16.dp),
        ) {
            Text(
                text = thread.postTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Spacer(Modifier.padding(4.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val poster = thread.posterDisplayName?.let { "Posted by $it" } ?: "Posted by listing author"
                Text(
                    text = poster,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0B0B0),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = thread.listingType,
                    style = MaterialTheme.typography.labelSmall,
                    color = WpiHeaderMaroon,
                )
            }
        }
        IconButton(
            onClick = onRequestDelete,
            enabled = deleteEnabled,
            modifier = Modifier.padding(end = 4.dp),
        ) {
            Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = "Delete conversation",
                tint = Color(0xFFFFB4AB),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
