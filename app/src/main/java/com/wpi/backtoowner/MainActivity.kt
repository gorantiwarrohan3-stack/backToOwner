package com.wpi.backtoowner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import coil.ImageLoader
import coil.compose.LocalImageLoader
import com.wpi.backtoowner.ui.BackToOwnerRoot
import com.wpi.backtoowner.ui.theme.BackToOwnerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                BackToOwnerTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        BackToOwnerRoot()
                    }
                }
            }
        }
    }
}
