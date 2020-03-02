package com.qicode.grid

import android.graphics.Point
import android.view.View

class GridDragBuilder(view: View? = null, private val touchX: Float, private val touchY: Float) : View.DragShadowBuilder(view) {
    override fun onProvideShadowMetrics(outShadowSize: Point, outShadowTouchPoint: Point) {
        // 保证点击位置和生成位置的一致性
        view?.apply {
            outShadowSize.set(view.width, view.height)
            outShadowTouchPoint.set(touchX.toInt(), touchY.toInt())
        }
    }
}