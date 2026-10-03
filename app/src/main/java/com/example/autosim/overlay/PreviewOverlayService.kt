package com.example.autosim.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.view.ContextThemeWrapper
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import androidx.core.view.isVisible
import com.example.autosim.ACTION_STOP_EDITING
import com.example.autosim.R
import com.example.autosim.db.AppDatabase
import com.example.autosim.db.ClickSpot
import com.example.autosim.db.OcrRegion
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class PreviewOverlayService : Service() {

    companion object {
        var showClickSpots = false
        var showOcrRegions = false
        var isEditing = false
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private lateinit var controlsContainer: LinearLayout
    private lateinit var overlayParams: WindowManager.LayoutParams

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var clickSpots: List<ClickSpot> = emptyList()
    private var ocrRegions: List<OcrRegion> = emptyList()
    
    // Selection and Interaction State
    private var selectedSpot: ClickSpot? = null
    private var selectedRegion: OcrRegion? = null
    private var isMoveMode = false
    private var isDrawingRegion = false
    
    // Dragging State
    private var initialDragX = 0f
    private var initialDragY = 0f
    private var currentDrawingRegion: Rect? = null
    private var drawingStartPoint: Point? = null
    
    // Resize State
    private var resizeMode: ResizeMode = ResizeMode.NONE
    
    // UI Elements that need to be accessed outside setupOverlay
    private lateinit var addRegionFab: FloatingActionButton
    private lateinit var moveFab: FloatingActionButton
    
    private enum class ResizeMode {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
    }

    @Suppress("DEPRECATION")
    private val serviceDisplay: Display?
        get() = windowManager?.defaultDisplay

    private val realScreenSize = Point()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        updateScreenSize()

        createNotificationChannel()
        startForeground(1338, createNotification())
        setupOverlay()
        observeData()
    }

    private fun updateScreenSize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager?.currentWindowMetrics
            realScreenSize.x = windowMetrics?.bounds?.width() ?: 0
            realScreenSize.y = windowMetrics?.bounds?.height() ?: 0
        } else {
            @Suppress("DEPRECATION")
            serviceDisplay?.getRealSize(realScreenSize)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "preview_overlay",
                "Preview Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "preview_overlay")
            .setContentTitle("AutoSim Preview")
            .setContentText("Showing click spots and regions")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlay() {
        val contextThemeWrapper = ContextThemeWrapper(this, R.style.Theme_AutoSim)

        // Drawing view
        overlayView = object : View(this) {
            override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
                super.onSizeChanged(w, h, oldw, oldh)
                updateScreenSize()
                invalidate()
            }
            
            private val arrowPaint = Paint().apply {
                color = Color.GREEN
                style = Paint.Style.STROKE
                strokeWidth = 5f
                isAntiAlias = true
            }
            private val selectedArrowPaint = Paint().apply {
                color = Color.YELLOW
                style = Paint.Style.STROKE
                strokeWidth = 5f
                isAntiAlias = true
            }
            private val regionPaint = Paint().apply {
                color = Color.RED
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }
            private val selectedRegionPaint = Paint().apply {
                color = Color.YELLOW
                style = Paint.Style.STROKE
                strokeWidth = 5f
                isAntiAlias = true
            }
            private val drawingRegionPaint = Paint().apply {
                color = Color.CYAN
                style = Paint.Style.STROKE
                strokeWidth = 3f
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
                isAntiAlias = true
            }
            private val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 30f
                isAntiAlias = true
                setShadowLayer(2f, 0f, 0f, Color.BLACK)
            }
            private val handlePaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                
                // Calculate offset
                val location = IntArray(2)
                getLocationOnScreen(location)
                val offsetX = location[0].toFloat()
                val offsetY = location[1].toFloat()

                if (showClickSpots || isEditing) {
                    val isPortrait = realScreenSize.y > realScreenSize.x
                    clickSpots.forEach { spot ->
                        val (canonicalX, canonicalY) = fromCanonical(context, spot.x, spot.y)
                        // Adjust for overlay offset
                        val drawX = canonicalX - offsetX
                        val drawY = canonicalY - offsetY
                        
                        val paint = if (spot == selectedSpot) selectedArrowPaint else arrowPaint

                        if (isPortrait) {
                            drawArrow(canvas, paint, drawX, drawY)
                        } else {
                            canvas.save()
                            canvas.rotate(-45f, drawX, drawY)
                            drawArrow(canvas, paint, drawX, drawY)
                            canvas.restore()
                        }

                        canvas.drawText(spot.name, drawX + 30f, drawY + 50f, textPaint)
                    }
                }
                
                if (showOcrRegions || isEditing) {
                    ocrRegions.forEach { region ->
                        val paint = if (region == selectedRegion) selectedRegionPaint else regionPaint
                        
                        val rLeft = region.left - offsetX
                        val rTop = region.top - offsetY
                        val rRight = rLeft + region.width
                        val rBottom = rTop + region.height
                        
                        canvas.drawRect(rLeft, rTop, rRight, rBottom, paint)
                        canvas.drawText(region.name, rLeft, rTop - 10f, textPaint)
                        
                        if (region == selectedRegion && isMoveMode) {
                            // Draw resize handles
                            val handleSize = 15f
                            canvas.drawCircle(rLeft, rTop, handleSize, handlePaint) // TL
                            canvas.drawCircle(rRight, rTop, handleSize, handlePaint) // TR
                            canvas.drawCircle(rLeft, rBottom, handleSize, handlePaint) // BL
                            canvas.drawCircle(rRight, rBottom, handleSize, handlePaint) // BR
                        }
                    }
                }
                
                currentDrawingRegion?.let { rect ->
                    canvas.drawRect(rect, drawingRegionPaint)
                }
            }

            private fun drawArrow(canvas: Canvas, paint: Paint, x: Float, y: Float) {
                val arrowBodyLength = 40f
                val headWidth = 30f
                val headHeight = 30f

                val path = Path().apply {
                    val headBaseY = y + headHeight
                    moveTo(x - headWidth / 2, headBaseY)
                    lineTo(x, y)
                    lineTo(x + headWidth / 2, headBaseY)
                    moveTo(x, headBaseY)
                    lineTo(x, headBaseY + arrowBodyLength)
                }
                canvas.drawPath(path, paint)
            }
        }

        // Controls
        val mainFab = FloatingActionButton(contextThemeWrapper).apply { setImageResource(R.drawable.ic_edit) }
        val addFab = FloatingActionButton(contextThemeWrapper).apply { setImageResource(R.drawable.ic_add) }
        addRegionFab = FloatingActionButton(contextThemeWrapper).apply { setImageResource(R.drawable.ic_ocr_region) }
        val deleteFab = FloatingActionButton(contextThemeWrapper).apply { setImageResource(R.drawable.ic_delete) }
        moveFab = FloatingActionButton(contextThemeWrapper).apply { setImageResource(R.drawable.ic_drag_handle) }
        val closeFab = FloatingActionButton(contextThemeWrapper).apply { setImageResource(R.drawable.ic_close) }

        val actionButtons = listOf(addFab, addRegionFab, deleteFab, moveFab, closeFab)
        actionButtons.forEach { it.isVisible = false }

        mainFab.setOnClickListener {
            val areActionsVisible = actionButtons.any { it.isVisible }
            actionButtons.forEach { button -> button.isVisible = !areActionsVisible }
        }

        addFab.setOnClickListener {
            serviceScope.launch {
                val spots = AppDatabase.getDatabase(this@PreviewOverlayService).clickSpotDao().getAll().first()
                val nextName = ((spots.mapNotNull { it.name.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()
                
                updateScreenSize()
                val centerX = realScreenSize.x / 2f
                val centerY = realScreenSize.y / 2f
                val (canonicalX, canonicalY) = toCanonical(this@PreviewOverlayService, centerX, centerY)

                val newSpot = ClickSpot(name = nextName, x = canonicalX.toInt(), y = canonicalY.toInt())
                AppDatabase.getDatabase(this@PreviewOverlayService).clickSpotDao().insert(newSpot)
            }
        }
        
        addRegionFab.setOnClickListener {
            isDrawingRegion = !isDrawingRegion
            addRegionFab.setColorFilter(if (isDrawingRegion) Color.GREEN else Color.BLACK)
            updateOverlayFlags()
            // If we start drawing, deselect everything
            if (isDrawingRegion) {
                selectedSpot = null
                selectedRegion = null
                isMoveMode = false
                moveFab.setImageResource(R.drawable.ic_drag_handle)
                overlayView?.invalidate()
            }
        }

        deleteFab.setOnClickListener {
            serviceScope.launch {
                selectedSpot?.let { 
                    AppDatabase.getDatabase(this@PreviewOverlayService).clickSpotDao().delete(it)
                    selectedSpot = null
                }
                selectedRegion?.let {
                    AppDatabase.getDatabase(this@PreviewOverlayService).ocrRegionDao().delete(it)
                    selectedRegion = null
                }
                // Fallback: delete last added if nothing selected (legacy behavior)
                if (selectedSpot == null && selectedRegion == null) {
                    val lastSpot = AppDatabase.getDatabase(this@PreviewOverlayService).clickSpotDao().getAll().first().maxByOrNull { it.id }
                    lastSpot?.let { AppDatabase.getDatabase(this@PreviewOverlayService).clickSpotDao().delete(it) }
                }
                overlayView?.invalidate()
            }
        }
        
        closeFab.setOnClickListener {
            sendBroadcast(Intent(ACTION_STOP_EDITING))
            isEditing = false
            stopSelf()
        }

        controlsContainer = LinearLayout(contextThemeWrapper).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(mainFab)
            actionButtons.forEach { addView(it) }
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            overlayType, 
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        
        windowManager?.addView(overlayView, overlayParams)

        val controlsParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { 
            gravity = Gravity.BOTTOM or Gravity.END
            x = 16
            y = 100 
        }

        moveFab.setOnClickListener {
            isMoveMode = !isMoveMode
            moveFab.setImageResource(if (isMoveMode) R.drawable.ic_done else R.drawable.ic_drag_handle)
            if (isMoveMode) {
                isDrawingRegion = false
                addRegionFab.clearColorFilter()
            }
            updateOverlayFlags()
            overlayView?.invalidate()
        }

        controlsContainer.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = controlsParams.x
                        initialY = controlsParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY
                        
                        // Only start dragging if moved more than 10 pixels
                        if (!isDragging && (abs(deltaX) > 10f || abs(deltaY) > 10f)) {
                            isDragging = true
                        }
                        
                        if (isDragging) {
                            controlsParams.x = (initialX - deltaX).toInt()
                            controlsParams.y = (initialY - deltaY).toInt()
                            windowManager?.updateViewLayout(controlsContainer, controlsParams)
                            return true
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isDragging) {
                            isDragging = false
                            return true
                        }
                    }
                }
                return false
            }
        })
        
        windowManager?.addView(controlsContainer, controlsParams)

        overlayView?.setOnTouchListener { v, event ->
            // Calculate offset
            val location = IntArray(2)
            v.getLocationOnScreen(location)
            val offsetX = location[0].toFloat()
            val offsetY = location[1].toFloat()
            
            // Raw coordinates on screen
            val rawX = event.x + offsetX
            val rawY = event.y + offsetY
            
            // Canonical coordinates for ClickSpots
            val (canonicalX, canonicalY) = toCanonical(this, rawX, rawY)

            if (isDrawingRegion) {
                handleDrawing(event, offsetX, offsetY)
            } else if (isMoveMode) {
                handleMoveAndResize(event, rawX, rawY, canonicalX, canonicalY, offsetX, offsetY)
            } else {
                false
            }
        }
    }
    
    private fun handleDrawing(event: MotionEvent, offsetX: Float, offsetY: Float): Boolean {
        val x = event.x
        val y = event.y
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                drawingStartPoint = Point(x.toInt(), y.toInt())
                currentDrawingRegion = Rect(x.toInt(), y.toInt(), x.toInt(), y.toInt())
                overlayView?.invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                drawingStartPoint?.let { start ->
                    currentDrawingRegion = Rect(
                        kotlin.math.min(start.x, x.toInt()),
                        kotlin.math.min(start.y, y.toInt()),
                        kotlin.math.max(start.x, x.toInt()),
                        kotlin.math.max(start.y, y.toInt())
                    )
                    overlayView?.invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                currentDrawingRegion?.let { rect ->
                    // Save region
                    // Note: Saving in raw screen coordinates relative to the overlay's current position + offset
                    // Ideally we want global screen coordinates.
                    // rect is in overlay local coordinates.
                    // Global rect:
                    val globalLeft = rect.left + offsetX.toInt()
                    val globalTop = rect.top + offsetY.toInt()
                    
                    serviceScope.launch {
                        val regions = AppDatabase.getDatabase(this@PreviewOverlayService).ocrRegionDao().getAll().first()
                        val nextName = "Region " + ((regions.size) + 1).toString()
                        
                        val newRegion = OcrRegion(
                            name = nextName,
                            left = globalLeft,
                            top = globalTop,
                            width = rect.width(),
                            height = rect.height()
                        )
                        AppDatabase.getDatabase(this@PreviewOverlayService).ocrRegionDao().insert(newRegion)
                    }
                }
                currentDrawingRegion = null
                drawingStartPoint = null
                isDrawingRegion = false
                addRegionFab.clearColorFilter()
                updateOverlayFlags()
                overlayView?.invalidate()
                return true
            }
        }
        return false
    }

    private fun handleMoveAndResize(
        event: MotionEvent, 
        rawX: Float, rawY: Float, 
        canonicalX: Float, canonicalY: Float,
        offsetX: Float, offsetY: Float
    ): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check for resize handles first if a region is selected
                if (selectedRegion != null) {
                    val region = selectedRegion!!
                    val rLeft = region.left - offsetX
                    val rTop = region.top - offsetY
                    val rRight = rLeft + region.width
                    val rBottom = rTop + region.height
                    val touchX = event.x
                    val touchY = event.y
                    val threshold = 40f
                    
                    if (abs(touchX - rLeft) < threshold && abs(touchY - rTop) < threshold) {
                        resizeMode = ResizeMode.TOP_LEFT
                        return true
                    } else if (abs(touchX - rRight) < threshold && abs(touchY - rTop) < threshold) {
                        resizeMode = ResizeMode.TOP_RIGHT
                        return true
                    } else if (abs(touchX - rLeft) < threshold && abs(touchY - rBottom) < threshold) {
                        resizeMode = ResizeMode.BOTTOM_LEFT
                        return true
                    } else if (abs(touchX - rRight) < threshold && abs(touchY - rBottom) < threshold) {
                        resizeMode = ResizeMode.BOTTOM_RIGHT
                        return true
                    }
                }
                
                // Check for regions (Move)
                val clickedRegion = findClosestRegion(rawX, rawY)
                if (clickedRegion != null) {
                    selectedRegion = clickedRegion
                    selectedSpot = null
                    resizeMode = ResizeMode.CENTER
                    initialDragX = clickedRegion.left - rawX
                    initialDragY = clickedRegion.top - rawY
                    overlayView?.invalidate()
                    return true
                }

                // Check for click spots (Move)
                val clickedSpot = findClosestClickSpot(canonicalX, canonicalY)
                if (clickedSpot != null) {
                    selectedSpot = clickedSpot
                    selectedRegion = null
                    resizeMode = ResizeMode.CENTER
                    initialDragX = clickedSpot.x - canonicalX
                    initialDragY = clickedSpot.y - canonicalY
                    overlayView?.invalidate()
                    return true
                }
                
                // Deselect if clicked empty space
                selectedSpot = null
                selectedRegion = null
                overlayView?.invalidate()
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (selectedRegion != null) {
                    val region = selectedRegion!!
                    var newLeft = region.left
                    var newTop = region.top
                    var newWidth = region.width
                    var newHeight = region.height
                    
                    val touchX = rawX
                    val touchY = rawY
                    
                    when (resizeMode) {
                        ResizeMode.CENTER -> {
                            newLeft = (touchX + initialDragX).toInt()
                            newTop = (touchY + initialDragY).toInt()
                        }
                        ResizeMode.TOP_LEFT -> {
                            val right = region.left + region.width
                            val bottom = region.top + region.height
                            newLeft = touchX.toInt()
                            newTop = touchY.toInt()
                            newWidth = right - newLeft
                            newHeight = bottom - newTop
                        }
                        ResizeMode.TOP_RIGHT -> {
                            val bottom = region.top + region.height
                            newTop = touchY.toInt()
                            newWidth = (touchX - region.left).toInt()
                            newHeight = bottom - newTop
                        }
                        ResizeMode.BOTTOM_LEFT -> {
                            val right = region.left + region.width
                            newLeft = touchX.toInt()
                            newWidth = right - newLeft
                            newHeight = (touchY - region.top).toInt()
                        }
                        ResizeMode.BOTTOM_RIGHT -> {
                            newWidth = (touchX - region.left).toInt()
                            newHeight = (touchY - region.top).toInt()
                        }
                        else -> {}
                    }
                    
                    // Min size constraint
                    if (newWidth < 50) newWidth = 50
                    if (newHeight < 50) newHeight = 50
                    
                    val updatedRegion = region.copy(left = newLeft, top = newTop, width = newWidth, height = newHeight)
                    selectedRegion = updatedRegion // Update local reference for smooth dragging
                    
                    // Debounced DB update could be better, but direct update for now
                    serviceScope.launch { AppDatabase.getDatabase(this@PreviewOverlayService).ocrRegionDao().update(updatedRegion) }
                    
                    return true
                } else if (selectedSpot != null) {
                    val newX = canonicalX + initialDragX
                    val newY = canonicalY + initialDragY
                    val updatedSpot = selectedSpot!!.copy(x = newX.toInt(), y = newY.toInt())
                    selectedSpot = updatedSpot
                    serviceScope.launch { AppDatabase.getDatabase(this@PreviewOverlayService).clickSpotDao().update(updatedSpot) }
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                resizeMode = ResizeMode.NONE
                overlayView?.invalidate()
                return true
            }
        }
        return false
    }

    private fun updateOverlayFlags() {
        if (overlayView?.parent == null || windowManager == null) return

        val overlayFlag = when {
            isMoveMode || isDrawingRegion -> WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            else -> WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        if (overlayParams.flags != overlayFlag) {
            overlayParams.flags = overlayFlag
            windowManager?.updateViewLayout(overlayView, overlayParams)
        }
    }

    private fun findClosestClickSpot(x: Float, y: Float): ClickSpot? {
        return clickSpots.minByOrNull { spot ->
            sqrt((spot.x - x).pow(2) + (spot.y - y).pow(2))
        }?.takeIf { spot ->
            sqrt((spot.x - x).pow(2) + (spot.y - y).pow(2)) < 100
        }
    }
    
    private fun findClosestRegion(x: Float, y: Float): OcrRegion? {
        // Find region that contains the point
        return ocrRegions.findLast { region -> 
            x >= region.left && x <= region.left + region.width &&
            y >= region.top && y <= region.top + region.height
        }
    }

    private fun observeData() {
        val db = AppDatabase.getDatabase(this)
        serviceScope.launch { db.clickSpotDao().getAll().collect { spots: List<ClickSpot> ->
            clickSpots = spots
            overlayView?.invalidate() 
        } }
        serviceScope.launch { db.ocrRegionDao().getAll().collect { regions: List<OcrRegion> ->
            ocrRegions = regions
            overlayView?.invalidate()
        } }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayView?.isAttachedToWindow == true) windowManager?.removeView(overlayView)
        if (controlsContainer.isAttachedToWindow) windowManager?.removeView(controlsContainer)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
