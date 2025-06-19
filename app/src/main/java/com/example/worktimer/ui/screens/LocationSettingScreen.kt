package com.example.worktimer.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.worktimer.MainActivity
import com.example.worktimer.ui.components.ProjectDialog
import com.example.worktimer.utils.Formatters

@Composable
fun LocationSettingScreen(activity: MainActivity) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("work_timer", Context.MODE_PRIVATE)

    var isLocationTrackingEnabled by remember {
        mutableStateOf(prefs.getBoolean("locationTrackingEnabled", false))
    }

    // 권한 요청 런처
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            isLocationTrackingEnabled = true
            prefs.edit().putBoolean("locationTrackingEnabled", true).apply()
            activity.startLocationUpdates()
            Toast.makeText(context, "위치 추적이 활성화되었습니다", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "위치 설정",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 자동 출퇴근 설정
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "자동 출퇴근",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "위치 기반 자동 출퇴근 처리",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    Switch(
                        checked = isLocationTrackingEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                val fineLocationGranted = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED

                                val coarseLocationGranted = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED

                                if (fineLocationGranted && coarseLocationGranted) {
                                    isLocationTrackingEnabled = true
                                    prefs.edit().putBoolean("locationTrackingEnabled", true).apply()
                                    activity.startLocationUpdates()
                                    Toast.makeText(context, "위치 추적이 활성화되었습니다", Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            } else {
                                isLocationTrackingEnabled = false
                                prefs.edit().putBoolean("locationTrackingEnabled", false).apply()
                                activity.stopLocationUpdates()
                                Toast.makeText(context, "위치 추적이 비활성화되었습니다", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 회사 위치 설정
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "회사 위치",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                activity.workLocation?.let { location ->
                    Text(
                        text = "위도: ${String.format("%.6f", location.latitude)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "경도: ${String.format("%.6f", location.longitude)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                } ?: run {
                    Text(
                        text = "회사 위치가 설정되지 않았습니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            activity.getCurrentLocation { location ->
                                location?.let {
                                    activity.updateWorkLocation(it)
                                    Toast.makeText(context, "회사 위치가 설정되었습니다", Toast.LENGTH_SHORT)
                                        .show()
                                } ?: run {
                                    Toast.makeText(context, "위치를 가져올 수 없습니다", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("현재 위치를 회사로 설정")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 집 위치 설정
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "집 위치",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                activity.homeLocation?.let { location ->
                    Text(
                        text = "위도: ${String.format("%.6f", location.latitude)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "경도: ${String.format("%.6f", location.longitude)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                } ?: run {
                    Text(
                        text = "집 위치가 설정되지 않았습니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { /* 집 위치 설정 로직 */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("현재 위치를 집으로 설정")
                    }

                    Button(
                        onClick = { /* 집 위치 초기화 로직 */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6B6B)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("초기화")
                    }
                }
            }
        }
    }
}