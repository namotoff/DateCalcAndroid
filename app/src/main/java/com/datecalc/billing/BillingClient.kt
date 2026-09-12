package com.datecalc.billing

import android.app.Activity

interface BillingClient {
    fun initialize()
    suspend fun purchase(activity: Activity): Boolean
    suspend fun checkPurchases(): Boolean
}

object BillingFactory {

    fun create(context: android.content.Context): BillingClient {
        if (!isRuStoreInstalled(context)) return LocalBillingClient()
        return try {
            val clazz = Class.forName("com.datecalc.billing.RuStoreBillingClient")
            val constructor = clazz.getConstructor(android.content.Context::class.java)
            constructor.newInstance(context) as BillingClient
        } catch (e: Throwable) {
            LocalBillingClient()
        }
    }

    private fun isRuStoreInstalled(context: android.content.Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("ru.rustore.app", 0)
            true
        } catch (_: Exception) {
            false
        }
    }
}
