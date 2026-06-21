package com.carbonwise.connect.ui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.carbonwise.connect.data.model.HealthStatus

@Composable
fun CompanionStatusScreen(
    justPaired: Boolean = false,
    onNavigateToSettings: () -> Unit,
    onUnpaired: () -> Unit,
    viewModel: CompanionStatusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val healthState by viewModel.healthState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showUnpairConfirmDialog by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val smsStatus = healthState.components.find { it.id == "sms" }?.status
    val notifStatus = healthState.components.find { it.id == "notification" }?.status
    val allPermissionsGranted = smsStatus == HealthStatus.HEALTHY && notifStatus == HealthStatus.HEALTHY
    var hasCheckedPermissionsAfterPairing by rememberSaveable { mutableStateOf(false) }
    var showPostPairingDialog by rememberSaveable { mutableStateOf(false) }
    var hasReturnedFromSettings by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(justPaired, healthState.components) {
        if (justPaired && !hasCheckedPermissionsAfterPairing && healthState.components.isNotEmpty()) {
            if (!allPermissionsGranted) {
                showPostPairingDialog = true
            }
            hasCheckedPermissionsAfterPairing = true
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.refreshPermissions()
        val isNotifMissing = notifStatus != HealthStatus.HEALTHY
        if (isNotifMissing) {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    val requestPermissions = {
        showPostPairingDialog = false
        hasReturnedFromSettings = true
        val isSmsMissing = smsStatus != HealthStatus.HEALTHY
        val isNotifMissing = notifStatus != HealthStatus.HEALTHY
        
        if (isSmsMissing) {
            smsPermissionLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
        } else if (isNotifMissing) {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.isUnpaired) {
        if (uiState.isUnpaired) {
            onUnpaired()
        }
    }

    LaunchedEffect(uiState.unpairError) {
        uiState.unpairError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showUnpairConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showUnpairConfirmDialog = false },
            title = { Text("Unpair Device") },
            text = { Text("Are you sure you want to disconnect this device from your Carbon Footprint account?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnpairConfirmDialog = false
                        viewModel.unpairDevice(force = false)
                    }
                ) {
                    Text("Unpair", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnpairConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPostPairingDialog) {
        AlertDialog(
            onDismissRequest = { showPostPairingDialog = false },
            title = { Text("Complete Setup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "CarbonWise is almost ready to start tracking your carbon footprint. To do this, we need a few permissions:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "📩 Transaction Messages – Used to identify bank and UPI transactions.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🔔 Notifications – Used to capture supported payment notifications in real time.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your personal conversations are never analyzed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = requestPermissions
                ) {
                    Text("Continue Setup")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPostPairingDialog = false }
                ) {
                    Text("Later")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Companion Status",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (hasReturnedFromSettings) {
                val isDark = isSystemInDarkTheme()
                if (allPermissionsGranted) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1B5E20) else Color(0xFFE8F5E9)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "🎉 CarbonWise is Ready",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFA5D6A7) else Color(0xFF1B5E20)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your device is ready to sync transaction data. We will start tracking your carbon footprint automatically.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) Color(0xFFA5D6A7) else Color(0xFF1B5E20)
                            )
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFFC62828) else Color(0xFFFFEBEE)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFFFCDD2) else Color(0xFFC62828),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Additional Permissions Required",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFFCDD2) else Color(0xFFB71C1C)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "To automatically track transactions, please grant the following permissions:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) Color(0xFFFFCDD2) else Color(0xFFB71C1C)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (smsStatus != HealthStatus.HEALTHY) {
                                Text(
                                    text = "• 📩 Transaction Messages",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFFFCDD2) else Color(0xFFB71C1C),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            if (notifStatus != HealthStatus.HEALTHY) {
                                Text(
                                    text = "• 🔔 Notifications",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFFFCDD2) else Color(0xFFB71C1C),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = requestPermissions,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFFE53935) else Color(0xFFD32F2F),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Continue Setup")
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatusRow(label = "Connection", value = uiState.connectionStatus, isGood = true)
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    StatusRow(label = "Account", value = uiState.pairedAccount)
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    StatusRow(label = "Last Sync", value = uiState.lastSync)
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    StatusRow(label = "Pending Uploads", value = uiState.pendingUploads.toString())
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    StatusRow(label = "Failed Uploads", value = uiState.failedUploads.toString(), isCritical = uiState.failedUploads > 0)
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    StatusRow(label = "Sync Status", value = uiState.syncStatus)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Companion Health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))

            // Show overall health banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (healthState.overallHealth) {
                        com.carbonwise.connect.data.model.HealthStatus.HEALTHY -> Color(0xFFE8F5E9)
                        com.carbonwise.connect.data.model.HealthStatus.WARNING -> Color(0xFFFFF3E0)
                        com.carbonwise.connect.data.model.HealthStatus.CRITICAL -> Color(0xFFFFEBEE)
                    }
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (healthState.overallHealth) {
                            com.carbonwise.connect.data.model.HealthStatus.HEALTHY -> Icons.Default.Check
                            else -> Icons.Default.Warning
                        },
                        contentDescription = null,
                        tint = when (healthState.overallHealth) {
                            com.carbonwise.connect.data.model.HealthStatus.HEALTHY -> Color(0xFF4CAF50)
                            com.carbonwise.connect.data.model.HealthStatus.WARNING -> Color(0xFFFF9800)
                            com.carbonwise.connect.data.model.HealthStatus.CRITICAL -> Color(0xFFF44336)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Status: ${healthState.overallHealth.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    healthState.components.forEachIndexed { index, component ->
                        StatusRow(
                            label = component.title, 
                            value = component.description, 
                            isGood = component.status == com.carbonwise.connect.data.model.HealthStatus.HEALTHY,
                            isWarning = component.status == com.carbonwise.connect.data.model.HealthStatus.WARNING,
                            isCritical = component.status == com.carbonwise.connect.data.model.HealthStatus.CRITICAL
                        )
                        if (index < healthState.components.size - 1) {
                            Divider(modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            LaunchedEffect(uiState.syncResult) {
                uiState.syncResult?.let {
                    snackbarHostState.showSnackbar(it)
                    viewModel.clearSyncResult()
                }
            }

            Button(
                onClick = { viewModel.triggerLocalSync() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !uiState.isSyncing
            ) {
                if (uiState.isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Syncing...")
                } else {
                    Text("Sync Now")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Permissions & Settings")
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.unpairError != null) {
                Button(
                    onClick = { viewModel.unpairDevice(force = true) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                ) {
                    Text("Force Remove From This Device")
                }
            } else {
                TextButton(
                    onClick = { showUnpairConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isUnpairing
                ) {
                    if (uiState.isUnpairing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unpairing...", color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Unpair Device", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp)) // padding at bottom
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun StatusRow(label: String, value: String, isGood: Boolean = false, isWarning: Boolean = false, isCritical: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isGood) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
            } else if (isWarning) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
            } else if (isCritical) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF44336), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isGood) Color(0xFF4CAF50) else if (isWarning) Color(0xFFFF9800) else if (isCritical) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
