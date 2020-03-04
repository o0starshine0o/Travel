package com.abelhu.travel.ui.main

interface ToolsInitListener {
    /**
     * 购买某一等级工具的次数
     */
    fun buyCount(level: Int): Int
}