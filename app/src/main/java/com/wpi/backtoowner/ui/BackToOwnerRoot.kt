package com.wpi.backtoowner.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wpi.backtoowner.R
import com.wpi.backtoowner.ui.auth.AuthScreen
import com.wpi.backtoowner.ui.auth.AuthUiState
import com.wpi.backtoowner.ui.auth.SessionViewModel
import com.wpi.backtoowner.ui.theme.WpiHeaderMaroon

@Composable
fun BackToOwnerRoot(
    modifier: Modifier = Modifier,
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val state by sessionViewModel.uiState.collectAsStateWithLifecycle()

    when (state) {
        AuthUiState.Loading -> {
            Box(
                modifier
                    .fillMaxSize()
                    .background(WpiHeaderMaroon),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.logo_wpi_backtoowner),
                        contentDescription = null,
                        modifier = Modifier
                            .height(52.dp)
                            .width(52.dp * 3f),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "BackToOwner",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(28.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = Color.White,
                        strokeWidth = 3.dp,
                    )
                }
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
