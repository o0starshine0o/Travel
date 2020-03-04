package com.abelhu.travel.data

import com.abelhu.travel.ui.main.ToolsInitListener
import java.io.Serializable
import kotlin.math.pow

/**
 * Created by admin on 2020-02-28
 */
data class ToolBean(
    // 位于第几行
    var row: Int,
    // 位于第几列
    var col: Int,
    // 工具的等级
    var level: Int,
    // 初始化工具时需要的接口
    var listener: ToolsInitListener,
    // 基础价格
    var basePrice: Long = 0,
    // 购买价格
    var buyPrice: Long = 0,
    // 回收价格
    var recyclePrice: Long = 0,
    // 工具上次产生资源的时间
    var update: Long = 0,
    // 工具每秒产生的资源数量
    var property: Int = 0,
    // 工具是否在界面内可见（如果不可见，表示在缓存队列）
    var visibility: Boolean = true
) : Serializable {
    init {
        update = System.currentTimeMillis()
        property = (4 * 2.05.pow(level - 1)).toInt()
        basePrice = when (level) {
            1 -> 100
            2 -> 1500
            3 -> 67500
            else -> 6750 * 2.66.pow(level - 3).toLong()
        }
        buyPrice = when (level) {
            1, 2 -> (basePrice * 1.07.pow(listener.buyCount(level) - 1)).toLong()
            else -> (basePrice * 1.17.pow(listener.buyCount(level) - 1)).toLong()
        }
        recyclePrice = (0.1 * basePrice).toLong()
    }

    fun addLevel() {
        level++
        property = (4 * 2.05.pow(level - 1)).toInt()
    }

}