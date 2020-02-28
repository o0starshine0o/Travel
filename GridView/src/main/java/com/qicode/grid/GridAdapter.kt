package com.qicode.grid

import android.support.annotation.NonNull

abstract class GridAdapter<VH : GridHolder> {
    abstract fun getItemCount(): Int
    abstract fun onCreateViewHolder(@NonNull parent: GridDragLayout): VH
    abstract fun onBindViewHolder(@NonNull holder: VH, position: Int): VH

    /**
     * 为了解决：Kotlin out-projected type prohibits the use
     * 参考：https://stackoverflow.com/questions/53093601/kotlin-out-projected-type-prohibits-the-use
     */
    @Suppress("UNCHECKED_CAST")
    fun onBindView(@NonNull holder: GridHolder, position: Int): VH {
        val item = holder as? VH ?: throw IllegalArgumentException("Invalid type ${holder.javaClass.name} passed to this parser")
        return onBindViewHolder(item, position)
    }
}