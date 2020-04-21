package com.qicode.cycle

import android.graphics.Bitmap

class CycleBitmap(origin: Bitmap, scale: Float = 1f, val bottom: Float = 0f, val speed: Float = 1f) {
    val bitmap: Bitmap

    constructor(origin: Bitmap, bottom: Float = 0f, speed: Float = 1f) : this(origin, 1f, bottom, speed)

    init {
        val width = (origin.width * scale).toInt()
        val height = (origin.height * scale).toInt()
        bitmap = Bitmap.createScaledBitmap(origin, width, height, true)
    }
}