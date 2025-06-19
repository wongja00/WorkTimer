package com.example.worktimer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.worktimer.data.Project

@Composable
fun ProjectDialog(
    project: Project?,
    onDismiss: () -> Unit,
    onSave: (Project) -> Unit
) {
    var name by remember { mutableStateOf(project?.name ?: "") }
    var hourlyRate by remember { mutableStateOf(project?.defaultHourlyRate?.toString() ?: "") }
    var description by remember { mutableStateOf(project?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (project != null) "프로젝트 편집" else "새 프로젝트 추가",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("프로젝트 이름") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = hourlyRate,
                    onValueChange = { hourlyRate = it },
                    label = { Text("시급 (₩)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("설명 (선택사항)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val rate = hourlyRate.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && rate > 0) {
                        val newProject = if (project != null) {
                            project.copy(
                                name = name,
                                defaultHourlyRate = rate,
                                description = description
                            )
                        } else {
                            Project(
                                name = name,
                                defaultHourlyRate = rate,
                                description = description
                            )
                        }
                        onSave(newProject)
                    }
                }
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}