package com.example.airacerdas.Ob
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.airacerdas.R

class CircularProgressBar(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var progress = 0
    private var max = 100

    private val circlePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 20f
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.circleColor)
    }

    private val waterPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.waterColor)
    }

    private val textPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.white)
        textSize = 80f
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val radius = (width / 2).toFloat()
        val centerX = width / 2
        val centerY = height / 2

        // Draw circle
        canvas.drawCircle(centerX.toFloat(), centerY.toFloat(), radius - 20, circlePaint)

        // Draw water
        val angle = 360 * progress / max
        val rect = RectF(centerX - radius + 20, centerY - radius + 20, centerX + radius - 20, centerY + radius - 20)
        canvas.drawArc(rect, 270f, angle.toFloat(), true, waterPaint)

        // Draw text
        val text = "$progress%"
        canvas.drawText(text, centerX.toFloat(), centerY.toFloat() + textPaint.textSize / 4, textPaint)
    }

    fun setProgress(progress: Int) {
        this.progress = progress
        invalidate()
    }

    fun setMax(max: Int) {
        this.max = max
        invalidate()
    }
}
