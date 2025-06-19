package com.example.worktimer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.worktimer.ui.theme.WorkTimerTheme
import com.example.worktimer.ui.screens.WorkTimerApp
import com.example.worktimer.data.*
import com.example.worktimer.services.*
import com.google.android.gms.location.*
import com.example.worktimer.utils.LocationUtils.toCustomLatLng
import com.example.worktimer.utils.LocationUtils.toGoogleLatLng
import com.google.android.gms.maps.model.LatLng as GoogleLatLng
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*
import com.example.worktimer.services.AdMobService


class MainActivity : ComponentActivity() {
    // Location Services
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isLocationUpdatesActive = false

    // Google Services
    private val googleSignInService = GoogleSignInService()

    // AdMob 서비스 추가
    private lateinit var adMobService: AdMobService
    // 광고 표시 횟수 추적
    private var workSessionCount = 0
    private val AD_FREQUENCY = 3 // 3번째 작업 완료마다 광고 표시

    // 권한 요청 런처들
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    // State variables
    var workSessions by mutableStateOf(listOf<WorkSession>())
        private set
    var projects by mutableStateOf(listOf<Project>())
        private set
    var currentProject by mutableStateOf<Project?>(null)
        private set
    var currentTaskDescription by mutableStateOf("")
        private set

    // 위치 추적 관련
    var locationPoints by mutableStateOf(listOf<LocationPoint>())
        private set

    // Location related
    var workLocation by mutableStateOf<GoogleLatLng?>(null)
        private set
    var homeLocation by mutableStateOf<GoogleLatLng?>(null)
        private set
    var currentLocation by mutableStateOf<GoogleLatLng?>(null)
        private set
    var currentLocationName by mutableStateOf("위치 확인 중...")
        private set

    // Timer related
    var isWorking by mutableStateOf(false)
        private set
    var startTime by mutableStateOf(0L)
        private set
    var elapsedTime by mutableStateOf(0L)
        private set

    companion object {
        private const val LOCATION_THRESHOLD = 100f // 100m
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setupPermissionLaunchers()
        initializeServices()
        loadSavedData()

        setContent {
            WorkTimerTheme {
                WorkTimerApp(this)
            }
        }
    }

    private fun setupPermissionLaunchers() {
        // 위치 권한 요청 런처
        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineLocationGranted && coarseLocationGranted) {
                startLocationUpdates()
            }
        }

        // 알림 권한 요청 런처 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    // 알림 권한이 허용됨
                }
            }
        }
    }

    private fun initializeServices() {
        // 기존 서비스 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        setupLocationCallback()

        // Google Sign-In 서비스 초기화 (Activity Result API 사용)
        googleSignInService.setup(this)

        // AdMob 초기화
        adMobService = AdMobService(this)
        adMobService.initialize()

        // 권한 요청
        requestPermissionsIfNeeded()
    }

    private fun requestPermissionsIfNeeded() {
        val permissionsToRequest = mutableListOf<String>()

        // 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.addAll(listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }

        // 알림 권한 확인 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 위치 권한 요청
        if (permissionsToRequest.isNotEmpty()) {
            locationPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // 권한 요청을 위한 public 함수들
    fun requestLocationPermissions() {
        locationPermissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    private fun loadSavedData() {
        val prefs = getSharedPreferences("work_timer", Context.MODE_PRIVATE)

        // 위치 설정 로드
        val workLat = prefs.getFloat("workLat", 0f)
        val workLng = prefs.getFloat("workLng", 0f)
        if (workLat != 0f && workLng != 0f) {
            workLocation = GoogleLatLng(workLat.toDouble(), workLng.toDouble())
        }

        val homeLat = prefs.getFloat("homeLat", 0f)
        val homeLng = prefs.getFloat("homeLng", 0f)
        if (homeLat != 0f && homeLng != 0f) {
            homeLocation = GoogleLatLng(homeLat.toDouble(), homeLng.toDouble())
        }

        // 타이머 상태 로드
        isWorking = prefs.getBoolean("isRunning", false)
        startTime = prefs.getLong("startTime", 0L)

        // 프로젝트 데이터 로드
        val projectsJson = prefs.getString("projects", "[]")
        projects = try {
            Gson().fromJson(projectsJson, Array<Project>::class.java).toList()
        } catch (e: Exception) {
            listOf()
        }

        // 작업 세션 데이터 로드
        val sessionsJson = prefs.getString("workSessions", "[]")
        workSessions = try {
            Gson().fromJson(sessionsJson, Array<WorkSession>::class.java).toList()
        } catch (e: Exception) {
            listOf()
        }

        // 위치 포인트 데이터 로드
        val pointsJson = prefs.getString("locationPoints", "[]")
        locationPoints = try {
            Gson().fromJson(pointsJson, Array<LocationPoint>::class.java).toList()
        } catch (e: Exception) {
            listOf()
        }
        // 기본 프로젝트가 없으면 생성
        if (projects.isEmpty()) {
            projects = createDefaultProjects()
            saveProjectsData()
        }

        // 현재 프로젝트 설정
        if (currentProject == null && projects.isNotEmpty()) {
            currentProject = projects.first()
        }
    }

    fun saveProjectsData() {
        val prefs = getSharedPreferences("work_timer", Context.MODE_PRIVATE)
        val projectsJson = Gson().toJson(projects)
        prefs.edit().putString("projects", projectsJson).apply()
    }

    private fun saveWorkSessionsData() {
        val prefs = getSharedPreferences("work_timer", Context.MODE_PRIVATE)
        val sessionsJson = Gson().toJson(workSessions)
        prefs.edit().putString("workSessions", sessionsJson).apply()
    }

    private fun saveLocationPointsData() {
        val prefs = getSharedPreferences("work_timer", Context.MODE_PRIVATE)
        val pointsJson = Gson().toJson(locationPoints)
        prefs.edit().putString("locationPoints", pointsJson).apply()
    }

    fun getDailyRoute(date: String): DailyRoute? {
        val dailyPoints = locationPoints.filter { point ->
            val pointDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(point.timestamp))
            pointDate == date
        }

        val dailySessions = workSessions.filter { it.date == date }

        return if (dailyPoints.isNotEmpty() || dailySessions.isNotEmpty()) {
            DailyRoute(
                date = date,
                points = dailyPoints,
                workSessions = dailySessions
            )
        } else {
            null
        }
    }

    private fun saveTimerState() {
        val prefs = getSharedPreferences("work_timer", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("isRunning", isWorking)
            putLong("startTime", startTime)
            apply()
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    currentLocation = GoogleLatLng(location.latitude, location.longitude)
                    updateLocationStatus()
                    checkAutoWorkTracking(location)
                }
            }
        }
    }

    private fun updateLocationStatus() {
        currentLocation?.let { current ->
            currentLocationName = when {
                workLocation != null && calculateDistance(current, workLocation!!) <= LOCATION_THRESHOLD -> "회사"
                homeLocation != null && calculateDistance(current, homeLocation!!) <= LOCATION_THRESHOLD -> "집"
                else -> "기타"
            }

            // 위치 포인트 저장
            saveLocationPoint(current, currentLocationName)
        }
    }

    private fun saveLocationPoint(latLng: GoogleLatLng, locationName: String) {
        val point = LocationPoint(
            timestamp = System.currentTimeMillis(),
            latLng = latLng.toCustomLatLng(),
            locationName = locationName,
            sessionId = if (isWorking) currentProject?.id else null
        )

        // 중복 방지: 마지막 포인트와 거리나 시간이 충분히 차이날 때만 저장
        val lastPoint = locationPoints.lastOrNull()
        if (lastPoint == null ||
            point.timestamp - lastPoint.timestamp > 60000 || // 1분 이상 차이
            calculateDistanceInMeters(point.latLng, lastPoint.latLng) > 50) { // 50m 이상 차이

            locationPoints = locationPoints + point
            saveLocationPointsData()
        }
    }

    private fun calculateDistanceInMeters(point1: com.example.worktimer.data.LatLng, point2: com.example.worktimer.data.LatLng): Double {
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

    private fun checkAutoWorkTracking(location: Location) {
        val prefs = getSharedPreferences("work_timer", Context.MODE_PRIVATE)
        val isLocationTrackingEnabled = prefs.getBoolean("locationTrackingEnabled", false)

        if (!isLocationTrackingEnabled || workLocation == null) return

        val workLoc = Location("work").apply {
            latitude = workLocation!!.latitude
            longitude = workLocation!!.longitude
        }

        val distanceToWork = location.distanceTo(workLoc)

        // 자동 출근 (회사에서 100m 이내, 아직 일하지 않는 상태)
        if (distanceToWork <= LOCATION_THRESHOLD && !isWorking) {
            startWork()
            showNotification("자동 출근", "자동으로 출근 처리되었습니다")
        }
        // 자동 퇴근 (집에서 100m 이내, 현재 일하는 상태)
        else if (homeLocation != null && isWorking) {
            val homeLoc = Location("home").apply {
                latitude = homeLocation!!.latitude
                longitude = homeLocation!!.longitude
            }
            val distanceToHome = location.distanceTo(homeLoc)

            if (distanceToHome <= LOCATION_THRESHOLD) {
                stopWork()
                showNotification("자동 퇴근", "자동으로 퇴근 처리되었습니다")
            }
        }
    }

    private fun calculateDistance(location1: GoogleLatLng, location2: GoogleLatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            location1.latitude, location1.longitude,
            location2.latitude, location2.longitude,
            results
        )
        return results[0]
    }


    fun startWork() {
        if (!isWorking && currentProject != null) {
            isWorking = true
            startTime = System.currentTimeMillis()
            saveTimerState()
            startLocationUpdates()
        }
    }

    fun stopWork() {
        if (isWorking && currentProject != null) {
            val endTime = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.format(Date(startTime))

            // 작업 세션 저장 (위치 정보 포함)
            val session = WorkSession(
                date = date,
                startTime = startTime,
                endTime = endTime,
                projectName = currentProject!!.name,
                taskDescription = currentTaskDescription,
                hourlyRate = currentProject!!.defaultHourlyRate,
                location = currentLocationName,
                startLatLng = currentLocation?.toCustomLatLng(),
                endLatLng = currentLocation?.toCustomLatLng()
            )

            workSessions = workSessions + session
            saveWorkSessionsData()

            // 상태 초기화
            isWorking = false
            elapsedTime = 0L
            currentTaskDescription = ""
            saveTimerState()
            stopLocationUpdates()

            // 광고 표시 로직
            workSessionCount++
            if (workSessionCount % AD_FREQUENCY == 0) {
                showInterstitialAd()
            }

            // 클라우드 동기화
            if (googleSignInService.getCurrentAccount() != null) {
                lifecycleScope.launch {
                    syncToCloud()
                }
            }
        }
    }

    fun updateCurrentProject(project: Project) {
        currentProject = project
    }

    fun updateCurrentTaskDescription(description: String) {
        currentTaskDescription = description
    }

    fun updateElapsedTime() {
        if (isWorking && startTime != 0L) {
            val currentTime = System.currentTimeMillis()
            elapsedTime = currentTime - startTime
        }
    }

    fun addProject(project: Project) {
        projects = projects + project
        saveProjectsData()
    }

    fun updateProject(project: Project) {
        projects = projects.map {
            if (it.id == project.id) project else it
        }
        saveProjectsData()
    }

    fun toggleProjectActive(projectId: String) {
        projects = projects.map {
            if (it.id == projectId) it.copy(isActive = !it.isActive)
            else it
        }
        saveProjectsData()
    }

    fun updateWorkLocation(location: GoogleLatLng) {
        workLocation = location
        val prefs = getSharedPreferences("work_timer", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat("workLat", location.latitude.toFloat())
            putFloat("workLng", location.longitude.toFloat())
            apply()
        }
    }

    fun updateHomeLocation(location: GoogleLatLng?) {
        homeLocation = location
        val prefs = getSharedPreferences("work_timer", Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (location != null) {
                putFloat("homeLat", location.latitude.toFloat())
                putFloat("homeLng", location.longitude.toFloat())
            } else {
                putFloat("homeLat", 0f)
                putFloat("homeLng", 0f)
            }
            apply()
        }
    }

    fun clearAllData() {
        val prefs = getSharedPreferences("work_timer", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        workSessions = listOf()
        projects = createDefaultProjects()
        saveProjectsData()
    }

    fun getCurrentGoogleAccount(): GoogleSignInAccount? {
        return googleSignInService.getCurrentAccount()
    }

    fun signInToGoogle(onResult: (Boolean) -> Unit) {
        googleSignInService.signIn(onResult)
    }

    fun signOutFromGoogle(onComplete: () -> Unit) {
        googleSignInService.signOut(onComplete)
    }

    suspend fun syncToCloud(): Boolean {
        return googleSignInService.syncToCloud(workSessions)
    }

    suspend fun syncFromCloud(): Boolean {
        val sessions = googleSignInService.syncFromCloud()
        return if (sessions != null) {
            workSessions = sessions
            saveWorkSessionsData()
            true
        } else {
            false
        }
    }

    // 기본 프로젝트 생성 함수 개선
    private fun createDefaultProjects(): List<Project> {
        return listOf(
            Project(
                name = "일반 업무",
                defaultHourlyRate = 15000.0,
                description = "기본 업무 프로젝트",
                isActive = true
            ),
            Project(
                name = "개발 프로젝트",
                defaultHourlyRate = 25000.0,
                description = "개발 관련 업무",
                isActive = true
            ),
            Project(
                name = "컨설팅",
                defaultHourlyRate = 35000.0,
                description = "컨설팅 업무",
                isActive = false
            )
        )
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "WORK_TIMER_CHANNEL",
                "Work Timer Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, content: String) {
        val notification = NotificationCompat.Builder(this, "WORK_TIMER_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(1, notification)
        }
    }

    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(15000)
                .build()

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            isLocationUpdatesActive = true
        }
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isLocationUpdatesActive = false
    }

    fun getCurrentLocation(onLocationReceived: (GoogleLatLng?) -> Unit) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    onLocationReceived(GoogleLatLng(location.latitude, location.longitude))
                } else {
                    onLocationReceived(null)
                }
            }.addOnFailureListener {
                onLocationReceived(null)
            }
        } else {
            onLocationReceived(null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }


    // 광고 관련 함수들 추가
    fun showInterstitialAd() {
        adMobService.showInterstitialAd(this) {
            // 광고 닫힌 후 실행할 코드
            showNotification("수고하셨습니다!", "오늘도 좋은 하루 되세요 😊")
        }
    }

    fun showRewardedAd(onRewarded: (Int) -> Unit) {
        adMobService.showRewardedAd(
            activity = this,
            onRewarded = { rewardAmount ->
                onRewarded(rewardAmount)
                showNotification("보상 획득!", "추가 기능이 해제되었습니다! 🎉")
            }
        ) {
            // 광고 닫힌 후 실행할 코드
        }
    }
    fun getAdMobService(): AdMobService = adMobService


    fun deleteProject(projectId: String) {
        projects = projects.filter { it.id != projectId }

        // 현재 선택된 프로젝트가 삭제된 경우 다른 프로젝트로 변경
        if (currentProject?.id == projectId) {
            currentProject = projects.filter { it.isActive }.firstOrNull()
        }

        saveProjectsData()
    }


}