package com.abelhu.travel.ui.main

import android.view.LayoutInflater
import android.view.View
import com.abelhu.travel.R
import com.abelhu.travel.data.ToolBean
import com.qicode.grid.GridAdapter
import com.qicode.grid.GridDragLayout
import com.qicode.grid.GridHolder
import kotlinx.android.synthetic.main.item_tool.view.*

class MainAdapter : GridAdapter<MainAdapter.ToolHolder>() {
    private val list = MutableList(3) { i -> ToolBean(i / 2, i % 2, i) }

    override fun getItemCount() = list.size

    override fun onCreateViewHolder(parent: GridDragLayout): ToolHolder {
        return ToolHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_tool, parent, false))
    }

    override fun onBindViewHolder(holder: ToolHolder, position: Int) = holder.onBind(list[position])

    class ToolHolder(view: View) : GridHolder(view) {
        fun onBind(bean: ToolBean): ToolHolder {
            row = bean.row
            col = bean.col
            view.text.text = bean.text.toString()
            return this
        }
    }
}