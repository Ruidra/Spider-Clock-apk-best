package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alarm.NotificationHelper
import com.example.ui.screens.AlarmsScreen
import com.example.ui.screens.ClockScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SpiderClockViewModel

enum class SpiderNavTab(val title: String, val icon: ImageVector, val tag: String) {
    CLOCK("Clock", Icons.Default.AccessTime, "nav_clock"),
    ALARMS("Alarms", Icons.Default.Alarm, "nav_alarms"),
    TIMER("Timer", Icons.Default.HourglassBottom, "nav_timer"),
    SETTINGS("Settings", Icons.Default.Settings, "nav_settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannels(this)

        setContent {
            val viewModel: SpiderClockViewModel = viewModel()
            val theme by viewModel.theme.collectAsState()

            MyApplicationTheme(darkTheme = true) {
                SpiderAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SpiderAppRoot(viewModel: SpiderClockViewModel) {
    val theme by viewModel.theme.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Request notification permission if needed
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Back handling: if not on Clock tab, return to Clock
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = theme.backgroundColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = theme.cardBackground,
                contentColor = theme.accentColor,
                tonalElevation = 8.dp
            ) {
                SpiderNavTab.values().forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                color = if (isSelected) theme.accentColor else Color.White.copy(alpha = 0.6f)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = theme.accentColor,
                            indicatorColor = theme.accentColor,
                            unselectedIconColor = Color.White.copy(alpha = 0.5f),
                            unselectedTextColor = Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ClockScreen(
                    viewModel = viewModel,
                    onNavigateToAlarms = { selectedTab = 1 },
                    snackbarHostState = snackbarHostState
                )
                1 -> AlarmsScreen(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState
                )
                2 -> TimerScreen(
                    viewModel = viewModel
                )
                3 -> SettingsScreen(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}
