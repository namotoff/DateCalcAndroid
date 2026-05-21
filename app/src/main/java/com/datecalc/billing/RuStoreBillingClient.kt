package com.datecalc.billing

import android.app.Activity
import android.content.Context

class RuStoreBillingClient(private val context: Context) : BillingClient {

    private var rustoreClient: Any? = null

    override fun initialize() {
        try {
            val builderClass = Class.forName("ru.rustore.sdk.billingclient.RuStoreBillingClient")
            val builder = builderClass.getMethod("builder", Context::class.java)
                .invoke(null, context) ?: return

            val setAppId = builder.javaClass.getMethod("applicationId", String::class.java)
            setAppId.invoke(builder, "com.datecalc")

            val build = builder.javaClass.getMethod("build")
            rustoreClient = build.invoke(builder)
        } catch (_: Exception) {}
    }

    override suspend fun purchase(activity: Activity): Boolean {
        return try {
            if (rustoreClient == null) return false
            val purchases = rustoreClient!!.javaClass.getMethod("purchases").invoke(rustoreClient)
            val purchaseMethod = purchases.javaClass.getMethod(
                "purchase",
                Activity::class.java,
                String::class.java
            )
            purchaseMethod.invoke(purchases, activity, SubscriptionManager.SUBSCRIPTION_PRODUCT_ID)
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun checkPurchases(): Boolean {
        return try {
            if (rustoreClient == null) return false
            val purchases = rustoreClient!!.javaClass.getMethod("purchases").invoke(rustoreClient)
            val getPurchases = purchases.javaClass.getMethod("getPurchases")
            val response = getPurchases.invoke(purchases)
            val purchaseList = response?.javaClass?.getMethod("getPurchases")
                ?.invoke(response) as? List<*> ?: return false
            purchaseList.any { purchase ->
                val state = purchase?.javaClass?.getMethod("getPurchaseState")?.invoke(purchase)
                state?.toString() == "PURCHASED"
            }
        } catch (_: Exception) {
            false
        }
    }
}
