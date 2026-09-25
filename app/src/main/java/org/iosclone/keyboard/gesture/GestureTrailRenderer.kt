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

    private val points = mutableListOf<GesturePoint>()
    private val maxTrailDurationMs = 250L

    fun addPoint(x: Float, y: Float) {
        val now = System.currentTimeMillis()
        points.add(GesturePoint(x, y, now))
        cleanOldPoints(now)
    }

    fun clear() {
        points.clear()
        trailPath.reset()
    }

    fun hasPoints(): Boolean = points.isNotEmpty()

    private fun cleanOldPoints(now: Long) {
        points.removeAll { now - it.timestamp > maxTrailDurationMs }
    }

    fun draw(canvas: Canvas, strokeColor: Int, strokeWidthPx: Float) {
        val now = System.currentTimeMillis()
        cleanOldPoints(now)

        if (points.size < 2) return

        paint.color = strokeColor
        paint.strokeWidth = strokeWidthPx

        trailPath.reset()
        trailPath.moveTo(points[0].x, points[0].y)

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            // Quadratic Bezier curve between points for silky smooth curve
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            trailPath.quadTo(prev.x, prev.y, midX, midY)
        }
        trailPath.lineTo(points.last().x, points.last().y)

        canvas.drawPath(trailPath, paint)
    }

    fun getPoints(): List<GesturePoint> = points.toList()
}
