package com.example.thesis_app.models

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.thesis_app.R

class MultiSegmentProgressView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    var correctPercent = 0f
    var wrongPercent = 0f
    var retryPercent = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = width.coerceAtMost(height) / 2f
        val centerX = width / 2f
        val centerY = height / 2f
        val rect = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

        var startAngle = -90f

        // Correct
        paint.color = ContextCompat.getColor(context, R.color.green)
        canvas.drawArc(rect, startAngle, correctPercent * 360f / 100f, true, paint)
        startAngle += correctPercent * 360f / 100f

        // Wrong
        paint.color = ContextCompat.getColor(context, R.color.red)
        canvas.drawArc(rect, startAngle, wrongPercent * 360f / 100f, true, paint)
        startAngle += wrongPercent * 360f / 100f

        // Retry
        paint.color = ContextCompat.getColor(context, R.color.yellow)
        canvas.drawArc(rect, startAngle, retryPercent * 360f / 100f, true, paint)
    }
}
