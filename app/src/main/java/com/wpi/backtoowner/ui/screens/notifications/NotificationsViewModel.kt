package com.wpi.backtoowner.ui.screens.notifications

import androidx.lifecycle.ViewModel
import com.wpi.backtoowner.notifications.InAppNotificationStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val inAppNotificationStore: InAppNotificationStore,
) : ViewModel() {

    val items = inAppNotificationStore.items

    fun clearAll() {
        inAppNotificationStore.clearAll()
    }

    fun dismiss(id: String) {
        inAppNotificationStore.remove(id)
    }
}
