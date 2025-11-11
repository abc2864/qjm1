package com.qujianma.app

import android.content.Context
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class AutoCleanupManager private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: AutoCleanupManager? = null
        
        fun getInstance(context: Context): AutoCleanupManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AutoCleanupManager(context).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 清理过期数据
     */
    fun cleanupExpiredData() {
        // 创建一个一次性工作请求来清理过期数据
        val cleanupWork = OneTimeWorkRequest.Builder(CleanupWorker::class.java)
            .setInitialDelay(1, TimeUnit.MINUTES) // 延迟1分钟执行
            .build()
            
        // 将工作请求加入WorkManager队列
        WorkManager.getInstance(context).enqueue(cleanupWork)
    }
    
    /**
     * 清理手动添加的过期取件信息
     */
    fun cleanupManualExpressInfo() {
        val dataManager = DataManager.getInstance(context)
        val manualExpressList = dataManager.getManualExpressInfoList()
        
        // 过滤掉30天前的手动添加取件信息
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -30)
        val thirtyDaysAgo = calendar.timeInMillis
        
        val expiredManualExpressList = manualExpressList.filter { 
            it.timestamp < thirtyDaysAgo 
        }
        
        // 从数据库中删除过期的手动添加取件信息
        expiredManualExpressList.forEach { 
            dataManager.deleteManualExpressInfo(it.id)
        }
    }
}