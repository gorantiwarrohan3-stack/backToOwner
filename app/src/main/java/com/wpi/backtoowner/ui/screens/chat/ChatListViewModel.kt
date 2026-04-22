package com.wpi.backtoowner.ui.screens.chat

import androidx.lifecycle.ViewModel
import com.wpi.backtoowner.data.local.ChatThreadStore
import com.wpi.backtoowner.data.local.ChatThreadSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    chatThreadStore: ChatThreadStore,
) : ViewModel() {
    val threads: StateFlow<List<ChatThreadSummary>> = chatThreadStore.threads
}
