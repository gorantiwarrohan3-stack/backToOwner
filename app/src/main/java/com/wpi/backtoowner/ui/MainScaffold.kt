package com.wpi.backtoowner.ui

import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wpi.backtoowner.navigation.BackToOwnerNavHost
import com.wpi.backtoowner.navigation.Screen
import com.wpi.backtoowner.ui.theme.WpiBottomNavDark
import com.wpi.backtoowner.ui.theme.WpiCrimson

@Composable
fun MainScaffold(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute?.startsWith("detail/") != true &&
        currentRoute?.startsWith("chat/") != true

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
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
                            navController.navigate(Screen.Feed.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
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
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
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
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
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
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
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
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
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
        },
    ) { innerPadding ->
        BackToOwnerNavHost(
            modifier = Modifier.padding(innerPadding),
            navController = navController,
            onLogout = onLogout,
        )
    }
}
