package com.abelhu.travel.ui.main

import android.util.Log
import android.util.SparseIntArray
import com.abelhu.travel.data.ToolBean
import com.qicode.extension.TAG
import com.qicode.grid.GridLayoutManager
import kotlin.math.max
import kotlin.math.pow

class Tools(private val listener: ToolsOperateListener) : ToolsInitListener {
    /**
     * 保存每个等级的工具已经购买的次数，需要服务器来设定
     * 注意：map需要再list之前初始化，因为list里面会根据购买的数量计算下一次购买的价格
     */
    val map = SparseIntArray(16).apply {
        put(1, 10)
        put(2, 10)
        put(3, 10)
        put(4, 10)
        put(5, 10)
    }
    /**
     * 保存所有的工具，需要服务器来设定
     */
    val list = MutableList(2) { i -> ToolBean(i / 2, i % 2, 1, this) }
    /**
     * 用户的总资产
     */
    var property = 10000L

    /**
     * 增加总资产
     * 防止多线程造成的计算错误
     */
    @Synchronized
    fun addProperty(value: Long) {
        property += value
        listener.onPropertyUpdate(property)
    }

    /**
     * 添加一个tool
     */
    fun addTool(tool: ToolBean? = null, rowMax: Int = 3, colMax: Int = 4) {
        Log.i(TAG(), "add tool")
        val newTool = tool ?: ToolBean(0, 0, 1, this)
        for (i in 0 until rowMax) {
            for (j in 0 until colMax) {
                if (getTool(i, j) == null) return listener.onToolsAdd(list.size + 1, newTool.apply { row = i;col = j }.also { list.add(it) })
            }
        }
        listener.onToolsAddError(newTool, Exception("no empty position for tool to put"))
    }

    /**
     * 产生资产的速率
     */
    fun getSpeed(): Long {
        var result = 0L
        list.forEach { if (it.visibility) result += it.property }
        return result
    }

    fun getQuickTool(): ToolBean {
        val maxLevel = maxLevel()
        return when (maxLevel) {
            in 0..5 -> ToolBean(Int.MIN_VALUE, Int.MIN_VALUE, 1, this)
            else -> {
                var targetLevel = Int.MAX_VALUE
                var minPrice = Double.MAX_VALUE
                for (i in max(maxLevel - 10, 1)..(maxLevel - 4)) {
                    (ToolBean(Int.MIN_VALUE, Int.MIN_VALUE, i, this).buyPrice * (2.0.pow(7 - i))).takeIf { it < minPrice }?.also {
                        minPrice = it
                        targetLevel = i
                    }
                }
                ToolBean(Int.MIN_VALUE, Int.MIN_VALUE, targetLevel, this)
            }
        }
    }

    /**
     * 计算最终要展示的字符
     */
    fun showText(value: Long): String {
        var c = 'a' - 1
        var all = value.toFloat()
        while (all / 10000 > 10000) {
            all /= 10000
            c += 1
        }
        return if (c == 'a' - 1) "${all.toInt()}" else "${String.format("%.2f", all)}$c$c"
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
                    target.second == origin -> listener.onToolsCancel(index, origin)
                    // 合并tool
                    target.second.level == origin.level -> listener.onToolsMerge(mergeTool((index to origin), target))
                    // 交换两个tool
                    else -> listener.onToolsExchange(exchangeTool((index to origin), target))
                }
            }
        }
    }

    override fun buyCount(level: Int) = map[level, 0]

    /**
     * 根据行列来寻找工具
     */
    private fun getTool(row: Int, col: Int): Pair<Int, ToolBean>? {
        for ((index, tool) in list.withIndex()) if (tool.row == row && tool.col == col) return (index to tool)
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
    private fun mergeTool(origin: Pair<Int, ToolBean>, target: Pair<Int, ToolBean>): List<Pair<Int, ToolBean>> {
        list.remove(origin.second)
        target.second.addLevel()
        // 移除origin的时候，list的结构发生了改变，需要重新获取target的索引
        return listOf(origin, (list.indexOf(target.second) to target.second))
    }

    /**
     * 交换两个工具
     * 使用位运算，快速交换2个工具的row和col值
     */
    private fun exchangeTool(origin: Pair<Int, ToolBean>, target: Pair<Int, ToolBean>): List<Pair<Int, ToolBean>> {
        target.second.row = target.second.row.xor(origin.second.row)
        origin.second.row = target.second.row.xor(origin.second.row)
        target.second.row = target.second.row.xor(origin.second.row)
        target.second.col = target.second.col.xor(origin.second.col)
        origin.second.col = target.second.col.xor(origin.second.col)
        target.second.col = target.second.col.xor(origin.second.col)
        return listOf(origin, target)
    }

    /**
     * 获取最高的等级
     */
    private fun maxLevel(): Int {
        var max = 0
        list.forEach { max = max(max, it.level) }
        return max
    }
}