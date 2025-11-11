package com.qujianma.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RulesAdapter(
    private val rules: MutableList<Rule>,
    private val onRuleAction: (Rule, String, Int) -> Unit
) : RecyclerView.Adapter<RulesAdapter.RuleViewHolder>() {

    class RuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.rule_name)
        val enabledSwitch: Switch = itemView.findViewById(R.id.rule_enabled)
        val editButton: Button = itemView.findViewById(R.id.rule_edit)
        val deleteButton: Button = itemView.findViewById(R.id.rule_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_setting_rule, parent, false)
        return RuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        val rule = rules[position]

        // 如果规则有名称则显示名称，否则显示"规则 + 序号"
        val displayName = if (rule.name.isNotEmpty()) {
            rule.name
        } else {
            "规则 ${position + 1}"
        }
        
        holder.nameTextView.text = displayName
        holder.enabledSwitch.isChecked = rule.enabled

        // 设置规则名称的点击事件（可选，如果需要的话）
        holder.nameTextView.setOnClickListener {
            // 可以在这里添加点击规则名称的处理逻辑
        }

        holder.enabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            onRuleAction(rule, "toggle", position)
        }

        holder.editButton.setOnClickListener {
            onRuleAction(rule, "edit", position)
        }

        holder.deleteButton.setOnClickListener {
            onRuleAction(rule, "delete", position)
        }
    }

    override fun getItemCount() = rules.size
}