package com.example.autosim.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.autosim.db.AppDatabase
import com.example.autosim.db.ClickSpot
import com.example.autosim.db.OcrRegion
import com.example.autosim.db.SequenceStep
import com.example.autosim.db.StepType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SequenceStepDialog(
    db: AppDatabase,
    sequenceId: Int,
    step: SequenceStep?,
    stepNumber: Int,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var selectedType by remember { mutableStateOf(step?.type ?: StepType.CLICK) }
    var selectedClickSpot by remember { mutableStateOf<Int?>(step?.targetId) }
    var selectedOcrRegion by remember { mutableStateOf<Int?>(step?.targetId) }
    var waitDelay by remember { mutableStateOf(step?.delay?.toString() ?: "1000") }
    var delayAfter by remember { mutableStateOf(step?.delayAfter?.toString() ?: "100") }
    var repeatCount by remember { mutableStateOf(step?.repeatCount?.toString() ?: "1") }
    var conditionalPhrase by remember { mutableStateOf(step?.conditionalPhrase ?: "") }
    var jumpToStep by remember { mutableStateOf(step?.jumpToStep?.toString() ?: "") }
    
    val clickSpots = db.clickSpotDao().getAll().collectAsState(initial = emptyList()).value
    val ocrRegions = db.ocrRegionDao().getAll().collectAsState(initial = emptyList()).value
    val scope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (step == null) "Add Step" else "Edit Step") },
        text = {
            Column {
                Text("Step Type:")
                StepType.values().forEach { type ->
                    Button(
                        onClick = { selectedType = type },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (selectedType == type) "✓ ${type.name}" else type.name)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                when (selectedType) {
                    StepType.CLICK -> {
                        Text("Select Click Spot:")
                        clickSpots.forEach { spot ->
                            Button(
                                onClick = { selectedClickSpot = spot.id },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (selectedClickSpot == spot.id) "✓ ${spot.name}" else spot.name)
                            }
                        }
                    }
                    StepType.OCR_REGION_SCAN -> {
                        Text("Select OCR Region:")
                        ocrRegions.forEach { region ->
                            Button(
                                onClick = { selectedOcrRegion = region.id },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (selectedOcrRegion == region.id) "✓ ${region.name}" else region.name)
                            }
                        }
                    }
                    StepType.WAIT -> {
                        TextField(
                            value = waitDelay,
                            onValueChange = { waitDelay = it },
                            label = { Text("Delay (ms)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    StepType.REPEAT -> {
                        TextField(
                            value = repeatCount,
                            onValueChange = { repeatCount = it },
                            label = { Text("Repeat Count") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    StepType.CONDITIONAL -> {
                        TextField(
                            value = conditionalPhrase,
                            onValueChange = { conditionalPhrase = it },
                            label = { Text("Phrase to Match") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = jumpToStep,
                            onValueChange = { jumpToStep = it },
                            label = { Text("Jump to Step Number") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {}
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = delayAfter,
                    onValueChange = { delayAfter = it },
                    label = { Text("Delay After (ms)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        val newStep = SequenceStep(
                            id = step?.id ?: 0,
                            sequenceId = sequenceId,
                            stepNumber = stepNumber,
                            type = selectedType,
                            targetId = when (selectedType) {
                                StepType.CLICK -> selectedClickSpot
                                StepType.OCR_REGION_SCAN -> selectedOcrRegion
                                else -> null
                            },
                            delay = if (selectedType == StepType.WAIT) waitDelay.toLongOrNull() else null,
                            delayAfter = delayAfter.toLongOrNull() ?: 100,
                            repeatCount = if (selectedType == StepType.REPEAT) repeatCount.toIntOrNull() else null,
                            jumpToStep = if (selectedType == StepType.CONDITIONAL) jumpToStep.toIntOrNull() else null,
                            conditionalPhrase = if (selectedType == StepType.CONDITIONAL) conditionalPhrase else null
                        )
                        
                        if (step == null) {
                            db.sequenceStepDao().insert(newStep)
                        } else {
                            db.sequenceStepDao().update(newStep)
                        }
                        onSave()
                    }
                },
                enabled = when (selectedType) {
                    StepType.CLICK -> selectedClickSpot != null
                    StepType.OCR_REGION_SCAN -> selectedOcrRegion != null
                    StepType.CONDITIONAL -> conditionalPhrase.isNotBlank() && jumpToStep.toIntOrNull() != null
                    else -> true
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

