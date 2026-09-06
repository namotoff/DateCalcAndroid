package com.datecalc.billing;

import android.content.Context;
import android.util.Log;

import ru.rustore.sdk.core.tasks.Task;
import ru.rustore.sdk.pay.RuStorePayClient;
import ru.rustore.sdk.pay.RuStorePayClientProvider;
import ru.rustore.sdk.pay.model.ConsoleApplicationId;
import ru.rustore.sdk.pay.model.ProductId;
import ru.rustore.sdk.pay.model.ProductPurchaseParams;
import ru.rustore.sdk.pay.model.ProductPurchaseResult;
import ru.rustore.sdk.pay.model.ProductType;
import ru.rustore.sdk.pay.model.Purchase;
import ru.rustore.sdk.pay.model.PurchaseStatus;
import ru.rustore.sdk.pay.model.SubscriptionPurchaseStatus;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class RuStorePayHelper {

    private static final String TAG = "RuStorePayHelper";

    public static RuStorePayClient createClient(Context context) {
        try {
            return new RuStorePayClientProvider().provide(
                    context,
                    new ConsoleApplicationId("com.datecalc"),
                    java.util.Collections.emptyMap()
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to create client", e);
            return null;
        }
    }

    public static boolean purchase(RuStorePayClient client, String productId) {
        try {
            ProductPurchaseParams params = new ProductPurchaseParams(
                    new ProductId(productId),
                    null, null, null, null, null
            );
            Task<ProductPurchaseResult> task = client.getPurchaseInteractor()
                    .purchase(params, null, null, null);
            ProductPurchaseResult result = awaitTask(task, 60);
            return result != null;
        } catch (Exception e) {
            Log.e(TAG, "Purchase failed", e);
            return false;
        }
    }

    public static boolean hasActiveSubscription(RuStorePayClient client) {
        try {
            @SuppressWarnings("unchecked")
            Task<List<Purchase>> task = client.getPurchaseInteractor()
                    .getPurchases(ProductType.SUBSCRIPTION, SubscriptionPurchaseStatus.ACTIVE, null);
            List<Purchase> purchases = awaitTask(task, 30);
            if (purchases == null) return false;
            for (Purchase p : purchases) {
                if (p.getStatus() == SubscriptionPurchaseStatus.ACTIVE) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Check purchases failed", e);
            return false;
        }
    }

    private static <T> T awaitTask(Task<T> task, int timeoutSeconds) {
        try {
            return task.await(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "Task await failed", e);
            return null;
        }
    }
}
