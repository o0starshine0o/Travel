package com.abelhu.travel.ui.main

import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.abelhu.travel.R
import com.abelhu.travel.utils.getJson
import com.google.gson.Gson
import com.qicode.cycle.CycleBitmap
import com.qicode.cycle.CycleDrawable
import com.qicode.extension.TAG
import com.qicode.extension.dp
import com.qicode.merge.data.ToolBean
import com.qicode.merge.ui.ToolsFragment
import kotlinx.android.synthetic.main.view_travel.*
import java.math.BigDecimal

class MergeFragment : ToolsFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.apply {
            // 旅行容器添加图片
            val near = CycleBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.bg_beijing_near), 0f, 64.dp)
            val far = CycleBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.bg_beijing_far), near.bitmap.height.toFloat(), 8.dp)
            val middle = CycleBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.bg_beijing_middle), near.bitmap.height.toFloat(), 8.dp)
            findViewById<View>(R.id.travelContainer).apply {
                background = CycleDrawable(lifecycle).addImages(listOf(far, near, middle))
                post { (background as CycleDrawable).start() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG(), "onResume")
        // 这里将来要从服务器获取
        context?.apply {
            userTool = Gson().fromJson(getJson(this, "userTool.json"), UserTool::class.java).apply { initTool(this@MergeFragment) }
        }
    }

    override fun travelView(inflater: LayoutInflater, travelContainer: ConstraintLayout): View {
        return inflater.inflate(R.layout.view_travel, travelContainer, false)
    }

    override fun onToolsRecycle(index: Int, tool: ToolBean) {
        // todo: 进行网络请求，如果返回成功过执行添加操作
        onToolsRecycleSuccess(index, tool)
    }

    override fun onToolsApply(index: Int, tool: ToolBean) {
        // 应用新动画
        travel.imageAssetsFolder = "lottie/walk/level_${tool.level}/images"
        travel.setAnimation("lottie/walk/level_${tool.level}/data.json")
        travel.playAnimation()
        // todo: 应用换装功能
        if (true) onToolsApplySuccess(index, tool)
    }

    override fun onToolsMove(index: Int, tool: ToolBean) {
        // todo: 进行网络请求，如果返回成功过执行添加操作
        if (true) onToolsMoveSuccess(index, tool)
    }

    override fun onToolsMerge(tools: List<Pair<Int, ToolBean>>) {
        // todo: 进行网络请求，如果返回成功过执行添加操作
        if (true) onToolsMergeSuccess(tools)
    }

    override fun onToolsExchange(tools: List<Pair<Int, ToolBean>>) {
        super.onToolsExchange(tools)
        // todo: 进行网络请求，如果返回成功过执行添加操作
        if (true) onToolsExchangeSuccess(tools)
    }

    override fun onPropertyUpdate(now: BigDecimal) {
        super.onPropertyUpdate(now)
    }

    override fun onCoefficient(coefficient: BigDecimal) {
        super.onCoefficient(coefficient)
    }
}