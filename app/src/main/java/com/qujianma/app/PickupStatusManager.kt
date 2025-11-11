package com.qujianma.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PickupStatusManager private constructor(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("pickup_status", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        @Volatile
        private var INSTANCE: PickupStatusManager? = null

        fun getInstance(context: Context): PickupStatusManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PickupStatusManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * 保存取件状态
     */
    fun savePickupStatus(id: String, isPickedUp: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(id, isPickedUp)
        editor.apply()
    }

    /**
     * 获取取件状态
     */
    fun getPickupStatus(id: String): Boolean {
        return sharedPreferences.getBoolean(id, false)
    }

    /**
     * 批量保存取件状态
     */
    fun saveAllPickupStatus(statusMap: Map<String, Boolean>) {
        val editor = sharedPreferences.edit()
        for ((id, isPickedUp) in statusMap) {
            editor.putBoolean(id, isPickedUp)
        }
        editor.apply()
    }

    /**
     * 获取所有取件状态
     */
    fun getAllPickupStatus(): Map<String, Boolean> {
        val allEntries = sharedPreferences.all
        val statusMap = mutableMapOf<String, Boolean>()
        
        for ((key, value) in allEntries) {
            if (value is Boolean) {
                statusMap[key] = value
            }
        }
        
        return statusMap
    }

    /**
     * 清除所有取件状态
     */
    fun clearAllPickupStatus() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }
}