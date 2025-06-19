package com.example.worktimer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.worktimer.data.LatLng as CustomLatLng
import com.google.android.gms.maps.model.LatLng as GoogleLatLng
import com.example.worktimer.utils.LocationUtils.toGoogleLatLng
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun GoogleMapComponent(
    dailyRoute: DailyRoute?,
    workLocation: CustomLatLng?,
    homeLocation: CustomLatLng?,
    mapType: String = "normal",
    modifier: Modifier = Modifier
) {
    val mapView = remember { MapView(androidx.compose.ui.platform.LocalContext.current) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // 지도 생명주기 관리
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    ) { map ->
        map.getMapAsync { googleMap ->
            setupMap(googleMap, dailyRoute, workLocation, homeLocation, mapType)
        }
    }
}

private fun setupMap(
    googleMap: GoogleMap,
    dailyRoute: DailyRoute?,
    workLocation: CustomLatLng?,
    homeLocation: CustomLatLng?,
    mapType: String
) {
    // 지도 타입 설정
    googleMap.mapType = when (mapType) {
        "satellite" -> GoogleMap.MAP_TYPE_SATELLITE
        "hybrid" -> GoogleMap.MAP_TYPE_HYBRID
        "terrain" -> GoogleMap.MAP_TYPE_TERRAIN
        else -> GoogleMap.MAP_TYPE_NORMAL
    }

    // 기존 마커와 폴리라인 제거
    googleMap.clear()

    // 회사 위치 마커
    workLocation?.let { location ->
        googleMap.addMarker(
            MarkerOptions()
                .position(location.toGoogleLatLng())
                .title("회사")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )
    }

    // 집 위치 마커
    homeLocation?.let { location ->
        googleMap.addMarker(
            MarkerOptions()
                .position(location.toGoogleLatLng())
                .title("집")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
    }

    dailyRoute?.let { route ->
        if (route.points.isNotEmpty()) {
            // 동선 폴리라인 그리기
            val polylineOptions = PolylineOptions()
                .color(android.graphics.Color.parseColor("#FF6B6B"))
                .width(8f)
                .geodesic(true)

            // 위치 포인트들을 시간순으로 정렬하여 연결
            val sortedPoints = route.points.sortedBy { it.timestamp }
            sortedPoints.forEach { point ->
                polylineOptions.add(point.latLng.toGoogleLatLng())
            }

            googleMap.addPolyline(polylineOptions)

            // 시작점과 끝점 마커
            if (sortedPoints.isNotEmpty()) {
                val startPoint = sortedPoints.first()
                val endPoint = sortedPoints.last()

                // 시작점 마커
                googleMap.addMarker(
                    MarkerOptions()
                        .position(startPoint.latLng.toGoogleLatLng())
                        .title("시작: ${startPoint.locationName}")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )

                // 끝점 마커 (시작점과 다른 경우에만)
                if (startPoint.id != endPoint.id) {
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(endPoint.latLng.toGoogleLatLng())
                            .title("끝: ${endPoint.locationName}")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                }

                // 근무 세션 마커들
                route.workSessions.forEach { session ->
                    session.startLatLng?.let { startLoc ->
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(startLoc.toGoogleLatLng())
                                .title("${session.projectName} 시작")
                                .snippet("${formatTime(session.startTime)} - ${session.taskDescription}")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                        )
                    }
                }

                // 카메라를 첫 번째 포인트로 이동
                val cameraPosition = CameraPosition.builder()
                    .target(startPoint.latLng.toGoogleLatLng())
                    .zoom(13f)
                    .build()

                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                // 모든 마커가 보이도록 카메라 조정
                val boundsBuilder = LatLngBounds.builder()
                sortedPoints.forEach { point ->
                    boundsBuilder.include(point.latLng.toGoogleLatLng())
                }

                // 회사, 집 위치도 포함
                workLocation?.let {
                    boundsBuilder.include(it.toGoogleLatLng())
                }
                homeLocation?.let {
                    boundsBuilder.include(it.toGoogleLatLng())
                }

                try {
                    val bounds = boundsBuilder.build()
                    val padding = 100 // 패딩 픽셀
                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(bounds, padding)
                    )
                } catch (e: Exception) {
                    // bounds가 비어있는 경우 기본 위치로 설정
                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            GoogleLatLng(37.5665, 126.9780), // 서울 기본 위치
                            13f
                        )
                    )
                }

                // 모든 마커가 보이도록 카메라 조정
                val boundsBuilder = LatLngBounds.builder()
                sortedPoints.forEach { point ->
                    boundsBuilder.include(
                        com.google.android.gms.maps.model.LatLng(
                            point.latLng.latitude,
                            point.latLng.longitude
                        )
                    )
                }

                // 회사, 집 위치도 포함
                workLocation?.let {
                    boundsBuilder.include(
                        com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude)
                    )
                }
                homeLocation?.let {
                    boundsBuilder.include(
                        com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude)
                    )
                }
            } else {
                // 데이터가 없으면 기본 위치 (서울)
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        GoogleLatLng(37.5665, 126.9780),
                        10f
                    )
                )
            }
        } ?: run {
            // 동선 데이터가 없으면 회사나 집 위치로 이동
            val defaultLocation = workLocation ?: homeLocation
            defaultLocation?.let { location ->
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        location.toGoogleLatLng(),
                        15f
                    )
                )
            } ?: run {
                // 모든 위치가 없으면 서울 기본 위치
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        GoogleLatLng(37.5665, 126.9780),
                        10f
                    )
                )
            }
        }

        // 지도 UI 설정
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMyLocationButtonEnabled = true
            isMapToolbarEnabled = true
        }
    }

    private fun formatTime(timeMillis: Long): String {
        val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return format.format(java.util.Date(timeMillis))
    }