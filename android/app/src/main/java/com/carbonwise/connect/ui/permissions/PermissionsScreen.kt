package com.carbonwise.connect.ui.permissions

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PermissionsViewModel = hiltViewModel()
) {
    val permissionState by viewModel.permissionState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

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

    val smsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            viewModel.refreshPermissions()
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Permissions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            PermissionItemCard(
                title = "SMS Access",
                description = "Required to detect purchase receipts.",
                icon = Icons.Default.Message,
                isGranted = permissionState.smsGranted,
                actionLabel = "Grant Access",
                onAction = {
                    smsLauncher.launch(Manifest.permission.READ_SMS)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            PermissionItemCard(
                title = "Notification Access",
                description = "Required to read payment notifications.",
                icon = Icons.Default.Notifications,
                isGranted = permissionState.notificationGranted,
                actionLabel = "Open Settings",
                onAction = {
                    viewModel.requestNotificationAccess()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            PermissionItemCard(
                title = "Battery Optimization",
                description = "Required to ensure background sync runs reliably.",
                icon = Icons.Default.BatteryAlert,
                isGranted = permissionState.batteryOptimizationIgnored,
                actionLabel = "Disable Optimization",
                onAction = {
                    viewModel.requestBatteryOptimization()
                }
            )
        }
    }
}

@Composable
private fun PermissionItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isGranted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!isGranted) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
