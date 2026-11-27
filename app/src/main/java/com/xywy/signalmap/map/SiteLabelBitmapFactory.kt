package com.xywy.signalmap.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.TypedValue
import kotlin.math.ceil
import kotlin.math.max

object SiteLabelBitmapFactory {
    fun create(
        context: Context,
        title: String,
        labelColor: String,
        haloColor: String,
    ): Bitmap {
        val textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            11.5f,
            context.resources.displayMetrics,
        )
        val strokeWidth = max(2f, textSize * 0.18f)
        val horizontalPadding = ceil(textSize * 0.45f).toInt()
        val verticalPadding = ceil(textSize * 0.28f).toInt()

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(labelColor)
            style = Paint.Style.FILL
            this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val strokePaint = Paint(fillPaint).apply {
            color = Color.parseColor(haloColor)
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeJoin = Paint.Join.ROUND
            strokeMiter = 10f
        }

        val textBounds = Rect()
        fillPaint.getTextBounds(title, 0, title.length, textBounds)
        val fontMetrics = fillPaint.fontMetrics
        val textWidth = ceil(fillPaint.measureText(title).toDouble()).toInt()
        val textHeight = ceil((fontMetrics.descent - fontMetrics.ascent).toDouble()).toInt()
        val bitmapWidth = max(1, textWidth + horizontalPadding * 2 + ceil(strokeWidth.toDouble()).toInt() * 2)
        val bitmapHeight = max(1, textHeight + verticalPadding * 2 + ceil(strokeWidth.toDouble()).toInt() * 2)
        val baselineX = horizontalPadding + strokeWidth
        val baselineY = verticalPadding + strokeWidth - fontMetrics.ascent

        return Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            canvas.drawText(title, baselineX, baselineY, strokePaint)
            canvas.drawText(title, baselineX, baselineY, fillPaint)
        }
    }
}
