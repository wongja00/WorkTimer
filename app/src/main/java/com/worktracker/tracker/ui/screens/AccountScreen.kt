package com.worktracker.tracker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.worktracker.tracker.MainActivity

@Composable
fun AccountScreen(activity: MainActivity) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentAccount by remember { mutableStateOf(activity.getCurrentGoogleAccount()) }
    var isSyncing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "계정 관리",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Google 계정 연동
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Google 계정 연동",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (currentAccount != null) {
                    Text(
                        text = "연결된 계정: ${currentAccount!!.email}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isSyncing = true
                                    val success = activity.syncToCloud()
                                    isSyncing = false
                                    Toast.makeText(
                                        context,
                                        if (success) "클라우드 백업 완료" else "백업 실패",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isSyncing) "동기화 중..." else "백업")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    isSyncing = true
                                    val success = activity.syncFromCloud()
                                    isSyncing = false
                                    Toast.makeText(
                                        context,
                                        if (success) "클라우드 복원 완료" else "복원 실패",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isSyncing) "동기화 중..." else "복원")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            activity.signOutFromGoogle {
                                currentAccount = null
                                Toast.makeText(context, "계정 연결이 해제되었습니다", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6B6B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("계정 연결 해제")
                    }
                } else {
                    Text(
                        text = "Google 계정을 연결하면 데이터를 클라우드에 백업할 수 있습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = {
                            activity.signInToGoogle { success ->
                                if (success) {
                                    currentAccount = activity.getCurrentGoogleAccount()
                                    Toast.makeText(context, "계정 연결 완료", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "계정 연결 실패", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Google 계정 연결")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 데이터 관리
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "데이터 관리",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${activity.workSessions.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Text("총 세션", style = MaterialTheme.typography.bodyMedium)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${activity.projects.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                        Text("총 프로젝트", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        activity.clearAllData()
                        Toast.makeText(context, "모든 데이터가 초기화되었습니다", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B6B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("모든 데이터 초기화")
                }
            }
        }
    }
}