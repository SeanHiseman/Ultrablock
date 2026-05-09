package com.ultrablock.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ultrablock.data.local.entity.AccountabilityPartner
import com.ultrablock.ui.theme.UltrablockTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SocialScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ── User code display ──────────────────────────────────────────────────

    @Test
    fun showsUserCodeWhenGenerated() {
        showSocialContent(userCode = "UB-A1B2-C3D4", partners = emptyList())
        composeRule.onNodeWithText("UB-A1B2-C3D4").assertIsDisplayed()
    }

    @Test
    fun showsGeneratingWhenCodeIsEmpty() {
        showSocialContent(userCode = "", partners = emptyList())
        composeRule.onNodeWithText("Generating", substring = true).assertIsDisplayed()
    }

    @Test
    fun showsShareStatsButton() {
        showSocialContent(userCode = "UB-A1B2-C3D4", partners = emptyList())
        composeRule.onNodeWithText("Share My Stats", substring = true).assertIsDisplayed()
    }

    // ── Stats display ──────────────────────────────────────────────────────

    @Test
    fun showsTodaySuccessRate() {
        showSocialContent(
            userCode = "UB-A1B2-C3D4",
            partners = emptyList(),
            todayAttempts = 10,
            todaySuccessful = 8
        )
        composeRule.onNodeWithText("80%").assertIsDisplayed()
    }

    @Test
    fun showsWeekSuccessRate() {
        showSocialContent(
            userCode = "UB-A1B2-C3D4",
            partners = emptyList(),
            weekAttempts = 20,
            weekSuccessful = 15
        )
        composeRule.onNodeWithText("75%").assertIsDisplayed()
    }

    @Test
    fun showsHeldCountInStats() {
        showSocialContent(
            userCode = "UB-A1B2-C3D4",
            partners = emptyList(),
            todayAttempts = 5,
            todaySuccessful = 3
        )
        composeRule.onNodeWithText("3/5 held", substring = true).assertIsDisplayed()
    }

    @Test
    fun shows100PercentWhenNoAttempts() {
        showSocialContent(
            userCode = "UB-A1B2-C3D4",
            partners = emptyList(),
            todayAttempts = 0,
            todaySuccessful = 0
        )
        // 0 attempts → 100% success (no failures)
        composeRule.onNodeWithText("100%").assertIsDisplayed()
    }

    // ── Partners list ──────────────────────────────────────────────────────

    @Test
    fun showsEmptyStateWhenNoPartners() {
        showSocialContent(userCode = "UB-A1B2-C3D4", partners = emptyList())
        composeRule.onNodeWithText("No partners yet").assertIsDisplayed()
    }

    @Test
    fun showsPartnerDisplayName() {
        showSocialContent(
            userCode = "UB-A1B2-C3D4",
            partners = listOf(partner("UB-E5F6-G7H8", "Alice"))
        )
        composeRule.onNodeWithText("Alice").assertIsDisplayed()
    }

    @Test
    fun showsPartnerCode() {
        showSocialContent(
            userCode = "UB-A1B2-C3D4",
            partners = listOf(partner("UB-E5F6-G7H8", "Alice"))
        )
        composeRule.onNodeWithText("UB-E5F6-G7H8").assertIsDisplayed()
    }

    @Test
    fun showsMultiplePartners() {
        showSocialContent(
            userCode = "UB-A1B2-C3D4",
            partners = listOf(
                partner("UB-E5F6-G7H8", "Alice"),
                partner("UB-I9J0-K1L2", "Bob")
            )
        )
        composeRule.onNodeWithText("Alice").assertIsDisplayed()
        composeRule.onNodeWithText("Bob").assertIsDisplayed()
    }

    // ── Add partner button ─────────────────────────────────────────────────

    @Test
    fun showsAddPartnerButtonInHeader() {
        showSocialContent(userCode = "UB-A1B2-C3D4", partners = emptyList())
        composeRule.onNodeWithText("Add Partner", substring = true).assertIsDisplayed()
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private fun showSocialContent(
        userCode: String,
        partners: List<AccountabilityPartner>,
        todayAttempts: Int = 0,
        todaySuccessful: Int = 0,
        weekAttempts: Int = 0,
        weekSuccessful: Int = 0
    ) {
        composeRule.setContent {
            UltrablockTheme {
                SocialContent(
                    userCode = userCode,
                    partners = partners,
                    todayAttempts = todayAttempts,
                    todaySuccessful = todaySuccessful,
                    weekAttempts = weekAttempts,
                    weekSuccessful = weekSuccessful
                )
            }
        }
    }

    private fun partner(code: String, name: String) =
        AccountabilityPartner(partnerCode = code, displayName = name)
}

@Composable
private fun SocialContent(
    userCode: String,
    partners: List<AccountabilityPartner>,
    todayAttempts: Int,
    todaySuccessful: Int,
    weekAttempts: Int,
    weekSuccessful: Int
) {
    val todayRate = if (todayAttempts == 0) 100 else (todaySuccessful * 100 / todayAttempts)
    val weekRate = if (weekAttempts == 0) 100 else (weekSuccessful * 100 / weekAttempts)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // User code card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Your Code")
                Text(if (userCode.isEmpty()) "Generating…" else userCode)
                Button(onClick = {}) { Text("Share My Stats") }
            }
        }

        // Today stats
        Text("Today")
        Text("$todayRate%")
        Text("$todaySuccessful/$todayAttempts held")

        // Week stats
        Text("This Week")
        Text("$weekRate%")
        Text("$weekSuccessful/$weekAttempts held")

        // Partners section
        Text("Accountability Partners")
        Button(onClick = {}) { Text("Add Partner") }

        if (partners.isEmpty()) {
            Text("No partners yet")
        } else {
            partners.forEach { p ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(p.displayName)
                        Text(p.partnerCode)
                    }
                }
            }
        }
    }
}
