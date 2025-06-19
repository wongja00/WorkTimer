package com.worktracker.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.worktracker.tracker.MainActivity
import com.worktracker.tracker.data.Project
import com.worktracker.tracker.ui.components.ProjectDialog
import com.worktracker.tracker.utils.Formatters

@Composable
fun ProjectSettingsScreen(activity: MainActivity) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<Project?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Project?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "프로젝트 관리",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "프로젝트 추가")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 프로젝트 통계
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = "${activity.projects.size}",
                    label = "총 프로젝트",
                    color = Color(0xFF1976D2)
                )
                StatItem(
                    value = "${activity.projects.count { it.isActive }}",
                    label = "활성 프로젝트",
                    color = Color(0xFF2E7D32)
                )
                StatItem(
                    value = Formatters.formatCurrency(
                        activity.projects
                            .filter { it.isActive }
                            .map { it.defaultHourlyRate }
                            .average()
                            .takeIf { !it.isNaN() } ?: 0.0
                    ),
                    label = "평균 시급",
                    color = Color(0xFFFF6B00)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 프로젝트 목록
        if (activity.projects.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "아직 프로젝트가 없습니다",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "첫 번째 프로젝트를 추가해보세요!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activity.projects) { project ->
                    ProjectCard(
                        project = project,
                        isSelected = activity.currentProject?.id == project.id,
                        onSelect = { activity.updateCurrentProject(project) },
                        onEdit = { editingProject = project },
                        onToggleActive = { activity.toggleProjectActive(project.id) },
                        onDelete = { showDeleteConfirm = project },
                        workSessions = activity.workSessions.filter { it.projectName == project.name }
                    )
                }
            }
        }
    }

    // 프로젝트 추가/편집 다이얼로그
    if (showAddDialog || editingProject != null) {
        ProjectDialog(
            project = editingProject,
            onDismiss = {
                showAddDialog = false
                editingProject = null
            },
            onSave = { project ->
                if (editingProject != null) {
                    activity.updateProject(project)
                } else {
                    activity.addProject(project)
                }
                showAddDialog = false
                editingProject = null
            }
        )
    }

    // 삭제 확인 다이얼로그
    showDeleteConfirm?.let { project ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("프로젝트 삭제") },
            text = {
                Text("'${project.name}' 프로젝트를 삭제하시겠습니까?\n관련된 작업 기록은 유지됩니다.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        activity.deleteProject(project.id)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
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
private fun ProjectCard(
    project: Project,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
    workSessions: List<com.worktracker.tracker.data.WorkSession>
) {
    val totalEarnings = workSessions.sumOf { it.earnings }
    val totalHours = workSessions.sumOf { it.duration } / 1000.0 / 3600.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> Color(0xFFE8F5E8)
                !project.isActive -> Color(0xFFFAFAFA)
                else -> Color.White
            }
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(
            2.dp,
            Color(0xFF2E7D32)
        ) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 프로젝트 기본 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (project.isActive) Color.Black else Color.Gray
                        )

                        if (isSelected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "선택됨",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (!project.isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "비활성",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    Text(
                        text = "${Formatters.formatCurrency(project.defaultHourlyRate)}/시간",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Medium
                    )

                    if (project.description.isNotEmpty()) {
                        Text(
                            text = project.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // 액션 버튼들
                Row {
                    if (!isSelected) {
                        TextButton(onClick = onSelect) {
                            Text("선택")
                        }
                    }

                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "편집")
                    }

                    IconButton(onClick = onToggleActive) {
                        Icon(
                            if (project.isActive) Icons.Default.Refresh else Icons.Default.PlayArrow,
                            contentDescription = if (project.isActive) "비활성화" else "활성화",
                            tint = if (project.isActive) Color(0xFFFF6B00) else Color(0xFF2E7D32)
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "삭제",
                            tint = Color(0xFFD32F2F)
                        )
                    }
                }
            }

            // 프로젝트 통계
            if (workSessions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        value = "${workSessions.size}",
                        label = "세션",
                        color = Color(0xFF1976D2)
                    )
                    StatItem(
                        value = String.format("%.1f시간", totalHours),
                        label = "총 시간",
                        color = Color(0xFF2E7D32)
                    )
                    StatItem(
                        value = Formatters.formatCurrency(totalEarnings),
                        label = "총 수익",
                        color = Color(0xFFFF6B00)
                    )
                }
            }
        }
    }
}