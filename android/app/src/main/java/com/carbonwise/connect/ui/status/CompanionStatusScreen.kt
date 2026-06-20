package com.carbonwise.connect.ui.status

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CompanionStatusScreen(
    onNavigateToSettings: () -> Unit,
    onUnpaired: () -> Unit,
    viewModel: CompanionStatusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showUnpairConfirmDialog by remember { mutableStateOf(false) }

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

            val healthState by viewModel.healthState.collectAsState()
            
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
                onClick = { 
                    android.util.Log.d("ManualSync", "SyncNow Button clicked")
                    viewModel.triggerLocalSync() 
                },
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
