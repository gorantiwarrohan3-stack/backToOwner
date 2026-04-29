package com.wpi.backtoowner.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wpi.backtoowner.navigation.BackToOwnerNavHost
import com.wpi.backtoowner.navigation.Screen
import com.wpi.backtoowner.ui.components.MainBottomNavigationBar

@Composable
fun MainScaffold(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    navUiViewModel: MainNavUiViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val chatUnread by navUiViewModel.chatUnreadTotal.collectAsStateWithLifecycle()

    val mainNavActions = remember(navController) {
        MainNavigationActions(
            navigateToNotifications = {
                navController.navigate(Screen.Notifications.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            navigateToInsights = {
                navController.navigate(Screen.Insights.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        )
    }

    CompositionLocalProvider(LocalMainNavigationActions provides mainNavActions) {
        Scaffold(
            modifier = modifier,
            bottomBar = {
                MainBottomNavigationBar(
                    navController = navController,
                    currentRoute = currentRoute,
                    chatUnreadCount = chatUnread,
                )
            },
        ) { innerPadding ->
            BackToOwnerNavHost(
                modifier = Modifier.padding(innerPadding),
                navController = navController,
                onLogout = onLogout,
            )
        }
    }
}
