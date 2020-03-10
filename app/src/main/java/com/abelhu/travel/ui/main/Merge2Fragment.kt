package com.abelhu.travel.ui.main

import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.abelhu.travel.R
import com.abelhu.travel.utils.getJson
import com.google.gson.Gson
import com.qicode.merge.data.ToolBean
import com.qicode.merge.ui.ToolsView
import com.qicode.merge.ui.ToolsViewHelp
import kotlinx.android.synthetic.main.view_travel.*
import java.math.BigDecimal

class Merge2Fragment : Fragment(), ToolsViewHelp {
    private lateinit var toolsView: ToolsView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.tools_view, container, false).apply {
            toolsView = findViewById(R.id.toolsView)
            toolsView.helper = this@Merge2Fragment
        }
    }

    override fun travelView(inflater: LayoutInflater, travelContainer: ConstraintLayout): View {
        return inflater.inflate(R.layout.view_travel, travelContainer, false)
    }

    override fun onResume() {
        super.onResume()
        // 这里将来要从服务器获取
        context?.apply {
            toolsView.userTool = Gson().fromJson(getJson(this, "userTool.json"), UserTool::class.java).apply { initTool(toolsView) }
        }
    }

    override fun onToolAdd(tool: ToolBean?) {
        tool?.apply { toolsView.onToolsAddSuccess(this) }
    }

    override fun onSpeedUp() {
        toolsView.onCoefficient(BigDecimal(2))
    }

    override fun onToolRecycle(index: Int, tool: ToolBean) {
        toolsView.onToolsRecycleSuccess(index, tool)
    }

    override fun onToolApply(index: Int, tool: ToolBean) {
        // 应用新动画
        travel.imageAssetsFolder = "lottie/walk/level_${tool.level}/images"
        travel.setAnimation("lottie/walk/level_${tool.level}/data.json")
        travel.playAnimation()
        // todo: 应用换装功能
        if (true) toolsView.onToolsApplySuccess(index, tool)
    }

    override fun onToolMove(index: Int, tool: ToolBean) {
        toolsView.onToolsMoveSuccess(index, tool)
    }

    override fun onToolsMerge(tools: List<Pair<Int, ToolBean>>) {
        toolsView.onToolsMergeSuccess(tools)
    }

    override fun onToolsExchange(tools: List<Pair<Int, ToolBean>>) {
        toolsView.onToolsExchangeSuccess(tools)
    }
}