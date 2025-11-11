package com.qujianma.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class DataManager private constructor(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        @Volatile
        private var INSTANCE: DataManager? = null

        fun getInstance(context: Context): DataManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        // 默认API密钥，用于在用户未设置自定义API密钥时访问服务器
        const val DEFAULT_API_KEY = "86849c0093c372c3edc7971ca49039b524720668950115e698a82677982eafa6"
    }
    
    /**
     * 保存手动添加的快递信息
     */
    fun saveManualExpressInfo(expressInfo: ExpressInfo) {
        val manualExpressList = getManualExpressInfoList().toMutableList()
        // 检查是否已存在相同的ID
        val existingIndex = manualExpressList.indexOfFirst { it.id == expressInfo.id }
        if (existingIndex >= 0) {
            manualExpressList[existingIndex] = expressInfo
        } else {
            manualExpressList.add(expressInfo)
        }
        
        val json = gson.toJson(manualExpressList)
        sharedPreferences.edit().putString("manual_express_list", json).apply()
    }
    
    /**
     * 获取手动添加的快递信息列表
     */
    fun getManualExpressInfoList(): List<ExpressInfo> {
        val json = sharedPreferences.getString("manual_express_list", null)
        return if (json != null) {
            val type = object : TypeToken<List<ExpressInfo>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }
    
    /**
     * 删除手动添加的快递信息
     */
    fun removeManualExpressInfo(id: String) {
        val manualExpressList = getManualExpressInfoList().toMutableList()
        val index = manualExpressList.indexOfFirst { it.id == id }
        if (index >= 0) {
            manualExpressList.removeAt(index)
            val json = gson.toJson(manualExpressList)
            sharedPreferences.edit().putString("manual_express_list", json).apply()
        }
    }
    
    /**
     * 保存备注信息
     */
    fun saveNote(id: String, note: String) {
        val notesMap = getNotes()
        notesMap[id] = note
        val json = gson.toJson(notesMap)
        sharedPreferences.edit().putString("notes", json).apply()
    }
    
    /**
     * 获取备注信息
     */
    fun getNote(id: String): String {
        val notesMap = getNotes()
        return notesMap[id] ?: ""
    }
    
    /**
     * 获取所有备注信息
     */
    private fun getNotes(): MutableMap<String, String> {
        val json = sharedPreferences.getString("notes", null)
        return if (json != null) {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableMapOf()
        }
    }
    
    /**
     * 添加已删除的项目ID到列表中
     */
    fun addDeletedItemId(id: String) {
        val deletedIds = getDeletedItemIds().toMutableSet()
        deletedIds.add(id)
        val json = gson.toJson(deletedIds.toList())
        sharedPreferences.edit().putString("deleted_item_ids", json).apply()
    }
    
    /**
     * 获取所有已删除的项目ID
     */
    fun getDeletedItemIds(): Set<String> {
        val json = sharedPreferences.getString("deleted_item_ids", null)
        return if (json != null) {
            val type = object : TypeToken<List<String>>() {}.type
            val list: List<String> = gson.fromJson(json, type)
            list.toSet()
        } else {
            emptySet()
        }
    }
    
    /**
     * 清除已删除项目ID列表
     */
    fun clearDeletedItemIds() {
        sharedPreferences.edit().remove("deleted_item_ids").apply()
    }
    
    /**
     * 设置首次授权标志
     */
    fun setFirstAuthorizationDone() {
        sharedPreferences.edit().putBoolean("first_authorization_done", true).apply()
    }
    
    /**
     * 检查是否已完成首次授权
     */
    fun isFirstAuthorizationDone(): Boolean {
        return sharedPreferences.getBoolean("first_authorization_done", false)
    }
    
    /**
     * 保存上次短信读取时间
     */
    fun saveLastSmsReadTime(time: Long) {
        sharedPreferences.edit().putLong("last_sms_read_time", time).apply()
    }
    
    /**
     * 获取上次短信读取时间
     */
    fun getLastSmsReadTime(): Long {
        return sharedPreferences.getLong("last_sms_read_time", 0L)
    }
    
    /**
     * 保存解析后的快递信息列表
     */
    fun saveExpressInfoList(expressList: List<ExpressInfo>) {
        val json = gson.toJson(expressList)
        sharedPreferences.edit().putString("express_info_list", json).apply()
    }
    
    /**
     * 获取已保存的快递信息列表
     */
    fun getExpressInfoList(): List<ExpressInfo> {
        val json = sharedPreferences.getString("express_info_list", null)
        return if (json != null) {
            val type = object : TypeToken<List<ExpressInfo>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }
    
    /**
     * 清除已保存的快递信息列表
     */
    fun clearExpressInfoList() {
        sharedPreferences.edit().remove("express_info_list").apply()
    }
    
    /**
     * 删除手动添加的快递信息（通过ID）
     */
    fun deleteManualExpressInfo(id: String) {
        val manualExpressList = getManualExpressInfoList().toMutableList()
        val index = manualExpressList.indexOfFirst { it.id == id }
        if (index >= 0) {
            manualExpressList.removeAt(index)
            val json = gson.toJson(manualExpressList)
            sharedPreferences.edit().putString("manual_express_list", json).apply()
        }
    }
}