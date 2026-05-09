package com.ultrablock.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultrablock.domain.model.FrictionLevel
import com.ultrablock.ui.screens.blocker.BlockerUiState
import com.ultrablock.ui.theme.UltrablockTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for each friction level's overlay content.
 * We use a lightweight [BlockerContent] composable that mirrors the real
 * screen's when-branch without depending on the ViewModel or Hilt.
 */
@RunWith(AndroidJUnit4::class)
class BlockerOverlayScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ── GENTLE ────────────────────────────────────────────────────────────

    @Test
    fun gentle_showsMotivationalHeading() {
        showContent(gentleState(countdownSeconds = 5, countdownComplete = false))
        composeRule.onNodeWithText("Take a breath").assertIsDisplayed()
    }

    @Test
    fun gentle_showsLiveCountdownValue() {
        showContent(gentleState(countdownSeconds = 3, countdownComplete = false))
        composeRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun gentle_showsWaitLabelDuringCountdown() {
        showContent(gentleState(countdownSeconds = 5, countdownComplete = false))
        composeRule.onNodeWithText("Wait 5s…").assertIsDisplayed()
    }

    @Test
    fun gentle_showsContinueButtonAfterCountdown() {
        showContent(gentleState(countdownSeconds = 0, countdownComplete = true))
        composeRule.onNodeWithText("Continue Anyway").assertIsDisplayed()
        composeRule.onNodeWithText("Continue Anyway").assertIsEnabled()
    }

    @Test
    fun gentle_alwaysShowsGoBack() {
        showContent(gentleState(countdownSeconds = 5, countdownComplete = false))
        composeRule.onNodeWithText("Go Back").assertIsDisplayed()
    }

    // ── MODERATE ──────────────────────────────────────────────────────────

    @Test
    fun moderate_showsReflectFirstHeading() {
        showContent(moderateState(countdownSeconds = 30, countdownComplete = false, reason = ""))
        composeRule.onNodeWithText("Reflect first").assertIsDisplayed()
    }

    @Test
    fun moderate_showsTimerValue() {
        showContent(moderateState(countdownSeconds = 18, countdownComplete = false, reason = ""))
        composeRule.onNodeWithText("18").assertIsDisplayed()
    }

    @Test
    fun moderate_showsWaitLabelDuringCountdown() {
        showContent(moderateState(countdownSeconds = 30, countdownComplete = false, reason = ""))
        composeRule.onNodeWithText("Wait 30s…").assertIsDisplayed()
    }

    @Test
    fun moderate_showsWriteReasonLabelWhenReasonTooShort() {
        showContent(moderateState(countdownSeconds = 0, countdownComplete = true, reason = "hi"))
        composeRule.onNodeWithText("Write a reason first").assertIsDisplayed()
    }

    @Test
    fun moderate_showsContinueWhenTimerDoneAndReasonSufficient() {
        showContent(
            moderateState(
                countdownSeconds = 0,
                countdownComplete = true,
                reason = "I need to check urgent work messages"
            )
        )
        composeRule.onNodeWithText("Continue").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").assertIsEnabled()
    }

    @Test
    fun moderate_alwaysShowsGoBack() {
        showContent(moderateState(countdownSeconds = 30, countdownComplete = false, reason = ""))
        composeRule.onNodeWithText("Go Back").assertIsDisplayed()
    }

    // ── STRICT ────────────────────────────────────────────────────────────

    @Test
    fun strict_showsAppBlockedHeading() {
        showContent(strictState(hasPaymentMethod = true))
        composeRule.onNodeWithText("App Blocked").assertIsDisplayed()
    }

    @Test
    fun strict_showsAppName() {
        showContent(strictState(hasPaymentMethod = true))
        composeRule.onNodeWithText("YouTube").assertIsDisplayed()
    }

    @Test
    fun strict_showsUnblockButtonWithCost() {
        showContent(strictState(hasPaymentMethod = true, costCents = 500))
        composeRule.onNodeWithText("Unblock for", substring = true).assertIsDisplayed()
    }

    @Test
    fun strict_showsNoPaymentMethodWarning() {
        showContent(strictState(hasPaymentMethod = false))
        composeRule.onNodeWithText("Add a payment method", substring = true).assertIsDisplayed()
    }

    @Test
    fun strict_showsPaymentErrorWhenPresent() {
        showContent(strictState(hasPaymentMethod = true, paymentError = "Card declined"))
        composeRule.onNodeWithText("Card declined").assertIsDisplayed()
    }

    @Test
    fun strict_alwaysShowsGoBack() {
        showContent(strictState(hasPaymentMethod = false))
        composeRule.onNodeWithText("Go Back").assertIsDisplayed()
    }

    // ── EXTREME ───────────────────────────────────────────────────────────

    @Test
    fun extreme_showsLockedHeading() {
        showContent(extremeState())
        composeRule.onNodeWithText("Locked").assertIsDisplayed()
    }

    @Test
    fun extreme_showsAppName() {
        showContent(extremeState())
        composeRule.onNodeWithText("Twitter").assertIsDisplayed()
    }

    @Test
    fun extreme_showsExtremeBlockingMessage() {
        showContent(extremeState())
        composeRule.onNodeWithText("Extreme blocking is active", substring = true).assertIsDisplayed()
    }

    @Test
    fun extreme_mentionsSettingsForChange() {
        showContent(extremeState())
        composeRule.onNodeWithText("Settings", substring = true).assertIsDisplayed()
    }

    @Test
    fun extreme_doesNotShowUnblockOption() {
        showContent(extremeState())
        composeRule.onNodeWithText("Unblock", substring = true).assertDoesNotExist()
    }

    @Test
    fun extreme_onlyShowsGoBackAsAction() {
        showContent(extremeState())
        composeRule.onNodeWithText("Go Back").assertIsDisplayed()
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun showContent(state: BlockerUiState) {
        composeRule.setContent {
            UltrablockTheme { BlockerContent(state = state) }
        }
    }

    private fun gentleState(countdownSeconds: Int, countdownComplete: Boolean) = BlockerUiState(
        appName = "Instagram",
        frictionLevel = FrictionLevel.GENTLE,
        countdownSeconds = countdownSeconds,
        countdownComplete = countdownComplete
    )

    private fun moderateState(countdownSeconds: Int, countdownComplete: Boolean, reason: String) =
        BlockerUiState(
            appName = "TikTok",
            frictionLevel = FrictionLevel.MODERATE,
            countdownSeconds = countdownSeconds,
            countdownComplete = countdownComplete,
            reflectionReason = reason
        )

    private fun strictState(
        hasPaymentMethod: Boolean,
        costCents: Int = 500,
        paymentError: String? = null
    ) = BlockerUiState(
        appName = "YouTube",
        frictionLevel = FrictionLevel.STRICT,
        hasPaymentMethod = hasPaymentMethod,
        costCents = costCents,
        hourlyRateCents = 2000,
        paymentError = paymentError
    )

    private fun extremeState() = BlockerUiState(
        appName = "Twitter",
        frictionLevel = FrictionLevel.EXTREME
    )
}

/**
 * Pure-Compose rendition of the four friction-level screens, decoupled from
 * the ViewModel so they can be driven by plain [BlockerUiState] in tests.
 */
@Composable
private fun BlockerContent(state: BlockerUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        when (state.frictionLevel) {
            FrictionLevel.GENTLE -> {
                Text("Take a breath")
                Text(state.appName)
                Text("${state.countdownSeconds}")
                Button(onClick = {}, enabled = state.countdownComplete) {
                    Text(
                        if (state.countdownComplete) "Continue Anyway"
                        else "Wait ${state.countdownSeconds}…"
                    )
                }
                Button(onClick = {}) { Text("Go Back") }
            }

            FrictionLevel.MODERATE -> {
                Text("Reflect first")
                Text(state.appName)
                Text("${state.countdownSeconds}")
                val reasonReady = state.reflectionReason.trim().length >= 10
                val canContinue = state.countdownComplete && reasonReady
                Button(onClick = {}, enabled = canContinue) {
                    Text(
                        when {
                            !state.countdownComplete -> "Wait ${state.countdownSeconds}…"
                            !reasonReady -> "Write a reason first"
                            else -> "Continue"
                        }
                    )
                }
                Button(onClick = {}) { Text("Go Back") }
            }

            FrictionLevel.STRICT -> {
                Text("App Blocked")
                Text(state.appName)
                val costLabel = "$%.2f".format(state.costCents / 100.0)
                Button(
                    onClick = {},
                    enabled = state.hasPaymentMethod && !state.isProcessingPayment
                ) {
                    Text("Unblock for $costLabel")
                }
                if (!state.hasPaymentMethod) {
                    Text("Add a payment method in Settings to unblock")
                }
                if (state.paymentError != null) {
                    Text(state.paymentError)
                }
                Button(onClick = {}) { Text("Go Back") }
            }

            FrictionLevel.EXTREME -> {
                Text("Locked")
                Text(state.appName)
                Text("Extreme blocking is active.")
                Text("You committed to zero access. Change in Settings → Friction Level.")
                Button(onClick = {}) { Text("Go Back") }
            }
        }
    }
}
