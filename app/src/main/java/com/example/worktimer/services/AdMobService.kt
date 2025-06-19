package com.example.worktimer.services

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdMobService(private val context: Context) {

    companion object {
        private const val TAG = "AdMobService"

        // 테스트 광고 ID들 (실제 배포시에는 본인의 광고 ID로 변경)
        const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

        // 실제 배포시 광고 ID 예시:
        // const val BANNER_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
        // const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
        // const val REWARDED_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
    }

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    fun initialize() {
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "AdMob 초기화 완료: ${initializationStatus.adapterStatusMap}")
        }

        // 전면 광고 미리 로드
        loadInterstitialAd()

        // 보상형 광고 미리 로드
        loadRewardedAd()
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

    // 광고 로드 상태 확인
    fun isInterstitialAdReady(): Boolean = interstitialAd != null
    fun isRewardedAdReady(): Boolean = rewardedAd != null
}