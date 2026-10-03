package com.example.autosim.ui.dialogs

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.autosim.db.AppDatabase
import com.example.autosim.utils.ImportExport
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun ImportExportDialog(
    db: AppDatabase,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val json = ImportExport.exportToJson(db)
                    context.contentResolver.openOutputStream(it)?.use { output ->
                        output.write(json.toByteArray())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { input ->
                        val json = input.bufferedReader().use { it.readText() }
                        ImportExport.importFromJson(db, json)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import/Export") },
        text = {
            Column {
                Button(
                    onClick = {
                        exportLauncher.launch("autosim_config_${System.currentTimeMillis()}.json")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Configuration")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        importLauncher.launch("application/json")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import Configuration")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

