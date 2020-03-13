package com.qicode.merge.data

import android.util.Log
import com.qicode.extension.TAG
import com.qicode.merge.ui.HolderHelp
import java.math.BigDecimal
import java.util.*
import kotlin.math.max

abstract class Tools(var listener: ToolsOperateListener) : ToolsBeanListener, HolderHelp {

    companion object {
        // 约定（-1， -1）表示增加tool
        val ADD = intArrayOf(-1, -1)
        // 约定（-2， -2）表示删除tool
        val RECYCLE = intArrayOf(-2, -2)
        // 约定（-3， -3）表示取消tool的操作
        val CANCEL = intArrayOf(-3, -3)
        // 其他约定，比如更换工具
        val APPLY = intArrayOf(-100, -100)
    }

    /**
     * 保存所有的工具，需要服务器来设定
     */
    abstract fun getList(): MutableList<ToolBean>

    /**
     * 获取快速购买按钮对应的工具，需要服务器来设定
     */
    abstract fun getQuickTool(): ToolBean

    /**
     * 生成资产的系数
     * 注意：coefficient需要再list之前初始化，因为list里面会使用到coefficient
     */
    var coefficient: BigDecimal = BigDecimal.ONE
        set(value) {
            field = value
            getList().forEach { it.coefficient = field }
        }

    /**
     * 用户的总资产(精确值）
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
    fun addTool(tool: ToolBean, rowMax: Int = 3, colMax: Int = 4): Int {
        Log.i(TAG(), "add tool with level: ${tool.level}")
        tool.apply {
            // 新增的ToolBean默认是不可见的，直到寻找到对应的位置
            visibility = false
            getEmptyPosition(rowMax, colMax)?.also {
                row = it / colMax
                col = it % colMax
                visibility = true
            }
            getList().add(this)
        }
        return getList().indexOf(tool)
    }

    /**
     * 回收工具
     * 注意：需要补充对应的回收价格
     */
    fun recycleTool(origin: ToolBean): ToolBean {
        getList().remove(origin)
        addProperty(origin.recyclePrice)
        return origin
    }

    /**
     * 从缓存队列中查找是否开源进行显示
     *
     * @return 在tool在列表中的索引
     */
    fun showCache(rowMax: Int = 3, colMax: Int = 4): Int? {
        getList().forEach { tool ->
            tool.takeIf { tool.index() >= rowMax * colMax }?.apply {
                getEmptyPosition()?.also { index ->
                    row = index / colMax
                    col = index % colMax
                    visibility = true
                    Log.i(TAG(), "showCache Index($index) [$row, $col]}")
                    return getList().indexOf(this)
                }
            }
        }
        return null
    }

    /**
     * 产生资产的速率
     */
    fun getSpeed(): BigDecimal {
        var result = BigDecimal.ZERO
        getList().forEach { if (it.visibility) result += it.propertyPer * it.coefficient }
        return result
    }

    /**
     * 根据row和col操作工具的行为
     */
    fun operateTool(index: Int, row: Int, col: Int) {
        // 获取原始的（操作的）tool
        if (index >= getList().size || index < 0) return
        val origin = getList()[index]
        // 判断是否是约定的特殊位置
        when {
            // 取消对tool的操作
            intArrayOf(row, col).contentEquals(CANCEL) -> listener.onToolsCancel(index, origin)
            // 删除这个tool
            intArrayOf(row, col).contentEquals(RECYCLE) -> listener.onToolsRecycle(index, origin)
            // 将这个tool应用到某个地方
            intArrayOf(row, col).contentEquals(APPLY) -> listener.onToolsApply(index, origin)
            else -> {
                val target = getTool(row, col)
                when {
                    // 移动tool
                    target == null -> listener.onToolsMove(index, intArrayOf(origin.row, origin.col), moveTool(origin, row, col))
                    // 在原地，只要是在原地，都默认为点击
                    target.second == origin -> listener.onToolsClick(index, origin)
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
     * 每秒产生资源数量的系数
     */
    override fun coefficient(toolBean: ToolBean) = coefficient

    /**
     * 根据文档来的: 6750 * 2.66.pow(level - 3)
     */
    override fun basePrice(toolBean: ToolBean) = when (toolBean.level) {
        1 -> BigDecimal(100)
        2 -> BigDecimal(1500)
        3 -> BigDecimal(67500)
        else -> BigDecimal(6750) * BigDecimal("2.66").pow(max(1, toolBean.level - 3))
    }

    /**
     * 根据文档来的: 0.1 * basePrice
     */
    override fun recyclePrice(toolBean: ToolBean) = BigDecimal("0.1") * toolBean.basePrice

    /**
     * 根据行列来寻找工具
     */
    private fun getTool(row: Int, col: Int): Pair<Int, ToolBean>? {
        for ((index, tool) in getList().withIndex()) if (tool.row == row && tool.col == col) return (index to tool)
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
        getList().remove(origin.second)
        target.second.addLevel()
        // 移除origin的时候，list的结构发生了改变，需要重新获取target的索引
        return listOf(origin, (getList().indexOf(target.second) to target.second))
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
     * 获取可以使用的空位
     */
    private fun getEmptyPosition(rowMax: Int = 3, colMax: Int = 4): Int? {
        val set = HashSet<Int>()
        getList().forEach { set.add(it.row * colMax + it.col) }
        for (i in 0 until rowMax * colMax) if (i !in set) return i
        return null
    }
}