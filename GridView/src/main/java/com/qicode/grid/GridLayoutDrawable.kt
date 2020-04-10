package com.qicode.grid

import android.graphics.*
import android.graphics.drawable.Drawable

class GridLayoutDrawable(
    private val position: Array<Array<FloatArray>>,
    private val itemWidth: Float,
    private val itemHeight: Float,
    private val padding: Float = 5f,
    private val gridColor: Int = Color.LTGRAY,
    private val radius: Float = 5f
) : Drawable() {

    /**
     * 绘制每个item
     */
    private val paint = Paint().apply {
        // 设置画笔
        isAntiAlias = true
        isDither = true
        isFilterBitmap = true
        strokeWidth = 0f
        strokeCap = Paint.Cap.ROUND
        color = gridColor
        textAlign = Paint.Align.CENTER
    }

    fun getItem(source: Bitmap): Bitmap {
        // 容器背景
        val bitmap = Bitmap.createBitmap(source)
        val canvas = Canvas(bitmap)
        // 绘制边框
        RectF().apply {
            left = padding
            top = padding
            right = left + itemWidth - padding * 2
            bottom = top + itemHeight - padding * 2
            canvas.drawRoundRect(this, radius, radius, paint)
        }
        // 绘制原图
        canvas.drawBitmap(source, 0f, 0f, paint)
        canvas.save()
        return bitmap
    }

    override fun draw(canvas: Canvas) {
        for (line in position) {
            for (topLeft in line) {
                RectF().apply {
                    left = topLeft[0] + padding
                    top = topLeft[1] + padding
                    right = left + itemWidth - padding * 2
                    bottom = top + itemHeight - padding * 2
                    canvas.drawRoundRect(this, radius, radius, paint)
                }
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun getOpacity() = paint.alpha

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }
}