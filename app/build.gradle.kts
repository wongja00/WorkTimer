plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.worktimer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.worktimer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    composeOptions {
        //kotlinCompilerExtensionVersion = "1.5.10"
    }

}

dependencies {
    val appcompat_version = "1.7.1"

    // 기존 의존성들
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.benchmark.macro)
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.appcompat:appcompat-resources:1.7.1")

    // 테스트 의존성
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // 기존 Compose 관련
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.1")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // 캘린더 라이브러리
    implementation("com.kizitonwose.calendar:view:2.7.0")

    // Google Play Services (위치 및 지도)
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation ("com.google.maps.android:maps-compose:2.11.4")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // ==================== WorkTimer 앱 추가 의존성들 ====================

    // Google Drive API - 더 안정적인 버전으로 변경
    implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0") {
        exclude(group = "org.apache.httpcomponents")
        exclude(module = "guava-jdk5")
        exclude(group = "com.google.guava", module = "listenablefuture")
    }
    implementation("com.google.api-client:google-api-client-android:2.0.0") {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1") {
        exclude(group = "org.apache.httpcomponents")
    }

    // JSON 처리
    implementation("com.google.code.gson:gson:2.10.1")

    // 코루틴 (이미 lifecycle-runtime-ktx에 포함되어 있지만 명시적으로 추가)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // 권한 처리 (Activity Result API)
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // HTTP Client 대체 (Apache HttpComponents 충돌 방지)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // ==================== 광고 관련 의존성 ====================

    // Google Mobile Ads (AdMob)
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    // AdMob Mediation (선택사항 - 더 많은 광고 네트워크)
    implementation("com.google.ads.mediation:facebook:6.16.0.0")
    implementation("com.google.ads.mediation:unity:4.9.2.0")

    // ==================== 선택사항 ====================
    // 만약 더 고급 캘린더 기능이 필요하다면:
    // implementation("com.kizitonwose.calendar:compose:2.5.0")

    // 만약 더 나은 위치 정확도가 필요하다면:
    // implementation("com.google.android.gms:play-services-location:21.0.1")
}