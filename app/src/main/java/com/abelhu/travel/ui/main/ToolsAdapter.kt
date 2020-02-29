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

    private val list = MutableList(30) { i -> ToolBean(i / 2, i % 2, i, 1) }

    override fun getItemCount() = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ToolsHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_tool, parent, false))

    override fun onBindViewHolder(holder: ToolsHolder, position: Int) {
        holder.onBind(list[position], position)
    }

    override fun onBindViewHolder(holder: ToolsHolder, position: Int, payloads: MutableList<Any>) {
        holder.onBind(list[position], position)
    }

    fun changeTools(origin: Int, targetRow: Int, targetCol: Int) {
        // TODO：移动到空位，待处理
        if (targetRow < 0 || targetCol < 0) {
            notifyDataSetChanged()
            return
        }
        val target = getTarget(targetRow, targetCol)
        list[origin].apply {
            when {
                // 移动到空位
                target == null -> {
                    row = targetRow
                    col = targetCol
                    notifyDataSetChanged()
                }
                // 原位，没有有效移动
                target == this -> notifyItemChanged(origin)
                // 等级相同，合并2个bean
                level == target.level -> {
                    target.level += 1
                    list.remove(this)
                    notifyDataSetChanged()
                }
                // 等级不同，交换2个bean
                else -> {
                    target.row = target.row.xor(row)
                    row = target.row.xor(row)
                    target.row = target.row.xor(row)
                    target.col = target.col.xor(col)
                    col = target.col.xor(col)
                    target.col = target.col.xor(col)
                    notifyDataSetChanged()
                }
            }
        }
    }

    private fun getTarget(row: Int, col: Int): ToolBean? {
        for (tool in list) if (tool.row == row && tool.col == col) return tool
        return null
    }

    class ToolsHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun onBind(bean: ToolBean, position: Int): ToolsHolder {
            // 根据保存的数据，设置item的位置
            (view.layoutParams as GridLayoutManager.LayoutParams).apply {
                row = bean.row
                col = bean.col
            }
            // 设置文本
            view.text.text = bean.text.toString()
            view.level.text = bean.level.toString()
            // 设置view可见
            view.visibility = View.VISIBLE
            // 设置drag
            view.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // 创建DragShadowBuilder，我把控件本身传进去
                        val builder = DragBuilder(view, event.x, event.y)
                        // 剪切板数据，可以在DragEvent.ACTION_DROP方法的时候获取。
                        val data = ClipData.newPlainText("position", position.toString())
                        // 开始拖拽
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            view.startDragAndDrop(data, builder, view, DRAG_FLAG_OPAQUE)
                        } else {
                            @Suppress("DEPRECATION")
                            view.startDrag(data, builder, view, DRAG_FLAG_OPAQUE)
                        }
                        view.visibility = View.INVISIBLE
                        true
                    }
                    else -> false
                }
            }
            return this
        }
    }
}