package com.qujianma.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SettingRulesAdapter(
    private val rules: MutableList<Rule>,
    private val onRuleAction: (Rule, String, Int) -> Unit
) : RecyclerView.Adapter<SettingRulesAdapter.RuleViewHolder>() {

    class RuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.rule_name)
        val typeTextView: TextView = itemView.findViewById(R.id.rule_type)
        val enabledSwitch: Switch = itemView.findViewById(R.id.rule_enabled)
        val viewButton: Button = itemView.findViewById(R.id.rule_view)
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
        
        // 显示规则类型（网络规则或本地规则）
        holder.typeTextView.text = if (rule.id.startsWith("network-")) {
            "[网络]"
        } else {
            "[本地]"
        }
        
        holder.enabledSwitch.isChecked = rule.enabled

        holder.enabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            onRuleAction(rule, "toggle", position)
        }

        holder.viewButton.setOnClickListener {
            onRuleAction(rule, "view", position)
        }

        holder.editButton.setOnClickListener {
            onRuleAction(rule, "edit", position)
        }

        holder.deleteButton.setOnClickListener {
            onRuleAction(rule, "delete", position)
        }
        
        // 网络规则不能编辑，只能查看、删除和启用/禁用
        if (rule.id.startsWith("network-")) {
            holder.viewButton.visibility = View.VISIBLE
            holder.editButton.visibility = View.GONE
        } else {
            holder.viewButton.visibility = View.GONE
            holder.editButton.visibility = View.VISIBLE
        }
    }

    override fun getItemCount() = rules.size
}