package com.example.autosim.ui.tabs

import android.content.Intent
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.autosim.db.AppDatabase
import com.example.autosim.db.OcrRegion
import com.example.autosim.overlay.PreviewOverlayService
import kotlinx.coroutines.launch

private sealed class OcrRegionsScreenState {
    object None : OcrRegionsScreenState()
    data class AddEditRegion(val region: OcrRegion?) : OcrRegionsScreenState()
    data class ShowGroups(val region: OcrRegion) : OcrRegionsScreenState()
    data class ShowHistory(val region: OcrRegion) : OcrRegionsScreenState()
}

@Composable
fun OcrRegionsScreen() {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val ocrRegionDao = db.ocrRegionDao()
    val regions by ocrRegionDao.getAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var screenState by remember { mutableStateOf<OcrRegionsScreenState>(OcrRegionsScreenState.None) }

    var showPreview by remember { mutableStateOf(false) }

    fun onDismissAll() {
        screenState = OcrRegionsScreenState.None
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
                Text(text = "OCR Regions", style = MaterialTheme.typography.headlineMedium)
                Button(
                    onClick = {
                        showPreview = !showPreview
                        PreviewOverlayService.showOcrRegions = showPreview
                        if (showPreview) {
                            ContextCompat.startForegroundService(context, Intent(context, PreviewOverlayService::class.java))
                        } else {
                            context.stopService(Intent(context, PreviewOverlayService::class.java))
                        }
                    }
                ) {
                    Text(if (showPreview) "Hide Preview" else "Show Preview")
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { screenState = OcrRegionsScreenState.AddEditRegion(null) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add OCR Region")
            }
        }
    ) { paddingValues ->
        // Main content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (regions.isEmpty()) {
                item {
                    Text("No OCR regions yet. Tap + to add one.")
                }
            } else {
                items(regions) { region ->
                    OcrRegionItem(
                        region = region,
                        onHistoryClick = { screenState = OcrRegionsScreenState.ShowHistory(it) },
                        onGroupsClick = { screenState = OcrRegionsScreenState.ShowGroups(it) },
                        onEditClick = { screenState = OcrRegionsScreenState.AddEditRegion(it) },
                        onDeleteClick = {
                            scope.launch {
                                ocrRegionDao.delete(it)
                            }
                        }
                    )
                }
            }
        }
    }

    when (val state = screenState) {
        is OcrRegionsScreenState.AddEditRegion -> {
            AddEditOcrRegionDialog(
                region = state.region,
                onDismiss = ::onDismissAll,
                onSave = { name, left, top, width, height ->
                    scope.launch {
                        val newRegion = state.region?.copy(name = name, left = left, top = top, width = width, height = height)
                            ?: OcrRegion(name = name, left = left, top = top, width = width, height = height)
                        
                        if (state.region == null) {
                            ocrRegionDao.insert(newRegion)
                        } else {
                            ocrRegionDao.update(newRegion)
                        }
                        onDismissAll()
                    }
                }
            )
        }
        is OcrRegionsScreenState.ShowGroups -> {
            OcrRegionGroupsScreen(
                region = state.region,
                db = db,
                onDismiss = ::onDismissAll
            )
        }
        is OcrRegionsScreenState.ShowHistory -> {
            com.example.autosim.ui.dialogs.OcrRegionHistoryDialog(
                region = state.region,
                db = db,
                onDismiss = ::onDismissAll
            )
        }
        OcrRegionsScreenState.None -> {}
    }
}

@Composable
private fun OcrRegionItem(
    region: OcrRegion,
    onHistoryClick: (OcrRegion) -> Unit,
    onGroupsClick: (OcrRegion) -> Unit,
    onEditClick: (OcrRegion) -> Unit,
    onDeleteClick: (OcrRegion) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = region.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Position: (${region.left}, ${region.top})", style = MaterialTheme.typography.bodySmall)
                Text(text = "Size: ${region.width} × ${region.height}", style = MaterialTheme.typography.bodySmall)
            }
            Row {
                if (region != null) {
                    IconButton(onClick = { onHistoryClick(region) }) { Icon(Icons.Filled.Info, contentDescription = "History") }
                }
                IconButton(onClick = { onGroupsClick(region) }) { Icon(Icons.Filled.Edit, contentDescription = "Groups") }
                IconButton(onClick = { onEditClick(region) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                IconButton(onClick = { onDeleteClick(region) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
        }
    }
}


@Composable
private fun AddEditOcrRegionDialog(
    region: OcrRegion?,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Int, Int) -> Unit
) {
    var isDrawing by remember { mutableStateOf(false) }
    var regionName by remember { mutableStateOf(region?.name ?: "") }
    val left by remember { mutableIntStateOf(region?.left ?: 0) }
    val top by remember { mutableIntStateOf(region?.top ?: 0) }
    val width by remember { mutableIntStateOf(region?.width ?: 0) }
    val height by remember { mutableIntStateOf(region?.height ?: 0) }

    fun startDrawing() { isDrawing = true }
    fun stopDrawing() { isDrawing = false }
    fun onSaveDrawing(name: String, l: Int, t: Int, w: Int, h: Int) {
        onSave(name, l, t, w, h)
        stopDrawing()
    }

    if (isDrawing) {
        com.example.autosim.ui.dialogs.OcrRegionDrawingScreen(
            onSave = ::onSaveDrawing,
            onCancel = ::stopDrawing
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (region == null) "Add OCR Region" else "Edit OCR Region") },
            text = {
                Column {
                    TextField(value = regionName, onValueChange = { regionName = it }, label = { Text("Region Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = ::startDrawing, modifier = Modifier.fillMaxWidth()) {
                        Text(if (region == null) "Draw Region" else "Redraw Region")
                    }
                    if (region != null) {
                        Text("Current: ($left, $top) ${width}×$height")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onSave(regionName, left, top, width, height) }) {
                    Text(if (region == null) "Draw" else "Save")
                }
            },
            dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
        )
    }
}

@Composable
private fun OcrRegionGroupsScreen(
    region: OcrRegion,
    db: AppDatabase,
    onDismiss: () -> Unit
) {
    val groups by db.ocrGroupDao().getGroupsForRegion(region.id).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var editingGroup by remember { mutableStateOf<com.example.autosim.db.OcrGroup?>(null) }
    var isAddingGroup by remember { mutableStateOf(false) }

    fun startAddingGroup() { isAddingGroup = true }
    fun stopAddingGroup() { isAddingGroup = false }

    if (isAddingGroup) {
        com.example.autosim.ui.dialogs.OcrGroupDialog(
            db = db,
            regionId = region.id,
            group = null,
            onDismiss = ::stopAddingGroup,
            onSave = ::stopAddingGroup
        )
    }

    if (editingGroup != null) {
        com.example.autosim.ui.dialogs.OcrGroupDialog(
            db = db,
            regionId = region.id,
            group = editingGroup,
            onDismiss = { editingGroup = null },
            onSave = { editingGroup = null }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Groups for ${region.name}") },
        text = {
            Column {
                Button(onClick = ::startAddingGroup, modifier = Modifier.fillMaxWidth()) {
                    Text("Add Group")
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (groups.isEmpty()) {
                    Text("No groups yet. Click 'Add Group' to create one.", 
                         style = MaterialTheme.typography.bodyMedium,
                         modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    LazyColumn {
                        items(groups) { group ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(group.name, modifier = Modifier.weight(1f))
                                    Row {
                                        Button(onClick = { editingGroup = group }) {
                                            Text("Edit")
                                        }
                                        Button(onClick = { scope.launch { db.ocrGroupDao().delete(group) } }) {
                                            Text("Delete")
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Close") } }
    )
}
