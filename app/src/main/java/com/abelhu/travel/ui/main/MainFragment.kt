package com.abelhu.travel.ui.main

import android.animation.Animator
import android.animation.AnimatorInflater
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.abelhu.travel.R
import com.abelhu.travel.data.ToolBean
import com.abelhu.travel.ui.empty.EmptyActivity
import com.qicode.cycle.CycleBitmap
import com.qicode.cycle.CycleDrawable
import com.qicode.extension.TAG
import com.qicode.extension.dp
import com.qicode.grid.GridLayoutDrawable
import com.qicode.grid.GridLayoutManager
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.fragment_main.view.*

class MainFragment : Fragment(), ToolsOperateListener {

    private val myTools = Tools(this)

    private lateinit var animator: Animator

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
            // 快速购买
            quick.setOnClickListener { myTools.addTool() }
            // 临时添加监听事件
            speedup.setOnClickListener { startActivity(Intent(context, EmptyActivity::class.java)) }
            // 设置拖拽监听
            setOnDragListener { _, event ->
                // 拖动完成时，判断拖动到了哪里，再进行下一步的操作
                if (event.action == DragEvent.ACTION_DROP) onToolsDrop(event)
                true
            }
            // 设置动画
            animator = AnimatorInflater.loadAnimator(context, R.animator.property_beat)
        }
    }

    private fun initToolsContainer(toolsContainer: RecyclerView) {
        toolsContainer.layoutManager = GridLayoutManager(3, 4) { position, itemWidth, itemHeight ->
            // 再layoutManager完成item的计算后，设置toolsContainer的背景
            toolsContainer.background = GridLayoutDrawable(position, itemWidth, itemHeight, 10.dp, Color.LTGRAY, 10.dp)
        }.apply {
            // 设置apply的区域，以toolsContainer的左上角为原点
            val toolsPosition = intArrayOf(0, 0)
            toolsContainer.getLocationInWindow(toolsPosition)
            travel.getGlobalVisibleRect(applyRect)
            applyRect.apply {
                left -= toolsPosition[0]
                right -= toolsPosition[0]
                top -= toolsPosition[1]
                bottom -= toolsPosition[1]
            }
        }
        toolsContainer.adapter = ToolsAdapter(myTools)
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
        onPropertyUpdate(myTools.property)
        speed.text = toolsContainer.context.resources.getString(R.string.per_second, myTools.showText(myTools.getSpeed()))
    }

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
            Log.i(this@MainFragment.TAG(), "item $index drop row-col is [$i, $j]")
            // 根据目标的row-col，再进行下一步操作
            myTools.operateTool(index, i, j)
        }
    }

    override fun onToolsCancel(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsCancel")
        toolsContainer.adapter.notifyItemChanged(index)
    }

    override fun onToolsAdd(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsAdd[$index]:(${tool.row}, ${tool.col})")
        // 更新产生速率
        speed.text = toolsContainer.context.resources.getString(R.string.per_second, myTools.showText(myTools.getSpeed()))
        toolsContainer.adapter.notifyItemInserted(index)
    }

    override fun onToolsAddError(tool: ToolBean, cause: Exception) {
        Log.i(TAG(), "onToolsAddError: ${cause.message}")
        Toast.makeText(context, R.string.recycler_tip, Toast.LENGTH_SHORT).show()
    }

    override fun onToolsDelete(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsDelete")
        // 更新产生速率
        speed.text = toolsContainer.context.resources.getString(R.string.per_second, myTools.showText(myTools.getSpeed()))
        toolsContainer.adapter.notifyItemRemoved(index)
    }

    override fun onToolsApply(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsApply")
        travel.imageAssetsFolder = "lottie/walk/level_${tool.level}/images"
        travel.setAnimation("lottie/walk/level_${tool.level}/data.json")
        travel.playAnimation()
        toolsContainer.adapter.notifyItemChanged(index)
    }

    override fun onToolsMove(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsMove")
        toolsContainer.adapter.notifyItemChanged(index)
    }

    override fun onToolsMerge(tools: List<Pair<Int, ToolBean>>) {
        Log.i(TAG(), "onToolsMerge")
        // 更新产生速率
        speed.text = toolsContainer.context.resources.getString(R.string.per_second, myTools.showText(myTools.getSpeed()))
        toolsContainer.adapter.notifyItemRemoved(tools[0].first)
        toolsContainer.adapter.notifyItemChanged(tools[1].first)
    }

    override fun onToolsExchange(tools: List<Pair<Int, ToolBean>>) {
        Log.i(TAG(), "onToolsExchange")
        toolsContainer.adapter.notifyItemChanged(tools[0].first)
        toolsContainer.adapter.notifyItemChanged(tools[1].first)
    }

    override fun onPropertyUpdate(now: Long) {
        // 如果展示的字符和当前的字符不一样，显示心跳动画
        myTools.showText(now).takeIf { it != property?.text }.apply {
            property.text = this
            animator.cancel()
            animator.setTarget(property)
            animator.start()
        }
    }
}