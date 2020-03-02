package com.abelhu.travel.ui.main

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
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

    private val tools = Tools(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)?.apply {
            // 旅行容器添加图片
            val near = CycleBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.bg_beijing_near), 0f, 64.dp)
            val far = CycleBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.bg_beijing_far), near.bitmap.height.toFloat(), 8.dp)
            val middle = CycleBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.bg_beijing_middle), near.bitmap.height.toFloat(), 8.dp)
            travelContainer.background = CycleDrawable(lifecycle).addImages(listOf(far, near, middle))
            travelContainer.post { (travelContainer.background as CycleDrawable).start() }
            // 当占位控件得到位置信息后再设置toolsContainer
            placeholder.post { initToolsContainer() }
            // 临时添加监听事件
            speedup.setOnClickListener { startActivity(Intent(context, EmptyActivity::class.java)) }
        }
    }

    private fun initToolsContainer() {
        // 拿到位置信息
        val holderLeftTop = intArrayOf(0, 0)
        placeholder.getLocationInWindow(holderLeftTop)
        val toolsLeftTop = intArrayOf(0, 0)
        toolsContainer.getLocationInWindow(toolsLeftTop)
        // 计算padding
        val right = toolsContainer.width - placeholder.width - holderLeftTop[0]
        val bottom = toolsContainer.height - placeholder.height - holderLeftTop[1] + toolsLeftTop[1]
        // 设置padding，adapter，layoutManager
        toolsContainer.setPadding(holderLeftTop[0] - toolsLeftTop[0], holderLeftTop[1] - toolsLeftTop[1], right, bottom)
        toolsContainer.adapter = ToolsAdapter(tools)
        toolsContainer.layoutManager = GridLayoutManager(3, 4) { position, itemWidth, itemHeight ->
            // 再layoutManager完成item的计算后，设置toolsContainer的背景
            toolsContainer.background = GridLayoutDrawable(position, itemWidth, itemHeight, 10.dp, Color.LTGRAY, 10.dp)
        }
        toolsContainer.setOnDragListener { _, event ->
            // 拖动完成时，判断拖动到了哪里，再进行下一步的操作
            if (event.action == DragEvent.ACTION_DROP) onToolsDrop(event)
            true
        }
    }

    private fun onToolsDrop(event: DragEvent) {
        Log.i(TAG(), "ACTION_DROP")
        // 根据保存的数据，获取原始的index
        val index = event.clipData.getItemAt(0).text.toString().toInt()
        // 根据GridLayoutManager里保存的位置信息，获取目标的row-col
        (toolsContainer.layoutManager as? GridLayoutManager)?.getRowCol(event.x, event.y) { i, j ->
            Log.i(this@MainFragment.TAG(), "item $index drop row-col is [$i, $j]")
            // 根据目标的row-col，再进行下一步操作
            tools.operateTool(index, i, j)
        }
    }

    override fun onToolsCancel(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsCancel")
        toolsContainer.adapter.notifyItemChanged(index)
    }

    override fun onToolsAdd(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsAdd")
        toolsContainer.adapter.notifyItemInserted(index)
    }

    override fun onToolsAddError(tool: ToolBean, cause: Exception) {
        Log.i(TAG(), "onToolsAddError: ${cause.message}")
        Toast.makeText(context, "位置占满，无法添加", Toast.LENGTH_SHORT).show()
    }

    override fun onToolsDelete(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsDelete")
        toolsContainer.adapter.notifyItemRemoved(index)
    }

    override fun onToolsApply(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsApply")
        toolsContainer.adapter.notifyItemChanged(index)
    }

    override fun onToolsMove(index: Int, tool: ToolBean) {
        Log.i(TAG(), "onToolsMove")
        // 为了避免动画，这里直接采用全局刷新的方式
        toolsContainer.adapter.notifyDataSetChanged()
    }

    override fun onToolsMerge(tool: ToolBean) {
        Log.i(TAG(), "onToolsMerge")
        // 为了避免动画，这里直接采用全局刷新的方式
        toolsContainer.adapter.notifyDataSetChanged()
    }

    override fun onToolsExchange(tools: List<ToolBean>) {
        Log.i(TAG(), "onToolsExchange")
        // 为了避免动画，这里直接采用全局刷新的方式
        toolsContainer.adapter.notifyDataSetChanged()
    }
}