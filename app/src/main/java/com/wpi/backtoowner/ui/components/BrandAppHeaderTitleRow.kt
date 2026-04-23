package com.wpi.backtoowner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wpi.backtoowner.R

/** WPI seal + wordmark (transparent PNG) and app title; no extra plate behind the logo. */
@Composable
fun BrandAppHeaderTitleRow(
    modifier: Modifier = Modifier,
    logoHeight: Dp = 40.dp,
    titleFontSize: TextUnit = 24.sp,
    spacerWidth: Dp = 12.dp,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_wpi_backtoowner),
            contentDescription = "WPI",
            modifier = Modifier
                .height(logoHeight)
                .width(logoHeight * 3f),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(spacerWidth))
        Text(
            text = "BackToOwner",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = titleFontSize,
        )
    }
}
