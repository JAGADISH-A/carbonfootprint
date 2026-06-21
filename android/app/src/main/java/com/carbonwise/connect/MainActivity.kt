package com.carbonwise.connect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.carbonwise.connect.data.local.SettingsStore
import com.carbonwise.connect.navigation.NavGraph
import com.carbonwise.connect.navigation.Screen
import com.carbonwise.connect.ui.theme.CarbonWiseTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsStore: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val defaultSmsApp = android.provider.Telephony.Sms.getDefaultSmsPackage(this)
        val isDefault = defaultSmsApp == packageName
        android.util.Log.d("SMSDiagnostic", "Default SMS App: $defaultSmsApp")
        android.util.Log.d("SMSDiagnostic", "Is this app default? $isDefault")
        android.util.Log.d("SMSDiagnostic", "API Level: ${android.os.Build.VERSION.SDK_INT}")

        setContent {
            CarbonWiseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val onboardingComplete by settingsStore.onboardingComplete.collectAsState(initial = false)
                    val isConnected by settingsStore.isConnected.collectAsState(initial = false)

                    val startDestination = when {
                        isConnected -> Screen.CompanionStatus.route
                        onboardingComplete -> Screen.CompanionStatus.route
                        else -> Screen.Pairing.route
                    }

                    LaunchedEffect(Unit) {
                        val complete = settingsStore.onboardingComplete.first()
                        val connected = settingsStore.isConnected.first()
                        val target = when {
                            connected -> Screen.CompanionStatus.route
                            complete -> Screen.CompanionStatus.route
                            else -> Screen.Pairing.route
                        }
                        if (navController.currentDestination?.route != target) {
                            navController.navigate(target) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }

                    NavGraph(
                        navController = navController,
                        settingsStore = settingsStore,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
