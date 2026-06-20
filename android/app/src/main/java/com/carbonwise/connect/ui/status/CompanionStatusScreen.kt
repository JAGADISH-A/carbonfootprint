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
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Permissions & Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatusRow(label = "SMS Access", value = if (uiState.smsPermission) "Granted" else "Not Granted", isWarning = !uiState.smsPermission)
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    StatusRow(label = "Notification Access", value = if (uiState.notificationPermission) "Granted" else "Not Granted", isWarning = !uiState.notificationPermission)
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    StatusRow(label = "Background Sync", value = if (uiState.backgroundSyncEnabled) "Running" else "Disabled", isWarning = !uiState.backgroundSyncEnabled)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { /* TODO Trigger Sync */ },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Sync Now")
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
fun StatusRow(label: String, value: String, isGood: Boolean = false, isWarning: Boolean = false) {
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
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isGood) Color(0xFF4CAF50) else if (isWarning) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
