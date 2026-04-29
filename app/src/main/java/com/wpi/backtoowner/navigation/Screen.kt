package com.wpi.backtoowner.navigation

import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    data object Feed : Screen("feed")
    data object Map : Screen("map")
    data object CreatePost : Screen("create_post")
    data object Detail : Screen("detail/{itemId}") {
        const val ARG_ITEM_ID = "itemId"

        fun createRoute(itemId: String): String =
            "detail/${Uri.encode(itemId)}"

        val navArguments = listOf(
            navArgument(ARG_ITEM_ID) { type = NavType.StringType },
        )
    }

    data object Profile : Screen("profile")

    data object ChatList : Screen("chats")

    data object Chat : Screen("chat/{itemId}") {
        const val ARG_ITEM_ID = "itemId"

        fun createRoute(itemId: String): String =
            "chat/${Uri.encode(itemId)}"

        val navArguments = listOf(
            navArgument(ARG_ITEM_ID) { type = NavType.StringType },
        )
    }
}
