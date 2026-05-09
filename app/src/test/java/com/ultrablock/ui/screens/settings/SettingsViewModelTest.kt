package com.ultrablock.ui.screens.settings

import app.cash.turbine.test
import com.ultrablock.data.preferences.UserPreferences
import com.ultrablock.domain.model.FrictionLevel
import com.ultrablock.service.StripePaymentService
import com.ultrablock.util.MainDispatcherRule
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import android.app.Application
import android.provider.Settings

@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val application = mockk<Application>(relaxed = true)
    private val userPreferences = mockk<UserPreferences>(relaxed = true)
    private val stripePaymentService = mockk<StripePaymentService>(relaxed = true)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        every { userPreferences.hourlyRateCents } returns flowOf(2000)
        every { userPreferences.paymentMethodLastFour } returns flowOf(null)
        every { userPreferences.defaultUnblockDuration } returns flowOf(15)
        every { userPreferences.globalFrictionLevel } returns flowOf("STRICT")
        coJustRun { userPreferences.setGlobalFrictionLevel(any()) }
        coJustRun { userPreferences.setHourlyRateCents(any()) }
        coJustRun { userPreferences.setDefaultUnblockDuration(any()) }

        viewModel = SettingsViewModel(application, userPreferences, stripePaymentService)
    }

    // ── Friction level ─────────────────────────────────────────────────────

    @Test
    fun `initial friction level is STRICT from preferences`() = runTest {
        advanceTimeBy(100)

        viewModel.uiState.test {
            assertEquals(FrictionLevel.STRICT, awaitItem().globalFrictionLevel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setFrictionLevel saves to preferences and hides dialog`() = runTest {
        viewModel.showFrictionDialog()
        advanceTimeBy(100)

        viewModel.setFrictionLevel(FrictionLevel.GENTLE)
        advanceTimeBy(100)

        coVerify { userPreferences.setGlobalFrictionLevel("GENTLE") }
        viewModel.uiState.test {
            assertFalse(awaitItem().showFrictionDialog)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `showFrictionDialog sets showFrictionDialog to true`() = runTest {
        viewModel.showFrictionDialog()
        advanceTimeBy(100)

        viewModel.uiState.test {
            assertTrue(awaitItem().showFrictionDialog)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `hideFrictionDialog sets showFrictionDialog to false`() = runTest {
        viewModel.showFrictionDialog()
        viewModel.hideFrictionDialog()
        advanceTimeBy(100)

        viewModel.uiState.test {
            assertFalse(awaitItem().showFrictionDialog)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `frictionOptions contains all four FrictionLevel values`() {
        assertEquals(FrictionLevel.values().size, viewModel.frictionOptions.size)
        assertTrue(viewModel.frictionOptions.containsAll(FrictionLevel.values().toList()))
    }

    // ── Hourly rate ────────────────────────────────────────────────────────

    @Test
    fun `setHourlyRate converts dollars to cents and saves`() = runTest {
        viewModel.setHourlyRate(30)
        advanceTimeBy(100)

        coVerify { userPreferences.setHourlyRateCents(3000) }
    }

    @Test
    fun `setHourlyRate hides rate dialog`() = runTest {
        viewModel.showRateDialog()
        viewModel.setHourlyRate(20)
        advanceTimeBy(100)

        viewModel.uiState.test {
            assertFalse(awaitItem().showRateDialog)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial hourly rate is derived from preferences (cents to dollars)`() = runTest {
        every { userPreferences.hourlyRateCents } returns flowOf(5000)
        viewModel = SettingsViewModel(application, userPreferences, stripePaymentService)
        advanceTimeBy(100)

        viewModel.uiState.test {
            assertEquals(50, awaitItem().hourlyRateDollars)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Unblock duration ───────────────────────────────────────────────────

    @Test
    fun `setDefaultUnblockDuration saves value and hides dialog`() = runTest {
        viewModel.showUnblockDurationDialog()
        viewModel.setDefaultUnblockDuration(45)
        advanceTimeBy(100)

        coVerify { userPreferences.setDefaultUnblockDuration(45) }
        viewModel.uiState.test {
            assertFalse(awaitItem().showUnblockDurationDialog)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Rate and duration options ──────────────────────────────────────────

    @Test
    fun `rateOptions is non-empty and contains reasonable values`() {
        assertTrue(viewModel.rateOptions.isNotEmpty())
        assertTrue(viewModel.rateOptions.contains(20))
    }

    @Test
    fun `durationOptions is non-empty and contains 15`() {
        assertTrue(viewModel.durationOptions.isNotEmpty())
        assertTrue(viewModel.durationOptions.contains(15))
    }
}
