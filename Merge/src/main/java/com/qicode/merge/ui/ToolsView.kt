package com.qicode.merge.ui

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.support.annotation.IntDef
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.qicode.extension.TAG
import com.qicode.extension.dp
import com.qicode.grid.GridLayoutDrawable
import com.qicode.grid.GridManager
import com.qicode.merge.R
import com.qicode.merge.data.ToolBean
import com.qicode.merge.data.Tools
import com.qicode.merge.data.ToolsOperateListener
import com.qicode.merge.exception.NotEnoughPropertyError
import com.qicode.merge.exception.NotEnoughSpaceError
import kotlinx.android.synthetic.main.tools_view_detail.view.*
import java.math.BigDecimal
import java.text.DecimalFormat

interface ToolsViewHelp {
    fun travelView(inflater: LayoutInflater, travelContainer: ConstraintLayout): View
    fun moreView(inflater: LayoutInflater, moreContainer: ConstraintLayout): View
    fun onSpeedUp()
    fun onShop()
    fun onToolAdd(tool: ToolBean?)
    fun onToolClick(index: Int, tool: ToolBean)
    fun onToolRecycle(index: Int, tool: ToolBean)
    fun onToolApply(index: Int, tool: ToolBean)
    fun onToolMove(index: Int, position: IntArray, tool: ToolBean)
    fun onToolsMerge(tools: List<Pair<Int, ToolBean>>)
    fun onToolsExchange(tools: List<Pair<Int, ToolBean>>)
}

class ToolsView(context: Context, set: AttributeSet) : ConstraintLayout(context, set), ToolsOperateListener {
    companion object {
        private const val MERGE = 0
        private const val MORE = 1
    }

    @IntDef(MERGE, MORE)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Show

    var userTool: Tools? = null
        set(value) {
            if (value != null) {
                Log.i(TAG(), "set user tools Size[${value.getList().size}]")
                field = value
                if (toolsContainer.adapter == null) toolsContainer.adapter = ToolsAdapter(value)
                else {
                    (toolsContainer.adapter as? ToolsAdapter)?.tools = value
                    toolsContainer.adapter.notifyDataSetChanged()
                }
                // 设置资产
                onPropertyUpdate(value.property)
                // 更新资产生产速度
                onPropertySpeed(value.getSpeed())
                // 启动快速购买动画
                shakeQuickAdd()
            }
        }
    /**
     * 对应内容的View，可用于动态增加减少view
     */
    lateinit var contentView: ConstraintLayout

    private val beatAnimator = AnimatorInflater.loadAnimator(context, R.animator.property_beat)
    private val shakeAnimator = AnimatorInflater.loadAnimator(context, R.animator.shake)
    private var travelView: View? = null
    private var moreView: View? = null
    /**
     * 使用viewCode表示哪个view正在显示
     */
    @Show
    private var viewCode = 0

    var helper: ToolsViewHelp? = null
        set(value) {
            field = value
            value?.apply {
                // travelContainer添加一个的view
                val travelParams = LayoutParams(LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)).apply {
                    topToTop = travelContainer.id
                    bottomToBottom = travelContainer.id
                    startToStart = travelContainer.id
                    endToEnd = travelContainer.id
                }
                travelContainer.addView(travelView(LayoutInflater.from(context), travelContainer).apply { travelView = this }, travelParams)
                // moreContainer添加一个的view
                val moreParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                moreContainer.addView(moreView(LayoutInflater.from(context), moreContainer).apply { moreView = this }, moreParams)
            }
        }

    fun showChange() {
        when (viewCode) {
            MERGE -> {
                towardsRight.visibility = View.VISIBLE
                towardsLeft.visibility = View.GONE
            }
            MORE -> {
                towardsRight.visibility = View.GONE
                towardsLeft.visibility = View.VISIBLE
            }
        }
    }

    fun hideChange() {
        towardsRight.visibility = View.GONE
        towardsLeft.visibility = View.GONE
    }

    init {
        addView(LayoutInflater.from(context).inflate(R.layout.tools_view_detail, this, false).apply {
            contentView = this as ConstraintLayout
            toolsContainer.layoutManager = GridManager(3, 4) { position, itemWidth, itemHeight ->
                // 再layoutManager完成item的计算后，设置toolsContainer的背景
                val gray = Color.rgb(241, 239, 242)
                toolsContainer.background = GridLayoutDrawable(position, itemWidth, itemHeight, 6.dp, gray, 10.dp)
            }
            // 去掉item的各种动画
            toolsContainer.itemAnimator.apply {
                addDuration = 0
                changeDuration = 0
                moveDuration = 0
                removeDuration = 0
            }
            // 去掉RecycleView的离屏缓存
            toolsContainer.setItemViewCacheSize(0)
            // 设置快速购买的动画
            shakeAnimator.setTarget(quick)
            // 快速购买的点击事件
            quick.setOnClickListener { helper?.onToolAdd(userTool?.getQuickTool()) }
            // 加速的点击事件
            speedupContainer.setOnClickListener { helper?.onSpeedUp() }
            // 商城的点击事件
            shopContainer.setOnClickListener { helper?.onShop() }
            // 设置拖拽释放监听
            setOnDragListener { _, event ->
                // 拖动完成时，判断拖动到了哪里，再进行下一步的操作
                if (event.action == DragEvent.ACTION_DROP) onToolsDrop(event)
                true
            }
            // 当加载完成时进一步初始化
            viewTreeObserver.addOnPreDrawListener {
                initGridManager()
                true
            }
            // 设置更多精彩切换监听
            towardsRight.setOnClickListener { showMore() }
            towardsLeft.setOnClickListener { showMerge() }
        })
    }

    override fun onToolsSelect(index: Int) {
        Log.i(TAG(), "onToolsSelect: $index")
        userTool?.also { tools ->
            // 更新回收站文本
            val price = ToolBean.getText(tools.getList()[index].recyclePrice)
            recycleContainer.recycleText.text = context?.resources?.getString(R.string.recycle_property, price)
            // 显示回收站
            recycleContainer.visibility = View.VISIBLE
        }
    }

    override fun onToolsStopDrag(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsStopDrag: Index[$index] Level[${tool.level}]")
        // 由于意外导致拖拽停止默认为是点击
        onToolsClick(index, tool)
    }

    override fun onToolsClick(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsClick: Index[$index] Level[${tool.level}]")
        // 更新item
        toolsContainer.adapter.notifyItemChanged(index)
        // 隐藏回收站
        recycleContainer.visibility = View.INVISIBLE
        // 外部处理
        helper?.onToolClick(index, tool)
    }

    override fun onToolsCancel(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsCancel")
        // 更新item
        toolsContainer.adapter.notifyItemChanged(index)
    }

    override fun onToolsAddSuccess(tool: ToolBean) {
        Log.i(TAG(), "onToolsAddSuccess Tool[${tool.level}]")
        userTool?.apply {
            // 更新工具
            val index = addTool(tool)
            toolsContainer.adapter.notifyItemInserted(index)
            // 更新产生速率
            onPropertySpeed(getSpeed())
            // 更新总资产
            onPropertyUpdate(property)
        }
    }

    override fun onToolsAddError(tool: ToolBean, cause: Exception) {
        Log.i(TAG(), "onToolsAddError: ${cause.message}")
        when (cause) {
            is NotEnoughSpaceError -> Toast.makeText(context, R.string.recycler_tip, Toast.LENGTH_SHORT).show()
            is NotEnoughPropertyError -> Toast.makeText(context, R.string.property_tip, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onToolsRecycle(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsRecycle: $index")
        helper?.onToolRecycle(index, tool)
    }

    override fun onToolsRecycleSuccess(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsRecycleSuccess: $index")
        userTool?.also { tools ->
            // 更新工具
            tools.recycleTool(tool)
            toolsContainer.adapter.notifyItemRemoved(index)
            // 更新产生速率
            onPropertySpeed(tools.getSpeed())
            // 更新快速购买
            updateQuickAdd(tools.getQuickTool())
            // 查看缓存队列
            tools.showCache()?.also { toolsContainer.adapter.notifyItemInserted(it) }
        }
    }

    override fun onToolsApply(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsApply index[$index] level[${tool.level}]")
        helper?.onToolApply(index, tool)
    }

    override fun onToolsApplySuccess(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsApplySuccess: $index")
        // 更新item
        toolsContainer.adapter.notifyItemChanged(index)
    }

    override fun onToolsMove(index: Int, position: IntArray, tool: ToolBean) {
        Log.i(TAG(), "onToolsMove: $index, from[${position[0]}, ${position[1]}] to [${tool.row}, ${tool.col}]")
        helper?.onToolMove(index, position, tool)
    }

    override fun onToolsMoveSuccess(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsMoveSuccess: $index")
        // 更新item
        toolsContainer.adapter.notifyItemChanged(index)
    }

    override fun onToolsMerge(tools: List<Pair<Int, ToolBean>>) {
        Log.i(TAG(), "onToolsMerge")
        helper?.onToolsMerge(tools)
    }

    override fun onToolsMergeSuccess(tools: List<Pair<Int, ToolBean>>) {
        val remove = "Remove Index(${tools[0].first}) [${tools[0].second.row}, ${tools[0].second.col}]"
        val change = "Change Index(${tools[1].first}) [${tools[1].second.row}, ${tools[1].second.col}]"
        Log.i(TAG(), "onToolsMergeSuccess $remove $change")
        userTool?.also { userTools ->
            // 更新产生速率
            onPropertySpeed(userTools.getSpeed())
            // 更新快速购买
            updateQuickAdd(userTools.getQuickTool())
            //更新工具
            toolsContainer.adapter.notifyItemRemoved(tools[0].first)
            toolsContainer.adapter.notifyItemChanged(tools[1].first, true)
            // 查看缓存队列
            userTools.showCache()?.also { toolsContainer.adapter.notifyItemInserted(it) }
        }
    }

    override fun onToolsExchange(tools: List<Pair<Int, ToolBean>>) {
        Log.i(TAG(), "onToolsExchange")
        helper?.onToolsExchange(tools)
    }

    override fun onToolsExchangeSuccess(tools: List<Pair<Int, ToolBean>>) {
        Log.i(TAG(), "onToolsExchangeSuccess")
        toolsContainer.adapter.notifyItemChanged(tools[0].first)
        toolsContainer.adapter.notifyItemChanged(tools[1].first)
    }

    override fun onPropertyUpdate(now: BigDecimal) {
        // 如果展示的字符和当前的字符不一样，显示心跳动画
        ToolBean.getText(now).takeIf { it != property?.text }?.apply {
            property.text = this
            beatAnimator.cancel()
            beatAnimator.setTarget(property)
            beatAnimator.start()
        }
        // 更新快速购买
        updateQuickAdd(userTool?.getQuickTool())
    }

    override fun onPropertySpeed(value: BigDecimal) {
        speed.text = toolsContainer.context.resources.getString(R.string.per_second, ToolBean.getText(value))
    }

    /**
     * 更新加速系数
     */
    @Suppress("DEPRECATION")
    override fun onCoefficient(coefficient: BigDecimal, second: Int) {
        userTool?.also { tools ->
            tools.coefficient = if (second > 0) coefficient else BigDecimal.ONE
            // 更新产生速率
            onPropertySpeed(tools.getSpeed())
            // 界面展示
            if (tools.coefficient > BigDecimal.ONE) {
                // 加速
                val format = DecimalFormat("00")
                speedup.text = speedup.context.resources.getString(R.string.speedup_with, format.format(second / 60), format.format(second % 60))
                val rocketDrawable = resources.getDrawable(R.mipmap.icon_rocket).apply { setBounds(0, 0, 20.dp.toInt(), 20.dp.toInt()) }
                speedup.setCompoundDrawables(rocketDrawable, null, null, null)
                speedup.setTextColor(Color.parseColor("#FE6C48"))
                // 资产界面
                speed.setTextColor(Color.parseColor("#1BB52A"))
                iconSub.visibility = View.VISIBLE
            } else {
                // 恢复到正常速度
                speedup.text = speedup.context.resources.getString(R.string.speedup)
                val speedDrawable = resources.getDrawable(R.mipmap.icon_speed_up).apply { setBounds(0, 0, 20.dp.toInt(), 20.dp.toInt()) }
                speedup.setCompoundDrawables(speedDrawable, null, null, null)
                speedup.setTextColor(Color.parseColor("#303030"))
                // 资产界面
                speed.setTextColor(Color.parseColor("#3b3b3b"))
                iconSub.visibility = View.GONE
            }
            // 刷新界面
            if (second > 0) handler.postDelayed({ onCoefficient(coefficient, second - 1) }, 1000)
        }
    }

    /**
     * 设置GridManager的不同响应区域
     */
    private fun initGridManager() {
        // 设置layout
        (toolsContainer.layoutManager as GridManager).apply {
            // 获取toolsContainer的左上角为原点
            val toolsPosition = intArrayOf(0, 0)
            toolsContainer.getLocationInWindow(toolsPosition)
            // 设置apply的区域，以toolsContainer的左上角为原点
            travelView?.getGlobalVisibleRect(applyRect)
            applyRect.apply {
                left -= toolsPosition[0]
                right -= toolsPosition[0]
                top -= toolsPosition[1]
                bottom -= toolsPosition[1]
            }
            apply = Tools.APPLY
            // 设置recycle的区域，以toolsContainer的左上角为原点
            recycleContainer.getGlobalVisibleRect(recycleRect)
            recycleRect.apply {
                left -= toolsPosition[0]
                right -= toolsPosition[0]
                top -= toolsPosition[1]
                bottom -= toolsPosition[1]
            }
            recycle = Tools.RECYCLE
            // 设置取消区域
            cancel = Tools.CANCEL
        }
    }

    /**
     * 快速购买的图片和文字
     */
    private fun updateQuickAdd(tool: ToolBean?) {
        if (context == null || tool == null) return
        userTool?.apply {
            quick.levelImage.setImageDrawable(toolDrawable(context, tool.level))
            quick.levelText.text = tool.level.toString()
            quick.tag = tool.level
            quick.quickBuy.text = ToolBean.getText(tool.basePrice)
            Log.i(TAG(), "updateQuickAdd with level ${tool.level}")
        }
    }

    /**
     * 抖动快速购买按钮
     */
    private fun shakeQuickAdd() {
        // 每10秒抖动一次快速购买
        Handler().postDelayed(this::shakeQuickAdd, 10000)
        // 根据文档，按钮一直抖动
        if (recycleContainer.visibility == View.INVISIBLE) shakeAnimator.start()
    }

    /**
     * 当tool的拖拽被释放时，相应的操作
     * 主要是计算出拖拽到的row和col
     */
    private fun onToolsDrop(event: DragEvent) {
        Log.i(TAG(), "ACTION_DROP")
        // 获取在控件内的坐标信息
        val toolsPosition = intArrayOf(0, 0)
        toolsContainer.getLocationInWindow(toolsPosition)
        val dragPosition = intArrayOf(0, 0)
        getLocationInWindow(dragPosition)
        val x = event.x + dragPosition[0] - toolsPosition[0]
        val y = event.y + dragPosition[1] - toolsPosition[1]
        // 根据保存的数据，获取原始的index
        val index = event.clipData.getItemAt(0).text.toString().toInt()
        // 根据GridLayoutManager里保存的位置信息，获取目标的row-col
        (toolsContainer.layoutManager as? GridManager)?.getRowCol(x, y) { i, j ->
            val tool = userTool?.getList()?.getOrNull(index)
            Log.i(TAG(), "item($index)[${tool?.row}, ${tool?.col}] drop row-col is [$i, $j]")
            // 根据目标的row-col，再进行下一步操作
            userTool?.operateTool(index, i, j)
        }
        // 隐藏回收站
        recycleContainer.visibility = View.INVISIBLE
    }

    /**
     * 显示更多精彩
     */
    private fun showMore(during: Long = 300) {
        showChange(MORE)
        moreContainer.visibility = View.VISIBLE
        val set = AnimatorSet()
        val toolsOut = ObjectAnimator.ofFloat(toolsContainer, "translationX", 0f, (0 - width).toFloat()).apply { duration = during }
        val toolsAlpha = ObjectAnimator.ofFloat(toolsContainer, "alpha", 1f, 0.8f).apply { duration = during }
        val functionOut = ObjectAnimator.ofFloat(functionContainer, "translationX", 0f, (0 - width).toFloat()).apply { duration = during }
        val functionAlpha = ObjectAnimator.ofFloat(functionContainer, "alpha", 1f, 0.8f).apply { duration = during }
        val moreIn = ObjectAnimator.ofFloat(moreContainer, "translationX", width.toFloat(), 0f).apply { duration = during }
        val moreAlpha = ObjectAnimator.ofFloat(moreContainer, "alpha", 0.8f, 1f).apply { duration = during }
        set.playTogether(toolsOut, toolsAlpha, functionOut, functionAlpha, moreIn, moreAlpha)
        set.start()
    }

    /**
     * 显示合并区域
     */
    private fun showMerge(during: Long = 300) {
        showChange(MERGE)
        val set = AnimatorSet()
        val moreOut = ObjectAnimator.ofFloat(moreContainer, "translationX", 0f, width.toFloat()).apply { duration = during }
        val moreAlpha = ObjectAnimator.ofFloat(moreContainer, "alpha", 1f, 0.8f).apply { duration = during }
        val toolsIn = ObjectAnimator.ofFloat(toolsContainer, "translationX", 0 - width.toFloat(), 0f).apply { duration = during }
        val toolsAlpha = ObjectAnimator.ofFloat(toolsContainer, "alpha", 0.8f, 1f).apply { duration = during }
        val functionIn = ObjectAnimator.ofFloat(functionContainer, "translationX", 0 - width.toFloat(), 0f).apply { duration = during }
        val functionAlpha = ObjectAnimator.ofFloat(functionContainer, "alpha", 0.8f, 1f).apply { duration = during }
        set.playTogether(moreOut, moreAlpha, toolsIn, toolsAlpha, functionIn, functionAlpha)
        set.start()
    }

    private fun showChange(@Show code: Int) {
        viewCode = code
        showChange()
    }
}