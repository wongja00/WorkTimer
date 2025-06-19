package com.worktracker.tracker.utils

import com.worktracker.tracker.data.LatLng as CustomLatLng
import com.google.android.gms.maps.model.LatLng as GoogleLatLng

object LocationUtils {

    // Google Maps LatLng -> 커스텀 LatLng 변환
    fun GoogleLatLng.toCustomLatLng(): CustomLatLng {
        return CustomLatLng(
            latitude = this.latitude,
            longitude = this.longitude
        )
    }

    // 커스텀 LatLng -> Google Maps LatLng 변환
    fun CustomLatLng.toGoogleLatLng(): GoogleLatLng {
        return GoogleLatLng(
            this.latitude,
            this.longitude
        )
    }

    // Nullable 버전들
    fun GoogleLatLng?.toCustomLatLngOrNull(): CustomLatLng? {
        return this?.toCustomLatLng()
    }

    fun CustomLatLng?.toGoogleLatLngOrNull(): GoogleLatLng? {
        return this?.toGoogleLatLng()
    }

    // 거리 계산 함수
    fun calculateDistance(point1: CustomLatLng, point2: CustomLatLng): Double {
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

    fun calculateDistanceFromGoogle(point1: GoogleLatLng, point2: GoogleLatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0]
    }
}