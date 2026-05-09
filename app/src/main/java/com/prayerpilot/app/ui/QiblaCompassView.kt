package com.prayerpilot.app.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class QiblaCompassView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {
    var qiblaBearing: Float = 0f
    var deviceAzimuth: Float = 0f

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 14f; color = Color.parseColor("#1A73E8")
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 28f; color = Color.parseColor("#3300E5FF")
    }
    private val qiblaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#34A853"); style = Paint.Style.FILL
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; textSize = 64f; isFakeBoldText = true
    }
    private val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#88FFFFFF"); strokeWidth = 4f
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w/2; val cy = h/2
        val r = min(w, h)/2 - 60f

        c.drawCircle(cx, cy, r, glowPaint)
        c.drawCircle(cx, cy, r, ringPaint)

        // tick marks
        for (i in 0 until 360 step 15) {
            val a = Math.toRadians(i.toDouble() - 90 - deviceAzimuth)
            val x1 = cx + (r-20) * Math.cos(a).toFloat()
            val y1 = cy + (r-20) * Math.sin(a).toFloat()
            val x2 = cx + r * Math.cos(a).toFloat()
            val y2 = cy + r * Math.sin(a).toFloat()
            c.drawLine(x1, y1, x2, y2, tick)
        }
        // direction labels (N, E, S, W)
        listOf("N" to 0, "E" to 90, "S" to 180, "W" to 270).forEach { (lbl, deg) ->
            val a = Math.toRadians(deg.toDouble() - 90 - deviceAzimuth)
            val x = cx + (r - 70) * Math.cos(a).toFloat()
            val y = cy + (r - 70) * Math.sin(a).toFloat() + 22f
            c.drawText(lbl, x, y, text)
        }
        // Qibla arrow
        val ang = Math.toRadians((qiblaBearing - deviceAzimuth).toDouble() - 90)
        val tipX = cx + (r-40) * Math.cos(ang).toFloat()
        val tipY = cy + (r-40) * Math.sin(ang).toFloat()
        val side = 30f
        val leftA = Math.toRadians((qiblaBearing - deviceAzimuth - 90).toDouble() - 90 + 160)
        val rightA = Math.toRadians((qiblaBearing - deviceAzimuth - 90).toDouble() - 90 - 160)
        val lx = tipX + side * Math.cos(leftA).toFloat()
        val ly = tipY + side * Math.sin(leftA).toFloat()
        val rx = tipX + side * Math.cos(rightA).toFloat()
        val ry = tipY + side * Math.sin(rightA).toFloat()
        val path = Path().apply { moveTo(tipX, tipY); lineTo(lx, ly); lineTo(cx, cy); lineTo(rx, ry); close() }
        c.drawPath(path, qiblaPaint)

        // center dot
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#39FF14") }
        c.drawCircle(cx, cy, 18f, centerPaint)
    }
}
