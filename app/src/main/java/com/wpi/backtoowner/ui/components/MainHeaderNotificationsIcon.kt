package com.wpi.backtoowner.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.wpi.backtoowner.ui.LocalMainNavigationActions

@Composable
fun MainHeaderNotificationsIcon(
    tint: Color = Color.White,
) {
    val actions = LocalMainNavigationActions.current
    IconButton(onClick = actions.navigateToNotifications) {
        Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = tint)
    }
}
