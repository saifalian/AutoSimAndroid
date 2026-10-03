package com.example.autosim.engine

import android.util.Log
import com.example.autosim.accessibility.AutomationAccessibilityService
import com.example.autosim.db.AppDatabase
import com.example.autosim.db.LogHistory
import com.example.autosim.db.OcrRegionTextHistory
import com.example.autosim.db.StepType
import com.example.autosim.overlay.fromCanonical
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object SequenceRunner {
    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused = _isPaused.asStateFlow()
    
    private val _logs = MutableStateFlow<LogHistory?>(null)
    val logs = _logs.asStateFlow()

    private var job: Job? = null
    private var accessibilityService: AutomationAccessibilityService? = null
    private var db: AppDatabase? = null
    private val runnerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun setAccessibilityService(service: AutomationAccessibilityService) {
        accessibilityService = service
    }

    fun setDatabase(database: AppDatabase) {
        db = database
    }

    suspend fun hasOcrSteps(sequenceId: Int): Boolean {
        val steps = db?.sequenceStepDao()?.getStepsForSequence(sequenceId)?.first() ?: emptyList()
        return steps.any { it.type == StepType.OCR_REGION_SCAN }
    }

    fun startSequence(sequenceId: Int) {
        if (_isRunning.value) return
        Log.i("SequenceRunner", "Starting sequence with ID: $sequenceId")
        
        job = runnerScope.launch {
            _isRunning.value = true
            log("Sequence started.", "SUCCESS")

            try {
                val sequence = db?.sequenceDao()?.getById(sequenceId)
                if (sequence == null) {
                    log("Sequence with ID $sequenceId not found.", "ERROR")
                    return@launch
                }

                val steps = db?.sequenceStepDao()?.getStepsForSequence(sequenceId)?.first() ?: emptyList()
                var repeatCount = sequence.repeatCount

                while (_isRunning.value && (repeatCount == -1 || repeatCount > 0)) {
                    var i = 0
                    while (i < steps.size) {
                        if (!_isRunning.value) break
                        
                        while (_isPaused.value) {
                            delay(100)
                        }
                        
                        val step = steps[i]
                        log("Executing step ${step.stepNumber}: ${step.type}")
                        
                        when (step.type) {
                            StepType.CLICK -> {
                                val clickSpotId = step.targetId
                                if (clickSpotId != null) {
                                    log("Looking for click spot with ID: $clickSpotId")
                                    val spot = db?.clickSpotDao()?.getById(clickSpotId)
                                    if (spot != null) {
                                        log("Click spot found: ${spot.name}")
                                        if (accessibilityService != null) {
                                            // Convert canonical coordinates to screen coordinates before tapping
                                            val (screenX, screenY) = fromCanonical(accessibilityService!!, spot.x, spot.y)
                                            val success = accessibilityService?.performTap(screenX.toInt(), screenY.toInt())
                                            if (success == true) {
                                                log("Click successful at ($screenX, $screenY)")
                                            } else {
                                                log("Click failed", "ERROR")
                                            }
                                        } else {
                                            log("Accessibility service is not available.", "ERROR")
                                        }
                                    } else {
                                        log("Click spot with ID $clickSpotId not found.", "ERROR")
                                    }
                                }
                            }
                            StepType.OCR_REGION_SCAN -> { 
                                val regionId = step.targetId
                                if (regionId != null) {
                                    log("Looking for OCR region with ID: $regionId")
                                    val region = db?.ocrRegionDao()?.getById(regionId)
                                    if (region != null) {
                                        log("OCR region found: ${region.name}")
                                        
                                        // Get latest screen capture
                                        val screenBitmap = ScreenCaptureService.getLatestBitmap()
                                        if (screenBitmap != null) {
                                            log("Screen captured, cropping to region bounds")
                                            
                                            // Crop to region
                                            val croppedBitmap = ScreenCaptureService.cropBitmapForRegion(
                                                screenBitmap,
                                                region.left,
                                                region.top,
                                                region.width,
                                                region.height
                                            )
                                            
                                            if (croppedBitmap != null) {
                                                log("Performing OCR on region")
                                                val recognizedText = OcrProcessor.recognizeBitmap(croppedBitmap)
                                                log("Recognized text: $recognizedText")
                                                
                                                // Save to history
                                                db?.ocrRegionTextHistoryDao()?.insert(
                                                    OcrRegionTextHistory(
                                                        regionId = region.id,
                                                        recognizedText = recognizedText
                                                    )
                                                )
                                                
                                                // Get all groups for this region
                                                val groups = db?.ocrGroupDao()?.getGroupsForRegion(region.id)?.first() ?: emptyList()
                                                log("Found ${groups.size} groups for region")
                                                
                                                // Check each group's phrases
                                                var matchFound = false
                                                for (group in groups) {
                                                    if (matchFound) break
                                                    
                                                    val phrases = db?.ocrPhraseDao()?.getPhrasesForGroup(group.id)?.first() ?: emptyList()
                                                    log("Checking ${phrases.size} phrases in group: ${group.name}")
                                                    
                                                    for (phrase in phrases) {
                                                        if (recognizedText.contains(phrase.text, ignoreCase = true)) {
                                                            log("Phrase matched: '${phrase.text}'", "SUCCESS")
                                                            
                                                            if (phrase.actionId != null) {
                                                                val clickSpot = db?.clickSpotDao()?.getById(phrase.actionId)
                                                                if (clickSpot != null) {
                                                                    log("Executing action: ${clickSpot.name}")
                                                                    if (accessibilityService != null) {
                                                                        val (screenX, screenY) = fromCanonical(accessibilityService!!, clickSpot.x, clickSpot.y)
                                                                        val success = accessibilityService?.performTap(screenX.toInt(), screenY.toInt())
                                                                        if (success == true) {
                                                                            log("Action click successful at ($screenX, $screenY)", "SUCCESS")
                                                                        } else {
                                                                            log("Action click failed", "ERROR")
                                                                        }
                                                                    } else {
                                                                        log("Accessibility service not available", "ERROR")
                                                                    }
                                                                    matchFound = true
                                                                    break
                                                                } else {
                                                                    log("Click spot with ID ${phrase.actionId} not found", "ERROR")
                                                                }
                                                            } else {
                                                                log("Phrase matched but no action assigned")
                                                            }
                                                        }
                                                    }
                                                }
                                                
                                                if (!matchFound) {
                                                    log("No matching phrases found in recognized text")
                                                }
                                                
                                                croppedBitmap.recycle()
                                            } else {
                                                log("Failed to crop bitmap for region", "ERROR")
                                            }
                                        } else {
                                            log("⚠️ Screen Capture Not Available", "ERROR")
                                            log("To use OCR scanning, you must enable screen capture:", "ERROR")
                                            log("1. Go to Settings tab", "ERROR")
                                            log("2. Tap 'Request Screen Capture Permission'", "ERROR")
                                            log("3. Allow the permission", "ERROR")
                                            log("4. Run your sequence again", "ERROR")
                                        }
                                    } else {
                                        log("OCR region with ID $regionId not found", "ERROR")
                                    }
                                } else {
                                    log("No region ID specified for OCR_REGION_SCAN step", "ERROR")
                                }
                            }
                            StepType.TEXT_DETECTION -> {
                                // TODO: Implement text detection
                            }
                            StepType.WAIT -> {
                                step.delay?.let { delay(it) }
                            }
                            StepType.REPEAT -> {
                                // TODO: Implement repeat
                            }
                            StepType.CONDITIONAL -> {
                                // TODO: Implement conditional
                            }
                        }
                        // Use the custom delay if available, otherwise use the default of 1000ms
                        val delayAmount = if (step.delayAfter > 0) step.delayAfter else 1000L
                        log("Waiting for ${delayAmount}ms")
                        delay(delayAmount)
                        i++
                    }
                    if (repeatCount > 0) {
                        repeatCount--
                    }
                }
            } catch (e: Exception) {
                log("Error during sequence execution: ${e.message}", "ERROR")
            } finally {
                _isRunning.value = false
                log("Sequence finished.", "SUCCESS")
            }
        }
    }

    fun pause() {
        _isPaused.value = true
        log("Sequence paused.")
    }

    fun resume() {
        _isPaused.value = false
        log("Sequence resumed.")
    }

    fun stop() {
        _isRunning.value = false
        _isPaused.value = false
        job?.cancel()
        job = null
        log("Sequence stopped.", "SUCCESS")
    }
    
    private fun log(message: String, type: String = "INFO") {
        runnerScope.launch {
            val logEntry = LogHistory(timestamp = System.currentTimeMillis(), message = message, type = type)
            db?.logHistoryDao()?.insert(logEntry)
            _logs.value = logEntry
        }
    }
}
