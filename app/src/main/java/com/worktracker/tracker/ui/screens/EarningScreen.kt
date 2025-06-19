package com.worktracker.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.worktracker.tracker.MainActivity
import com.worktracker.tracker.ui.components.AdMobBannerComponent
import com.worktracker.tracker.ui.components.CompactBannerAd
import com.worktracker.tracker.utils.Formatters
import com.worktracker.tracker.utils.EarningsCalculator

@Composable
fun EarningsScreen(activity: MainActivity) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "수익성 분석",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (activity.workSessions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "아직 작업 기록이 없습니다",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "작업을 시작해보세요!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 빈 상태에서도 광고 표시
            AdMobBannerComponent(modifier = Modifier.fillMaxWidth())

            return
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 전체 수익 요약
            item {
                val totalEarnings = activity.workSessions.sumOf { it.earnings }
                val totalHours = activity.workSessions.sumOf { it.duration } / 1000.0 / 3600.0
                val averageRate = if (totalHours > 0) totalEarnings / totalHours else 0.0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "전체 수익 요약",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = Formatters.formatCurrency(totalEarnings),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                                Text("총 수익", style = MaterialTheme.typography.bodyMedium)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format("%.1f", totalHours),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("총 시간", style = MaterialTheme.typography.bodyMedium)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = Formatters.formatCurrency(averageRate),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1976D2)
                                )
                                Text("평균 시급", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // 첫 번째 광고 삽입
            item {
                CompactBannerAd(
                    modifier = Modifier.fillMaxWidth(),
                    showLabel = true
                )
            }

            // 프로젝트별 수익 분석
            item {
                val projectEarnings = activity.workSessions
                    .groupBy { it.projectName }
                    .mapValues { (_, sessions) ->
                        sessions.sumOf { it.earnings } to sessions.sumOf { it.duration } / 1000.0 / 3600.0
                    }
                    .toList()
                    .sortedByDescending { it.second.first }

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "프로젝트별 수익",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        projectEarnings.forEachIndexed { index, (projectName, earningsAndHours) ->
                            val (earnings, hours) = earningsAndHours
                            val avgRate = if (hours > 0) earnings / hours else 0.0

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = projectName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = Formatters.formatCurrency(earnings),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${String.format("%.1f", hours)}시간 작업",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "평균 ${Formatters.formatCurrency(avgRate)}/h",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF1976D2)
                                        )
                                    }
                                }
                            }

                            // 프로젝트 3개마다 광고 삽입
                            if ((index + 1) % 3 == 0 && index < projectEarnings.size - 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                                CompactBannerAd(
                                    modifier = Modifier.fillMaxWidth(),
                                    showLabel = false
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            // 월별 수익 트렌드
            item {
                val monthlyEarnings = EarningsCalculator.calculateMonthlyEarnings(activity.workSessions)

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "월별 수익 트렌드",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        monthlyEarnings.forEach { (month, earnings) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = month,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = Formatters.formatCurrency(earnings),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }

            // 마지막 광고
            item {
                Spacer(modifier = Modifier.height(8.dp))
                AdMobBannerComponent(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFF0F8FF)
                )
            }
        }
    }
}