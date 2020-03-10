package com.abelhu.travel.ui.main

import com.qicode.merge.data.ToolBean
import com.qicode.merge.data.Tools
import com.qicode.merge.data.ToolsOperateListener
import java.math.BigDecimal

/**
 * Created by admin on 2020-03-05
 */
data class UserTool(
    // 金币余额（带单位）
    var gold: String,
    // 用户级别
    var level: Int,
    // 金币生产速率（带单位）
    var goldSpeed: String,
    // 背景中使用的道具ID
    var itemShow: Int,
    // 快速购买的最优的道具ID
    var bestItemId: Int,
    // 快速购买的最优的道具价格
    var bestItemGoldPrice: String,
    // 道具信息
    var items: List<Items>?,
    // 加速信息
    var produceQuick: ProduceQuick,
    // 监听器
    var operateListener: ToolsOperateListener
) : Tools(operateListener) {
    /**
     * 需要把Items转换成ToolBean
     */
    override fun getList(): MutableList<ToolBean> = toolList

    /**
     * 快速购买
     */
    override fun getQuickTool() = ToolBean(this, level = bestItemId)

    /**
     * 需要把Items转换成ToolBean
     */
    private lateinit var toolList: MutableList<ToolBean>

    /**
     * 在UserTool被反射构建后，需要调用此方法进一步完成初始化
     */
    fun initTool(listener: ToolsOperateListener) {
        // 从json里面反射的时候没有listener
        this.listener = listener
        // 资产
        property = ToolBean.getValue(gold)
        // 需要把List<Items>转换成List<ToolBean>
        toolList = MutableList(0) { ToolBean(this) }
        items?.forEach { it.apply { toolList.add(ToolBean(this@UserTool, index / 4, index % 4, item.itemId, ToolBean.getValue(item.itemGoldGenerateSpeed))) } }
        // 增益值， 需要再toolList初始化之后进行
        coefficient = BigDecimal(produceQuick.param / 100.0 + 1)
    }

    data class ProduceQuick(
        var start: Int,
        var times: Int,
        var param: Int
    )

    data class Items(
        var item: Item,
        var index: Int
    ) {
        data class Item(
            var itemId: Int,
            var itemBuyStyle: Int,
            var itemGoldPrice: String,
            var itemRedPacketPrice: Int,
            var itemGoldGenerateSpeed: String
        )
    }
}