package com.qujianma.app

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import com.qujianma.app.ExpressInfo
import android.view.ContextMenu
import android.view.MenuItem

data class ExpressCodeInfo(
    val id: String,
    val code: String,
    val isPickedUp: Boolean
)

class ExpressGroupAdapter(
    private val context: Context,
    private var expressGroups: MutableList<ExpressGroup>,
    val onShowOriginalSms: (String) -> Unit = { _ -> },
    val onAddNote: (String, String) -> Unit = { _, _ -> },
    val onPickupStatusChanged: (String, Boolean) -> Unit = { _, _ -> },
    val onDeleteItem: (String) -> Unit = { _ -> }
) : RecyclerView.Adapter<ExpressGroupAdapter.GroupViewHolder>() {

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val stationNameTextView: TextView = itemView.findViewById(R.id.tv_station_name)
        val addressTextView: TextView = itemView.findViewById(R.id.tv_address)
        val pickupCodesContainer: LinearLayout = itemView.findViewById(R.id.ll_pickup_codes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_express_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = expressGroups[position]
        
        holder.stationNameTextView.text = group.stationName
        holder.addressTextView.text = group.address
        
        // 清除之前的视图
        holder.pickupCodesContainer.removeAllViews()
        
        // 为每个项目添加视图
        for (item in group.expressItems) {
            val itemView = LayoutInflater.from(context)
                .inflate(R.layout.item_express_info, holder.pickupCodesContainer, false)
            
            val pickupCodeTextView: TextView = itemView.findViewById(R.id.tv_pickup_code)
            val multipleCodesLayout: LinearLayout = itemView.findViewById(R.id.multiple_codes_layout)
            val stationNameTextView: TextView = itemView.findViewById(R.id.tv_station_name)
            val addressTextView: TextView = itemView.findViewById(R.id.tv_address)
            val timeTextView: TextView = itemView.findViewById(R.id.tv_time)
            val noteTextView: TextView = itemView.findViewById(R.id.tv_note)
            val originalSmsButton: Button = itemView.findViewById(R.id.btn_original_sms)
            val noteButton: Button = itemView.findViewById(R.id.btn_note)
            val deleteButton: Button = itemView.findViewById(R.id.btn_delete)
            
            // 隐藏驿站名称和地址，因为我们已经在组标题中显示了
            stationNameTextView.visibility = View.GONE
            addressTextView.visibility = View.GONE
            
            // 显示取件码（支持多个取件码垂直显示）
            if (item.pickupCodes.isNotEmpty() && item.pickupCodes.size > 1) {
                // 多个取件码：隐藏单个取件码TextView，显示多个取件码LinearLayout布局
                pickupCodeTextView.visibility = View.GONE
                multipleCodesLayout.visibility = View.VISIBLE
                multipleCodesLayout.removeAllViews()
                
                // 为每个取件码创建TextView并垂直排列
                val expressCodeList = mutableListOf<ExpressCodeInfo>()
                for ((index, code) in item.pickupCodes.withIndex()) {
                    // 为每个取件码生成独立的ID，与主逻辑层保持一致
                    val independentId = if (item.pickupCodes.size > 1) {
                        // 对于多取件码情况，生成独立ID
                        // 使用与MainActivity中processExpressList方法一致的ID生成方式
                        ExpressInfo.generateStableId(
                            code,
                            group.stationName,
                            group.address,
                            item.timestamp + index
                        )
                    } else {
                        // 单取件码情况，使用原始ID
                        item.id
                    }
                    expressCodeList.add(ExpressCodeInfo(independentId, code, item.isPickedUp))
                }
                
                // 为每个取件码创建独立的布局容器
                for (expressCodeInfo in expressCodeList) {
                    // 使用item_multiple_code布局为每个取件码创建独立的UI容器
                    val codeItemLayout = LayoutInflater.from(context)
                        .inflate(R.layout.item_multiple_code, multipleCodesLayout, false)
                    
                    val codeTextView: TextView = codeItemLayout.findViewById(R.id.tv_pickup_code)
                    val codeOriginalSmsButton: Button = codeItemLayout.findViewById(R.id.btn_code_original_sms)
                    val codeNoteButton: Button = codeItemLayout.findViewById(R.id.btn_code_note)
                    val codeDeleteButton: Button = codeItemLayout.findViewById(R.id.btn_code_delete)
                    
                    codeTextView.text = expressCodeInfo.code
                    
                    // 设置取件码样式
                    if (expressCodeInfo.isPickedUp) {
                        codeTextView.setTextColor(ContextCompat.getColor(context, R.color.darker_gray))
                        codeTextView.paintFlags = codeTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    } else {
                        codeTextView.setTextColor(ContextCompat.getColor(context, R.color.red))
                        codeTextView.paintFlags = codeTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    }
                    
                    // 为每个取件码TextView设置独立的点击事件
                    codeTextView.setOnClickListener {
                        onPickupStatusChanged(expressCodeInfo.id, !expressCodeInfo.isPickedUp)
                    }
                    
                    // 为每个取件码的按钮设置独立的点击事件，使用对应的独立ID
                    codeOriginalSmsButton.setOnClickListener {
                        onShowOriginalSms(expressCodeInfo.id)
                    }
                    
                    codeNoteButton.setOnClickListener {
                        onAddNote(expressCodeInfo.id, item.note)
                    }
                    
                    codeDeleteButton.setOnClickListener {
                        onDeleteItem(expressCodeInfo.id)
                    }
                    
                    multipleCodesLayout.addView(codeItemLayout)
                }
                
                // 设置整行点击事件来切换取件状态
                itemView.setOnClickListener {
                    // 如果只有一个取件码，整行点击切换取件状态
                    if (item.pickupCodes.size <= 1) {
                        onPickupStatusChanged(item.id, !item.isPickedUp)
                    }
                    // 多个取件码时不处理整行点击，因为每个取件码有自己的点击事件
                }

                // 隐藏外层的按钮，因为已经在每个取件码内部提供了操作按钮
                originalSmsButton.visibility = View.GONE
                noteButton.visibility = View.GONE
                deleteButton.visibility = View.GONE
            } else {
                // 单个取件码：显示单个取件码TextView，隐藏多个取件码LinearLayout布局
                multipleCodesLayout.visibility = View.GONE
                pickupCodeTextView.visibility = View.VISIBLE
                pickupCodeTextView.text = item.pickupCode
                
                // 设置状态样式
                if (item.isPickedUp) {
                    pickupCodeTextView.setTextColor(ContextCompat.getColor(context, R.color.darker_gray))
                    pickupCodeTextView.paintFlags = pickupCodeTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    pickupCodeTextView.setTextColor(ContextCompat.getColor(context, R.color.red))
                    pickupCodeTextView.paintFlags = pickupCodeTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
                
                // 设置整行点击事件来切换取件状态
                itemView.setOnClickListener {
                    onPickupStatusChanged(item.id, !item.isPickedUp)
                }
                
                // 为取件码TextView设置点击事件 - 单个取件码的情况
                pickupCodeTextView.setOnClickListener {
                    onPickupStatusChanged(item.id, !item.isPickedUp)
                }
            }
            
            // 使用日期加时间格式
            timeTextView.text = formatDateTime(item.timestamp)
            
            if (item.note.isNotEmpty()) {
                noteTextView.text = "备注: ${item.note}"
                noteTextView.visibility = View.VISIBLE
            } else {
                noteTextView.visibility = View.GONE
            }
            
            // 设置按钮点击事件，使用当前项目的完整ID
            originalSmsButton.setOnClickListener {
                onShowOriginalSms(item.id)
            }
            
            noteButton.setOnClickListener {
                onAddNote(item.id, item.note)
            }
            
            deleteButton.setOnClickListener {
                onDeleteItem(item.id)
            }
            
            holder.pickupCodesContainer.addView(itemView)
        }
    }

    override fun getItemCount(): Int = expressGroups.size

    /**
     * 刷新数据
     */
    fun refreshGroups(newGroups: List<ExpressGroup>) {
        expressGroups.clear()
        expressGroups.addAll(newGroups)
        notifyDataSetChanged()
    }

    /**
     * 根据快递信息列表创建分组
     * 现在只负责将数据转换为ExpressGroup，不进行排序或过滤
     */
    fun createGroupsFromExpressList(expressList: List<ExpressInfo>): List<ExpressGroup> {
        // 创建一个映射来存储每个驿站、地址和日期组合的项目
        val stationDateGroups = mutableMapOf<String, MutableList<ExpressInfo>>()
        
        // 按驿站名称、地址和日期分组
        for (expressInfo in expressList) {
            // 获取日期（年月日）
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = expressInfo.timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val dateKey = calendar.timeInMillis
            
            // 创建唯一的组键：驿站名称|地址|日期
            val groupKey = "${expressInfo.stationName}|${expressInfo.address}|$dateKey"
            
            if (stationDateGroups.containsKey(groupKey)) {
                stationDateGroups[groupKey]!!.add(expressInfo)
            } else {
                stationDateGroups[groupKey] = mutableListOf(expressInfo)
            }
        }
        
        // 创建最终的分组列表
        val resultGroups = mutableListOf<ExpressGroup>()
        val pickedUpGroups = mutableListOf<ExpressGroup>() // 存储已取件的分组
        
        // 按日期倒序排序（最新的在前），然后按驿站名称排序
        val sortedGroups = stationDateGroups.toSortedMap(
            compareByDescending<String> { groupKey ->
                val parts = groupKey.split("|")
                parts[2].toLong() // 日期（倒序）
            }.thenBy { groupKey ->
                val parts = groupKey.split("|")
                parts[0] // 驿站名称
            }
        )
        
        for ((groupKey, items) in sortedGroups) {
            val parts = groupKey.split("|")
            val stationName = parts[0]
            val address = parts[1]
            
            // 分离未取件和已取件的项目
            val unpickedItems = items.filter { !it.isPickedUp }
            val pickedItems = items.filter { it.isPickedUp }
            
            // 对未取件项目按时间倒序排列（最新的在前）
            val sortedUnpickedItems = unpickedItems.sortedWith(
                compareByDescending<ExpressInfo> { it.timestamp }
            ).map { expressInfo ->
                ExpressItem(
                    id = expressInfo.id,
                    pickupCode = expressInfo.pickupCode,
                    pickupCodes = expressInfo.pickupCodes,
                    timestamp = expressInfo.timestamp,
                    isPickedUp = expressInfo.isPickedUp,
                    note = expressInfo.note,
                    smsContent = expressInfo.smsContent
                )
            }
            
            // 如果有未取件的项目，创建未取件分组
            if (sortedUnpickedItems.isNotEmpty()) {
                val groupWithNameAndDate = ExpressGroup(
                    stationName = stationName,
                    address = address,
                    expressItems = sortedUnpickedItems.toMutableList()
                )
                resultGroups.add(groupWithNameAndDate)
            }
            
            // 对已取件项目按时间倒序排列（最新的在前）
            val sortedPickedItems = pickedItems.sortedWith(
                compareByDescending<ExpressInfo> { it.timestamp }
            ).map { expressInfo ->
                ExpressItem(
                    id = expressInfo.id,
                    pickupCode = expressInfo.pickupCode,
                    pickupCodes = expressInfo.pickupCodes,
                    timestamp = expressInfo.timestamp,
                    isPickedUp = expressInfo.isPickedUp,
                    note = expressInfo.note,
                    smsContent = expressInfo.smsContent
                )
            }
            
            // 如果有已取件的项目，创建已取件分组并添加到单独的列表中
            if (sortedPickedItems.isNotEmpty()) {
                val pickedUpGroupName = "$stationName (已取件)"
                val pickedUpGroup = ExpressGroup(
                    stationName = pickedUpGroupName,
                    address = address,
                    expressItems = sortedPickedItems.toMutableList()
                )
                pickedUpGroups.add(pickedUpGroup)
            }
        }
        
        // 将已取件的分组添加到结果列表的末尾
        resultGroups.addAll(pickedUpGroups)
        
        return resultGroups
    }

    private fun formatDateTime(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        
        // 格式化为日期加时间的格式
        val dateFormat = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
        return dateFormat.format(calendar.time)
    }
    
    private fun formatDate(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        
        val now = Calendar.getInstance()
        
        return when {
            calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) && 
            calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) -> "今天"
            calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) && 
            calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) - 1 -> "昨天"
            else -> "${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
        }
    }
}