package com.worktracker.tracker.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.worktracker.tracker.data.LatLng as CustomLatLng
import com.worktracker.tracker.data.DailyRoute
import com.worktracker.tracker.utils.LocationUtils.toGoogleLatLng
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.LatLng as GoogleLatLng

@Composable
fun GoogleMapComponent(
    dailyRoute: DailyRoute?,
    workLocation: CustomLatLng?,
    homeLocation: CustomLatLng?,
    mapType: String = "normal",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
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
        modifier = modifier,
        update = { map ->
            map.getMapAsync { googleMap ->
                setupMap(googleMap, dailyRoute, workLocation, homeLocation, mapType)
            }
        }
    )
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

    // 카메라 이동을 위한 bounds builder
    val boundsBuilder = LatLngBounds.builder()
    var hasPoints = false

    // 회사 위치 마커
    workLocation?.let { location ->
        val googleLatLng = location.toGoogleLatLng()
        googleMap.addMarker(
            MarkerOptions()
                .position(googleLatLng)
                .title("회사")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )
        boundsBuilder.include(googleLatLng)
        hasPoints = true
    }

    // 집 위치 마커
    homeLocation?.let { location ->
        val googleLatLng = location.toGoogleLatLng()
        googleMap.addMarker(
            MarkerOptions()
                .position(googleLatLng)
                .title("집")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
        boundsBuilder.include(googleLatLng)
        hasPoints = true
    }

    // 동선 표시
    dailyRoute?.let { route ->
        if (route.points.isNotEmpty()) {
            // 위치 포인트들을 시간순으로 정렬
            val sortedPoints = route.points.sortedBy { it.timestamp }

            // 동선 폴리라인 그리기
            if (sortedPoints.size > 1) {
                val polylineOptions = PolylineOptions()
                    .color(android.graphics.Color.parseColor("#FF6B6B"))
                    .width(8f)
                    .geodesic(true)

                sortedPoints.forEach { point ->
                    val googleLatLng = point.latLng.toGoogleLatLng()
                    polylineOptions.add(googleLatLng)
                    boundsBuilder.include(googleLatLng)
                    hasPoints = true
                }

                googleMap.addPolyline(polylineOptions)
            }

            // 위치 포인트별 마커 추가
            sortedPoints.forEachIndexed { index, point ->
                val googleLatLng = point.latLng.toGoogleLatLng()

                when {
                    index == 0 -> {
                        // 시작점
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(googleLatLng)
                                .title("시작: ${point.locationName}")
                                .snippet(formatTime(point.timestamp))
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        )
                    }
                    index == sortedPoints.size - 1 -> {
                        // 끝점
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(googleLatLng)
                                .title("끝: ${point.locationName}")
                                .snippet(formatTime(point.timestamp))
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        )
                    }
                    else -> {
                        // 중간 지점들은 작은 마커로
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(googleLatLng)
                                .title(point.locationName)
                                .snippet(formatTime(point.timestamp))
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                        )
                    }
                }
            }

            // 근무 세션 마커들 (별도 색상으로)
            route.workSessions.forEach { session ->
                session.startLatLng?.let { startLoc ->
                    val googleLatLng = startLoc.toGoogleLatLng()
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(googleLatLng)
                            .title("🏢 ${session.projectName}")
                            .snippet("${formatTime(session.startTime)} - ${session.taskDescription}")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                    )
                    boundsBuilder.include(googleLatLng)
                    hasPoints = true
                }
            }
        }
    }

    // 카메라 위치 설정
    if (hasPoints) {
        try {
            val bounds = boundsBuilder.build()
            val padding = 100 // 패딩 픽셀
            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, padding)
            )
        } catch (e: Exception) {
            // bounds가 비어있거나 잘못된 경우 기본 위치
            setDefaultCamera(googleMap, workLocation, homeLocation)
        }
    } else {
        // 데이터가 없으면 기본 위치
        setDefaultCamera(googleMap, workLocation, homeLocation)
    }

    // 지도 UI 설정
    googleMap.uiSettings.apply {
        isZoomControlsEnabled = true
        isCompassEnabled = true
        isMyLocationButtonEnabled = true
        isMapToolbarEnabled = true
        isRotateGesturesEnabled = true
        isScrollGesturesEnabled = true
        isTiltGesturesEnabled = true
        isZoomGesturesEnabled = true
    }
}

private fun setDefaultCamera(
    googleMap: GoogleMap,
    workLocation: CustomLatLng?,
    homeLocation: CustomLatLng?
) {
    val defaultLocation = workLocation ?: homeLocation
    if (defaultLocation != null) {
        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                defaultLocation.toGoogleLatLng(),
                15f
            )
        )
    } else {
        // 모든 위치가 없으면 서울 기본 위치
        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                GoogleLatLng(37.5665, 126.9780),
                10f
            )
        )
    }
}

private fun formatTime(timeMillis: Long): String {
    val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return format.format(java.util.Date(timeMillis))
}