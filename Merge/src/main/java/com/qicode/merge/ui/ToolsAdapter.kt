package com.qicode.merge.ui

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.qicode.extension.TAG
import com.qicode.merge.R
import com.qicode.merge.data.Tools

class ToolsAdapter(var tools: Tools) : RecyclerView.Adapter<ToolsHolder>() {

    override fun getItemCount() = tools.getList().size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolsHolder {
        return ToolsHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_tool, parent, false), tools)
    }

    override fun onBindViewHolder(holder: ToolsHolder, position: Int) {
        holder.onBind(tools, position)
    }

    override fun onBindViewHolder(holder: ToolsHolder, position: Int, payloads: MutableList<Any>) {
        holder.onBind(tools, position, payloads)
    }

    override fun onViewRecycled(holder: ToolsHolder) {
        Log.i(TAG(), "onViewRecycled [${holder.bean.row},${holder.bean.col}]")
        holder.handler.removeCallbacksAndMessages(null)
        super.onViewRecycled(holder)
    }
}