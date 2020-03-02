package com.qicode.grid

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.qicode.extension.TAG

typealias GridLayoutListener = (position: Array<Array<FloatArray>>, itemWidth: Float, itemHeight: Float) -> Unit

class GridLayoutManager(private val row: Int = 3, private val col: Int = 4, private val listener: GridLayoutListener? = null) : RecyclerView.LayoutManager() {

    companion object {
        // 约定（-1， -1）表示增加tool
        val ADD = intArrayOf(-1, -1)
        // 约定（-2， -2）表示删除tool
        val DELETE = intArrayOf(-2, -2)
        // 约定（-3， -3）表示取消tool的操作
        val CANCEL = intArrayOf(-3, -3)
        // 其他约定，比如更换工具
        val APPLY = intArrayOf(-100, -100)
    }

    var applyRect = Rect()
    /**
     * 使用二维数组保存绘制位置的左上角信息(top,left)，相对于本控件而言
     */
    private var position = Array(row) { Array(col) { floatArrayOf(0f, 0f) } }
    /**
     * item的宽度
     */
    private var itemWidth = 0f
    /**
     * item的高度
     */
    private var itemHeight = 0f

    fun getRowCol(x: Float, y: Float, result: (row: Int, col: Int) -> Unit) {
        applyRect.apply { if (contains(x.toInt(), y.toInt())) return result.invoke(APPLY[0], APPLY[1]) }
        for (i in position.indices) {
            for (j in position[i].indices) {
                if (RectF().apply {
                        left = position[i][j][0]
                        top = position[i][j][1]
                        right = left + itemWidth
                        bottom = top + itemHeight
                    }.contains(x, y)) {
                    result.invoke(i, j)
                    return
                }
            }
        }
        // TODO，添加删除的操作
        return result.invoke(APPLY[0], APPLY[1])
    }

    override fun generateDefaultLayoutParams() = LayoutParams(itemWidth.toInt(), itemHeight.toInt())
    override fun generateLayoutParams(lp: ViewGroup.LayoutParams?) = LayoutParams(itemWidth.toInt(), itemHeight.toInt())
    override fun generateLayoutParams(c: Context?, attrs: AttributeSet?) = LayoutParams(itemWidth.toInt(), itemHeight.toInt())
    override fun checkLayoutParams(lp: RecyclerView.LayoutParams?) = lp != null && lp is LayoutParams

    override fun onAttachedToWindow(view: RecyclerView?) {
        super.onAttachedToWindow(view)
        itemWidth = (width - paddingStart - paddingEnd).toFloat() / col
        itemHeight = (height - paddingTop - paddingBottom).toFloat() / row
        Log.i(TAG(), "get item width-height:[$itemWidth, $itemHeight]")
        // 计算position
        for (i in 0 until row) {
            for (j in 0 until col) {
                position[i][j] = floatArrayOf(j * itemWidth + paddingStart, i * itemHeight + paddingTop)
                Log.i(TAG(), "get position[$i][$j]:[${position[i][j][0]}, ${position[i][j][1]}]")
            }
        }
        listener?.invoke(position, itemWidth, itemHeight)
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        // 无数据时不计算
        if (recycler == null || itemCount < 0 || state?.isPreLayout == true) return
        Log.i(TAG(), "onLayoutChildren")
        // 缓存所有view
        detachAndScrapAttachedViews(recycler)
        // 遍历所有item
        for (i in 0 until itemCount) {
            // 获取本次的childView
            val childView = recycler.getViewForPosition(i)
            // 获取并设置childView放置区域
            getAvailableRect(childView)?.apply {
                // 添加childView
                addView(childView)
                // 完成对childView的测量
                measureChild(childView, 0, 0)
                // 放置childView
                layoutDecoratedWithMargins(childView, left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
            }
        }
    }

    private fun getAvailableRect(view: View): RectF? {
        val params = view.layoutParams as LayoutParams
        // 非法参数，直接返回
        if (params.row < 0 || params.col < 0 || params.row >= position.size || params.col >= position[params.row].size) return null
        // 获取行列对应的位置坐标
        val leftTop = position[params.row][params.col]
        return RectF(leftTop[0], leftTop[1], leftTop[0] + itemWidth, leftTop[1] + itemHeight).apply {
            Log.i(this@GridLayoutManager.TAG(), "available rect for view[${params.row}, ${params.col}]:[$left, $top, $right, $bottom]")
        }
    }

    class LayoutParams(width: Int, height: Int) : RecyclerView.LayoutParams(width, height) {
        var row = 0
        var col = 0
    }
}