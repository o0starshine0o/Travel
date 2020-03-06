package com.abelhu.travel.ui.main

import android.animation.Animator
import android.animation.AnimatorInflater
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.abelhu.travel.R
import com.abelhu.travel.utils.getJson
import com.google.gson.Gson
import com.qicode.cycle.CycleBitmap
import com.qicode.cycle.CycleDrawable
import com.qicode.extension.TAG
import com.qicode.extension.dp
import com.qicode.grid.GridLayoutDrawable
import com.qicode.grid.GridLayoutManager
import com.qicode.merge.data.ToolBean
import com.qicode.merge.data.Tools
import com.qicode.merge.data.ToolsOperateListener
import com.qicode.merge.exception.NotEnoughPropertyError
import com.qicode.merge.exception.NotEnoughSpaceError
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.fragment_main.view.*
import java.math.BigDecimal


class MainFragment : Fragment(), ToolsOperateListener {

    //    private val myTools = Tools(this)
    private lateinit var userTool: UserTool

    private lateinit var beatAnimator: Animator
    private lateinit var shakeAnimator: Animator

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // 后期从服务器获取
        userTool = Gson().fromJson(getJson(context, "userTool.json"), UserTool::class.java).apply { initTool(this@MainFragment) }
        beatAnimator = AnimatorInflater.loadAnimator(context, R.animator.property_beat)
        shakeAnimator = AnimatorInflater.loadAnimator(context, R.animator.shake)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)?.apply {
            // 旅行容器添加图片
            val near = CycleBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.bg_beijing_near), 0f, 64.dp)
            val far = CycleBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.bg_beijing_far), near.bitmap.height.toFloat(), 8.dp)
            val middle = CycleBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.bg_beijing_middle), near.bitmap.height.toFloat(), 8.dp)
            travelContainer.background = CycleDrawable(lifecycle).addImages(listOf(far, near, middle))
            travelContainer.post { (travelContainer.background as CycleDrawable).start() }
            // 当占位控件得到位置信息后再设置toolsContainer
            toolsContainer.post { initToolsContainer(toolsContainer) }
            // 临时添加监听事件
            speedup.setOnClickListener { onCoefficient(if (userTool.coefficient > BigDecimal.ONE) BigDecimal.ONE else BigDecimal(2)) }
            // 设置拖拽监听
            setOnDragListener { _, event ->
                // 拖动完成时，判断拖动到了哪里，再进行下一步的操作
                if (event.action == DragEvent.ACTION_DROP) onToolsDrop(event)
                true
            }
        }
    }

    private fun initToolsContainer(toolsContainer: RecyclerView) {
        // 设置layout
        toolsContainer.layoutManager = GridLayoutManager(3, 4) { position, itemWidth, itemHeight ->
            // 再layoutManager完成item的计算后，设置toolsContainer的背景
            toolsContainer.background = GridLayoutDrawable(position, itemWidth, itemHeight, 10.dp, Color.LTGRAY, 10.dp)
        }.apply {
            // 获取toolsContainer的左上角为原点
            val toolsPosition = intArrayOf(0, 0)
            toolsContainer.getLocationInWindow(toolsPosition)
            // 设置apply的区域，以toolsContainer的左上角为原点
            travel.getGlobalVisibleRect(applyRect)
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
        toolsContainer.adapter = ToolsAdapter(userTool)
        // 去掉item的各种动画
        toolsContainer.itemAnimator.apply {
            addDuration = 0
            changeDuration = 0
            moveDuration = 0
            removeDuration = 0
        }
        // 去掉RecycleView的离屏缓存
        toolsContainer.setItemViewCacheSize(0)
        // 设置资产
        onPropertyUpdate(userTool.property)
        speed.text = toolsContainer.context.resources.getString(R.string.per_second, ToolBean.getText(userTool.getSpeed()))
        // 快速购买
        quick.setOnClickListener { userTool.addTool(userTool.getQuickTool()) }
        shakeAnimator.setTarget(quick)
        shakeQuickAdd()
        updateQuickAdd()
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
        view?.getLocationInWindow(dragPosition)
        val x = event.x + dragPosition[0] - toolsPosition[0]
        val y = event.y + dragPosition[1] - toolsPosition[1]
        // 根据保存的数据，获取原始的index
        val index = event.clipData.getItemAt(0).text.toString().toInt()
        // 根据GridLayoutManager里保存的位置信息，获取目标的row-col
        (toolsContainer.layoutManager as? GridLayoutManager)?.getRowCol(x, y) { i, j ->
            val tool = userTool.getList()[index]
            Log.i(this@MainFragment.TAG(), "item($index)[${tool.row}, ${tool.col}] drop row-col is [$i, $j]")
            // 根据目标的row-col，再进行下一步操作
            userTool.operateTool(index, i, j)
        }
        // 隐藏回收站
        recycleContainer.visibility = View.INVISIBLE
    }

    /**
     * 抖动快速购买按钮
     */
    private fun shakeQuickAdd() {
        // 每10秒抖动一次快速购买
        Handler().postDelayed(this::shakeQuickAdd, 10000)
//        if (myTools.property > myTools.getQuickTool().buyPrice && recycle.visibility == View.INVISIBLE)
        // 根据文档，按钮一直抖动
        shakeAnimator.start()
    }

    /**
     * 快速购买的图片和文字
     */
    private fun updateQuickAdd() {
        val tool = userTool.getQuickTool()
        val fileName = "lottie/dog/ic_dog_level${tool.level}.png"
        quick.levelImage.setImageDrawable(Drawable.createFromStream(context?.assets?.open(fileName), null))
        quick.levelText.text = tool.level.toString()
        Log.i(TAG(), "updateQuickAdd with level ${tool.level}")
    }

    override fun onToolsSelect(index: Int) {
        Log.i(TAG(), "onToolsSelect: $index")
        // 更新回收站文本
        recycleContainer.recycleText.text =
            recycleContainer.context.resources.getString(R.string.recycle_property, ToolBean.getText(userTool.getList()[index].recyclePrice))
        // 显示回收站
        recycleContainer.visibility = View.VISIBLE
    }

    override fun onToolsCancel(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsCancel")
        // 更新item
        toolsContainer.adapter.notifyItemChanged(index)
    }

    override fun onToolsAdd(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsAdd[$index]:(${tool.row}, ${tool.col})")
        // 更新产生速率
        speed.text = toolsContainer.context.resources.getString(R.string.per_second, ToolBean.getText(userTool.getSpeed()))
        toolsContainer.adapter.notifyItemInserted(index)
        // 更新快速购买
        updateQuickAdd()
        // TODO:通知服务器完成了工具的添加
    }

    override fun onToolsAddError(tool: ToolBean, cause: Exception) {
        Log.i(TAG(), "onToolsAddError: ${cause.message}")
        when (cause) {
            is NotEnoughSpaceError -> Toast.makeText(context, R.string.recycler_tip, Toast.LENGTH_SHORT).show()
            is NotEnoughPropertyError -> Toast.makeText(context, R.string.property_tip, Toast.LENGTH_SHORT).show()
        }

    }

    override fun onToolsRecycle(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsRecycle")
        // 更新产生速率
        speed.text = toolsContainer.context.resources.getString(R.string.per_second, ToolBean.getText(userTool.getSpeed()))
        toolsContainer.adapter.notifyItemRemoved(index)
        // 更新快速购买
        updateQuickAdd()
    }

    override fun onToolsApply(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsApply")
        // 应用新动画
        travel.imageAssetsFolder = "lottie/walk/level_${tool.level}/images"
        travel.setAnimation("lottie/walk/level_${tool.level}/data.json")
        travel.playAnimation()
        // 更新item
        toolsContainer.adapter.notifyItemChanged(index)
    }

    override fun onToolsMove(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsMove")
        // 更新item
        toolsContainer.adapter.notifyItemChanged(index)
    }

    override fun onToolsMerge(tools: List<Pair<Int, ToolBean>>) {
        Log.i(TAG(), "onToolsMerge")
        // 更新产生速率
        speed.text = toolsContainer.context.resources.getString(R.string.per_second, ToolBean.getText(userTool.getSpeed()))
        // 更新快速购买
        updateQuickAdd()
        toolsContainer.adapter.notifyItemRemoved(tools[0].first)
        toolsContainer.adapter.notifyItemChanged(tools[1].first)
    }

    override fun onToolsExchange(tools: List<Pair<Int, ToolBean>>) {
        Log.i(TAG(), "onToolsExchange")
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
        updateQuickAdd()
    }

    override fun onCoefficient(coefficient: BigDecimal) {
        // TODO：请求服务器，暂时默认成功
        userTool.coefficient = coefficient
        // 更新产生速率
        speed.text = toolsContainer.context.resources.getString(R.string.per_second, ToolBean.getText(userTool.getSpeed()))
        // 界面展示
        if (userTool.coefficient > BigDecimal.ONE) {
            // 加速
            speedup.text = speedup.context.resources.getString(R.string.speedup_with, coefficient)
            speed.setTextColor(Color.GREEN)
        } else {
            // 恢复到正常速度
            speedup.text = speedup.context.resources.getString(R.string.speedup)
            speed.setTextColor(Color.BLACK)
        }
    }
}