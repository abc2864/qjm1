package com.qujianma.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class KeywordsAdapter(
    private val keywords: List<Map<String, Any>>,
    private val onKeywordAction: (Map<String, Any>, String, Int) -> Unit
) : RecyclerView.Adapter<KeywordsAdapter.KeywordViewHolder>() {

    class KeywordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.rule_name)
        val editButton: Button = view.findViewById(R.id.rule_edit)
        val deleteButton: Button = view.findViewById(R.id.rule_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeywordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rule, parent, false)
        return KeywordViewHolder(view)
    }

    override fun onBindViewHolder(holder: KeywordViewHolder, position: Int) {
        val keyword = keywords[position]
        val keywordText = keyword["keyword"].toString()
        
        holder.nameTextView.text = keywordText
        
        // 隐藏开关，因为关键词不需要启用/禁用
        val enabledSwitch = holder.itemView.findViewById<Switch>(R.id.rule_enabled)
        enabledSwitch.visibility = View.GONE

        holder.editButton.setOnClickListener {
            onKeywordAction(keyword, "edit", position)
        }

        holder.deleteButton.setOnClickListener {
            onKeywordAction(keyword, "delete", position)
        }
    }

    override fun getItemCount() = keywords.size
}