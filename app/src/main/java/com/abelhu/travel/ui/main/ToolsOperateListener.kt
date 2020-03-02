package com.abelhu.travel.ui.main

import com.abelhu.travel.data.ToolBean

interface ToolsOperateListener {
    /**
     * 取消对tool的操作
     */
    fun onToolsCancel(index: Int, tool: ToolBean)

    /**
     * 添加了一个tool
     */
    fun onToolsAdd(index: Int, tool: ToolBean)

    /**
     * 添加一个tool，但是失败了
     */
    fun onToolsAddError(tool: ToolBean, cause: Exception)

    /**
     * 删除tool
     */
    fun onToolsDelete(index: Int, tool: ToolBean)

    /**
     * 把工具应用到某地方
     */
    fun onToolsApply(index: Int, tool: ToolBean)

    /**
     * 工具被移动
     */
    fun onToolsMove(index: Int, tool: ToolBean)

    /**
     * 工具被合成，返回合成后的工具
     */
    fun onToolsMerge(tool: ToolBean)

    /**
     * 工具被交换，返回交换后的两个工具
     */
    fun onToolsExchange(tools: List<ToolBean>)
}