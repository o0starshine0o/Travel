package com.abelhu.travel.data

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
    }

    fun addLevel() {
        level++
        property = (4 * 2.05.pow(level - 1)).toInt()
    }

}