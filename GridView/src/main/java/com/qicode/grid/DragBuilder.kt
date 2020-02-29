package com.qicode.grid

import android.graphics.Point
import android.view.View

class DragBuilder(view: View? = null, private val touchX: Float, private val touchY: Float) : View.DragShadowBuilder(view) {
    override fun onProvideShadowMetrics(outShadowSize: Point, outShadowTouchPoint: Point) {
        view?.apply {
            outShadowSize.set(view.width, view.height)
            outShadowTouchPoint.set(touchX.toInt(), touchY.toInt())
        }
    }
}