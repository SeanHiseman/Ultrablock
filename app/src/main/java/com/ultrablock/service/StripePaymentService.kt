package com.ultrablock.service

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.ultrablock.data.preferences.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles Stripe payment operations.
 *
 * IMPORTANT: For production use, you should:
 * 1. Create PaymentIntents on your backend server
 * 2. Never expose your secret key in the app
 * 3. Use webhook endpoints to confirm payments
 *
 * This implementation uses a simplified flow for demonstration.
 * In production, replace the mock implementations with actual API calls.
 */
@Singleton
class StripePaymentService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences
) {
    companion object {
        // Replace with your Stripe publishable key
        const val STRIPE_PUBLISHABLE_KEY = "pk_test_YOUR_PUBLISHABLE_KEY_HERE"

        // For demo mode - set to true to bypass actual Stripe calls
        const val DEMO_MODE = true
    }

    private var stripe: Stripe? = null

    fun initialize() {
        if (STRIPE_PUBLISHABLE_KEY != "pk_test_YOUR_PUBLISHABLE_KEY_HERE") {
            PaymentConfiguration.init(context, STRIPE_PUBLISHABLE_KEY)
            stripe = Stripe(context, STRIPE_PUBLISHABLE_KEY)
        }
    }

    suspend fun hasPaymentMethod(): Boolean {
        return userPreferences.stripePaymentMethodId.first() != null
    }

    suspend fun getPaymentMethodLastFour(): String? {
        return userPreferences.paymentMethodLastFour.first()
    }

    /**
     * Process a payment for unblocking an app.
     *
     * In production:
     * 1. Call your backend to create a PaymentIntent
     * 2. Confirm the PaymentIntent with the saved payment method
     * 3. Return the result
     *
     * For demo mode, this simulates a successful payment.
     */
    suspend fun processPayment(amountCents: Int, description: String): PaymentResult {
        return withContext(Dispatchers.IO) {
            if (DEMO_MODE) {
                // Simulate payment processing delay
                kotlinx.coroutines.delay(1000)
                return@withContext PaymentResult.Success(
                    paymentIntentId = "demo_pi_${System.currentTimeMillis()}"
                )
            }

            val paymentMethodId = userPreferences.stripePaymentMethodId.first()
                ?: return@withContext PaymentResult.Error("No payment method configured")

            try {
                // In production, this would call your backend API
                // val response = api.createPaymentIntent(amountCents, paymentMethodId, description)
                // Then confirm the payment...

                PaymentResult.Success(paymentIntentId = "pi_simulated")
            } catch (e: Exception) {
                PaymentResult.Error(e.message ?: "Payment failed")
            }
        }
    }

    /**
     * Save a payment method after user adds a card.
     *
     * In production, use Stripe's PaymentSheet or CardInputWidget
     * to securely collect card details.
     */
    suspend fun savePaymentMethod(paymentMethodId: String, lastFour: String) {
        userPreferences.setStripePaymentMethod(paymentMethodId, lastFour)
    }

    /**
     * For demo purposes - simulate adding a card
     */
    suspend fun addDemoCard() {
        userPreferences.setStripePaymentMethod(
            paymentMethodId = "pm_demo_${System.currentTimeMillis()}",
            lastFour = "4242"
        )
    }

    suspend fun removePaymentMethod() {
        userPreferences.setStripePaymentMethod(null, null)
    }

    sealed class PaymentResult {
        data class Success(val paymentIntentId: String) : PaymentResult()
        data class Error(val message: String) : PaymentResult()
    }
}
