package com.wpi.backtoowner.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wpi.backtoowner.ui.components.BrandAppHeaderTitleRow
import com.wpi.backtoowner.ui.components.MainHeaderTrailingIcons
import com.wpi.backtoowner.ui.theme.WpiHeaderMaroon
import com.wpi.backtoowner.ui.theme.WpiOnCrimson

private val BubbleSurface = Color(0xFFEEEEEE)
private val BubbleSurfaceHighlight = Color(0xFFFFF8E1)
private val BubbleOnSurface = Color(0xFF1A1A1A)
private val ScreenBg = Color(0xFF0E0E0E)
private val UnreadHighlightBorder = Color(0xFFC41230)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    itemId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val post by viewModel.post.collectAsStateWithLifecycle()
    val loadError by viewModel.loadError.collectAsStateWithLifecycle()
    val messagesLoadError by viewModel.messagesLoadError.collectAsStateWithLifecycle()
    val currentUserLabel by viewModel.currentUserLabel.collectAsStateWithLifecycle()
    val otherPartyLabel by viewModel.otherPartyLabel.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val sendError by viewModel.sendError.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()

    var draft by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        containerColor = ScreenBg,
        topBar = {
            Column {
                Surface(color = WpiHeaderMaroon) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        BrandAppHeaderTitleRow(
                            modifier = Modifier.weight(1f),
                            logoHeight = 36.dp,
                            titleFontSize = 22.sp,
                            spacerWidth = 8.dp,
                        )
                        MainHeaderTrailingIcons()
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFE8E8E8),
                ) {
                    Text(
                        text = "Chat / Recovery Coordination",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black,
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "I'm heading to the Gordon Library now to drop this off.",
                        "Can we meet at the Campus Center?",
                        "What time works for you?",
                    ).forEach { q ->
                        Button(
                            onClick = { draft = q },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WpiHeaderMaroon, contentColor = WpiOnCrimson),
                        ) {
                            Text(q, style = MaterialTheme.typography.bodySmall, color = WpiOnCrimson)
                        }
                    }
                }
                Spacer(Modifier.size(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message...", color = Color(0xFF9E9E9E)) },
                        shape = RoundedCornerShape(28.dp),
                        singleLine = false,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.85f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.45f),
                            cursorColor = WpiHeaderMaroon,
                            focusedContainerColor = Color(0xFF2A2A2A),
                            unfocusedContainerColor = Color(0xFF2A2A2A),
                        ),
                    )
                    IconButton(
                        enabled = !isSending,
                        onClick = {
                            if (draft.isNotBlank()) {
                                viewModel.sendMessage(draft)
                                draft = ""
                            }
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (isSending) Color.Gray else WpiHeaderMaroon,
                        )
                    }
                }
                sendError?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFB4AB),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
    ) { innerPadding ->
        when {
            loadError != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(loadError!!, color = Color(0xFFFFB4AB), modifier = Modifier.padding(24.dp))
                }
            }

            post == null && loadError == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = WpiHeaderMaroon)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        messagesLoadError?.let { err ->
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFB4AB),
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        Text(
                            text = "Listing: ${post?.title.orEmpty()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Text(
                            text = "Thread id: $itemId",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9E9E9E),
                        )
                        currentUserLabel?.let { me ->
                            Text(
                                text = "Signed in as $me",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB8B8B8),
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        otherPartyLabel?.let { other ->
                            Text(
                                text = "Messaging: $other",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB8B8B8),
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                    itemsIndexed(messages, key = { _, m -> m.id }) { _, msg ->
                        ChatBubbleRow(msg)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleRow(msg: ChatBubbleUi) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFDDDDDD)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                msg.displayName.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                color = BubbleOnSurface,
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = if (msg.isUnreadHighlight) BubbleSurfaceHighlight else BubbleSurface,
            contentColor = BubbleOnSurface,
            border = if (msg.isUnreadHighlight) BorderStroke(2.dp, UnreadHighlightBorder) else null,
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = "${msg.displayName} (${msg.roleLabel})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = BubbleOnSurface,
                )
                Text(
                    text = msg.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BubbleOnSurface,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
