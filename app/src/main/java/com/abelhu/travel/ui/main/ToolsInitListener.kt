package com.abelhu.travel.ui.main

import com.abelhu.travel.data.ToolBean
import java.math.BigDecimal

interface ToolsInitListener {
    /**
     * 更新资产的时间
     */
    fun updateTime(toolBean: ToolBean): Long

    /**
     * 获取当前的生产资源系数
     */
    fun coefficient(toolBean: ToolBean): BigDecimal

    /**
     * 每秒产生的资源数量
     */
    fun propertyPerSecond(toolBean: ToolBean): BigDecimal

    /**
     * 基础价格
     */
    fun basePrice(toolBean: ToolBean): BigDecimal

    /**
     * 购买价格
     */
    fun buyPrice(toolBean: ToolBean): BigDecimal

    /**
     * 回收价格
     */
    fun recyclePrice(toolBean: ToolBean): BigDecimal
    /**
     * 购买某一等级工具的次数
     */
    fun buyCount(level: Int): Int
}