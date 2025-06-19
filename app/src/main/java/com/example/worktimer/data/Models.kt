package com.example.worktimer.data

import java.util.UUID

data class WorkSession(
    val id: String = UUID.randomUUID().toString(),
    val date: String, // yyyy-MM-dd 형식
    val startTime: Long,
    val endTime: Long,
    val projectName: String = "일반 업무",
    val taskDescription: String = "",
    val hourlyRate: Double = 0.0,
    val location: String = "기타",
    val startLatLng: LatLng? = null,
    val endLatLng: LatLng? = null
) {
    val duration: Long get() = endTime - startTime
    val totalSeconds: Int get() = (duration / 1000).toInt()
    val earnings: Double get() = (duration / 1000.0 / 3600.0) * hourlyRate
}

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val defaultHourlyRate: Double,
    val color: Int = android.graphics.Color.BLUE,
    val description: String = "",
    val isActive: Boolean = true
)

data class EarningsReport(
    val totalHours: Double,
    val totalEarnings: Double,
    val averageHourlyRate: Double,
    val sessionsCount: Int,
    val topProject: String?,
    val mostProductiveHour: Int?
)

data class LatLng(
    val latitude: Double,
    val longitude: Double
)

data class LocationPoint(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long,
    val latLng: LatLng,
    val locationName: String = "알 수 없음",
    val sessionId: String? = null
)

data class DailyRoute(
    val date: String, // yyyy-MM-dd
    val points: List<LocationPoint>,
    val workSessions: List<WorkSession>
) {
    val totalDistance: Double get() = calculateTotalDistance()

    private fun calculateTotalDistance(): Double {
        if (points.size < 2) return 0.0

        var totalDistance = 0.0
        for (i in 1 until points.size) {
            totalDistance += calculateDistance(points[i-1].latLng, points[i].latLng)
        }
        return totalDistance
    }

    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val earthRadius = 6371000.0 // 지구 반지름 (미터)
        val lat1Rad = Math.toRadians(point1.latitude)
        val lat2Rad = Math.toRadians(point2.latitude)
        val deltaLatRad = Math.toRadians(point2.latitude - point1.latitude)
        val deltaLngRad = Math.toRadians(point2.longitude - point1.longitude)

        val a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLngRad / 2) * Math.sin(deltaLngRad / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }
}