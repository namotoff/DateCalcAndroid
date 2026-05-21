package com.datecalc.billing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "subscription")

object SubscriptionManager {

    private val KEY_TRIAL_STARTED = booleanPreferencesKey("trial_started")
    private val KEY_TRIAL_START_TIME = longPreferencesKey("trial_start_time")
    private val KEY_IS_SUBSCRIBED = booleanPreferencesKey("is_subscribed")

    const val TRIAL_DAYS = 7L
    const val SUBSCRIPTION_PRODUCT_ID = "datecalc_premium"

    fun trialStarted(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_TRIAL_STARTED] ?: false }

    fun trialStartTime(context: Context): Flow<Long> =
        context.dataStore.data.map { it[KEY_TRIAL_START_TIME] ?: 0L }

    fun isSubscribed(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_IS_SUBSCRIBED] ?: false }

    fun isTrialActive(context: Context): Flow<Boolean> = flow {
        val started = trialStarted(context).first()
        if (!started) { emit(false); return@flow }
        val startTime = trialStartTime(context).first()
        val now = System.currentTimeMillis()
        val elapsedDays = (now - startTime) / (1000 * 60 * 60 * 24)
        emit(elapsedDays < TRIAL_DAYS)
    }

    fun trialDaysRemaining(context: Context): Flow<Int> = flow {
        val startTime = trialStartTime(context).first()
        if (startTime == 0L) { emit(0); return@flow }
        val now = System.currentTimeMillis()
        val elapsedDays = ((now - startTime) / (1000 * 60 * 60 * 24)).toInt()
        val remaining = (TRIAL_DAYS - elapsedDays).toInt().coerceAtLeast(0)
        emit(remaining)
    }

    suspend fun startTrial(context: Context) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TRIAL_STARTED] = true
            prefs[KEY_TRIAL_START_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun setSubscribed(context: Context, subscribed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_SUBSCRIBED] = subscribed
        }
    }

    fun hasAccess(context: Context): Flow<Boolean> = flow {
        val subscribed = isSubscribed(context).first()
        if (subscribed) { emit(true); return@flow }
        val trialActive = isTrialActive(context).first()
        emit(trialActive)
    }
}
