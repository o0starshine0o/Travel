package com.qicode.grid

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.qicode.extension.TAG
import kotlin.math.min

class GridDragLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defAttr: Int = 0) : ViewGroup(context, attrs, defAttr),
    GridPosition {
    /**
     * 适配器
     */
    var adapter: GridAdapter<*>? = null

    /**
     * 行数
     */
    private var row = 3
    /**
     * 列数
     */
    private var col = 4
    /**
     * 使用二维数组保存绘制位置的左上角信息(top,left)，相对于本控件而言
     */
    private var position: Array<Array<FloatArray>>
    /**
     * item的宽度
     */
    private var itemWidth = 0f
    /**
     * item的高度
     */
    private var itemHeight = 0f

    /**
     * 缓存holder
     */
    private var pool = ArrayList<GridHolder?>(row * col)

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.GridDragLayout)
        val placeholderId = typedArray.getResourceId(R.styleable.GridDragLayout_gridView, View.NO_ID)
        row = typedArray.getInt(R.styleable.GridDragLayout_row, row)
        col = typedArray.getInt(R.styleable.GridDragLayout_col, col)
        typedArray.recycle()

        // 根据row和col初始化position
        position = Array(row) { Array(col) { floatArrayOf(0f, 0f) } }

        post {
            // 获取占位的view，需要再首次绘制完成之后才能计算得出
            val placeholder = rootView.findViewById<View>(placeholderId)
            if (placeholder != null) {
                // 计算需要绘制网格的区域
                val placeholderPosition = intArrayOf(0, 0)
                val thisPosition = intArrayOf(0, 0)
                placeholder.getLocationOnScreen(placeholderPosition)
                getLocationOnScreen(thisPosition)
                val left = placeholderPosition[0] - thisPosition[0]
                val top = placeholderPosition[1] - thisPosition[1]
                // 绘制网格背景，依赖反转，对item的一些处理交给GridDrawable完成
                background = GridDrawable(row, col, constraint = Rect(left, top, left + placeholder.width, top + placeholder.height), listener = this)
            }
        }
    }

    override fun generateDefaultLayoutParams() = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

    override fun generateLayoutParams(params: ViewGroup.LayoutParams) = LayoutParams(params)

    override fun checkLayoutParams(params: ViewGroup.LayoutParams?) = params != null && params is LayoutParams

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Log.i(TAG(), "onMeasure")
        // 先回收所有的view，之后再重新添加
        removeAllViews()
        val maxCount = min(row * col, adapter?.getItemCount() ?: 0)
        for (i in 0 until maxCount) {
            val holder = if (pool.size > 0) pool.removeAt(0) else adapter?.onCreateViewHolder(this)
            holder?.apply {
                // 添加view
                addView(view)
                // 保存holder，方便回收
                val params = view.layoutParams as LayoutParams
                params.holder = holder
                params.width = itemWidth.toInt()
                params.height = itemHeight.toInt()
            }
        }
        measureChildren(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        Log.i(TAG(), "onLayout($changed):[$l, $t, $r, $b]")
        // 回收所有的view，根据adapter的数量再进行展示
        removeAllViews()
        adapter?.apply {
            val maxCount = min(row * col, getItemCount())
            for (i in 0 until maxCount) {
                pool.removeAt(0)?.apply {
                    // 添加view
                    addView(view)
                    // 更新数据
                    adapter?.onBindView(this, i)
                    // 找到真正的位置
                    val leftTop = position[row][col]
                    val left = leftTop[0].toInt()
                    val top = leftTop[1].toInt()
                    val right = (left + itemWidth).toInt()
                    val bottom = (top + itemHeight).toInt()
                    // 布局
                    view.layout(left, top, right, bottom)
                }
            }
        }
    }

    override fun removeAllViews() {
        // 先回收holder，再移除所有的view
        for (i in 0 until childCount) {
            pool.add((getChildAt(i).layoutParams as LayoutParams).holder)
        }
        super.removeAllViews()
    }

    override fun updatePosition(row: Int, col: Int, left: Float, top: Float) {
        Log.i(TAG(), "updatePosition position for [$row, $col] with left-top:[$left, $top]")
        position[row][col] = floatArrayOf(left, top)
    }

    override fun updateItemSize(width: Float, height: Float) {
        Log.i(TAG(), "updateItemSize with size [$width, $height]")
        itemWidth = width
        itemHeight = height
    }

    class LayoutParams(width: Int, height: Int) : ViewGroup.LayoutParams(width, height) {
        var holder: GridHolder? = null

        constructor(params: ViewGroup.LayoutParams) : this(params.width, params.height)
    }
}