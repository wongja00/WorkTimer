package com.worktracker.tracker.utils

import com.worktracker.tracker.data.WorkSession
import com.worktracker.tracker.data.DailyRoute
import com.worktracker.tracker.data.LatLng
import java.text.SimpleDateFormat
import java.util.*

object Formatters {
    fun formatTime(timeMillis: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timeMillis))
    }

    fun formatElapsedTime(millis: Long): String {
        val hours = millis / 1000 / 3600
        val minutes = (millis / 1000 % 3600) / 60
        val seconds = millis / 1000 % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatDate(timeMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
        return sdf.format(Date(timeMillis))
    }

    fun formatCurrency(amount: Double): String {
        return "₩${String.format("%,.0f", amount)}"
    }

    fun formatDistance(meters: Double): String {
        return when {
            meters < 1000 -> String.format("%.0fm", meters)
            else -> String.format("%.1fkm", meters / 1000)
        }
    }
}

object EarningsCalculator {
    fun calculateMonthlyEarnings(workSessions: List<WorkSession>): List<Pair<String, Double>> {
        val monthlyMap = mutableMapOf<String, Double>()
        val calendar = Calendar.getInstance()

        workSessions.forEach { session ->
            calendar.timeInMillis = session.startTime
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val key = "${year}년 ${month}월"

            monthlyMap[key] = (monthlyMap[key] ?: 0.0) + session.earnings
        }

        return monthlyMap.toList().sortedByDescending { it.first }
    }

    fun calculateWeeklyEarnings(workSessions: List<WorkSession>): List<Pair<String, Double>> {
        val calendar = Calendar.getInstance()
        val weeklyMap = mutableMapOf<String, Double>()

        workSessions.forEach { session ->
            calendar.timeInMillis = session.startTime
            val year = calendar.get(Calendar.YEAR)
            val week = calendar.get(Calendar.WEEK_OF_YEAR)
            val key = "${year}년 ${week}주차"

            weeklyMap[key] = (weeklyMap[key] ?: 0.0) + session.earnings
        }

        return weeklyMap.toList().sortedByDescending { it.first }
    }

    fun calculateHourlyProductivity(workSessions: List<WorkSession>): List<Pair<Int, Double>> {
        val hourlyMap = mutableMapOf<Int, Double>()

        workSessions.forEach { session ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = session.startTime
            val hour = calendar.get(Calendar.HOUR_OF_DAY)

            hourlyMap[hour] = (hourlyMap[hour] ?: 0.0) + session.earnings
        }

        return (0..23).map { hour ->
            hour to (hourlyMap[hour] ?: 0.0)
        }.sortedByDescending { it.second }
    }

    fun getTodaySessions(workSessions: List<WorkSession>): List<WorkSession> {
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(today.time)
        return workSessions.filter { it.date == todayStr }
    }
}

object LocationAnalyzer {
    fun analyzeMovementPatterns(dailyRoutes: List<DailyRoute>): MovementAnalysis {
        val totalDistance = dailyRoutes.sumOf { it.totalDistance }
        val totalDays = dailyRoutes.size
        val averageDistance = if (totalDays > 0) totalDistance / totalDays else 0.0

        val mostVisitedLocations = dailyRoutes
            .flatMap { it.points }
            .groupBy { it.locationName }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        val peakMovementHours = dailyRoutes
            .flatMap { it.points }
            .groupBy {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = it.timestamp
                calendar.get(Calendar.HOUR_OF_DAY)
            }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(3)

        return MovementAnalysis(
            totalDistance = totalDistance,
            averageDailyDistance = averageDistance,
            totalDays = totalDays,
            mostVisitedLocations = mostVisitedLocations,
            peakMovementHours = peakMovementHours
        )
    }

    fun calculateCommutingPattern(dailyRoutes: List<DailyRoute>): CommutingPattern {
        val workDays = dailyRoutes.filter { it.workSessions.isNotEmpty() }

        val averageStartTime = workDays
            .mapNotNull { route -> route.workSessions.minByOrNull { it.startTime }?.startTime }
            .average()
            .toLong()

        val averageEndTime = workDays
            .mapNotNull { route -> route.workSessions.maxByOrNull { it.endTime }?.endTime }
            .average()
            .toLong()

        val averageCommuteDistance = workDays
            .filter { it.points.size >= 2 }
            .map { route ->
                val firstPoint = route.points.minByOrNull { it.timestamp }
                val lastPoint = route.points.maxByOrNull { it.timestamp }
                if (firstPoint != null && lastPoint != null) {
                    calculateDistance(firstPoint.latLng, lastPoint.latLng)
                } else 0.0
            }
            .average()

        return CommutingPattern(
            averageStartTime = averageStartTime,
            averageEndTime = averageEndTime,
            averageCommuteDistance = averageCommuteDistance,
            workDaysCount = workDays.size
        )
    }

    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val earthRadius = 6371000.0
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

data class MovementAnalysis(
    val totalDistance: Double,
    val averageDailyDistance: Double,
    val totalDays: Int,
    val mostVisitedLocations: List<Pair<String, Int>>,
    val peakMovementHours: List<Pair<Int, Int>>
)

data class CommutingPattern(
    val averageStartTime: Long,
    val averageEndTime: Long,
    val averageCommuteDistance: Double,
    val workDaysCount: Int
)


fun calculateHourlyProductivity(workSessions: List<WorkSession>): List<Pair<Int, Double>> {
    val hourlyMap = mutableMapOf<Int, Double>()

    workSessions.forEach { session ->
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = session.startTime
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        hourlyMap[hour] = (hourlyMap[hour] ?: 0.0) + session.earnings
    }

    return (0..23).map { hour ->
        hour to (hourlyMap[hour] ?: 0.0)
    }.sortedByDescending { it.second }
}

fun getTodaySessions(workSessions: List<WorkSession>): List<WorkSession> {
    val today = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayStr = dateFormat.format(today.time)
    return workSessions.filter { it.date == todayStr }
}
