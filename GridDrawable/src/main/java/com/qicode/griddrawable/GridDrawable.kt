package com.qicode.griddrawable

import android.graphics.*
import android.graphics.drawable.Drawable
import com.qicode.extension.sp

/**
 * @param row 有多少行
 * @param col 有多少列
 * @param dividerSize 每个item之前的间隔
 * @param radius 每个item的圆角半径
 * @param gridColor 每个item的颜色
 */
class GridDrawable constructor(row: Int = 3, col: Int = 4, dividerSize: Int? = 10, radius: Int? = 5, gridColor: Int? = Color.LTGRAY) : Drawable() {
    /**
     * 有多少行
     */
    var row = 3
    set(value) {
        field = value
        invalidateSelf()
    }
    /**
     * 有多少列
     */
    var col = 4
        set(value) {
            field = value
            invalidateSelf()
        }
    /**
     * 每个item之前的间隔
     */
    var dividerSize = 10.sp
        set(value) {
            field = value
            invalidateSelf()
        }
    /**
     * 每个item的圆角半径
     */
    var radius = 5.sp
        set(value) {
            field = value
            invalidateSelf()
        }
    /**
     * 每个item的颜色
     */
    var itemColor = Color.rgb(233, 233, 233)
        set(value) {
            field = value
            invalidateSelf()
        }
    /**
     * 用于确定每个item的位置
     */
    private val rect = RectF()
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
        color = itemColor
        textAlign = Paint.Align.CENTER
    }

    init {
        this.row = row
        this.col = col
        dividerSize?.sp?.also { this.dividerSize = it }
        radius?.sp?.also { this.radius = it }
        gridColor?.also { this.itemColor = it }
    }

    override fun draw(canvas: Canvas) {
        val itemWidth = (bounds.width() - (col + 1) * dividerSize) / col
        val itemHeight = (bounds.height() - (row + 1) * dividerSize) / row
        for (i in 0 until row) {
            for (j in 0 until col) {
                rect.apply {
                    left = (j + 1) * dividerSize + j * itemWidth
                    top = (i + 1) * dividerSize + i * itemHeight
                    right = left + itemWidth
                    bottom = top + itemHeight
                }
                canvas.drawRoundRect(rect, radius, radius, paint)
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun getOpacity() = paint.alpha

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }
}