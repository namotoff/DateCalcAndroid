package com.datecalc.billing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "subscription")

object SubscriptionManager {

    private val KEY_IS_SUBSCRIBED = booleanPreferencesKey("is_subscribed")

    const val SUBSCRIPTION_PRODUCT_ID = "unlimited"

    fun isSubscribed(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_IS_SUBSCRIBED] ?: false }

    suspend fun setSubscribed(context: Context, subscribed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_SUBSCRIBED] = subscribed
        }
    }

    fun hasAccess(context: Context): Flow<Boolean> =
        isSubscribed(context)
}
