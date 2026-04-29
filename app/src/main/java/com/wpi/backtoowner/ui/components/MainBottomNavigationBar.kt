package com.wpi.backtoowner.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import com.wpi.backtoowner.navigation.Screen
import com.wpi.backtoowner.ui.theme.WpiBottomNavDark
import com.wpi.backtoowner.ui.theme.WpiCrimson

/** Same bottom bar on every signed-in route (including detail and chat). */
@Composable
fun MainBottomNavigationBar(
    navController: NavHostController,
    currentRoute: String?,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = WpiBottomNavDark,
        contentColor = Color.White,
    ) {
        val colors = NavigationBarItemDefaults.colors(
            selectedIconColor = WpiCrimson,
            selectedTextColor = WpiCrimson,
            unselectedIconColor = Color.White.copy(alpha = 0.65f),
            unselectedTextColor = Color.White.copy(alpha = 0.65f),
            indicatorColor = Color.White.copy(alpha = 0.12f),
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Feed.route,
            onClick = {
                if (currentRoute == Screen.Feed.route) return@NavigationBarItem
                // Pop feed → detail (or chat layered on detail) reliably.
                if (!navController.popBackStack(Screen.Feed.route, inclusive = false)) {
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home") },
            colors = colors,
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Map.route,
            onClick = {
                navController.navigate(Screen.Map.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Filled.Map, contentDescription = "Map") },
            label = { Text("Map") },
            colors = colors,
        )
        NavigationBarItem(
            selected = currentRoute == Screen.CreatePost.route,
            onClick = {
                navController.navigate(Screen.CreatePost.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Filled.Add, contentDescription = "Report") },
            label = { Text("Report") },
            colors = colors,
        )
        NavigationBarItem(
            selected = currentRoute == Screen.ChatList.route,
            onClick = {
                navController.navigate(Screen.ChatList.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Filled.Forum, contentDescription = "Chats") },
            label = { Text("Chats") },
            colors = colors,
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Profile.route,
            onClick = {
                navController.navigate(Screen.Profile.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            colors = colors,
        )
    }
}
