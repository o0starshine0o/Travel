package com.abelhu.travel.ui.main

import android.util.Log
import android.util.SparseIntArray
import com.abelhu.travel.data.ToolBean
import com.abelhu.travel.data.ToolsBeanListener
import com.abelhu.travel.data.ToolsOperateListener
import com.abelhu.travel.exception.NotEnoughPropertyError
import com.abelhu.travel.exception.NotEnoughSpaceError
import com.qicode.extension.TAG
import com.qicode.grid.GridLayoutManager
import java.math.BigDecimal
import kotlin.math.max

class Tools(val listener: ToolsOperateListener) : ToolsBeanListener {
    /**
     * 保存每个等级的工具已经购买的次数，需要服务器来设定
     * 注意：map需要再list之前初始化，因为list里面会根据购买的数量计算下一次购买的价格
     */
    private val map = SparseIntArray(16).apply {
        put(1, 10)
        put(2, 10)
        put(3, 10)
        put(4, 10)
        put(5, 10)
    }
    /**
     * 生成资产的系数
     * 注意：coefficient需要再list之前初始化，因为list里面会使用到coefficient
     */
    var coefficient: BigDecimal = BigDecimal.ONE
        set(value) {
            field = value
            list.forEach { it.coefficient = field }
        }
    /**
     * 保存所有的工具，需要服务器来设定
     */
    val list = MutableList(10) { i -> ToolBean(i / 4, i % 4, 30, this) }
    /**
     * 用户的总资产
     */
    var property = BigDecimal("100")

    /**
     * 增加总资产
     * 防止多线程造成的计算错误
     */
    @Synchronized
    fun addProperty(value: BigDecimal) {
        property += value
        listener.onPropertyUpdate(property)
    }

    /**
     * 添加一个tool
     */
    fun addTool(tool: ToolBean? = null, rowMax: Int = 3, colMax: Int = 4) {
        Log.i(TAG(), "add tool with level: ${tool?.level}")
        val newTool = tool ?: ToolBean(0, 0, 1, this)
        // 如果资产不够了，需要抛出
        if (property < newTool.buyPrice) {
            listener.onToolsAddError(newTool, NotEnoughPropertyError())
            return
        }
        // 寻找放置tool的空间
        for (i in 0 until rowMax) {
            for (j in 0 until colMax) {
                if (getTool(i, j) == null) return listener.onToolsAdd(list.size + 1, newTool.apply {
                    row = i
                    col = j
                    list.add(this)
                    // 需要减去需要的费用
                    this@Tools.addProperty(-buyPrice)
                })
            }
        }
        return listener.onToolsAddError(newTool, NotEnoughSpaceError())
    }

    /**
     * 产生资产的速率
     */
    fun getSpeed(): BigDecimal {
        var result = BigDecimal.ZERO
        list.forEach { if (it.visibility) result += it.propertyPerSecond * it.coefficient }
        return result
    }

    fun getQuickTool(): ToolBean {
        val maxLevel = maxLevel()
        return when (maxLevel) {
            in 0..5 -> ToolBean(Int.MIN_VALUE, Int.MIN_VALUE, 1, this)
            else -> {
                var targetLevel = Int.MAX_VALUE
                var maxPrice = BigDecimal.ZERO
                for (level in max(maxLevel - 10, 1)..(maxLevel - 4)) {
                    val result = ToolBean(Int.MIN_VALUE, Int.MIN_VALUE, level, this).buyPrice * BigDecimal(2).pow(max(0, 7 - level))
                    if (maxPrice == BigDecimal.ZERO || result > maxPrice && result <= property) {
                        maxPrice = result
                        targetLevel = level
                    }
                }
                ToolBean(Int.MIN_VALUE, Int.MIN_VALUE, targetLevel, this)
            }
        }
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
            intArrayOf(row, col).contentEquals(GridLayoutManager.RECYCLE) -> listener.onToolsRecycle(index, recycleTool(origin))
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

    /**
     * 更新资产的时间
     */
    override fun updateTime(toolBean: ToolBean) = System.currentTimeMillis()

    /**
     * 某一个等级的工具的购买数量
     */
    override fun buyCount(level: Int) = map[level, 0]

    /**
     * 每秒产生资源数量的系数
     */
    override fun coefficient(toolBean: ToolBean) = coefficient

    /**
     * 根据文档来的: 4 * 2.05.pow(level - 1)
     */
    override fun propertyPerSecond(toolBean: ToolBean) = BigDecimal(4) * BigDecimal("2.05").pow(toolBean.level - 1)

    /**
     * 根据文档来的: 6750 * 2.66.pow(level - 3)
     */
    override fun basePrice(toolBean: ToolBean) = when (toolBean.level) {
        1 -> BigDecimal(100)
        2 -> BigDecimal(1500)
        3 -> BigDecimal(67500)
        else -> BigDecimal(6750) * BigDecimal("2.66").pow(toolBean.level - 3)
    }

    /**
     * 根据文档来的:
     * basePrice * 1.07.pow(listener.buyCount(level) - 1)
     * basePrice * 1.17.pow(listener.buyCount(level) - 1)
     */
    override fun buyPrice(toolBean: ToolBean): BigDecimal {
        return when (toolBean.level) {
            1, 2 -> toolBean.basePrice * BigDecimal("1.07").pow(max(0, buyCount(toolBean.level) - 1))
            else -> toolBean.basePrice * BigDecimal("1.17").pow(max(0, buyCount(toolBean.level) - 1))
        }
    }

    /**
     * 根据文档来的: 0.1 * basePrice
     */
    override fun recyclePrice(toolBean: ToolBean) = BigDecimal("0.1") * toolBean.basePrice

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
        Log.i(TAG(), "move tool [${tool.row}, ${tool.col}] -> [$row, $col]")
        tool.row = row
        tool.col = col
        return tool
    }

    /**
     * 合并两个工具
     */
    private fun mergeTool(origin: Pair<Int, ToolBean>, target: Pair<Int, ToolBean>): List<Pair<Int, ToolBean>> {
        Log.i(TAG(), "merge tool [${origin.second.row}, ${origin.second.col}] -> [${target.second.row}, ${target.second.col}]")
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
        Log.i(TAG(), "exchange tool [${origin.second.row}, ${origin.second.col}] -> [${target.second.row}, ${target.second.col}]")
        target.second.row = target.second.row.xor(origin.second.row)
        origin.second.row = target.second.row.xor(origin.second.row)
        target.second.row = target.second.row.xor(origin.second.row)
        target.second.col = target.second.col.xor(origin.second.col)
        origin.second.col = target.second.col.xor(origin.second.col)
        target.second.col = target.second.col.xor(origin.second.col)
        return listOf(origin, target)
    }

    /**
     * 回收工具
     * 注意：需要补充对应的回收价格
     */
    private fun recycleTool(origin: ToolBean): ToolBean {
        list.remove(origin)
        addProperty(origin.recyclePrice)
        return origin
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