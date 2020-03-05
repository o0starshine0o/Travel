package com.abelhu.travel.data

import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode

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
    var listener: ToolsBeanListener,
    // 工具上次产生资源的时间（单位：毫秒）
    var updateTime: Long = 0,
    // 工具每秒产生的资源数量
    var propertyPerSecond: BigDecimal = BigDecimal.ZERO,
    // 基础价格
    var basePrice: BigDecimal = BigDecimal.ZERO,
    // 购买价格
    var buyPrice: BigDecimal = BigDecimal.ZERO,
    // 回收价格
    var recyclePrice: BigDecimal = BigDecimal.ZERO,
    // 工具每秒产生资源数量的系数
    var coefficient: BigDecimal = BigDecimal.ONE,
    // 工具是否在界面内可见（如果不可见，表示在缓存队列）
    var visibility: Boolean = true
) : Serializable {
    companion object {
        /**
         * 计算最终要展示的字符
         */
        fun showText(value: BigDecimal): String {
            var c = 'a' - 1
            var all = value
            while (all / BigDecimal(10000) >= BigDecimal.ONE) {
                all /= BigDecimal(10000)
                c += 1
            }
            return if (c == 'a' - 1) "${all.setScale(0, RoundingMode.HALF_UP)}" else "${all.setScale(2, RoundingMode.HALF_UP)}$c$c"
        }
    }

    init {
        // 更新的时间需要根据接口确定
        updateTime = listener.updateTime(this)
        // 每秒产生资源数量的系数，需要根据用户的行为来确定，一旦更改要应用到所有的实例
        coefficient = listener.coefficient(this)
        // 要转换成BigDecimal类型
        propertyPerSecond = listener.propertyPerSecond(this)
        // 要转换成BigDecimal类型
        basePrice = listener.basePrice(this)
        // 要转换成BigDecimal类型
        buyPrice = listener.buyPrice(this)
        // 要转换成BigDecimal类型
        recyclePrice = listener.recyclePrice(this)
    }

    /**
     * 当执行合并操作时
     */
    fun addLevel() {
        level++
        propertyPerSecond = listener.propertyPerSecond(this)
    }

}