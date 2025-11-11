package com.qujianma.app

import androidx.recyclerview.widget.RecyclerView

/**
 * 用于管理RecyclerView项目左滑状态的单例类
 */
class SwipeManager private constructor() {
    // 当前滑动的项的位置
    private var currentSwipedPosition: Int = RecyclerView.NO_POSITION
    
    // 当前滑动的ViewHolder
    private var currentSwipedViewHolder: RecyclerView.ViewHolder? = null

    companion object {
        @Volatile
        private var instance: SwipeManager? = null

        fun getInstance(): SwipeManager {
            return instance ?: synchronized(this) {
                instance ?: SwipeManager().also { instance = it }
            }
        }
    }

    /**
     * 设置当前滑动项
     * @param position 滑动项的位置
     * @param viewHolder 滑动项的ViewHolder
     */
    fun setSwipedItem(position: Int, viewHolder: RecyclerView.ViewHolder) {
        // 如果之前有其他滑动项，先复位它
        if (currentSwipedPosition != RecyclerView.NO_POSITION && currentSwipedPosition != position) {
            resetSwipedItem()
        }
        
        currentSwipedPosition = position
        currentSwipedViewHolder = viewHolder
    }

    /**
     * 清除当前滑动项
     */
    fun clearSwipedItem() {
        currentSwipedPosition = RecyclerView.NO_POSITION
        currentSwipedViewHolder = null
    }
    
    /**
     * 获取当前滑动项位置
     */
    fun getSwipedPosition(): Int = currentSwipedPosition
    
    /**
     * 检查指定位置是否为当前滑动项
     */
    fun isSwipedPosition(position: Int): Boolean = currentSwipedPosition != RecyclerView.NO_POSITION && currentSwipedPosition == position
    
    /**
     * 复位当前滑动项
     */
    fun resetSwipedItem() {
        currentSwipedViewHolder?.itemView?.animate()?.translationX(0f)?.setDuration(200)?.start()
        clearSwipedItem()
    }
}