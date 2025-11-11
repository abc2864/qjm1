package com.qujianma.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GeneralAdapter(private var generalList: List<GeneralInfo>) : 
    RecyclerView.Adapter<GeneralAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
        val tvContent: TextView = view.findViewById(R.id.tv_content)
        val tvSender: TextView = view.findViewById(R.id.tv_sender)
        val tvSmsContent: TextView = view.findViewById(R.id.tv_sms_content)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_general_info, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val generalInfo = generalList[position]
        
        holder.tvTitle.text = if (generalInfo.title.isNotEmpty()) {
            generalInfo.title
        } else {
            "未知类型"
        }
        
        holder.tvContent.text = if (generalInfo.content.isNotEmpty()) {
            generalInfo.content
        } else {
            "无内容"
        }
        
        holder.tvSender.text = if (generalInfo.sender.isNotEmpty()) {
            "发送方: ${generalInfo.sender}"
        } else {
            "发送方: 未知"
        }
        
        holder.tvSmsContent.text = generalInfo.smsContent
    }
    
    override fun getItemCount() = generalList.size
    
    fun updateData(newList: List<GeneralInfo>) {
        this.generalList = newList
        notifyDataSetChanged()
    }
}