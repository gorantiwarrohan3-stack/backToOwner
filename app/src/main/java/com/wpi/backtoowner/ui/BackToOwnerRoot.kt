package com.wpi.backtoowner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wpi.backtoowner.ui.auth.AuthScreen
import com.wpi.backtoowner.ui.auth.AuthUiState
import com.wpi.backtoowner.ui.auth.SessionViewModel

@Composable
fun BackToOwnerRoot(
    modifier: Modifier = Modifier,
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val state by sessionViewModel.uiState.collectAsStateWithLifecycle()

    when (state) {
        AuthUiState.Loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        AuthUiState.SignedOut -> {
            AuthScreen(sessionViewModel = sessionViewModel, modifier = modifier.fillMaxSize())
        }

        AuthUiState.SignedIn -> {
            MainScaffold(
                modifier = modifier.fillMaxSize(),
                onLogout = { sessionViewModel.logout() },
            )
        }
    }
}
