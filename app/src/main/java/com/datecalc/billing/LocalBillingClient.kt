package com.datecalc.billing

import android.app.Activity

class LocalBillingClient : BillingClient {

    override fun initialize() {}

    override suspend fun purchase(activity: Activity): Boolean {
        return false
    }

    override suspend fun checkPurchases(): Boolean {
        return false
    }
}
