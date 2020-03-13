package com.qicode.merge.ui

import android.animation.AnimatorInflater
import android.content.ClipData
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.qicode.extension.TAG
import com.qicode.grid.GridDragBuilder
import com.qicode.grid.GridManager
import com.qicode.merge.R
import com.qicode.merge.data.ToolBean
import com.qicode.merge.data.Tools
import kotlinx.android.synthetic.main.item_tool.view.*
import java.math.BigDecimal

interface HolderHelp {
    fun toolDrawable(context: Context, level: Int): Drawable
    fun toolLevel(level: Int): String
    fun toolLevelVisibility(level: Int): Int
    fun toolPayLoads(view: View, payloads: MutableList<Any>)
}

class ToolsHolder(private val view: View, private val help: HolderHelp) : RecyclerView.ViewHolder(view) {
    var tools: Tools? = null
    lateinit var bean: ToolBean
    var handler = Handler()
    private val animator = AnimatorInflater.loadAnimator(view.context, R.animator.property_show)

    fun onBind(tools: Tools?, position: Int, payloads: MutableList<Any>? = null) {
        this.tools = tools ?: return
        bean = tools.getList()[position]
        Log.i(TAG(), "onBind tools[${bean.row}, ${bean.col}]")
        // 根据bean的显示属性判断bean是否在缓存队列，如果在缓存队列，直接跳过不显示
        view.visibility = if (bean.visibility) View.VISIBLE else View.GONE
        if (view.visibility == View.GONE) return
        // 根据保存的数据，设置item的位置
        (view.layoutParams as GridManager.LayoutParams).apply { row = bean.row;col = bean.col }
        // 设置文本， 图片
        view.image.setImageDrawable(help.toolDrawable(view.context, bean.level))
        view.level.text = help.toolLevel(bean.level)
        view.level.visibility = help.toolLevelVisibility(bean.level)
        // 设置view可见
        view.visibility = View.VISIBLE
        // 设置drag
        view.setOnTouchListener { _, event ->
            when (event.action) {
                // 注意：bind时候的position和drag时候的position可能已经由于（merge、delete、add等操作）不一样了，这里需要重新获取下目前的索引
                MotionEvent.ACTION_DOWN -> drag(event, tools.getList().indexOf(bean), bean)
                else -> true
            }
        }
        // 设置动画
        propertyShow()
        // 支持动效，或者其他额外内容
        payloads?.apply { if (size > 0) help.toolPayLoads(view, this) }
    }

    private fun propertyShow() {
        bean.apply {
            // 计算产生的资源
            val current = System.currentTimeMillis()
            val resource = BigDecimal((current - updateTime) / 1000) * propertyPer * coefficient
            // 查看数据
            Log.i(TAG(), "propertyShow tools[${row}, ${col}] with resource: $resource")
            Log.i(TAG(), " resource[$resource] = time[${(current - updateTime) / 1000}] * propertyPer[$propertyPer] * coefficient[$coefficient]")
            if (resource > BigDecimal.ZERO) {
                // 更新tool
                updateTime = current
                tools?.addProperty(resource)
                // 更新显示
                view.property.text = view.context.resources.getString(R.string.add_resource, ToolBean.getText(resource))
                view.propertyContainer.visibility = View.VISIBLE
                // 启动动画
                animator.cancel()
                animator.setTarget(view.propertyContainer)
                animator.start()
            }
            // 准备下一轮更新
            handler.postDelayed(this@ToolsHolder::propertyShow, 4000)
        }
    }

    private fun drag(event: MotionEvent, position: Int, tool: ToolBean): Boolean {
        tools?.listener?.onToolsSelect(position)
        // 隐藏view中的动画部分
        view.propertyContainer.visibility = View.INVISIBLE
        // 创建DragShadowBuilder，我把控件本身传进去
        val builder = GridDragBuilder(view, event.x, event.y)
        // 剪切板数据，可以在DragEvent.ACTION_DROP方法的时候获取。
        val data = ClipData.newPlainText("position", position.toString())
        // 开始拖拽
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val result = view.startDragAndDrop(data, builder, view, View.DRAG_FLAG_OPAQUE)
            if (!result) tools?.listener?.onToolsStopDrag(position, tool)
        } else {
            @Suppress("DEPRECATION")
            val result = view.startDrag(data, builder, view, 0)
            if (!result) tools?.listener?.onToolsStopDrag(position, tool)
        }
        // 隐藏原始view
        view.visibility = View.INVISIBLE
        return true
    }
}