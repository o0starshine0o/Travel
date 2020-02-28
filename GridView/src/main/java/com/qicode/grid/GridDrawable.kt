package com.qicode.grid

import android.graphics.*
import android.graphics.drawable.Drawable
import com.qicode.extension.dp
import com.qicode.extension.sp

/**
 * @param row 有多少行
 * @param col 有多少列
 * @param dividerSize 每个item之前的间隔
 * @param radius 每个item的圆角半径
 * @param gridColor 每个item的颜色
 * @param constraint 约束在某一区域内
 * @param listener 需要把计算好的布局同步到其他地方
 */
class GridDrawable constructor(
    row: Int = 3,
    col: Int = 4,
    dividerSize: Int = 10,
    radius: Int = 5,
    gridColor: Int = Color.LTGRAY,
    private val constraint: Rect? = null,
    private val listener: GridPosition? = null
) : Drawable() {
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
    private var rectList = MutableList(0) { RectF() }
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
    /**
     * item的宽度
     */
    private var itemWidth = 0f
    /**
     * item的高度
     */
    private var itemHeight = 0f

    init {
        this.row = row
        this.col = col
        this.dividerSize = dividerSize.dp
        this.radius = radius.dp
        this.itemColor = gridColor

        initWithSize()
    }

    override fun draw(canvas: Canvas) {
        // 遍历所有的item，在对应的位置绘制圆角矩形
        for (rect in rectList) canvas.drawRoundRect(rect, radius, radius, paint)
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

    private fun initWithSize() {
        // 计算item的size
        itemWidth = ((constraint?.width() ?: bounds.width()) - (col + 1) * this.dividerSize) / col
        itemHeight = ((constraint?.height() ?: bounds.height()) - (row + 1) * this.dividerSize) / row
        listener?.updateItemSize(itemWidth, itemHeight)
        // 计算rectList
        rectList.clear()
        for (i in 0 until row) {
            for (j in 0 until col) {
                RectF().apply {
                    left = (j + 1) * dividerSize + j * itemWidth + (constraint?.left ?: 0)
                    top = (i + 1) * dividerSize + i * itemHeight + (constraint?.top ?: 0)
                    right = left + itemWidth
                    bottom = top + itemHeight
                    // 更新记录的位置信息
                    listener?.updatePosition(i, j, left, top)
                    rectList.add(this)
                }
            }
        }
    }
}