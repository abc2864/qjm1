package com.qujianma.app

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.qujianma.app.ExpressInfo
import com.qujianma.app.PickupStatusManager
import com.qujianma.app.DataManager

class ExpressAdapter(
    private val context: Context,
    private val expressList: MutableList<ExpressInfo>,
    val onItemDismiss: (Int) -> Unit = { _ -> },
    val onShowOriginalSms: (Int) -> Unit = { _ -> },
    val onAddNote: (Int) -> Unit = { _ -> },
    val onPickupStatusChanged: (ExpressInfo, Boolean) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<ExpressAdapter.ViewHolder>() {

    private val pickupStatusManager = PickupStatusManager.getInstance(context)
    private val dataManager = DataManager.getInstance(context)
    private val swipeManager = SwipeManager.getInstance()
    // RecyclerView引用
    private lateinit var recyclerView: RecyclerView

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val backgroundLayout: LinearLayout = itemView.findViewById(R.id.item_background)
        val pickupCodeTextView: TextView = itemView.findViewById(R.id.tv_pickup_code)
        val stationNameTextView: TextView = itemView.findViewById(R.id.tv_station_name)
        val addressTextView: TextView = itemView.findViewById(R.id.tv_address)
        val timeTextView: TextView = itemView.findViewById(R.id.tv_time)
        val noteTextView: TextView = itemView.findViewById(R.id.tv_note)
        val rightInfoLayout: View = itemView.findViewById(R.id.right_info_layout)
        val originalSmsButton: Button = itemView.findViewById(R.id.btn_original_sms)
        val noteButton: Button = itemView.findViewById(R.id.btn_note)
        val deleteButton: Button = itemView.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_express_info, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var currentExpressInfo = expressList[position]
        val expressInfo = expressList[position]
        
        // 检查是否有保存的取件状态
        val savedStatus = pickupStatusManager.getPickupStatus(expressInfo.id)
        // 如果保存的状态与当前状态不同，则更新
        if (savedStatus != expressInfo.isPickedUp) {
            expressList[position] = expressInfo.copy(isPickedUp = savedStatus)
            // 更新currentExpressInfo以确保使用最新数据
            currentExpressInfo = expressList[position]
        }
        
        // 显示取件码（支持多个取件码垂直显示）
        if (currentExpressInfo.pickupCodes.isNotEmpty() && currentExpressInfo.pickupCodes.size > 1) {
            // 多个取件码：使用换行符垂直显示（保持原有实现以确保兼容性）
            val pickupCodeText = currentExpressInfo.pickupCodes.joinToString("\n")
            holder.pickupCodeTextView.text = pickupCodeText
        } else {
            // 单个取件码
            holder.pickupCodeTextView.text = currentExpressInfo.pickupCode
        }
        
        // 根据最长取件码长度调整字体大小
        val maxCodeLength = currentExpressInfo.pickupCodes.maxOfOrNull { it.length } ?: currentExpressInfo.pickupCode.length
        when {
            maxCodeLength > 11 -> {
                holder.pickupCodeTextView.textSize = 24f // 超过11位时使用较小字体
            }
            maxCodeLength > 8 -> {
                holder.pickupCodeTextView.textSize = 28f // 9-11位时使用中等字体
            }
            else -> {
                holder.pickupCodeTextView.textSize = 32f // 8位及以下时使用默认字体
            }
        }
        
        holder.stationNameTextView.text = currentExpressInfo.stationName
        holder.addressTextView.text = currentExpressInfo.address
        holder.timeTextView.text = formatTime(currentExpressInfo.timestamp)
        
        // 显示备注内容
        if (currentExpressInfo.note.isNotEmpty()) {
            holder.noteTextView.text = "备注: ${currentExpressInfo.note}"
            holder.noteTextView.visibility = View.VISIBLE
        } else {
            holder.noteTextView.visibility = View.GONE
        }
        
        // 动态调整取件码位置，避免与右侧信息重叠
        holder.itemView.post {
            try {
                // 获取右侧信息区域的宽度
                val rightInfoWidth = holder.rightInfoLayout.width
                if (rightInfoWidth <= 0) return@post // 如果宽度为0，跳过调整

                // 获取取件码文本的宽度
                holder.pickupCodeTextView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                val pickupCodeWidth = holder.pickupCodeTextView.measuredWidth
                if (pickupCodeWidth <= 0) return@post // 如果宽度为0，跳过调整

                // 获取容器宽度
                val containerWidth = holder.itemView.width
                if (containerWidth <= 0) return@post // 如果宽度为0，跳过调整

                // 计算右侧信息区域的左侧边界（考虑一些边距）
                val rightInfoLeft = containerWidth - rightInfoWidth - 20

                // 计算取件码在居中位置时的左右边界
                val pickupCodeLeftCentered = (containerWidth - pickupCodeWidth) / 2
                val pickupCodeRightCentered = pickupCodeLeftCentered + pickupCodeWidth

                val layoutParams = holder.pickupCodeTextView.layoutParams as RelativeLayout.LayoutParams

                // 如果取件码与右侧信息重叠，则调整取件码位置
                if (pickupCodeRightCentered > rightInfoLeft && rightInfoWidth > 0) {
                    // 计算新的左边距，使取件码左移避免重叠
                    val newLeftMargin = rightInfoLeft - pickupCodeWidth - 30 // 30是间距
                    if (newLeftMargin > 0) {
                        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0) // 移除居中规则
                        layoutParams.leftMargin = newLeftMargin
                        layoutParams.rightMargin = 0
                    } else {
                        // 如果计算出的左边距太小，则保持居中
                        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
                        layoutParams.leftMargin = 0
                        layoutParams.rightMargin = 0
                    }
                } else {
                    // 没有重叠，保持居中
                    layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
                    layoutParams.leftMargin = 0
                    layoutParams.rightMargin = 0
                }

                holder.pickupCodeTextView.layoutParams = layoutParams
            } catch (e: Exception) {
                // 出现异常时保持默认居中
                try {
                    val layoutParams = holder.pickupCodeTextView.layoutParams as RelativeLayout.LayoutParams
                    layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
                    layoutParams.leftMargin = 0
                    layoutParams.rightMargin = 0
                    holder.pickupCodeTextView.layoutParams = layoutParams
                } catch (innerE: Exception) {
                    // 忽略内部异常
                }
            }
        }
        
        // 设置按钮点击事件
        holder.originalSmsButton.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onShowOriginalSms(pos)
            }
        }
        
        holder.noteButton.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onAddNote(pos)
            }
        }
        
        holder.deleteButton.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onItemDismiss(pos)
            }
        }
        
        // 根据取件状态设置视觉效果
        if (currentExpressInfo.isPickedUp) {
            // 已取件状态：背景变灰色，所有文字变灰色，取件码添加删除线
            holder.backgroundLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.light_gray))
            holder.pickupCodeTextView.setTextColor(ContextCompat.getColor(context, R.color.darker_gray))
            holder.stationNameTextView.setTextColor(ContextCompat.getColor(context, R.color.darker_gray))
            holder.addressTextView.setTextColor(ContextCompat.getColor(context, R.color.darker_gray))
            holder.timeTextView.setTextColor(ContextCompat.getColor(context, R.color.darker_gray))
            holder.noteTextView.setTextColor(ContextCompat.getColor(context, R.color.darker_gray))
            holder.pickupCodeTextView.paintFlags = holder.pickupCodeTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            // 未取件状态：背景恢复正常，文字颜色恢复正常，取件码去除删除线
            holder.backgroundLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            holder.pickupCodeTextView.setTextColor(ContextCompat.getColor(context, R.color.red))
            holder.stationNameTextView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
            holder.addressTextView.setTextColor(ContextCompat.getColor(context, R.color.grey))
            holder.timeTextView.setTextColor(ContextCompat.getColor(context, R.color.light_grey))
            holder.noteTextView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
            holder.pickupCodeTextView.paintFlags = holder.pickupCodeTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
        
        // 设置取件码点击事件来切换取件状态
        holder.pickupCodeTextView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onPickupStatusChanged(currentExpressInfo, !currentExpressInfo.isPickedUp)
            }
        }
        
        // 设置整个项目的点击事件来切换取件状态
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onPickupStatusChanged(currentExpressInfo, !currentExpressInfo.isPickedUp)
            }
        }
    }

    override fun getItemCount(): Int {
        return expressList.size
    }
    
    /**
     * 格式化时间显示
     */
    private fun formatTime(timestamp: Long): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        
        return "${year}-${String.format("%02d", month)}-${String.format("%02d", day)} ${String.format("%02d", hour)}:${String.format("%02d", minute)}"
    }
    
    /**
     * 更新取件状态
     */
    fun updatePickupStatus(position: Int, isPickedUp: Boolean) {
        if (position >= 0 && position < expressList.size) {
            val expressInfo = expressList[position]
            expressList[position] = expressInfo.copy(isPickedUp = isPickedUp)
            notifyItemChanged(position)
        }
    }
    
    /**
     * 更新列表
     */
    fun updateList(newList: List<ExpressInfo>) {
        expressList.clear()
        expressList.addAll(newList)
        notifyDataSetChanged()
    }
    
    /**
     * 删除项目
     */
    fun removeItem(position: Int) {
        if (position >= 0 && position < expressList.size) {
            expressList.removeAt(position)
            notifyItemRemoved(position)
        }
    }
    
    /**
     * 添加项目
     */
    fun addItem(expressInfo: ExpressInfo) {
        expressList.add(0, expressInfo)
        notifyItemInserted(0)
    }
    
    /**
     * 获取指定位置的项目
     */
    fun getItem(position: Int): ExpressInfo {
        return expressList[position]
    }
}