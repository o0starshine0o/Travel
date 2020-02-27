package com.qicode.cycle

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.Log
import com.qicode.extension.TAG
import kotlin.math.max
import kotlin.math.min

/**
 * @param lifecycle 必须要和调用的控件绑定，以防止内存泄漏
 * @param minRefreshTime 最小刷新时间，避免频繁刷新界面
 */
class CycleDrawable(lifecycle: Lifecycle, private val minRefreshTime: Long = 33) : Drawable(), LifecycleObserver {
    companion object {
        const val START = 1
        const val RUNNING = 2
        const val PAUSE = 3
        const val DESTROY = 4
    }

    /**
     * 记录开始的时间
     */
    var startTime = System.currentTimeMillis()
    /**
     * 记录目前Drawable的状态
     */
    private var status: Int = START
    /**
     * 保存所有需要绘制的图片
     */
    private val list = MutableList(0) { CycleBitmap(Bitmap.createBitmap(0, 0, Bitmap.Config.ALPHA_8)) }
    /**
     * 绘制每个item
     */
    private val paint = Paint().apply {
        // 设置画笔
        isAntiAlias = true
        isDither = true
        isFilterBitmap = true
        strokeWidth = 0f
        strokeCap = Paint.Cap.ROUND
        textAlign = Paint.Align.CENTER
    }

    fun addImages(images: List<CycleBitmap>): CycleDrawable {
        list.addAll(images)
        return this
    }

    fun start() {
        status = RUNNING
        cycle()
    }

    init {
        lifecycle.addObserver(this)
    }

    override fun draw(canvas: Canvas) {
        val current = System.currentTimeMillis()
        for (cycleBitmap in list) {
            cycleBitmap.bitmap.apply {
                // 根据图片记录的到底部的距离，计算出图片到顶部的距离
                val top = bounds.height() - this.height - cycleBitmap.bottom
                // 根据时间差和速率，计算出需要向左偏移的距离
                val left = (current - startTime) * cycleBitmap.speed / 1000 % width
                canvas.drawBitmap(this, 0 - left, top, paint)
                // 需要在绘制一遍，保证图片的无限循环
                if (left + bounds.width() > width) {
                    canvas.drawBitmap(this, width - left, top, paint)
                }
            }
        }
        Log.i(TAG(), "draw cycle drawable")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        Log.i(TAG(), "draw cycle onCreate")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        Log.i(TAG(), "draw cycle onStart")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        Log.i(TAG(), "draw cycle onResume")
        status = if (status == PAUSE) RUNNING else status
        cycle()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        Log.i(TAG(), "draw cycle onPause")
        // 只需要设置PAUSE就可以了，当执行到cycle函数时，自己就暂停了
        status = PAUSE
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        Log.i(TAG(), "draw cycle onStop")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        Log.i(TAG(), "draw cycle onDestroy")
        status = DESTROY
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun getOpacity() = paint.alpha

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    private fun cycle(time: Long = 0) {
        // 只有在正常运行状态才更新，invalidateSelf
        // 暂停状态跳过更新，等待恢复后继续刷新
        // 销毁状态跳过更新，由于没有对象再持有`CycleDrawable`，可以做到被JVM回收
        if (status == RUNNING) {
            // 根据最快的图片移动速率，算出最大的刷新时间
            val delay = if (time <= 0) getRefreshTime() else time
            // 到时间点定时调用这个方法无限循环
            Handler().postDelayed({
                invalidateSelf()
                cycle(delay)
            }, delay)
        }
    }

    private fun getRefreshTime(): Long {
        var result = Long.MAX_VALUE
        for (cycleBitmap in list) result = min((1000 / cycleBitmap.speed).toLong(), result)
        return max(result, minRefreshTime).apply { Log.i(this@CycleDrawable.TAG(), "calculate min refresh time: $this") }
    }
}