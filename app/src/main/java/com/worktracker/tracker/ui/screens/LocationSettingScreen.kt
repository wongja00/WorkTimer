package com.worktracker.tracker.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
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
import androidx.core.content.ContextCompat
import com.worktracker.tracker.MainActivity

@Composable
fun LocationSettingScreen(activity: MainActivity) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("work_timer", Context.MODE_PRIVATE)

    var isLocationTrackingEnabled by remember {
        mutableStateOf(prefs.getBoolean("locationTrackingEnabled", false))
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

        // 권한 상태 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hasLocationPermissions(context))
                    Color(0xFFE8F5E8) else Color(0xFFFFF3E0)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (hasLocationPermissions(context)) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (hasLocationPermissions(context)) Color(0xFF2E7D32) else Color(0xFFFF6B00)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasLocationPermissions(context)) "위치 권한 허용됨" else "위치 권한 필요",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (hasLocationPermissions(context))
                        "위치 기반 기능을 사용할 수 있습니다."
                    else "자동 출퇴근 기능을 사용하려면 위치 권한이 필요합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                if (!hasLocationPermissions(context)) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { activity.requestLocationPermissions() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("위치 권한 허용")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                        checked = isLocationTrackingEnabled && hasLocationPermissions(context),
                        enabled = hasLocationPermissions(context),
                        onCheckedChange = { enabled ->
                            if (hasLocationPermissions(context)) {
                                isLocationTrackingEnabled = enabled
                                prefs.edit().putBoolean("locationTrackingEnabled", enabled).apply()

                                if (enabled) {
                                    activity.startLocationUpdates()
                                    Toast.makeText(context, "위치 추적이 활성화되었습니다", Toast.LENGTH_SHORT).show()
                                } else {
                                    activity.stopLocationUpdates()
                                    Toast.makeText(context, "위치 추적이 비활성화되었습니다", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "먼저 위치 권한을 허용해주세요", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                if (!hasLocationPermissions(context)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "위치 권한이 필요합니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 회사 위치 설정
        LocationCard(
            title = "회사 위치",
            location = activity.workLocation,
            onSetLocation = {
                if (hasLocationPermissions(context)) {
                    activity.getCurrentLocation { location ->
                        location?.let {
                            activity.updateWorkLocation(it)
                            Toast.makeText(context, "회사 위치가 설정되었습니다", Toast.LENGTH_SHORT).show()
                        } ?: run {
                            Toast.makeText(context, "위치를 가져올 수 없습니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show()
                }
            },
            hasPermission = hasLocationPermissions(context)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 집 위치 설정
        LocationCard(
            title = "집 위치",
            location = activity.homeLocation,
            onSetLocation = {
                if (hasLocationPermissions(context)) {
                    activity.getCurrentLocation { location ->
                        location?.let {
                            activity.updateHomeLocation(it)
                            Toast.makeText(context, "집 위치가 설정되었습니다", Toast.LENGTH_SHORT).show()
                        } ?: run {
                            Toast.makeText(context, "위치를 가져올 수 없습니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show()
                }
            },
            onClearLocation = {
                activity.updateHomeLocation(null)
                Toast.makeText(context, "집 위치가 초기화되었습니다", Toast.LENGTH_SHORT).show()
            },
            hasPermission = hasLocationPermissions(context)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 위치 추적 정보
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "위치 추적 정보",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• 자동 출퇴근: 회사/집에서 100m 이내 진입 시 자동 처리",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• 배터리 최적화: 스마트 위치 추적으로 배터리 절약",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• 프라이버시: 위치 데이터는 기기에만 저장됩니다",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun LocationCard(
    title: String,
    location: com.google.android.gms.maps.model.LatLng?,
    onSetLocation: () -> Unit,
    onClearLocation: (() -> Unit)? = null,
    hasPermission: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            location?.let { loc ->
                Text(
                    text = "위도: ${String.format("%.6f", loc.latitude)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "경도: ${String.format("%.6f", loc.longitude)}",
                    style = MaterialTheme.typography.bodySmall
                )
            } ?: run {
                Text(
                    text = "${title}이(가) 설정되지 않았습니다",
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
                    onClick = onSetLocation,
                    enabled = hasPermission,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("현재 위치로 설정")
                }

                onClearLocation?.let { clearAction ->
                    Button(
                        onClick = clearAction,
                        enabled = location != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6B6B)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("초기화")
                    }
                }
            }

            if (!hasPermission) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "위치 권한이 필요합니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun hasLocationPermissions(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}