package com.example.worktimer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.worktimer.MainActivity
import com.example.worktimer.utils.Formatters
import com.example.worktimer.utils.EarningsCalculator

@Composable
fun TimerScreen(activity: MainActivity) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 현재 프로젝트 선택
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "현재 프로젝트",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activity.projects.filter { it.isActive }) { project ->
                        FilterChip(
                            onClick = {
                                activity.updateCurrentProject(project)
                            },
                            label = {
                                Column {
                                    Text(project.name)
                                    Text(
                                        Formatters.formatCurrency(project.defaultHourlyRate) + "/h",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            selected = activity.currentProject?.id == project.id
                        )
                    }
                }
            }
        }

        // 작업 설명 입력
        if (activity.currentProject != null) {
            OutlinedTextField(
                value = activity.currentTaskDescription,
                onValueChange = { activity.updateCurrentTaskDescription(it) },
                label = { Text("작업 내용 (선택사항)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )
        }

        // 위치 상태
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (activity.currentLocationName) {
                    "회사" -> Color.Green.copy(alpha = 0.1f)
                    "집" -> Color.Blue.copy(alpha = 0.1f)
                    else -> Color.Gray.copy(alpha = 0.1f)
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "현재 위치",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = activity.currentLocationName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (activity.currentLocationName) {
                        "회사" -> Color(0xFF2E7D32)
                        "집" -> Color(0xFF1976D2)
                        else -> Color.Gray
                    }
                )
            }
        }

        // 타이머 표시
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (activity.isWorking) "작업 중" else "대기 중",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (activity.isWorking) Color(0xFF2E7D32) else Color.Gray
                )

                Text(
                    text = Formatters.formatElapsedTime(activity.elapsedTime),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (activity.isWorking) Color(0xFF2E7D32) else Color.Gray
                )

                // 실시간 수익 표시
                if (activity.isWorking && activity.currentProject != null && activity.currentProject!!.defaultHourlyRate > 0) {
                    val currentEarnings = (activity.elapsedTime / 1000.0 / 3600.0) * activity.currentProject!!.defaultHourlyRate
                    Text(
                        text = "예상 수익: ${Formatters.formatCurrency(currentEarnings)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // 작업 시작/중지 버튼
        Button(
            onClick = {
                if (activity.isWorking) {
                    activity.stopWork()
                } else {
                    if (activity.currentProject != null) {
                        activity.startWork()
                    } else {
                        Toast.makeText(context, "프로젝트를 선택해주세요", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (activity.isWorking) Color(0xFFD32F2F) else Color(0xFF2E7D32)
            )
        ) {
            Icon(
                imageVector = if (activity.isWorking) Icons.Default.Close else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (activity.isWorking) "작업 중지" else "작업 시작",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // 오늘의 수익 요약
        if (activity.workSessions.isNotEmpty()) {
            val todaySessions = EarningsCalculator.getTodaySessions(activity.workSessions)
            val todayEarnings = todaySessions.sumOf { it.earnings }
            val todayHours = todaySessions.sumOf { it.duration } / 1000.0 / 3600.0

            if (todayEarnings > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "오늘의 실적",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format("%.1f시간", todayHours),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "작업 시간",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = Formatters.formatCurrency(todayEarnings),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = "수익",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}