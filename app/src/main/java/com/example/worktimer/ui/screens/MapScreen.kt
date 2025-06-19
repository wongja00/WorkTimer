package com.example.worktimer.ui.screens

import android.widget.CalendarView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.worktimer.MainActivity
import com.example.worktimer.data.DailyRoute
import com.example.worktimer.data.LocationPoint
import com.example.worktimer.utils.Formatters
import java.text.SimpleDateFormat
import com.example.worktimer.ui.components.GoogleMapComponent

@Composable
fun MapScreen(activity: MainActivity) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(Calendar.getInstance().timeInMillis) }
    var selectedRoute by remember { mutableStateOf<DailyRoute?>(null) }

    // 선택된 날짜의 동선 데이터 로드
    LaunchedEffect(selectedDate) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(Date(selectedDate))
        selectedRoute = activity.getDailyRoute(dateString)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "동선 지도",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 날짜 선택 캘린더
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            AndroidView(
                factory = { ctx ->
                    CalendarView(ctx).apply {
                        setOnDateChangeListener { _, year, month, dayOfMonth ->
                            val calendar = Calendar.getInstance()
                            calendar.set(year, month, dayOfMonth)
                            selectedDate = calendar.timeInMillis
                        }
                    }
                },
                modifier = Modifier.wrapContentHeight()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 선택된 날짜 정보
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
                Text(
                    text = dateFormat.format(Date(selectedDate)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (selectedRoute != null && selectedRoute!!.points.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${selectedRoute!!.points.size}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Text("위치 포인트", style = MaterialTheme.typography.bodySmall)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%.1f km", selectedRoute!!.totalDistance / 1000),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2)
                            )
                            Text("총 이동거리", style = MaterialTheme.typography.bodySmall)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${selectedRoute!!.workSessions.size}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF6B00)
                            )
                            Text("근무 세션", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    Text(
                        text = "이 날짜에는 동선 기록이 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Maps 영역
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "동선 지도",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // 지도 타입 토글 버튼들
                    Row {
                        var mapType by remember { mutableStateOf("normal") }

                        FilterChip(
                            onClick = { mapType = "normal" },
                            label = { Text("일반") },
                            selected = mapType == "normal"
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        FilterChip(
                            onClick = { mapType = "satellite" },
                            label = { Text("위성") },
                            selected = mapType == "satellite"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 실제 Google Maps 컴포넌트
                GoogleMapComponent(
                    dailyRoute = selectedRoute,
                    workLocation = activity.workLocation?.toCustomLatLng(),
                    homeLocation = activity.homeLocation?.toCustomLatLng(),
                    mapType = mapType,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 동선 세부 정보
        if (selectedRoute != null && selectedRoute!!.points.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "이동 기록",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    selectedRoute!!.points.sortedBy { it.timestamp }.forEach { point ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = Formatters.formatTime(point.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                text = point.locationName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}