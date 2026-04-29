package com.wpi.backtoowner.ui

import androidx.compose.runtime.compositionLocalOf

/** Actions for main signed-in graph (scaffold); used by headers e.g. notifications bell. */
data class MainNavigationActions(
    val navigateToNotifications: () -> Unit = {},
    val navigateToInsights: () -> Unit = {},
)

val LocalMainNavigationActions = compositionLocalOf { MainNavigationActions() }
