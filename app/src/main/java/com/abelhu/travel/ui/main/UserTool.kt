package com.abelhu.travel.ui.main

import com.abelhu.travel.data.ToolBean
import com.abelhu.travel.data.ToolsOperateListener
import java.math.BigDecimal

/**
 * Created by admin on 2020-03-05
 */
data class UserTool(
    var gold: String,
    var produceQuick: ProduceQuick,
    var items: List<Items>,
    var operateListener: ToolsOperateListener
) : Tools(operateListener) {
    // 需要把Items转换成ToolBean
    override fun getList(): MutableList<ToolBean> = toolList

    // 需要把Items转换成ToolBean
    private lateinit var toolList: MutableList<ToolBean>

    fun initTool(listener: ToolsOperateListener) {
        // 从json里面反射的时候没有listener
        this.listener = listener
        // 资产
        property = ToolBean.getValue(gold)
        // 需要把List<Items>转换成List<ToolBean>
        toolList = MutableList(0) { ToolBean(this) }
        items.forEach { it.apply { toolList.add(ToolBean(this@UserTool, index / 4, index % 4, item.itemId, ToolBean.getValue(item.itemGoldGenerateSpeed))) } }
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