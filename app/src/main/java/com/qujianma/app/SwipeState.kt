package com.qujianma.app

/**
 * 用于管理RecyclerView项目左滑状态的类
 */
data class SwipeState(
    var isSwiped: Boolean = false,
    var swipePosition: Int = -1
)