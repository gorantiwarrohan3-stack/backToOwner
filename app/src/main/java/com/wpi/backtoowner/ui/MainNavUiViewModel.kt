package com.wpi.backtoowner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpi.backtoowner.notifications.ChatReadTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MainNavUiViewModel @Inject constructor(
    private val chatReadTracker: ChatReadTracker,
) : ViewModel() {

    val chatUnreadTotal = chatReadTracker.unreadTotal

    init {
        viewModelScope.launch {
            chatReadTracker.refreshUnreadCount()
        }
    }
}
