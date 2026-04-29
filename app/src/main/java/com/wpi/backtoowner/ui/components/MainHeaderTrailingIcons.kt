package com.wpi.backtoowner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wpi.backtoowner.ui.LocalMainNavigationActions

@Composable
fun MainHeaderInsightsIcon(
    tint: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    val actions = LocalMainNavigationActions.current
    IconButton(onClick = actions.navigateToInsights, modifier = modifier) {
        Icon(Icons.Filled.InsertChart, contentDescription = "Insights dashboard", tint = tint)
    }
}

/**
 * Standard header actions: open insights (archive analytics), then notifications.
 */
@Composable
fun MainHeaderTrailingIcons(
    tint: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        MainHeaderInsightsIcon(tint = tint)
        MainHeaderNotificationsIcon(tint = tint)
    }
}
