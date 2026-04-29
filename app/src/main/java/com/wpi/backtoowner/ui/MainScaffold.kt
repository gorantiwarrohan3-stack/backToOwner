package com.wpi.backtoowner.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wpi.backtoowner.navigation.BackToOwnerNavHost
import com.wpi.backtoowner.ui.components.MainBottomNavigationBar

@Composable
fun MainScaffold(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = modifier,
        bottomBar = {
            MainBottomNavigationBar(
                navController = navController,
                currentRoute = currentRoute,
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
