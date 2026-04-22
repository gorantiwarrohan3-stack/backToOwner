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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wpi.backtoowner.data.local.ChatThreadSummary
import com.wpi.backtoowner.ui.theme.WpiHeaderMaroon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onOpenThread: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val threads by viewModel.threads.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = Color(0xFF0E0E0E),
        topBar = {
            TopAppBar(
                title = {
                    Text("Chats", color = Color.White, fontWeight = FontWeight.SemiBold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WpiHeaderMaroon,
                    titleContentColor = Color.White,
                ),
            )
        },
    ) { innerPadding ->
        if (threads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No conversations yet. Open a listing from the feed and tap \"Message Founder\" to start one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFB8B8B8),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(threads, key = { it.itemId }) { row ->
                    ChatThreadRow(thread = row, onClick = { onOpenThread(row.itemId) })
                }
            }
        }
    }
}

@Composable
private fun ChatThreadRow(
    thread: ChatThreadSummary,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1C1C1C))
            .clickable(onClick = onClick)
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
}
