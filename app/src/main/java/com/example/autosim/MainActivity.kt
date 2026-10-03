package com.example.autosim

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.autosim.accessibility.AutomationAccessibilityService
import com.example.autosim.db.AppDatabase
import com.example.autosim.db.ClickSpot
import com.example.autosim.engine.ScreenCaptureService
import com.example.autosim.ui.tabs.AddEditClickSpotScreen
import com.example.autosim.ui.tabs.ClickSpotsScreen
import com.example.autosim.ui.tabs.LogsScreen
import com.example.autosim.ui.tabs.OcrRegionsScreen
import com.example.autosim.ui.tabs.OverlayControlsScreen
import com.example.autosim.ui.tabs.SequenceBuilderScreen
import com.example.autosim.ui.tabs.SettingsScreen
import com.example.autosim.ui.tabs.TextDetectionScreen
import com.example.autosim.ui.theme.AutoSimTheme

sealed class Screen(val route: String, val resourceId: String, @DrawableRes val icon: Int) {
    object ClickSpots : Screen("click_spots", "Click Spots", R.drawable.ic_click_spot)
    object OcrRegions : Screen("ocr_regions", "OCR Regions", R.drawable.ic_ocr_region)
    object TextDetection : Screen("text_detection", "Text Detection", R.drawable.ic_text_detection)
    object SequenceBuilder : Screen("sequence_builder", "Sequence Builder", R.drawable.ic_sequence_builder)
    object OverlayControls : Screen("overlay_controls", "Overlay Controls", R.drawable.ic_overlay_controls)
    object Logs : Screen("logs", "Logs", R.drawable.ic_logs)
    object Settings : Screen("settings", "Settings", R.drawable.ic_settings)
}

object Routes {
    const val ADD_EDIT_CLICK_SPOT = "add_edit_click_spot"
}

class MainActivity : ComponentActivity() {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private val requestProjection =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val data = result.data!!
                val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val projection = mediaProjectionManager.getMediaProjection(result.resultCode, data)
                if (projection != null) {
                    ScreenCaptureService.setMediaProjection(projection)

                    // Start screen capture service
                    val serviceIntent = Intent(this, ScreenCaptureService::class.java)
                    ContextCompat.startForegroundService(this, serviceIntent)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        setContent {
            AutoSimTheme {
                var showAccessibilityDialog by remember { mutableStateOf(!isAccessibilityServiceEnabled()) }

                if (showAccessibilityDialog) {
                    AccessibilityPermissionDialog(
                        onConfirm = { 
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) 
                            showAccessibilityDialog = false
                        },
                        onDismiss = { showAccessibilityDialog = false }
                    )
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(
                        onRequestScreenCapture = { startScreenCaptureIntent() },
                        onOpenAccessibilitySettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        onOpenOverlaySettings = { requestOverlayPermission() }
                    )
                }
            }
        }
        
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("REQUEST_SCREEN_CAPTURE", false) == true) {
            startScreenCaptureIntent()
        }
    }

    fun startScreenCaptureIntent() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        requestProjection.launch(captureIntent)
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        } else {
            val svc = Intent(this, com.example.autosim.overlay.ExecutionOverlayService::class.java)
            ContextCompat.startForegroundService(this, svc)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "${packageName}/${AutomationAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(expectedComponentName) == true
    }
}

@Composable
fun MainScreen(
    onRequestScreenCapture: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.ClickSpots,
        Screen.OcrRegions,
        Screen.TextDetection,
        Screen.SequenceBuilder,
        Screen.OverlayControls,
        Screen.Logs,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(painterResource(id = screen.icon), contentDescription = screen.resourceId) },
                        label = { Text(screen.resourceId) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.ClickSpots.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.ClickSpots.route) { ClickSpotsScreen(navController = navController) }
            composable(Screen.OcrRegions.route) { OcrRegionsScreen() }
            composable(Screen.TextDetection.route) { TextDetectionScreen() }
            composable(Screen.SequenceBuilder.route) { SequenceBuilderScreen() }
            composable(Screen.OverlayControls.route) { OverlayControlsScreen() }
            composable(Screen.Logs.route) { LogsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }

            composable(Routes.ADD_EDIT_CLICK_SPOT) {
                AddEditClickSpotScreen(navController = navController)
            }
            composable("${Routes.ADD_EDIT_CLICK_SPOT}/{spotId}") { backStackEntry ->
                val spotId = backStackEntry.arguments?.getString("spotId")?.toIntOrNull()
                AddEditClickSpotScreen(navController = navController, spotId = spotId)
            }
        }
    }
}

@Composable
fun AccessibilityPermissionDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accessibility Permission Needed") },
        text = { Text("To perform clicks and other actions, you need to enable the accessibility service for this app.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

