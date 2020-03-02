package com.abelhu.travel.ui.main

import android.util.Log
import com.abelhu.travel.data.ToolBean
import com.qicode.extension.TAG
import com.qicode.grid.GridLayoutManager

class Tools(private val listener: ToolsOperateListener) {
    /**
     * 保存所有的工具
     */
    val list = MutableList(30) { i -> ToolBean(i / 2, i % 2, 1) }

    /**
     * 添加一个tool
     */
    fun addTool(tool: ToolBean? = null, rowMax: Int = 3, colMax: Int = 4) {
        Log.i(TAG(), "add tool")
        val newTool = tool ?: ToolBean(0, 0, 1)
        for (i in 0 until rowMax) {
            for (j in 0 until colMax) {
                if (getTool(i, j) == null) return listener.onToolsAdd(list.size + 1, newTool.apply { row = i;col = j }.also { list.add(it) })
            }
        }
        listener.onToolsAddError(newTool, Exception("no empty position for tool to put"))
    }

    /**
     * 根据row和col操作工具的行为
     */
    fun operateTool(index: Int, row: Int, col: Int) {
        // 获取原始的（操作的）tool
        if (index >= list.size || index < 0) return
        val origin = list[index]
        // 判断是否是约定的特殊位置
        when {
            // 取消对tool的操作
            intArrayOf(row, col).contentEquals(GridLayoutManager.CANCEL) -> listener.onToolsCancel(index, origin)
            // 删除这个tool
            intArrayOf(row, col).contentEquals(GridLayoutManager.DELETE) -> listener.onToolsDelete(index, origin.apply { list.remove(this) })
            // 将这个tool应用到某个地方
            intArrayOf(row, col).contentEquals(GridLayoutManager.APPLY) -> listener.onToolsApply(index, origin)
            else -> {
                val target = getTool(row, col)
                when {
                    // 移动tool
                    target == null -> listener.onToolsMove(index, moveTool(origin, row, col))
                    // 在原地
                    target == origin -> listener.onToolsCancel(index, origin)
                    // 合并tool
                    target.level == origin.level -> listener.onToolsMerge(mergeTool(origin, target))
                    // 交换两个tool
                    else -> listener.onToolsExchange(exchangeTool(origin, target))
                }
            }
        }
    }

    /**
     * 根据行列来寻找工具
     */
    private fun getTool(row: Int, col: Int): ToolBean? {
        for (tool in list) if (tool.row == row && tool.col == col) return tool
        return null
    }

    /**
     * 把工具移动到指定的行列
     */
    private fun moveTool(tool: ToolBean, row: Int, col: Int): ToolBean {
        tool.row = row
        tool.col = col
        return tool
    }

    /**
     * 合并两个工具
     */
    private fun mergeTool(origin: ToolBean, target: ToolBean): ToolBean {
        target.level += 1
        list.remove(origin)
        return target
    }

    /**
     * 交换两个工具
     * 使用位运算，快速交换2个工具的row和col值
     */
    private fun exchangeTool(origin: ToolBean, target: ToolBean): List<ToolBean> {
        target.row = target.row.xor(origin.row)
        origin.row = target.row.xor(origin.row)
        target.row = target.row.xor(origin.row)
        target.col = target.col.xor(origin.col)
        origin.col = target.col.xor(origin.col)
        target.col = target.col.xor(origin.col)
        return listOf(origin, target)
    }
}