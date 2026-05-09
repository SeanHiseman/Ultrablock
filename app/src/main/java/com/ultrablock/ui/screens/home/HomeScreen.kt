package com.ultrablock.ui.screens.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.ultrablock.ui.theme.BlockedRed
import com.ultrablock.ui.theme.MoneyGreen
import com.ultrablock.ui.theme.TimePurple
import com.ultrablock.ui.theme.UnblockedGreen

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshPermissions()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Ultrablock",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Break your phone addiction",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Main blocking toggle ───────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.blockingEnabled) UnblockedGreen.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (uiState.blockingEnabled) "Blocking Active" else "Blocking Inactive",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (uiState.blockingEnabled) "Protecting you from distractions"
                        else "Turn on to start blocking",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.blockingEnabled,
                    onCheckedChange = { viewModel.toggleBlocking(it) },
                    enabled = uiState.hasAllPermissions,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = UnblockedGreen,
                        checkedTrackColor = UnblockedGreen.copy(alpha = 0.5f)
                    )
                )
            }
        }

        // ── Permissions warning ────────────────────────────────────────────
        if (!uiState.hasAllPermissions) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BlockedRed.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = BlockedRed, modifier = Modifier.size(24.dp))
                        Text(
                            text = "  Permissions Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = BlockedRed
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Grant permissions in Settings to enable app blocking.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Open Accessibility Settings") }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Allow Display Over Apps") }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Today's focus score ────────────────────────────────────────────
        if (uiState.todayBlockAttempts > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Today's Focus Score", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            text = "${(uiState.todaySuccessRate * 100).toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = successRateColor(uiState.todaySuccessRate)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = uiState.todaySuccessRate,
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = successRateColor(uiState.todaySuccessRate),
                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "${uiState.todaySuccessfulBlocks} blocks held",
                            style = MaterialTheme.typography.bodySmall,
                            color = UnblockedGreen
                        )
                        Text(
                            text = "${uiState.todayBlockAttempts - uiState.todaySuccessfulBlocks} bypassed",
                            style = MaterialTheme.typography.bodySmall,
                            color = BlockedRed
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Stats grid ─────────────────────────────────────────────────────
        Text(
            text = "Statistics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Apps,
                value = "${uiState.blockedAppCount}",
                label = "Apps Blocked",
                iconTint = MaterialTheme.colorScheme.primary
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AttachMoney,
                value = "$${String.format("%.2f", uiState.totalSpent)}",
                label = "Total Spent",
                iconTint = MoneyGreen
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AccessTime,
                value = viewModel.formatTime(uiState.todayTimeSavedMinutes),
                label = "Est. Time Saved",
                iconTint = TimePurple
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Savings,
                value = "$${String.format("%.2f", uiState.estimatedTimeSavedValue)}",
                label = "Value Saved",
                iconTint = MoneyGreen
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Weekly summary ─────────────────────────────────────────────────
        if (uiState.weekBlockAttempts > 0) {
            val weekRate = if (uiState.weekBlockAttempts == 0) 1f
            else uiState.weekSuccessfulBlocks.toFloat() / uiState.weekBlockAttempts

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (weekRate >= 0.7f) Icons.Default.CheckCircle else Icons.Default.Block,
                        contentDescription = null,
                        tint = successRateColor(weekRate),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("This Week", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${uiState.weekSuccessfulBlocks} of ${uiState.weekBlockAttempts} block attempts held",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(weekRate * 100).toInt()}% success rate",
                            style = MaterialTheme.typography.bodySmall,
                            color = successRateColor(weekRate)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── How it works ───────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("How It Works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. Select apps you want to block\n" +
                            "2. Set your block schedule\n" +
                            "3. Choose a friction level in Settings\n" +
                            "4. Add accountability partners in the Social tab",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    iconTint: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun successRateColor(rate: Float): Color = when {
    rate >= 0.8f -> UnblockedGreen
    rate >= 0.5f -> Color(0xFFFFB74D)
    else -> BlockedRed
}
