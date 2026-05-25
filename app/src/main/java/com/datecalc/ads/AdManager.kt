package com.datecalc.ads

import android.app.Activity
import android.content.Context
import android.widget.FrameLayout
import com.my.target.ads.MyTargetView
import com.my.target.ads.InterstitialAd
import com.my.target.common.MyTargetManager
import com.my.target.common.models.IAdLoadingError

object AdManager {

    private const val BANNER_SLOT_ID = 0
    private const val INTERSTITIAL_SLOT_ID = 0

    fun hasBannerSlot() = BANNER_SLOT_ID != 0

    fun initialize(context: Context) {
        MyTargetManager.initSdk(context)
    }

    fun createBanner(activity: Activity, container: FrameLayout): MyTargetView? {
        if (BANNER_SLOT_ID == 0) return null

        val adView = MyTargetView(activity)
        adView.setSlotId(BANNER_SLOT_ID)
        adView.setAdSize(MyTargetView.AdSize.ADSIZE_320x50)
        adView.setListener(object : MyTargetView.MyTargetViewListener {
            override fun onLoad(adView: MyTargetView) {
                container.removeAllViews()
                container.addView(adView)
            }
            override fun onNoAd(reason: IAdLoadingError, adView: MyTargetView) {}
            override fun onClick(adView: MyTargetView) {}
            override fun onShow(adView: MyTargetView) {}
        })
        adView.load()
        return adView
    }

    fun loadInterstitial(activity: Activity) {
        if (INTERSTITIAL_SLOT_ID == 0) return
        val ad = InterstitialAd(INTERSTITIAL_SLOT_ID, activity)
        ad.setListener(object : InterstitialAd.InterstitialAdListener {
            override fun onLoad(ad: InterstitialAd) { ad.show() }
            override fun onNoAd(reason: IAdLoadingError, ad: InterstitialAd) {}
            override fun onClick(ad: InterstitialAd) {}
            override fun onDismiss(ad: InterstitialAd) {}
            override fun onVideoCompleted(ad: InterstitialAd) {}
            override fun onDisplay(ad: InterstitialAd) {}
            override fun onFailedToShow(ad: InterstitialAd) {}
        })
        ad.load()
    }
}
