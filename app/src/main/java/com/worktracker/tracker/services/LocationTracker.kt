package com.worktracker.tracker.services

import android.content.Context
import android.location.Location
import com.worktracker.tracker.data.LatLng
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationTracker(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    // 위치 추적 설정
    companion object {
        private const val UPDATE_INTERVAL_ACTIVE = 30000L      // 활성 상태: 30초
        private const val UPDATE_INTERVAL_PASSIVE = 300000L    // 비활성 상태: 5분
        private const val FASTEST_INTERVAL = 15000L            // 최소 간격: 15초
        private const val MIN_DISTANCE_CHANGE = 50f            // 최소 이동 거리: 50m
    }

    fun startTracking(isWorkActive: Boolean = false) {
        if (_isTracking.value) return

        try {
            val locationRequest = createLocationRequest(isWorkActive)

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        val latLng = LatLng(location.latitude, location.longitude)
                        _currentLocation.value = latLng

                        // 위치 변화 콜백 (MainActivity에서 처리)
                        onLocationChanged?.invoke(location)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                null
            )

            _isTracking.value = true

        } catch (e: SecurityException) {
            // 권한이 없는 경우
            _isTracking.value = false
        }
    }

    fun stopTracking() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }
        locationCallback = null
        _isTracking.value = false
    }

    fun updateTrackingMode(isWorkActive: Boolean) {
        if (_isTracking.value) {
            stopTracking()
            startTracking(isWorkActive)
        }
    }

    private fun createLocationRequest(isWorkActive: Boolean): LocationRequest {
        val interval = if (isWorkActive) UPDATE_INTERVAL_ACTIVE else UPDATE_INTERVAL_PASSIVE
        val priority = if (isWorkActive) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY

        return LocationRequest.Builder(priority, interval)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_CHANGE)
            .setMaxUpdateDelayMillis(interval * 2)
            .build()
    }

    // 위치 변화 콜백
    var onLocationChanged: ((Location) -> Unit)? = null

    // 현재 위치 한 번만 가져오기
    fun getCurrentLocation(callback: (LatLng?) -> Unit) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    callback(LatLng(location.latitude, location.longitude))
                } else {
                    callback(null)
                }
            }.addOnFailureListener {
                callback(null)
            }
        } catch (e: SecurityException) {
            callback(null)
        }
    }

    // 배터리 최적화를 위한 스마트 추적
    fun optimizeForBattery() {
        // 배터리 절약 모드에서는 추적 간격을 늘림
        if (_isTracking.value) {
            updateTrackingMode(false) // 비활성 모드로 전환
        }
    }

    // 정확도 우선 모드
    fun optimizeForAccuracy() {
        // 정확도 우선 모드에서는 추적 간격을 줄임
        if (_isTracking.value) {
            updateTrackingMode(true) // 활성 모드로 전환
        }
    }
}