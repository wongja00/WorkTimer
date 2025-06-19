package com.example.worktimer.services

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.test.uiautomator.v18.BuildConfig
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform


class AdMobService(private val context: Context) {

    companion object {
        private const val TAG = "AdMobService"

        // 🔥 실제 배포용 광고 ID (AdMob 콘솔에서 발급받은 ID로 교체)
        private const val IS_DEBUG = BuildConfig.DEBUG

        // 테스트/실제 광고 ID 분기
        val BANNER_AD_UNIT_ID = if (IS_DEBUG) {
            "ca-app-pub-3940256099942544/6300978111" // 테스트 ID
        } else {
            "ca-app-pub-5787012535588341/5725061780" // 실제 ID
        }

        val INTERSTITIAL_AD_UNIT_ID = if (IS_DEBUG) {
            "ca-app-pub-3940256099942544/1033173712" // 테스트 ID
        } else {
            "ca-app-pub-5787012535588341/8089971870" // 실제 ID
        }

        val REWARDED_AD_UNIT_ID = if (IS_DEBUG) {
            "ca-app-pub-3940256099942544/5224354917" // 테스트 ID
        } else {
            "ca-app-pub-5787012535588341/4625369581" // 실제 ID
        }
    }

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    fun initialize() {
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "AdMob 초기화 완료: ${initializationStatus.adapterStatusMap}")

            // 실제 광고 사용 시 추가 설정
            if (!IS_DEBUG) {
                setupAdRequestConfiguration()
            }

        }

        // 전면 광고 미리 로드
        loadInterstitialAd()

        // 보상형 광고 미리 로드
        loadRewardedAd()
    }

    private fun setupAdRequestConfiguration() {
        // 실제 광고 요청 시 추가 설정
        val configuration = RequestConfiguration.Builder()
            .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
            .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
            .build()

        MobileAds.setRequestConfiguration(configuration)
    }

    // 배너 광고 생성
    fun createBannerAd(): AdView {
        return AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BANNER_AD_UNIT_ID

            adListener = object : AdListener() {
                override fun onAdClicked() {
                    Log.d(TAG, "배너 광고 클릭됨")
                }

                override fun onAdClosed() {
                    Log.d(TAG, "배너 광고 닫힘")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "배너 광고 로드 실패: ${adError.message}")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "배너 광고 노출됨")
                }

                override fun onAdLoaded() {
                    Log.d(TAG, "배너 광고 로드 완료")
                }

                override fun onAdOpened() {
                    Log.d(TAG, "배너 광고 열림")
                }
            }

            loadAd(AdRequest.Builder().build())
        }
    }

    // 전면 광고 로드
    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "전면 광고 로드 실패: ${adError.message}")
                    interstitialAd = null
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "전면 광고 로드 완료")
                    interstitialAd = ad

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdClicked() {
                            Log.d(TAG, "전면 광고 클릭됨")
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "전면 광고 닫힘")
                            interstitialAd = null
                            loadInterstitialAd() // 다음 광고 미리 로드
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "전면 광고 표시 실패: ${adError.message}")
                            interstitialAd = null
                        }

                        override fun onAdImpression() {
                            Log.d(TAG, "전면 광고 노출됨")
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "전면 광고 표시됨")
                        }
                    }
                }
            }
        )
    }

    // 전면 광고 표시
    fun showInterstitialAd(activity: Activity, onAdClosed: (() -> Unit)? = null) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "전면 광고 닫힘")
                    interstitialAd = null
                    loadInterstitialAd() // 다음 광고 미리 로드
                    onAdClosed?.invoke()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "전면 광고 표시 실패: ${adError.message}")
                    interstitialAd = null
                    onAdClosed?.invoke()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "전면 광고 표시됨")
                }
            }

            interstitialAd?.show(activity)
        } else {
            Log.w(TAG, "전면 광고가 아직 로드되지 않음")
            onAdClosed?.invoke()
            loadInterstitialAd() // 광고 다시 로드 시도
        }
    }

    // 보상형 광고 로드
    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "보상형 광고 로드 실패: ${adError.message}")
                    rewardedAd = null
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "보상형 광고 로드 완료")
                    rewardedAd = ad

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdClicked() {
                            Log.d(TAG, "보상형 광고 클릭됨")
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "보상형 광고 닫힘")
                            rewardedAd = null
                            loadRewardedAd() // 다음 광고 미리 로드
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "보상형 광고 표시 실패: ${adError.message}")
                            rewardedAd = null
                        }

                        override fun onAdImpression() {
                            Log.d(TAG, "보상형 광고 노출됨")
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "보상형 광고 표시됨")
                        }
                    }
                }
            }
        )
    }

    // 보상형 광고 표시
    fun showRewardedAd(
        activity: Activity,
        onRewarded: (rewardAmount: Int) -> Unit,
        onAdClosed: (() -> Unit)? = null
    ) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "보상형 광고 닫힘")
                    rewardedAd = null
                    loadRewardedAd()
                    onAdClosed?.invoke()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "보상형 광고 표시 실패: ${adError.message}")
                    rewardedAd = null
                    onAdClosed?.invoke()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "보상형 광고 표시됨")
                }
            }

            rewardedAd?.show(activity) { rewardItem ->
                val rewardAmount = rewardItem.amount
                Log.d(TAG, "보상 획득: $rewardAmount ${rewardItem.type}")
                onRewarded(rewardAmount)
            }
        } else {
            Log.w(TAG, "보상형 광고가 아직 로드되지 않음")
            onAdClosed?.invoke()
            loadRewardedAd()
        }
    }

    // 광고 요청 시 추가 파라미터
    private fun createAdRequest(): AdRequest {
        return AdRequest.Builder()
            .build()
    }

    // 실제 광고 사용 시 수익 추적 (간단 버전)
    fun trackAdRevenue(adUnitId: String, estimatedRevenue: Double = 0.0) {
        // 간단한 로깅 (실제로는 Firebase Analytics 등 사용)
        Log.d(TAG, "광고 수익 추정: ${estimatedRevenue} from $adUnitId")

        // 여기에 분석 도구 연동 코드 추가 가능
        // 예: Firebase Analytics, Adjust, AppsFlyer 등
    }

    // 실제 광고 사용 시 주의사항 체크
    fun validateAdSetup(): Boolean {
        val isValid = when {
            // 테스트 ID 사용 중인지 확인
            BANNER_AD_UNIT_ID.contains("3940256099942544") && !IS_DEBUG -> {
                Log.w(TAG, "⚠️ 경고: 실제 배포에서 테스트 광고 ID 사용 중!")
                false
            }
            // 앱 ID 확인
            !context.packageName.contains("example") -> {
                Log.d(TAG, "✅ 정상: 실제 패키지명 사용 중")
                true
            }
            else -> {
                Log.w(TAG, "⚠️ 경고: 예제 패키지명 사용 중")
                false
            }
        }

        return isValid
    }

    // 광고 로드 상태 확인
    fun isInterstitialAdReady(): Boolean = interstitialAd != null
    fun isRewardedAdReady(): Boolean = rewardedAd != null
}