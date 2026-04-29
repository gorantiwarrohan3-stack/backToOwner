package com.wpi.backtoowner.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wpi.backtoowner.ui.screens.chat.ChatListScreen
import com.wpi.backtoowner.ui.screens.chat.ChatScreen
import com.wpi.backtoowner.ui.screens.createpost.CreatePostScreen
import com.wpi.backtoowner.ui.screens.detail.DetailScreen
import com.wpi.backtoowner.ui.screens.feed.FeedScreen
import com.wpi.backtoowner.ui.screens.map.MapScreen
import com.wpi.backtoowner.ui.screens.profile.ProfileScreen

@Composable
fun BackToOwnerNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Feed.route,
    onLogout: () -> Unit,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Screen.Feed.route) {
            FeedScreen(
                onNavigateToDetail = { itemId ->
                    navController.navigate(Screen.Detail.createRoute(itemId))
                },
            )
        }
        composable(Screen.Map.route) {
            MapScreen(
                onOpenPost = { itemId ->
                    navController.navigate(Screen.Detail.createRoute(itemId))
                },
            )
        }
        composable(Screen.ChatList.route) {
            ChatListScreen(
                onOpenThread = { id ->
                    navController.navigate(Screen.Chat.createRoute(id))
                },
            )
        }
        composable(Screen.CreatePost.route) {
            CreatePostScreen(
                onPostSuccess = {
                    navController.navigate(Screen.Feed.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateBack = {
                    navController.navigate(Screen.Feed.route) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(
            route = Screen.Detail.route,
            arguments = Screen.Detail.navArguments,
        ) { backStackEntry ->
            val raw = backStackEntry.arguments?.getString(Screen.Detail.ARG_ITEM_ID).orEmpty()
            val itemId = Uri.decode(raw)
            DetailScreen(
                itemId = itemId,
                onBack = { navController.popBackStack() },
                onMessageFounder = {
                    navController.navigate(Screen.Chat.createRoute(itemId))
                },
                onOpenSuggestedMatch = { otherPostId ->
                    navController.navigate(Screen.Detail.createRoute(otherPostId))
                },
            )
        }
        composable(
            route = Screen.Chat.route,
            arguments = Screen.Chat.navArguments,
        ) { backStackEntry ->
            val raw = backStackEntry.arguments?.getString(Screen.Chat.ARG_ITEM_ID).orEmpty()
            val itemId = Uri.decode(raw)
            ChatScreen(
                itemId = itemId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onLogout = onLogout,
                onOpenPost = { itemId ->
                    navController.navigate(Screen.Detail.createRoute(itemId))
                },
            )
        }
    }
}
