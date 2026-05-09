package com.ultrablock.ui.screens.blocker

import app.cash.turbine.test
import com.ultrablock.data.local.entity.BlockedApp
import com.ultrablock.data.preferences.UserPreferences
import com.ultrablock.data.repository.AppRepository
import com.ultrablock.data.repository.PaymentRepository
import com.ultrablock.data.repository.UsageRepository
import com.ultrablock.domain.model.FrictionLevel
import com.ultrablock.service.StripePaymentService
import com.ultrablock.util.MainDispatcherRule
import io.mockk.coEvery
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class BlockerViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val userPreferences = mockk<UserPreferences>(relaxed = true)
    private val appRepository = mockk<AppRepository>(relaxed = true)
    private val paymentRepository = mockk<PaymentRepository>(relaxed = true)
    private val usageRepository = mockk<UsageRepository>(relaxed = true)
    private val stripePaymentService = mockk<StripePaymentService>(relaxed = true)

    private lateinit var viewModel: BlockerViewModel

    @Before
    fun setup() {
        every { userPreferences.hourlyRateCents } returns flowOf(2000)
        every { userPreferences.stripePaymentMethodId } returns flowOf(null)
        every { userPreferences.globalFrictionLevel } returns flowOf("STRICT")
        coEvery { usageRepository.recordBlockAttempt(any(), any(), any()) } returns 1L
        coJustRun { usageRepository.recordUnblock(any(), any()) }
        coJustRun { appRepository.temporarilyUnblockApp(any(), any()) }

        viewModel = BlockerViewModel(
            userPreferences = userPreferences,
            appRepository = appRepository,
            paymentRepository = paymentRepository,
            usageRepository = usageRepository,
            stripePaymentService = stripePaymentService
        )
    }

    // ── Friction level resolution ──────────────────────────────────────────

    @Test
    fun `setBlockedApp uses global friction level when app has no override`() = runTest {
        every { userPreferences.globalFrictionLevel } returns flowOf("GENTLE")
        coEvery { appRepository.getAppByPackage(any()) } returns
                BlockedApp("com.test", "Test", frictionLevel = null)

        viewModel.setBlockedApp("com.test", "Test")
        advanceTimeBy(100)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(FrictionLevel.GENTLE, state.frictionLevel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setBlockedApp uses per-app friction level over global`() = runTest {
        every { userPreferences.globalFrictionLevel } returns flowOf("STRICT")
        coEvery { appRepository.getAppByPackage(any()) } returns
                BlockedApp("com.test", "Test", frictionLevel = "EXTREME")

        viewModel.setBlockedApp("com.test", "Test")
        advanceTimeBy(100)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(FrictionLevel.EXTREME, state.frictionLevel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setBlockedApp defaults to STRICT for unknown friction level string`() = runTest {
        every { userPreferences.globalFrictionLevel } returns flowOf("INVALID_LEVEL")
        coEvery { appRepository.getAppByPackage(any()) } returns null

        viewModel.setBlockedApp("com.test", "Test")
        advanceTimeBy(100)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(FrictionLevel.STRICT, state.frictionLevel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Session tracking ───────────────────────────────────────────────────

    @Test
    fun `setBlockedApp records a usage session`() = runTest {
        coEvery { appRepository.getAppByPackage(any()) } returns null

        viewModel.setBlockedApp("com.example", "Example")
        advanceTimeBy(100)

        coVerify { usageRepository.recordBlockAttempt("com.example", "Example", any()) }
    }

    // ── GENTLE countdown ───────────────────────────────────────────────────

    @Test
    fun `gentle friction starts 5-second countdown`() = runTest {
        every { userPreferences.globalFrictionLevel } returns flowOf("GENTLE")
        coEvery { appRepository.getAppByPackage(any()) } returns null

        viewModel.setBlockedApp("com.test", "Test")
        advanceTimeBy(100)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(FrictionLevel.GENTLE, state.frictionLevel)
            assertEquals(5, state.countdownSeconds)
            assertFalse(state.countdownComplete)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `gentle countdown reaches zero after 5 seconds`() = runTest {
        every { userPreferences.globalFrictionLevel } returns flowOf("GENTLE")
        coEvery { appRepository.getAppByPackage(any()) } returns null

        viewModel.setBlockedApp("com.test", "Test")
        advanceTimeBy(5100) // just past 5 seconds

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.countdownSeconds)
            assertTrue(state.countdownComplete)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── MODERATE countdown ─────────────────────────────────────────────────

    @Test
    fun `moderate friction starts 30-second countdown`() = runTest {
        every { userPreferences.globalFrictionLevel } returns flowOf("MODERATE")
        coEvery { appRepository.getAppByPackage(any()) } returns null

        viewModel.setBlockedApp("com.test", "Test")
        advanceTimeBy(100)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(FrictionLevel.MODERATE, state.frictionLevel)
            assertEquals(30, state.countdownSeconds)
            assertFalse(state.countdownComplete)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── bypassFree (GENTLE / MODERATE) ────────────────────────────────────

    @Test
    fun `bypassFree temporarily unblocks app for 5 minutes`() = runTest {
        coEvery { appRepository.getAppByPackage(any()) } returns null

        viewModel.setBlockedApp("com.test", "Test")
        advanceTimeBy(100)
        viewModel.bypassFree()
        advanceTimeBy(100)

        coVerify { appRepository.temporarilyUnblockApp("com.test", 5) }
    }

    @Test
    fun `bypassFree records unblock in usage session`() = runTest {
        coEvery { usageRepository.recordBlockAttempt(any(), any(), any()) } returns 99L
        coEvery { appRepository.getAppByPackage(any()) } returns null

        viewModel.setBlockedApp("com.test", "Test")
        advanceTimeBy(100)
        viewModel.bypassFree()
        advanceTimeBy(100)

        coVerify { usageRepository.recordUnblock(99L, 0) }
    }

    @Test
    fun `bypassFree sets paymentSuccess to true`() = runTest {
        coEvery { appRepository.getAppByPackage(any()) } returns null

        viewModel.setBlockedApp("com.test", "Test")
        advanceTimeBy(100)
        viewModel.bypassFree()
        advanceTimeBy(100)

        viewModel.uiState.test {
            assertTrue(awaitItem().paymentSuccess)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── STRICT: processUnblock ─────────────────────────────────────────────

    @Test
    fun `processUnblock fails immediately when no payment method`() = runTest {
        every { userPreferences.stripePaymentMethodId } returns flowOf(null)
        coEvery { appRepository.getAppByPackage(any()) } returns null

        viewModel.setBlockedApp("com.test", "Test")
        advanceTimeBy(100)
        viewModel.processUnblock()
        advanceTimeBy(100)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.paymentSuccess)
            assertEquals("Please add a payment method in settings first", state.paymentError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `processUnblock succeeds and sets paymentSuccess on payment success`() = runTest {
        every { userPreferences.stripePaymentMethodId } returns flowOf("pm_test_123")
        coEvery { appRepository.getAppByPackage(any()) } returns null
        coEvery { stripePaymentService.processPayment(any(), any()) } returns
                StripePaymentService.PaymentResult.Success("pi_test_123")
        coJustRun { paymentRepository.recordUnblock(any(), any(), any(), any(), any()) }

        viewModel.setBlockedApp("com.test", "Test")
        advanceTimeBy(100)
        viewModel.processUnblock()
        advanceTimeBy(100)

        viewModel.uiState.test {
            assertTrue(awaitItem().paymentSuccess)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `processUnblock sets paymentError on payment failure`() = runTest {
        every { userPreferences.stripePaymentMethodId } returns flowOf("pm_test_123")
        coEvery { appRepository.getAppByPackage(any()) } returns null
        coEvery { stripePaymentService.processPayment(any(), any()) } returns
                StripePaymentService.PaymentResult.Error("Card declined")

        viewModel.setBlockedApp("com.test", "Test")
        advanceTimeBy(100)
        viewModel.processUnblock()
        advanceTimeBy(100)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.paymentSuccess)
            assertEquals("Card declined", state.paymentError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Cost calculation ───────────────────────────────────────────────────

    @Test
    fun `cost is zero for zero minutes`() {
        assertEquals(0, viewModel.formatCost(0))
    }

    @Test
    fun `formatCost formats cents as dollar string`() {
        assertEquals("$5.00", viewModel.formatCost(500))
        assertEquals("$20.00", viewModel.formatCost(2000))
        assertEquals("$0.50", viewModel.formatCost(50))
    }

    // ── setDuration ────────────────────────────────────────────────────────

    @Test
    fun `setDuration updates selectedDuration and clears payment error`() = runTest {
        coEvery { appRepository.getAppByPackage(any()) } returns null
        every { userPreferences.stripePaymentMethodId } returns flowOf(null)

        viewModel.setBlockedApp("com.test", "Test")
        advanceTimeBy(100)
        viewModel.processUnblock() // sets paymentError
        advanceTimeBy(100)
        viewModel.setDuration(30)
        advanceTimeBy(100)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(30, state.selectedDuration)
            assertNull(state.paymentError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── reflectionReason ───────────────────────────────────────────────────

    @Test
    fun `setReflectionReason updates state`() = runTest {
        viewModel.setReflectionReason("I need to check my messages")
        advanceTimeBy(100)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("I need to check my messages", state.reflectionReason)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── durationOptions ────────────────────────────────────────────────────

    @Test
    fun `durationOptions contains expected values`() {
        assertTrue(viewModel.durationOptions.contains(15))
        assertTrue(viewModel.durationOptions.contains(30))
        assertTrue(viewModel.durationOptions.contains(60))
        assertTrue(viewModel.durationOptions.isNotEmpty())
    }
}
