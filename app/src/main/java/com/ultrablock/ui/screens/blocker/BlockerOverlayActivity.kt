package com.ultrablock.ui.screens.blocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultrablock.domain.model.FrictionLevel
import com.ultrablock.ui.theme.BlockedRed
import com.ultrablock.ui.theme.MoneyGreen
import com.ultrablock.ui.theme.UltrablockTheme
import com.ultrablock.ui.theme.UnblockedGreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class BlockerOverlayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
    }

    private val viewModel: BlockerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: packageName

        viewModel.setBlockedApp(packageName, appName)

        setContent {
            UltrablockTheme {
                BlockerOverlayScreen(
                    viewModel = viewModel,
                    onGoBack = { goToHome() },
                    onUnblockSuccess = { finish() }
                )
            }
        }
    }

    private fun goToHome() {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        goToHome()
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BlockerOverlayScreen(
    viewModel: BlockerViewModel,
    onGoBack: () -> Unit,
    onUnblockSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.paymentSuccess) {
        if (uiState.paymentSuccess) {
            delay(1500)
            onUnblockSuccess()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.95f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Success overlay (shared across all friction levels)
            AnimatedVisibility(visible = uiState.paymentSuccess, enter = fadeIn(), exit = fadeOut()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = UnblockedGreen,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Unblocked!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    val durationText = if (uiState.frictionLevel == FrictionLevel.GENTLE ||
                        uiState.frictionLevel == FrictionLevel.MODERATE
                    ) "5 minutes" else "${uiState.selectedDuration} minutes"
                    Text(
                        text = "${uiState.appName} is accessible for $durationText",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }

            AnimatedVisibility(visible = !uiState.paymentSuccess, enter = fadeIn(), exit = fadeOut()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (uiState.frictionLevel) {
                        FrictionLevel.GENTLE -> GentleContent(uiState, viewModel, onGoBack)
                        FrictionLevel.MODERATE -> ModerateContent(uiState, viewModel, onGoBack)
                        FrictionLevel.STRICT -> StrictContent(uiState, viewModel, onGoBack)
                        FrictionLevel.EXTREME -> ExtremeContent(uiState, onGoBack)
                    }
                }
            }
        }
    }
}

// ── GENTLE ────────────────────────────────────────────────────────────────────

@Composable
private fun GentleContent(
    uiState: BlockerUiState,
    viewModel: BlockerViewModel,
    onGoBack: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.SelfImprovement,
        contentDescription = null,
        tint = Color(0xFF64B5F6),
        modifier = Modifier.size(72.dp)
    )
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = "Take a breath",
        style = MaterialTheme.typography.headlineMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = uiState.appName,
        style = MaterialTheme.typography.titleLarge,
        color = Color.White.copy(alpha = 0.9f)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Is this app helping you right now?\nOr are you just avoiding something?",
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.75f),
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(32.dp))

    if (!uiState.countdownComplete) {
        CountdownRing(seconds = uiState.countdownSeconds, total = 5)
    }

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = { viewModel.bypassFree() },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        enabled = uiState.countdownComplete,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = if (uiState.countdownComplete) "Continue Anyway" else "Wait ${uiState.countdownSeconds}s…",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedButton(
        onClick = onGoBack,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Go Back", style = MaterialTheme.typography.titleMedium)
    }
}

// ── MODERATE ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModerateContent(
    uiState: BlockerUiState,
    viewModel: BlockerViewModel,
    onGoBack: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.Edit,
        contentDescription = null,
        tint = Color(0xFFFFB74D),
        modifier = Modifier.size(72.dp)
    )
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = "Reflect first",
        style = MaterialTheme.typography.headlineMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = uiState.appName,
        style = MaterialTheme.typography.titleLarge,
        color = Color.White.copy(alpha = 0.9f)
    )
    Spacer(modifier = Modifier.height(24.dp))

    if (!uiState.countdownComplete) {
        CountdownRing(seconds = uiState.countdownSeconds, total = 30)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Use this time to write your reason below",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    OutlinedTextField(
        value = uiState.reflectionReason,
        onValueChange = { viewModel.setReflectionReason(it) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text("Why are you opening this app?", color = Color.White.copy(alpha = 0.4f))
        },
        minLines = 3,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFFFFB74D),
            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    )

    val reasonReady = uiState.reflectionReason.trim().length >= 10
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = if (reasonReady) "Good. You've reflected." else "${maxOf(0, 10 - uiState.reflectionReason.trim().length)} more characters needed",
        style = MaterialTheme.typography.bodySmall,
        color = if (reasonReady) UnblockedGreen else Color.White.copy(alpha = 0.5f)
    )

    Spacer(modifier = Modifier.height(20.dp))

    val canContinue = uiState.countdownComplete && reasonReady
    Button(
        onClick = { viewModel.bypassFree() },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        enabled = canContinue,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = when {
                !uiState.countdownComplete -> "Wait ${uiState.countdownSeconds}s…"
                !reasonReady -> "Write a reason first"
                else -> "Continue"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (canContinue) Color.Black else Color.White.copy(alpha = 0.5f)
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedButton(
        onClick = onGoBack,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Go Back", style = MaterialTheme.typography.titleMedium)
    }
}

// ── STRICT (payment) ──────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun StrictContent(
    uiState: BlockerUiState,
    viewModel: BlockerViewModel,
    onGoBack: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.Block,
        contentDescription = null,
        tint = BlockedRed,
        modifier = Modifier.size(80.dp)
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "App Blocked",
        style = MaterialTheme.typography.headlineMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = uiState.appName,
        style = MaterialTheme.typography.titleLarge,
        color = Color.White.copy(alpha = 0.9f)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "is blocked during your focus time",
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.7f)
    )
    Spacer(modifier = Modifier.height(32.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Unblock Duration",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                viewModel.durationOptions.forEach { duration ->
                    FilterChip(
                        selected = uiState.selectedDuration == duration,
                        onClick = { viewModel.setDuration(duration) },
                        label = { Text("${duration}m") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.1f),
                            labelColor = Color.White
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Cost: ", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.8f))
                Text(
                    text = viewModel.formatCost(uiState.costCents),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MoneyGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "at ${viewModel.formatCost(uiState.hourlyRateCents)}/hour",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (uiState.paymentError != null) {
        Card(
            colors = CardDefaults.cardColors(containerColor = BlockedRed.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = BlockedRed, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = uiState.paymentError!!, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    Button(
        onClick = { viewModel.processUnblock() },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MoneyGreen),
        enabled = !uiState.isProcessingPayment && uiState.hasPaymentMethod,
        shape = RoundedCornerShape(12.dp)
    ) {
        if (uiState.isProcessingPayment) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Processing…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        } else {
            Text(
                text = "Unblock for ${viewModel.formatCost(uiState.costCents)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (!uiState.hasPaymentMethod) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add a payment method in Settings to unblock",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
    OutlinedButton(
        onClick = onGoBack,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = "Go Back", style = MaterialTheme.typography.titleMedium)
    }
}

// ── EXTREME ───────────────────────────────────────────────────────────────────

@Composable
private fun ExtremeContent(
    uiState: BlockerUiState,
    onGoBack: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = null,
        tint = BlockedRed,
        modifier = Modifier.size(80.dp)
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "Locked",
        style = MaterialTheme.typography.headlineMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = uiState.appName,
        style = MaterialTheme.typography.titleLarge,
        color = Color.White.copy(alpha = 0.9f)
    )
    Spacer(modifier = Modifier.height(24.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BlockedRed.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Extreme blocking is active.",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You committed to zero access. To change this, go to Settings → Friction Level.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
        }
    }
    Spacer(modifier = Modifier.height(32.dp))
    OutlinedButton(
        onClick = onGoBack,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Go Back", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

// ── Shared UI component ───────────────────────────────────────────────────────

@Composable
private fun CountdownRing(seconds: Int, total: Int) {
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = seconds.toFloat() / total,
            modifier = Modifier.size(80.dp),
            color = Color.White.copy(alpha = 0.8f),
            strokeWidth = 6.dp
        )
        Text(
            text = "$seconds",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
