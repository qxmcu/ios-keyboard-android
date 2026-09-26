package org.iosclone.keyboard.gesture

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

/**
 * Renders the iOS-style smooth fading glide typing trail.
 */
class GestureTrailRenderer {

    private val trailPath = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val visualPoints = mutableListOf<GesturePoint>()
    private val maxTrailDurationMs = 280L

    fun addPoint(x: Float, y: Float) {
        val now = System.currentTimeMillis()
        visualPoints.add(GesturePoint(x, y, now))
        cleanOldPoints(now)
    }

    fun clear() {
        visualPoints.clear()
        trailPath.reset()
    }

    fun hasPoints(): Boolean = visualPoints.isNotEmpty()

    private fun cleanOldPoints(now: Long) {
        visualPoints.removeAll { now - it.timestamp > maxTrailDurationMs }
    }

    fun draw(canvas: Canvas, strokeColor: Int, strokeWidthPx: Float) {
        val now = System.currentTimeMillis()
        cleanOldPoints(now)

        if (visualPoints.size < 2) return

        paint.color = strokeColor
        paint.strokeWidth = strokeWidthPx

        trailPath.reset()
        trailPath.moveTo(visualPoints[0].x, visualPoints[0].y)

        for (i in 1 until visualPoints.size) {
            val prev = visualPoints[i - 1]
            val curr = visualPoints[i]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            trailPath.quadTo(prev.x, prev.y, midX, midY)
        }
        trailPath.lineTo(visualPoints.last().x, visualPoints.last().y)

        canvas.drawPath(trailPath, paint)
    }
}
