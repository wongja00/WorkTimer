package com.example.worktimer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.worktimer.MainActivity
import com.example.worktimer.data.DailyRoute
import com.example.worktimer.ui.components.GoogleMapComponent
import com.example.worktimer.utils.Formatters
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MapScreen(activity: MainActivity) {
    val context = LocalContext.current

    // 오늘 날짜를 기본값으로 설정
    val today = Calendar.getInstance()
    var selectedDate by remember { mutableStateOf(today.timeInMillis) }
    var selectedRoute by remember { mutableStateOf<DailyRoute?>(null) }
    var mapType by remember { mutableStateOf("normal") }
    var showDatePicker by remember { mutableStateOf(false) }

    // 선택된 날짜의 동선 데이터 로드
    LaunchedEffect(selectedDate) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(Date(selectedDate))
        selectedRoute = activity.getDailyRoute(dateString)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 전체 화면 지도
        GoogleMapComponent(
            dailyRoute = selectedRoute,
            workLocation = activity.workLocation?.let {
                com.example.worktimer.data.LatLng(it.latitude, it.longitude)
            },
            homeLocation = activity.homeLocation?.let {
                com.example.worktimer.data.LatLng(it.latitude, it.longitude)
            },
            mapType = mapType,
            modifier = Modifier.fillMaxSize()
        )

        // 상단 컨트롤 패널
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 제목과 컨트롤
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "동선 지도",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 지도 타입 버튼
                        FilterChip(
                            onClick = { mapType = if (mapType == "normal") "satellite" else "normal" },
                            label = {
                                Text(
                                    if (mapType == "normal") "위성" else "일반",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            selected = false,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        // 날짜 선택 버튼
                        FilledTonalButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
                            Text(
                                dateFormat.format(Date(selectedDate)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 통계 정보
                if (selectedRoute != null && selectedRoute!!.points.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCard(
                            value = "${selectedRoute!!.points.size}",
                            label = "포인트",
                            color = Color(0xFF2E7D32)
                        )
                        StatCard(
                            value = String.format("%.1f km", selectedRoute!!.totalDistance / 1000),
                            label = "이동거리",
                            color = Color(0xFF1976D2)
                        )
                        StatCard(
                            value = "${selectedRoute!!.workSessions.size}",
                            label = "근무세션",
                            color = Color(0xFFFF6B00)
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                    ) {
                        Text(
                            text = "이 날짜에는 동선 기록이 없습니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }

        // 하단 정보 패널 (슬라이드 업 가능하도록)
        if (selectedRoute != null && selectedRoute!!.points.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "이동 기록",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 최근 몇 개의 위치만 표시
                    val recentPoints = selectedRoute!!.points
                        .sortedByDescending { it.timestamp }
                        .take(3)

                    recentPoints.forEach { point ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = point.locationName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = Formatters.formatTime(point.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // 날짜 선택 다이얼로그
        if (showDatePicker) {
            DatePickerDialog(
                selectedDate = selectedDate,
                onDateSelected = { date ->
                    selectedDate = date
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun DatePickerDialog(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = selectedDate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("날짜 선택") },
        text = {
            Column {
                Text("날짜를 선택하세요:")
                Spacer(modifier = Modifier.height(16.dp))

                // 간단한 날짜 선택 버튼들 (최근 7일)
                repeat(7) { dayOffset ->
                    val date = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -dayOffset)
                    }

                    val dateFormat = SimpleDateFormat("MM월 dd일 (E)", Locale.KOREAN)
                    TextButton(
                        onClick = {
                            onDateSelected(date.timeInMillis)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (dayOffset == 0) "오늘 - ${dateFormat.format(date.time)}"
                            else dateFormat.format(date.time),
                            color = if (dayOffset == 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인")
            }
        }
    )
}