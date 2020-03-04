package com.abelhu.travel.ui.main

interface ToolsInitListener {
    /**
     * 购买某一等级工具的次数
     */
    fun buyCount(level: Int): Int

    /**
     * 获取当前的生产资源系数
     */
    fun coefficient(): Float
}