package com.abelhu.travel.ui.main

import android.content.ClipData
import android.os.Build
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.DRAG_FLAG_OPAQUE
import android.view.ViewGroup
import com.abelhu.travel.R
import com.abelhu.travel.data.ToolBean
import com.qicode.grid.DragBuilder
import com.qicode.grid.GridLayoutManager
import kotlinx.android.synthetic.main.item_tool.view.*


class ToolsAdapter : RecyclerView.Adapter<ToolsAdapter.ToolsHolder>() {

    private val list = MutableList(30) { i -> ToolBean(i / 2, i % 2, i) }

    override fun getItemCount() = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ToolsHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_tool, parent, false))

    override fun onBindViewHolder(holder: ToolsHolder, position: Int) {
        holder.onBind(list[position])
    }

    class ToolsHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private var moveX = 0f
        private var moveY = 0f
        fun onBind(bean: ToolBean): ToolsHolder {
            // 根据保存的数据，设置item的位置
            (view.layoutParams as GridLayoutManager.LayoutParams).apply {
                row = bean.row
                col = bean.col
            }
            // 设置文本
            view.text.text = bean.text.toString()
            // 设置drag
            view.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {// 创建DragShadowBuilder，我把控件本身传进去
                        event.y
                        view.top
                        // 创建DragShadowBuilder，我把控件本身传进去
                        val builder = DragBuilder(view, event.x, event.y)
                        // 剪切板数据，可以在DragEvent.ACTION_DROP方法的时候获取。
                        // 剪切板数据，可以在DragEvent.ACTION_DROP方法的时候获取。
                        val data = ClipData.newPlainText("Label", "我是文本内容！")
                        // 开始拖拽
                        // 开始拖拽
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            view.startDragAndDrop(data, builder, view, DRAG_FLAG_OPAQUE)
                        } else {
                            view.startDrag(data, builder, view, DRAG_FLAG_OPAQUE)
                        }
                        true
                    }
                    else -> false
                }
            }
            return this
        }
    }
}