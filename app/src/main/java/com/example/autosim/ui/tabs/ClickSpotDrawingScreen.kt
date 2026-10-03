package com.example.autosim.ui.tabs

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ClickSpotDrawingScreen(
    onSave: (Int, Int) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var tapLocalOffset by remember { mutableStateOf<Offset?>(null) }
    var tapScreenOffset by remember { mutableStateOf<Offset?>(null) }

    val statusBarHeight = remember {
        val resources = context.resources
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInteropFilter {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        tapLocalOffset = Offset(it.x, it.y)
                        val correctedScreenY = it.rawY - statusBarHeight
                        tapScreenOffset = Offset(it.rawX, correctedScreenY)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        tapLocalOffset = Offset(it.x, it.y)
                        val correctedScreenY = it.rawY - statusBarHeight
                        tapScreenOffset = Offset(it.rawX, correctedScreenY)
                        true
                    }
                    else -> false
                }
            }
        ) {
            tapLocalOffset?.let { touchPointInCanvas ->
                val arrowHeadInCanvas = Offset(touchPointInCanvas.x, touchPointInCanvas.y - statusBarHeight)

                drawLine(
                    color = Color.Green,
                    start = touchPointInCanvas,
                    end = arrowHeadInCanvas,
                    strokeWidth = 5f
                )

                val path = Path().apply {
                    moveTo(arrowHeadInCanvas.x - 15f, arrowHeadInCanvas.y + 15f)
                    lineTo(arrowHeadInCanvas.x, arrowHeadInCanvas.y)
                    lineTo(arrowHeadInCanvas.x + 15f, arrowHeadInCanvas.y + 15f)
                }
                drawPath(path, color = Color.Green, style = Stroke(width = 5f))
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    tapScreenOffset?.let { onSave(it.x.toInt(), it.y.toInt()) }
                },
                enabled = tapScreenOffset != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save This Spot")
            }
            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}
