package com.example.thesis_app.models

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.thesis_app.R
import kotlin.math.max

class BarGraphView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    var activeCount = 0f
    var inactiveCount = 0f
    var inLectureCount = 0f
    var totalStudents = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (totalStudents <= 0f) return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val barCount = 4 // ✅ Now we have 4 bars: Active, In Lecture, Inactive, Total
        val totalSpacing = viewWidth * 0.25f // 25% of width reserved for gaps
        val spacing = totalSpacing / (barCount + 1)
        val barWidth = (viewWidth - totalSpacing) / barCount
        val maxBarHeight = viewHeight * 0.75f
        val bottom = viewHeight * 0.85f

        // Determine max value for normalization
        val maxValue = max(
            totalStudents,
            max(activeCount, max(inactiveCount, inLectureCount))
        )

        // Compute X positions for bars (centered evenly)
        val barPositions = FloatArray(barCount) { i ->
            spacing * (i + 1) + barWidth * i
        }

        // --- Active Bar ---
        paint.color = ContextCompat.getColor(context, R.color.green)
        val activeHeight = (activeCount / maxValue) * maxBarHeight
        canvas.drawRoundRect(
            barPositions[0],
            bottom - activeHeight,
            barPositions[0] + barWidth,
            bottom,
            24f, 24f, paint
        )

        // --- In Lecture Bar ---
        paint.color = ContextCompat.getColor(context, R.color.blue)
        val inLectureHeight = (inLectureCount / maxValue) * maxBarHeight
        canvas.drawRoundRect(
            barPositions[1],
            bottom - inLectureHeight,
            barPositions[1] + barWidth,
            bottom,
            24f, 24f, paint
        )

        // --- Inactive Bar ---
        paint.color = ContextCompat.getColor(context, R.color.red)
        val inactiveHeight = (inactiveCount / maxValue) * maxBarHeight
        canvas.drawRoundRect(
            barPositions[2],
            bottom - inactiveHeight,
            barPositions[2] + barWidth,
            bottom,
            24f, 24f, paint
        )

        // --- Total Bar ---
        paint.color = ContextCompat.getColor(context, R.color.yellow)
        val totalHeight = (totalStudents / maxValue) * maxBarHeight
        canvas.drawRoundRect(
            barPositions[3],
            bottom - totalHeight,
            barPositions[3] + barWidth,
            bottom,
            24f, 24f, paint
        )

        // --- Labels below bars ---
        paint.color = ContextCompat.getColor(context, R.color.black)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 32f
    }
}
