package com.ruthlessmallard.wavetrace

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

class SignalGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val readings = ArrayDeque<Float>(MAX_READINGS)
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#1a1a2a")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val linePaint = Paint().apply {
        color = Color.parseColor("#00FFFF")
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val fillPaint = Paint().apply {
        color = Color.parseColor("#00FFFF")
        alpha = 30
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = Color.parseColor("#4169E1")
        textSize = 24f
        isAntiAlias = true
    }

    companion object {
        private const val MAX_READINGS = 300 // 5 minutes at 1 reading/sec
        private const val MIN_RSSI = -90f
        private const val MAX_RSSI = -30f
    }

    fun addReading(rssi: Float) {
        readings.addLast(rssi)
        if (readings.size > MAX_READINGS) {
            readings.removeFirst()
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 40f
        val graphWidth = width - padding * 2
        val graphHeight = height - padding * 2

        // Draw grid lines
        val gridLines = 5
        for (i in 0..gridLines) {
            val y = padding + (graphHeight / gridLines) * i
            canvas.drawLine(padding, y, width - padding, y, gridPaint)
        }

        // Draw time labels
        val timeLabels = listOf("-5m", "-4m", "-3m", "-2m", "-1m", "Now")
        for (i in timeLabels.indices) {
            val x = padding + (graphWidth / (timeLabels.size - 1)) * i
            canvas.drawText(timeLabels[i], x - 20f, height - 10f, textPaint)
        }

        // Draw RSSI labels
        val rssiLabels = listOf("-30", "-45", "-60", "-75", "-90")
        for (i in rssiLabels.indices) {
            val y = padding + (graphHeight / (rssiLabels.size - 1)) * i
            canvas.drawText(rssiLabels[i], 5f, y + 8f, textPaint)
        }

        if (readings.size < 2) return

        // Draw signal line
        val path = Path()
        val fillPath = Path()

        val stepX = graphWidth / (MAX_READINGS - 1)

        readings.forEachIndexed { index, rssi ->
            val x = padding + index * stepX
            val normalizedRssi = (rssi - MIN_RSSI) / (MAX_RSSI - MIN_RSSI)
            val clampedNormalized = max(0f, min(1f, normalizedRssi))
            val y = padding + graphHeight - (clampedNormalized * graphHeight)

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        // Close fill path
        val lastX = padding + (readings.size - 1) * stepX
        fillPath.lineTo(lastX, padding + graphHeight)
        fillPath.lineTo(padding, padding + graphHeight)
        fillPath.close()

        // Draw fill
        canvas.drawPath(fillPath, fillPaint)

        // Draw line
        canvas.drawPath(path, linePaint)

        // Draw current value dot
        val lastRssi = readings.last()
        val lastNormalized = (lastRssi - MIN_RSSI) / (MAX_RSSI - MIN_RSSI)
        val lastClamped = max(0f, min(1f, lastNormalized))
        val lastY = padding + graphHeight - (lastClamped * graphHeight)
        val lastXPos = padding + (readings.size - 1) * stepX

        val dotPaint = Paint().apply {
            color = Color.parseColor("#00FFFF")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(lastXPos, lastY, 6f, dotPaint)
    }
}
