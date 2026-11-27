package com.xywy.signalmap.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.TypedValue
import com.xywy.signalmap.domain.model.LayerPointIconCatalog
import com.xywy.signalmap.domain.model.LayerPointIconPreset
import kotlin.math.max

object SitePointBitmapFactory {
    /** The six reusable sprites live for the whole map style, including empty viewport updates. */
    fun createStyleImages(context: Context): Map<String, Bitmap> =
        LayerPointIconPreset.entries.associate { it.iconId to create(context, it) }

    fun create(
        context: Context,
        preset: LayerPointIconPreset,
    ): Bitmap {
        val metrics = context.resources.displayMetrics
        val size = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            SiteSymbolLayout.POINT_SIZE_DP,
            metrics,
        ).toInt().coerceAtLeast(24)
        return Bitmap.createBitmap(metrics, size, size, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            val style = LayerPointIconCatalog.style(preset)
            if (style.targetMode) {
                drawTarget(canvas, size, style.fillColor, style.strokeColor)
            } else {
                drawPin(canvas, size, style.fillColor, style.strokeColor)
            }
        }
    }

    private fun drawPin(
        canvas: Canvas,
        size: Int,
        fillColor: String,
        strokeColor: String,
    ) {
        val centerX = size / 2f
        val circleRadius = size * 0.24f
        val circleCenterY = size * 0.34f
        val strokeWidth = max(2f, size * 0.08f)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(fillColor)
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(fillPaint).apply {
            color = Color.parseColor(strokeColor)
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
        }
        val innerPaint = Paint(fillPaint).apply {
            color = Color.WHITE
        }
        val path = Path().apply {
            moveTo(centerX, size * 0.90f)
            lineTo(centerX - size * 0.18f, size * 0.50f)
            lineTo(centerX + size * 0.18f, size * 0.50f)
            close()
        }
        canvas.drawCircle(centerX, circleCenterY, circleRadius, fillPaint)
        canvas.drawPath(path, fillPaint)
        canvas.drawCircle(centerX, circleCenterY, circleRadius, strokePaint)
        canvas.drawPath(path, strokePaint)
        canvas.drawCircle(centerX, circleCenterY, circleRadius * 0.42f, innerPaint)
    }

    private fun drawTarget(
        canvas: Canvas,
        size: Int,
        fillColor: String,
        strokeColor: String,
    ) {
        val centerX = size / 2f
        val centerY = size / 2f
        val outerRadius = size * 0.28f
        val middleRadius = size * 0.18f
        val innerRadius = size * 0.08f
        val strokeWidth = max(2f, size * 0.08f)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(fillColor)
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(strokeColor)
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
        }
        canvas.drawCircle(centerX, centerY, outerRadius, strokePaint)
        canvas.drawCircle(centerX, centerY, middleRadius, strokePaint)
        canvas.drawCircle(centerX, centerY, innerRadius, fillPaint)
        canvas.drawLine(centerX, centerY - outerRadius - size * 0.12f, centerX, centerY - outerRadius + size * 0.02f, strokePaint)
        canvas.drawLine(centerX, centerY + outerRadius - size * 0.02f, centerX, centerY + outerRadius + size * 0.12f, strokePaint)
        canvas.drawLine(centerX - outerRadius - size * 0.12f, centerY, centerX - outerRadius + size * 0.02f, centerY, strokePaint)
        canvas.drawLine(centerX + outerRadius - size * 0.02f, centerY, centerX + outerRadius + size * 0.12f, centerY, strokePaint)
    }
}
