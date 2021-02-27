package com.androidDev.drawingapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewTreeObserver
import androidx.core.content.res.ResourcesCompat

class CanvasView(context: Context) : View(context) {

    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap

    private val backgroundColor = ResourcesCompat.getColor(resources, R.color.colorWhite, null)

    var STROKE_WIDTH = 10f // has to be float
    var drawColor = ResourcesCompat.getColor(resources, R.color.colorBlack, null)

    private var paint = Paint().apply {
        color = drawColor
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = STROKE_WIDTH
    }

    private var path = Path()

    private var motionTouchEventX = 0f
    private var motionTouchEventY = 0f

    private var currentX = 0f
    private var currentY = 0f

    private val touchTolerance = ViewConfiguration.get(context).scaledTouchSlop

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if(::extraBitmap.isInitialized) extraBitmap.recycle()

        extraBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        extraCanvas.drawColor(backgroundColor)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawBitmap(extraBitmap, 0f, 0f, null)
        // DRAW STUFF HERE
        // canvas.drawBitmap()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            motionTouchEventX = event.x
            motionTouchEventY = event.y
        }

        when(event?.action){
            MotionEvent.ACTION_DOWN -> { touchStart() }
            MotionEvent.ACTION_MOVE -> { touchMove() }
            MotionEvent.ACTION_UP -> { touchUp() }
        }
        return true
    }

    private fun touchStart() {
        path.reset()
        path.moveTo(motionTouchEventX, motionTouchEventY)
        currentX = motionTouchEventX
        currentY = motionTouchEventY
    }

    private fun touchMove() {
        val dx = Math.abs(motionTouchEventX - currentX)
        val dy = Math.abs(motionTouchEventY - currentY)
        if (dx >= touchTolerance || dy >= touchTolerance) {
            // QuadTo() adds a quadratic bezier from the last point,
            // approaching control point (x1,y1), and ending at (x2,y2).
            path.quadTo(currentX, currentY, (motionTouchEventX + currentX) / 2, (motionTouchEventY + currentY) / 2)
            currentX = motionTouchEventX
            currentY = motionTouchEventY
            // Draw the path in the extra bitmap to cache it.
            extraCanvas.drawPath(path, paint)
        }
        invalidate()
    }

    private fun touchUp() {
        // Reset the path so it doesn't get drawn again.
        path.reset()
        // extraCanvas.restore()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            extraCanvas.clipOutPath(path)
        }
    }

    fun changeStrokeSize() { paint.strokeWidth = STROKE_WIDTH }
    fun changeStrokeColor(newColor:Int) {
        drawColor = newColor
        paint.color = drawColor
    }
    fun clearCanvas(){ if(::extraBitmap.isInitialized){
        extraCanvas.drawColor(backgroundColor)
    }}
}