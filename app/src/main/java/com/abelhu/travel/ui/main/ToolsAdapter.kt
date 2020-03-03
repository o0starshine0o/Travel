package com.abelhu.travel.ui.main

import android.animation.AnimatorInflater
import android.content.ClipData
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.DRAG_FLAG_OPAQUE
import android.view.ViewGroup
import com.abelhu.travel.R
import com.abelhu.travel.data.ToolBean
import com.qicode.grid.GridDragBuilder
import com.qicode.grid.GridLayoutManager
import kotlinx.android.synthetic.main.item_tool.view.*
import java.util.*

class ToolsAdapter(private val tools: Tools) : RecyclerView.Adapter<ToolsAdapter.ToolsHolder>() {

    override fun getItemCount() = tools.list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ToolsHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_tool, parent, false))

    override fun onBindViewHolder(holder: ToolsHolder, position: Int) {
        holder.onBind(tools.list[position], position)
    }

    class ToolsHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private var random = 0
        fun onBind(bean: ToolBean, position: Int): ToolsHolder {
            // 根据保存的数据，设置item的位置
            (view.layoutParams as GridLayoutManager.LayoutParams).apply { row = bean.row;col = bean.col }
            // 设置文本
            val fileName = "lottie/dog/ic_dog_level${bean.level}.png"
            view.image.setImageDrawable(Drawable.createFromStream(view.context.assets.open(fileName), null))
            view.level.text = bean.level.toString()
            // 设置view可见
            view.visibility = View.VISIBLE
            // 设置drag
            view.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> drag(event, position)
                    else -> false
                }
            }
            // 设置动画
            random = Random().nextInt()
            view.setTag(R.integer.property_id, random)
            propertyShow()
            return this
        }

        private fun propertyShow() {
            // 当脏数据到这里来的时候直接跳过
            if (view.getTag(R.integer.property_id) == random) {
                val animator = AnimatorInflater.loadAnimator(view.context, R.animator.property_show)
                animator.setTarget(view.propertyContainer)
                animator.start()
                Handler().postDelayed(this::propertyShow, 5000)
            }
        }

        private fun drag(event: MotionEvent, position: Int): Boolean {
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