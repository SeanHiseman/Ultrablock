package com.ultrablock.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        private val HOURLY_RATE_CENTS = intPreferencesKey("hourly_rate_cents")
        private val STRIPE_CUSTOMER_ID = stringPreferencesKey("stripe_customer_id")
        private val STRIPE_PAYMENT_METHOD_ID = stringPreferencesKey("stripe_payment_method_id")
        private val PAYMENT_METHOD_LAST_FOUR = stringPreferencesKey("payment_method_last_four")
        private val BLOCKING_ENABLED = booleanPreferencesKey("blocking_enabled")
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val DEFAULT_UNBLOCK_DURATION = intPreferencesKey("default_unblock_duration")

        const val DEFAULT_HOURLY_RATE_CENTS = 2000 // $20/hour
        const val DEFAULT_UNBLOCK_MINUTES = 15
    }

    val hourlyRateCents: Flow<Int> = dataStore.data.map { preferences ->
        preferences[HOURLY_RATE_CENTS] ?: DEFAULT_HOURLY_RATE_CENTS
    }

    val stripeCustomerId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[STRIPE_CUSTOMER_ID]
    }

    val stripePaymentMethodId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[STRIPE_PAYMENT_METHOD_ID]
    }

    val paymentMethodLastFour: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PAYMENT_METHOD_LAST_FOUR]
    }

    val blockingEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[BLOCKING_ENABLED] ?: false
    }

    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED] ?: false
    }

    val defaultUnblockDuration: Flow<Int> = dataStore.data.map { preferences ->
        preferences[DEFAULT_UNBLOCK_DURATION] ?: DEFAULT_UNBLOCK_MINUTES
    }

    suspend fun setHourlyRateCents(cents: Int) {
        dataStore.edit { preferences ->
            preferences[HOURLY_RATE_CENTS] = cents
        }
    }

    suspend fun setStripeCustomerId(customerId: String?) {
        dataStore.edit { preferences ->
            if (customerId != null) {
                preferences[STRIPE_CUSTOMER_ID] = customerId
            } else {
                preferences.remove(STRIPE_CUSTOMER_ID)
            }
        }
    }

    suspend fun setStripePaymentMethod(paymentMethodId: String?, lastFour: String?) {
        dataStore.edit { preferences ->
            if (paymentMethodId != null) {
                preferences[STRIPE_PAYMENT_METHOD_ID] = paymentMethodId
            } else {
                preferences.remove(STRIPE_PAYMENT_METHOD_ID)
            }
            if (lastFour != null) {
                preferences[PAYMENT_METHOD_LAST_FOUR] = lastFour
            } else {
                preferences.remove(PAYMENT_METHOD_LAST_FOUR)
            }
        }
    }

    suspend fun setBlockingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BLOCKING_ENABLED] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setDefaultUnblockDuration(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_UNBLOCK_DURATION] = minutes
        }
    }
}
