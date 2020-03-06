package com.qicode.merge.data

import java.math.BigDecimal

/**
 * 用于ToolBean的初始化，一般由实例化ToolBean的类实现
 */
interface ToolsBeanListener {
    /**
     * 更新资产的时间
     */
    fun updateTime(toolBean: ToolBean): Long

    /**
     * 获取当前的生产资源系数
     */
    fun coefficient(toolBean: ToolBean): BigDecimal

    /**
     * 基础价格
     */
    fun basePrice(toolBean: ToolBean): BigDecimal

    /**
     * 回收价格
     */
    fun recyclePrice(toolBean: ToolBean): BigDecimal
}