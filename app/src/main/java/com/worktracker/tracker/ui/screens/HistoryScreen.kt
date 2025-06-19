package com.worktracker.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.worktracker.tracker.MainActivity
import com.worktracker.tracker.utils.Formatters

@Composable
fun HistoryScreen(activity: MainActivity) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "작업 기록",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (activity.workSessions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "아직 작업 기록이 없습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(32.dp)
                )
            }
            return
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activity.workSessions.sortedByDescending { it.startTime }) { session ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = session.date,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = Formatters.formatElapsedTime(session.duration),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF2E7D32)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "시작: ${Formatters.formatTime(session.startTime)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "종료: ${Formatters.formatTime(session.endTime)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "위치: ${session.location}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "프로젝트: ${session.projectName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (session.taskDescription.isNotEmpty()) {
                            Text(
                                text = "작업: ${session.taskDescription}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Text(
                            text = "수익: ${Formatters.formatCurrency(session.earnings)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }
}