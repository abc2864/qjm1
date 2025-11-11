package com.qujianma.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class CleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // 执行清理过期数据的操作
            val autoCleanupManager = AutoCleanupManager.getInstance(applicationContext)
            autoCleanupManager.cleanupExpiredData()
            autoCleanupManager.cleanupManualExpressInfo()
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}