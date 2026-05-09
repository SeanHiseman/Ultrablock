package com.ultrablock.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ultrablock.domain.model.FrictionLevel
import com.ultrablock.ui.theme.MoneyGreen
import com.ultrablock.ui.theme.UnblockedGreen

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
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
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(24.dp))

        // ── Blocking Friction ──────────────────────────────────────────────
        SectionHeader("Blocking")

        FrictionLevelItem(
            current = uiState.globalFrictionLevel,
            onClick = { viewModel.showFrictionDialog() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Payment ────────────────────────────────────────────────────────
        SectionHeader("Payment")

        SettingsItem(
            icon = Icons.Default.AttachMoney,
            title = "Hourly Rate",
            subtitle = "$${uiState.hourlyRateDollars}/hour",
            onClick = { viewModel.showRateDialog() }
        )

        SettingsItem(
            icon = Icons.Default.AccessTime,
            title = "Default Unblock Duration",
            subtitle = "${uiState.defaultUnblockMinutes} minutes",
            onClick = { viewModel.showUnblockDurationDialog() }
        )

        SettingsItem(
            icon = Icons.Default.CreditCard,
            title = "Payment Method",
            subtitle = if (uiState.hasPaymentMethod) "Card ending in ${uiState.paymentMethodLastFour}" else "No card added",
            onClick = { if (!uiState.hasPaymentMethod) viewModel.addPaymentMethod() },
            trailing = {
                if (uiState.isAddingCard) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else if (uiState.hasPaymentMethod) {
                    TextButton(onClick = { viewModel.removePaymentMethod() }) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    TextButton(onClick = { viewModel.addPaymentMethod() }) { Text("Add Card") }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Permissions ────────────────────────────────────────────────────
        SectionHeader("Permissions")

        PermissionItem(
            title = "Accessibility Service",
            description = "Required to detect app launches",
            isGranted = uiState.hasAccessibilityPermission,
            onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        )

        PermissionItem(
            title = "Display Over Apps",
            description = "Required to show blocker screen",
            isGranted = uiState.hasOverlayPermission,
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Demo Mode Active", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Payments are simulated. Configure Stripe API keys to enable real charges.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────
    if (uiState.showRateDialog) {
        SelectionDialog(
            title = "Set Hourly Rate",
            options = viewModel.rateOptions,
            selectedValue = uiState.hourlyRateDollars,
            formatOption = { "$$it/hour" },
            onSelect = { viewModel.setHourlyRate(it) },
            onDismiss = { viewModel.hideRateDialog() }
        )
    }

    if (uiState.showUnblockDurationDialog) {
        SelectionDialog(
            title = "Default Unblock Duration",
            options = viewModel.durationOptions,
            selectedValue = uiState.defaultUnblockMinutes,
            formatOption = { "$it minutes" },
            onSelect = { viewModel.setDefaultUnblockDuration(it) },
            onDismiss = { viewModel.hideUnblockDurationDialog() }
        )
    }

    if (uiState.showFrictionDialog) {
        FrictionLevelDialog(
            current = uiState.globalFrictionLevel,
            options = viewModel.frictionOptions,
            onSelect = { viewModel.setFrictionLevel(it) },
            onDismiss = { viewModel.hideFrictionDialog() }
        )
    }
}

// ── Components ─────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun FrictionLevelItem(current: FrictionLevel, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = frictionColor(current), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Friction Level", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(text = current.displayName, style = MaterialTheme.typography.bodyMedium, color = frictionColor(current), fontWeight = FontWeight.SemiBold)
                Text(text = current.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onClick) { Text("Change") }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailing != null) trailing()
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (isGranted) UnblockedGreen else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!isGranted) {
                Button(onClick = onClick) { Text("Grant") }
            }
        }
    }
}

@Composable
private fun FrictionLevelDialog(
    current: FrictionLevel,
    options: List<FrictionLevel>,
    onSelect: (FrictionLevel) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Friction Level") },
        text = {
            Column {
                Text(
                    text = "Choose how hard it is to bypass a block.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                options.forEach { level ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelect(level) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (level == current)
                                frictionColor(level).copy(alpha = 0.12f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = level.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = frictionColor(level)
                                )
                                Text(
                                    text = level.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (level == current) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = frictionColor(level))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun <T> SelectionDialog(
    title: String,
    options: List<T>,
    selectedValue: T,
    formatOption: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(options) { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(option) }.padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = formatOption(option), style = MaterialTheme.typography.bodyLarge)
                        if (option == selectedValue) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun frictionColor(level: FrictionLevel): Color = when (level) {
    FrictionLevel.GENTLE -> Color(0xFF64B5F6)
    FrictionLevel.MODERATE -> Color(0xFFFFB74D)
    FrictionLevel.STRICT -> Color(0xFF9C27B0)
    FrictionLevel.EXTREME -> Color(0xFFE53935)
}
