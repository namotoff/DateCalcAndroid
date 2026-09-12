package com.datecalc.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.rustore.sdk.pay.RuStorePayClient

class RuStoreBillingClient(private val context: Context) : BillingClient {

    private var payClient: RuStorePayClient? = null

    override fun initialize() {
        try {
            payClient = RuStorePayHelper.createClient(context)
        } catch (e: Exception) {
            Log.e("RuStoreBilling", "Init failed", e)
        }
    }

    override suspend fun purchase(activity: Activity): Boolean {
        val client = payClient ?: return false
        return withContext(Dispatchers.IO) {
            try {
                RuStorePayHelper.purchase(client, SubscriptionManager.SUBSCRIPTION_PRODUCT_ID)
            } catch (e: Exception) {
                Log.e("RuStoreBilling", "Purchase failed", e)
                false
            }
        }
    }

    override suspend fun checkPurchases(): Boolean {
        val client = payClient ?: return false
        return withContext(Dispatchers.IO) {
            try {
                RuStorePayHelper.hasActiveSubscription(client)
            } catch (e: Exception) {
                Log.e("RuStoreBilling", "Check purchases failed", e)
                false
            }
        }
    }
}
