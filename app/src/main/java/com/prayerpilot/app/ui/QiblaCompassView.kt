package com.prayerpilot.app.ui

import android.content.Context
import android.graphics.*
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class QiblaCompassView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {
    var qiblaBearing: Float = 0f
    var deviceAzimuth: Float = 0f
    var accuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 14f
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 28f; color = Color.parseColor("#3300E5FF")
    }
    private val qiblaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#34A853"); style = Paint.Style.FILL
    }
    private val northPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EA4335"); style = Paint.Style.FILL
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; textSize = 60f; isFakeBoldText = true
    }
    private val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#88FFFFFF"); strokeWidth = 4f
    }
    private val center = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#39FF14") }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w/2; val cy = h/2
        val r = min(w, h)/2 - 60f

        // Ring color reflects accuracy
        ringPaint.color = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> Color.parseColor("#34A853")
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> Color.parseColor("#FBBC04")
            else -> Color.parseColor("#EA4335")
        }
        c.drawCircle(cx, cy, r, glowPaint)
        c.drawCircle(cx, cy, r, ringPaint)

        // Tick marks every 15 degrees, longer at cardinals
        for (i in 0 until 360 step 5) {
            val a = Math.toRadians(i.toDouble() - 90 - deviceAzimuth)
            val outer = r
            val inner = if (i % 90 == 0) r - 36f else if (i % 30 == 0) r - 24f else r - 14f
            val x1 = cx + inner * Math.cos(a).toFloat()
            val y1 = cy + inner * Math.sin(a).toFloat()
            val x2 = cx + outer * Math.cos(a).toFloat()
            val y2 = cy + outer * Math.sin(a).toFloat()
            c.drawLine(x1, y1, x2, y2, tick)
        }
        // Cardinal labels
        listOf("N" to 0, "E" to 90, "S" to 180, "W" to 270).forEach { (lbl, deg) ->
            val a = Math.toRadians(deg.toDouble() - 90 - deviceAzimuth)
            val labelR = r - 80f
            val x = cx + labelR * Math.cos(a).toFloat()
            val y = cy + labelR * Math.sin(a).toFloat() + 22f
            text.color = if (lbl == "N") Color.parseColor("#EA4335") else Color.WHITE
            c.drawText(lbl, x, y, text)
        }

        // North needle (red, smaller)
        drawArrow(c, cx, cy, r * 0.85f, 0f - deviceAzimuth, 18f, northPaint)

        // Qibla arrow (green, dominant)
        drawArrow(c, cx, cy, r * 0.95f, qiblaBearing - deviceAzimuth, 36f, qiblaPaint)

        // Center dot
        c.drawCircle(cx, cy, 18f, center)
    }

    private fun drawArrow(c: Canvas, cx: Float, cy: Float, length: Float,
                          angleDeg: Float, headSize: Float, paint: Paint) {
        val a = Math.toRadians((angleDeg - 90).toDouble())
        val tipX = cx + length * Math.cos(a).toFloat()
        val tipY = cy + length * Math.sin(a).toFloat()

        // Body line
        val body = Paint(paint).apply { strokeWidth = 8f; style = Paint.Style.STROKE }
        c.drawLine(cx, cy, tipX, tipY, body)

        // Triangle head
        val left = Math.toRadians(angleDeg.toDouble() - 90 + 150)
        val right = Math.toRadians(angleDeg.toDouble() - 90 - 150)
        val lx = tipX + headSize * Math.cos(left).toFloat()
        val ly = tipY + headSize * Math.sin(left).toFloat()
        val rx = tipX + headSize * Math.cos(right).toFloat()
        val ry = tipY + headSize * Math.sin(right).toFloat()
        val path = Path().apply { moveTo(tipX, tipY); lineTo(lx, ly); lineTo(rx, ry); close() }
        c.drawPath(path, paint)
    }
}
