package com.qicode.grid

interface GridPosition {
    /**
     * 根据行列值，更新对应记录的数据
     */
    fun updatePosition(row: Int, col: Int, left: Float, top: Float)

    /**
     * 记录每个item的size，可以用来确定LayoutParams
     */
    fun updateItemSize(width: Float, height: Float)
}