package com.abelhu.travel.ui.main

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.abelhu.travel.R
import com.abelhu.travel.utils.getJson
import com.google.gson.Gson
import com.qicode.cycle.CycleBitmap
import com.qicode.cycle.CycleDrawable
import com.qicode.extension.dp
import com.qicode.merge.data.ToolBean
import com.qicode.merge.ui.ToolsView
import com.qicode.merge.ui.ToolsViewHelp
import java.math.BigDecimal

class Merge2Fragment : Fragment(), ToolsViewHelp {
    private lateinit var toolsView: ToolsView

    override fun travelView(inflater: LayoutInflater, travelContainer: ConstraintLayout): View {
        return inflater.inflate(R.layout.view_travel, travelContainer, false)
    }

    override fun moreView(inflater: LayoutInflater, travelContainer: ConstraintLayout): View {
        return View(context).apply { background = ColorDrawable(Color.LTGRAY) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.tools_view, container, false).apply {
            // 工具
            toolsView = findViewById(R.id.toolsView)
            toolsView.helper = this@Merge2Fragment
            // 旅行容器添加图片
            val near = CycleBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.bg_beijing_near), 0.5f, 0f, 64.dp)
            val far = CycleBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.bg_beijing_far), 0.5f, near.bitmap.height.toFloat(), 8.dp)
            val middle = CycleBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.bg_beijing_middle), 0.5f, near.bitmap.height.toFloat(), 8.dp)
            findViewById<View>(R.id.travelContainer).apply {
                background = CycleDrawable(lifecycle).addImages(listOf(far, near, middle))
                post { (background as CycleDrawable).start() }
            }
            // 显示切换按钮
            findViewById<View>(R.id.towardsRight).visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        // 这里将来要从服务器获取
        context?.apply {
            toolsView.userTool = Gson().fromJson(getJson(this, "userTool.json"), UserTool::class.java).apply { initTool(toolsView) }
        }
    }

    override fun onToolAdd(tool: ToolBean?) {
        tool?.apply {
            propertyPer = BigDecimal(8)
            toolsView.onToolsAddSuccess(this)
        }
    }

    override fun onToolClick(index: Int, tool: ToolBean) {
    }

    override fun onShop() {
    }

    override fun onSpeedUp() {
        toolsView.onCoefficient(BigDecimal(2), 10)
    }

    override fun onToolRecycle(index: Int, tool: ToolBean) {
        toolsView.onToolsRecycleSuccess(index, tool)
    }

    override fun onToolApply(index: Int, tool: ToolBean) {
        // todo: 应用换装功能
        if (true) toolsView.onToolsApplySuccess(index, tool)
    }

    override fun onToolMove(index: Int, position: IntArray, tool: ToolBean) {
        toolsView.onToolsMoveSuccess(index, tool)
    }

    override fun onToolsMerge(tools: List<Pair<Int, ToolBean>>) {
        toolsView.onToolsMergeSuccess(tools)
    }

    override fun onToolsExchange(tools: List<Pair<Int, ToolBean>>) {
        toolsView.onToolsExchangeSuccess(tools)
    }
}