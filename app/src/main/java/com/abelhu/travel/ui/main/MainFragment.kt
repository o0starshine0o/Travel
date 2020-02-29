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
import com.abelhu.travel.R
import com.abelhu.travel.ui.empty.EmptyActivity
import com.qicode.cycle.CycleBitmap
import com.qicode.cycle.CycleDrawable
import com.qicode.extension.TAG
import com.qicode.extension.dp
import com.qicode.grid.GridLayoutDrawable
import com.qicode.grid.GridLayoutManager
import kotlinx.android.synthetic.main.fragment_main.view.*


class MainFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)?.apply {
            // 旅行容器添加图片
            val near = CycleBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.bg_beijing_near), 0f, 64.dp)
            val far = CycleBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.bg_beijing_far), near.bitmap.height.toFloat(), 8.dp)
            val middle = CycleBitmap(BitmapFactory.decodeResource(context.resources, R.mipmap.bg_beijing_middle), near.bitmap.height.toFloat(), 8.dp)
            travelContainer.background = CycleDrawable(lifecycle).addImages(listOf(far, near, middle))
            travelContainer.post { (travelContainer.background as CycleDrawable).start() }
            // 工具容器添加adapter
//            toolsContainer.adapter = MainAdapter()
            // 当占位控件得到位置信息后再设置toolsContainer
            placeholder.post {
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
                toolsContainer.adapter = ToolsAdapter()
                toolsContainer.layoutManager = GridLayoutManager(3, 4) { position, itemWidth, itemHeight ->
                    // 再layoutManager完成item的计算后，设置toolsContainer的背景
                    toolsContainer.background = GridLayoutDrawable(position, itemWidth, itemHeight, 10.dp, Color.LTGRAY, 10.dp)
                }
                toolsContainer.setOnDragListener { view, event ->
                    when (event.action) {
                        DragEvent.ACTION_DRAG_STARTED -> Log.i(this@MainFragment.TAG(), "ACTION_DRAG_STARTED")
                        DragEvent.ACTION_DRAG_ENDED -> Log.i(this@MainFragment.TAG(), "ACTION_DRAG_ENDED")
                        DragEvent.ACTION_DRAG_ENTERED -> Log.i(this@MainFragment.TAG(), "ACTION_DRAG_ENTERED")
                        DragEvent.ACTION_DRAG_EXITED -> Log.i(this@MainFragment.TAG(), "ACTION_DRAG_EXITED")
                        DragEvent.ACTION_DRAG_LOCATION -> Log.i(this@MainFragment.TAG(), "ACTION_DRAG_LOCATION:[${event.x}, ${event.y}]")
                        DragEvent.ACTION_DROP -> Log.i(this@MainFragment.TAG(), "ACTION_DROP")
                    }
                    true
                }
            }
            // 临时添加监听事件
            speedup.setOnClickListener { startActivity(Intent(context, EmptyActivity::class.java)) }
        }
    }
}