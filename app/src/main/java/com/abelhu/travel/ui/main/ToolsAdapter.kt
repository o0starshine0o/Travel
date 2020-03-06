package com.abelhu.travel.ui.main

import android.animation.AnimatorInflater
import android.content.ClipData
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.DRAG_FLAG_OPAQUE
import android.view.ViewGroup
import com.abelhu.travel.R
import com.abelhu.travel.data.ToolBean
import com.qicode.extension.TAG
import com.qicode.grid.GridDragBuilder
import com.qicode.grid.GridLayoutManager
import kotlinx.android.synthetic.main.item_tool.view.*
import java.math.BigDecimal

class ToolsAdapter(private val tools: Tools) : RecyclerView.Adapter<ToolsAdapter.ToolsHolder>() {

    override fun getItemCount() = tools.getList().size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ToolsHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_tool, parent, false))

    override fun onBindViewHolder(holder: ToolsHolder, position: Int) {
        holder.onBind(tools.getList()[position])
    }

    override fun onViewRecycled(holder: ToolsHolder) {
        Log.i(TAG(), "onViewRecycled [${holder.bean?.row},${holder.bean?.col}]")
        holder.handler.removeCallbacksAndMessages(null)
        super.onViewRecycled(holder)
    }

    inner class ToolsHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        var bean: ToolBean? = null
        var handler = Handler()
        private val animator = AnimatorInflater.loadAnimator(view.context, R.animator.property_show)
        fun onBind(bean: ToolBean) {
            Log.i(TAG(), "onBind tools[${bean.row}, ${bean.col}]")
            this.bean = bean
            // 设置tool可见
            bean.visibility = true
            // 根据保存的数据，设置item的位置
            (view.layoutParams as GridLayoutManager.LayoutParams).apply { row = bean.row;col = bean.col }
            // 设置文本， 图片
            val fileName = "lottie/dog/ic_dog_level${bean.level}.png"
            view.image.setImageDrawable(Drawable.createFromStream(view.context.assets.open(fileName), null))
            view.level.text = bean.level.toString()
            // 设置view可见
            view.visibility = View.VISIBLE
            // 设置drag
            view.setOnTouchListener { _, event ->
                when (event.action) {
                    // 注意：bind时候的position和drag时候的position可能已经由于（merge、delete、add等操作）不一样了，这里需要重新获取下目前的索引
                    MotionEvent.ACTION_DOWN -> drag(event, tools.getList().indexOf(bean))
                    else -> false
                }
            }
            // 设置动画
            propertyShow()
        }

        private fun propertyShow() {
            bean?.apply {
                Log.i(TAG(), "propertyShow tools[${row}, ${col}]")
                // 计算产生的资源
                val current = System.currentTimeMillis()
                val resource = BigDecimal((current - updateTime) / 1000) * propertyPer * coefficient
                if (resource > BigDecimal.ZERO) {
                    // 更新tool
                    updateTime = current
                    tools.addProperty(resource)
                    // 更新显示
                    view.property.text = view.context.resources.getString(R.string.add_resource, ToolBean.getText(resource))
                    view.propertyContainer.visibility = View.VISIBLE
                    // 启动动画
                    animator.setTarget(view.propertyContainer)
                    animator.start()
                }
                // 准备下一轮更新
                handler.postDelayed(this@ToolsHolder::propertyShow, 5000)
            }
        }

        private fun drag(event: MotionEvent, position: Int): Boolean {
            tools.listener.onToolsSelect(position)
            // 隐藏view中的动画部分
            view.propertyContainer.visibility = View.INVISIBLE
            // 创建DragShadowBuilder，我把控件本身传进去
            val builder = GridDragBuilder(view, event.x, event.y)
            // 剪切板数据，可以在DragEvent.ACTION_DROP方法的时候获取。
            val data = ClipData.newPlainText("position", position.toString())
            // 开始拖拽
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                view.startDragAndDrop(data, builder, view, DRAG_FLAG_OPAQUE)
            } else {
                @Suppress("DEPRECATION")
                view.startDrag(data, builder, view, DRAG_FLAG_OPAQUE)
            }
            // 隐藏原始view
            view.visibility = View.INVISIBLE
            return true
        }
    }
}