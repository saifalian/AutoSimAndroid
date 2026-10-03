package com.example.autosim.ui.tabs

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.autosim.MainActivity
import com.example.autosim.db.AppDatabase

@Composable
fun SettingsScreen() {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.titleLarge
            )
            
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Accessibility Settings")
            }
            
            Button(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Overlay Permission Settings")
            }
            
            Button(
                onClick = {
                    (context as? MainActivity)?.startScreenCaptureIntent()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Request Screen Capture Permission")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Data Management",
                style = MaterialTheme.typography.titleLarge
            )
            
            var showImportExport by remember { mutableStateOf(false) }
            
            Button(
                onClick = { showImportExport = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import/Export Configuration")
            }
            
            if (showImportExport) {
                com.example.autosim.ui.dialogs.ImportExportDialog(
                    db = AppDatabase.getDatabase(context),
                    onDismiss = { showImportExport = false }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "About",
                style = MaterialTheme.typography.titleLarge
            )
            
            Text(
                text = "AutoSim v0.1\nAdvanced Android automation with OCR",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
