package com.example.worktimer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import com.example.worktimer.MainActivity

@Composable
fun WorkTimerApp(activity: MainActivity) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("타이머", "기록", "수익성", "지도", "위치 설정", "계정")

    // 타이머 업데이트
    LaunchedEffect(activity.isWorking) {
        while (activity.isWorking) {
            delay(1000)
            activity.updateElapsedTime()
        }
    }

    // 초기 경과 시간 계산
    LaunchedEffect(Unit) {
        activity.updateElapsedTime()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            color = if (selectedTab == index) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> TimerScreen(activity)
            1 -> HistoryScreen(activity)
            2 -> EarningsScreen(activity)
            3 -> MapScreen(activity)
            4 -> LocationSettingScreen(activity)
            5 -> AccountScreen(activity)
        }
    }
}