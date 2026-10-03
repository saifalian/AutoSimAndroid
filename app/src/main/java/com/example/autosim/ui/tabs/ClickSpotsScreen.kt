package com.example.autosim.ui.tabs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.autosim.ACTION_STOP_EDITING
import com.example.autosim.Routes
import com.example.autosim.db.AppDatabase
import com.example.autosim.db.ClickSpot
import com.example.autosim.overlay.PreviewOverlayService
import kotlinx.coroutines.launch

@Composable
fun ClickSpotsScreen(navController: NavController) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val clickSpotDao = db.clickSpotDao()
    val spots by clickSpotDao.getAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var showPreview by remember { mutableStateOf(false) }
    var isEditingOnScreen by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_STOP_EDITING) {
                    isEditingOnScreen = false
                }
            }
        }
        val intentFilter = IntentFilter(ACTION_STOP_EDITING)
        context.registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        onDispose {
            context.unregisterReceiver(broadcastReceiver)
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Click Spots",
                    style = MaterialTheme.typography.headlineMedium
                )
                Row {
                    Button(
                        onClick = {
                            showPreview = !showPreview
                            PreviewOverlayService.showClickSpots = showPreview
                            if (showPreview) {
                                val intent = Intent(context, PreviewOverlayService::class.java)
                                ContextCompat.startForegroundService(context, intent)
                            } else {
                                context.stopService(Intent(context, PreviewOverlayService::class.java))
                            }
                        }
                    ) {
                        Text(if (showPreview) "Hide Preview" else "Show Preview")
                    }
                    Spacer(modifier = Modifier.padding(4.dp))
                    Button(
                        onClick = {
                            isEditingOnScreen = !isEditingOnScreen
                            PreviewOverlayService.isEditing = isEditingOnScreen
                            if (isEditingOnScreen) {
                                val intent = Intent(context, PreviewOverlayService::class.java)
                                ContextCompat.startForegroundService(context, intent)
                            } else {
                                context.stopService(Intent(context, PreviewOverlayService::class.java))
                            }
                        }
                    ) {
                        Text(if(isEditingOnScreen) "Stop" else "Edit")
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isEditingOnScreen) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(Routes.ADD_EDIT_CLICK_SPOT)
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Click Spot")
                }
            }
        }
    ) { paddingValues ->
        if (spots.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No click spots yet. Tap + to add one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(spots) { spot ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = spot.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Position: (${spot.x}, ${spot.y})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (spot.delay > 0) {
                                    Text(
                                        text = "Delay: ${spot.delay}ms",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (spot.repeat > 1) {
                                    Text(
                                        text = "Repeat: ${spot.repeat}x",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        navController.navigate("${Routes.ADD_EDIT_CLICK_SPOT}/${spot.id}")
                                    }
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            clickSpotDao.delete(spot)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
