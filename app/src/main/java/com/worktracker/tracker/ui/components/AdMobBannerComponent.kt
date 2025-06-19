package com.worktracker.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.worktracker.tracker.services.AdMobService

@Composable
fun AdMobBannerComponent(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFF5F5F5)
) {
    val context = LocalContext.current
    val adMobService = remember { AdMobService(context) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 광고 라벨
            Text(
                text = "광고",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(4.dp))

            // AdMob 배너 광고
            AndroidView(
                factory = { adMobService.createBannerAd() },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(8.dp))
            )
        }
    }
}

@Composable
fun SmallBannerAd(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val adMobService = remember { AdMobService(context) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Color(0xFFF8F8F8),
                RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { adMobService.createBannerAd() },
            modifier = Modifier.wrapContentSize()
        )
    }
}

@Composable
fun CompactBannerAd(
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val context = LocalContext.current
    val adMobService = remember { AdMobService(context) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showLabel) {
            Text(
                text = "스폰서",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        AndroidView(
            factory = { adMobService.createBannerAd() },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        )
    }
}