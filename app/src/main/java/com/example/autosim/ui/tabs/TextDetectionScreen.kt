package com.example.autosim.ui.tabs

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
import com.example.autosim.db.AppDatabase
import com.example.autosim.db.TextDetectionPhrase
import kotlinx.coroutines.launch

@Composable
fun TextDetectionScreen() {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val phraseDao = db.textDetectionPhraseDao()
    val phrases by phraseDao.getAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPhrase by remember { mutableStateOf<TextDetectionPhrase?>(null) }

    Scaffold(
        topBar = {
            Text(
                text = "Text Detection",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Text Phrase")
            }
        }
    ) { paddingValues ->
        if (phrases.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No text detection phrases yet. Tap + to add one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(phrases) { phrase ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Phrase: ${phrase.text}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (phrase.actionId != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Action: Click Spot #${phrase.actionId}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = { editingPhrase = phrase }
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            phraseDao.delete(phrase)
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

    if (showAddDialog || editingPhrase != null) {
        com.example.autosim.ui.dialogs.TextDetectionDialog(
            db = db,
            phrase = editingPhrase,
            onDismiss = {
                showAddDialog = false
                editingPhrase = null
            },
            onSave = {
                showAddDialog = false
                editingPhrase = null
            }
        )
    }
}
