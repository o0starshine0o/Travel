package com.qicode.merge.data

import android.util.Log
import com.qicode.extension.TAG
import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Created by admin on 2020-02-28
 */
data class ToolBean(
    // 初始化工具时需要的接口
    var listener: ToolsBeanListener,
    // 位于第几行
    var row: Int = Int.MAX_VALUE,
    // 位于第几列
    var col: Int = Int.MAX_VALUE,
    // 工具的最大等级
    var level: Int = Int.MIN_VALUE,
    // 工具每秒产生的资源数量
    var propertyPer: BigDecimal = BigDecimal.ZERO,
    // 工具上次产生资源的时间（单位：毫秒）
    var updateTime: Long = 0,
    // 基础价格
    var basePrice: BigDecimal = BigDecimal.ZERO,
    // 回收价格
    var recyclePrice: BigDecimal = BigDecimal.ZERO,
    // 工具每秒产生资源数量的系数
    var coefficient: BigDecimal = BigDecimal.ONE,
    // 工具是否在界面内可见（如果不可见，表示在缓存队列）
    var visibility: Boolean = true
) : Serializable {
    companion object {
        private const val O = 'O'
        private const val W = 'W'
        private const val E = 'E'
        /**
         * 计算最终要展示的字符
         * "1", "w", "e", "aa", "bb" ... "yy", "zz"
         */
        fun getText(value: BigDecimal): String {
            var c = O
            var all = value
            while (all / BigDecimal(10000) >= BigDecimal.ONE) {
                all /= BigDecimal(10000)
                c = when (c) {
                    // 用'2'表示"w"
                    O -> W
                    // 用'3'表示"e"
                    W -> E
                    // 开始使用双字幕组合
                    E -> 'a'
                    // 其他值都依次增加
                    else -> c + 1
                }
            }
            return when (c) {
                O -> "${all.setScale(0, RoundingMode.HALF_UP)}"
                W, E -> "${all.setScale(2, RoundingMode.HALF_UP)}${c.toLowerCase()}"
                else -> "${all.setScale(2, RoundingMode.HALF_UP)}$c$c"
            }.apply { Log.i(TAG(), "value change , from [$value] to [$this]") }
        }

        /**
         * 根据字符，计算真实的数值
         * "1", "w", "e", "aa", "bb" ... "yy", "zz"
         */
        fun getValue(text: String): BigDecimal {
            // 还原末尾字符
            var c = when (text.last()) {
                in '0'..'9' -> O
                else -> when (text[text.length - 2]) {
                    in '0'..'9' -> text.last().toUpperCase()
                    else -> text.last()
                }
            }
            // 还原具体数值
            val value = when (c) {
                O -> text
                else -> text.replace(c.toLowerCase(), ' ').trim()
            }
            // 还原最终数值
            var result = BigDecimal(value)
            while (c != O) {
                c = when (c) {
                    W -> O
                    E -> W
                    'a' -> E
                    else -> c - 1
                }
                result *= BigDecimal(10000)
            }
            return result.apply { Log.i(TAG(), "value change , from [$text] to [$this]") }
        }
    }

    init {
        // 更新的时间需要根据接口确定
        updateTime = listener.updateTime(this)
        // 每秒产生资源数量的系数，需要根据用户的行为来确定，一旦更改要应用到所有的实例
        coefficient = listener.coefficient(this)
        // 要转换成BigDecimal类型
        basePrice = listener.basePrice(this)
        // 要转换成BigDecimal类型
        recyclePrice = listener.recyclePrice(this)
    }

    /**
     * 当执行合并操作时
     */
    fun addLevel() {
        level++
    }

    fun index() = row * 4 + col

}