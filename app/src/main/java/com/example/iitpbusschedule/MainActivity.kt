package com.example.iitpbusschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.content.Intent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.*
import com.example.iitpbusschedule.repository.SettingsManager
import com.example.iitpbusschedule.ui.components.AdminInfoDialog
import com.example.iitpbusschedule.ui.components.IITPBlue
import com.example.iitpbusschedule.ui.components.IITPGold
import com.example.iitpbusschedule.ui.components.TextPrimary
import com.example.iitpbusschedule.ui.components.openSheet
import com.example.iitpbusschedule.ui.screens.HomeScreen
import com.example.iitpbusschedule.ui.screens.SettingsScreen
import com.example.iitpbusschedule.viewmodels.MainViewModel
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enqueue periodic background sync (every 4 hours)
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(4, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork("bus_sync", ExistingPeriodicWorkPolicy.KEEP, syncRequest)

        setContent {
            BusAppTheme(viewModel.settingsManager) {
                BusScheduleApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSchedule()
    }
}

@Composable
fun BusAppTheme(settings: SettingsManager, content: @Composable () -> Unit) {
    val isDark = when (settings.darkMode.value) {
        "on" -> true
        "off" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = Color(0xFF90CAF9), // Lighter blue for dark mode
            secondary = IITPGold,
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onBackground = Color.White,
            onSurface = Color.White,
            primaryContainer = Color(0xFF0D47A1),
            onPrimaryContainer = Color.White
        )
    } else {
        lightColorScheme(
            primary = IITPBlue,
            secondary = IITPGold,
            background = Color(0xFFF8FAFC), // Very light grey/blue
            surface = Color.White,
            onBackground = Color(0xFF0F172A), // Slate 900
            onSurface = Color(0xFF0F172A),
            primaryContainer = Color(0xFFE0E7FF),
            onPrimaryContainer = Color(0xFF1A3A6B)
        )
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}

@Composable
fun OnboardingScreen(onSkip: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha"
    )

    Box(
        Modifier.fillMaxSize().background(IITPBlue),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🚌", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "IIT Patna Bus Schedule",
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Fetching latest schedule...",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = alpha)
            )
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = IITPGold, strokeWidth = 3.dp)

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { onSkip() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Taking too long? Load last updated data.", fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusScheduleApp(viewModel: com.example.iitpbusschedule.viewmodels.MainViewModel) {
    val context = LocalContext.current

    if (viewModel.isLoading) {
        OnboardingScreen(onSkip = { viewModel.skipLoading() })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(viewModel.currentScreen, label = "title") { screen ->
                        if (screen == "HOME") {
                            Column {
                                Text("IIT Patna Bus", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                                val statusText = if (viewModel.isLive) " 🟢 Live" else " ⚪ Offline • Updated ${getTimeAgo(viewModel.lastUpdated)}"
                                Text(statusText, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                            }
                        } else {
                            Text("Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        }
                    }
                },
                navigationIcon = {
                    AnimatedVisibility(viewModel.currentScreen == "SETTINGS") {
                        IconButton(onClick = { viewModel.currentScreen = "HOME" }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                },
                actions = {
                    if (viewModel.currentScreen == "HOME") {
                        IconButton(onClick = { viewModel.showFilterModal = true; performHaptic(context) }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.White)
                        }
                        IconButton(onClick = { openSheet(context); performHaptic(context) }) {
                            Icon(Icons.Default.TableChart, contentDescription = "View Source Sheet", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.showInfo = true }) {
                            Icon(Icons.Default.SupportAgent, contentDescription = "Admin contacts", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.currentScreen = "SETTINGS"; performHaptic(context) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IITPBlue)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            Crossfade(
                targetState = viewModel.currentScreen,
                animationSpec = tween(300),
                label = "screen"
            ) { screen ->
                if (screen == "SETTINGS") {
                    BackHandler { viewModel.currentScreen = "HOME" }
                    SettingsScreen(
                        settings = viewModel.settingsManager,
                        onShowSupport = { viewModel.showInfo = true }
                    )
                } else {
                    HomeScreen(viewModel)
                }
            }
        }
    }

    if (viewModel.showInfo) AdminInfoDialog(onDismiss = { viewModel.showInfo = false })
    if (viewModel.showFilterModal) com.example.iitpbusschedule.ui.components.FilterModal(viewModel = viewModel, onDismiss = { viewModel.showFilterModal = false })

    // Welcome Onboarding
    var showWelcome by remember { mutableStateOf(!viewModel.settingsManager.hasSeenOnboarding) }
    if (showWelcome) {
        com.example.iitpbusschedule.ui.components.WelcomeModal(onDismiss = { 
            showWelcome = false
            viewModel.settingsManager.markOnboardingSeen()
        })
    }
}