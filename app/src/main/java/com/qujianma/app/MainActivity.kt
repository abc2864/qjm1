package com.qujianma.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import android.app.Dialog


class MainActivity : AppCompatActivity() {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddExpress: FloatingActionButton
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var smsCountTextView: TextView

    private lateinit var expressGroupAdapter: ExpressGroupAdapter
    private lateinit var dataManager: DataManager
    private lateinit var serverSyncManager: ServerSyncManager
    private lateinit var ruleManager: RuleManager  // 添加这行声明

    private val expressList = mutableListOf<ExpressInfo>()
    
    // 用于跟踪当前滑动的项目
    private var swipedViewHolder: RecyclerView.ViewHolder? = null
    
    // 用于浮动按钮拖拽功能
    private var dX = 0f
    private var dY = 0f
    private var isDragging = false

    companion object {
        private const val REQUEST_SMS_PERMISSION = 1001
        private const val REQUEST_CODE_MANAGE_RULES = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setListeners()
        initData()
    }
    
    /**
     * 初始化数据管理器和其他必要组件
     */
    private fun initData() {
        // 初始化数据管理器
        dataManager = DataManager.getInstance(this)
        
        // 初始化服务器同步管理器
        serverSyncManager = ServerSyncManager(this)
        
        // 初始化规则管理器
        ruleManager = RuleManager(this)
        
        // 设置RecyclerView
        setupRecyclerView()
        
        // 加载规则
        loadRules()
        
        // 如果不是首次授权，则加载已保存的数据
        loadDataIfNotFirstTime()
        
        // 检查短信权限并加载数据
        checkSMSPermissionAndLoadData()
    }
    
    /**
     * 加载规则和关键词
     */
    private fun loadRules() {
        // 首次启动时初始化规则
        ruleManager.initializeRulesOnFirstLaunch()
        
        // 从本地加载规则和关键词
        val rulesData = ruleManager.loadRulesFromLocal()
        
        // 这里可以将规则数据传递给ExpressParser或其他需要的地方
        // 例如：ExpressParser.setDownloadedRules(rulesData)
        
        // 启动后台任务下载最新的规则数据
        ruleManager.downloadAndSaveRules()
    }
    

    
    /**
     * 如果不是首次授权，则加载已保存的数据
     */
    private fun loadDataIfNotFirstTime() {
        // 检查是否是首次授权
        val isFirstAuthorization = !dataManager.isFirstAuthorizationDone()
        
        // 如果不是首次授权，先加载已保存的数据
        if (!isFirstAuthorization) {
            val savedExpressList = dataManager.getExpressInfoList()
            // 过滤掉已删除的项目
            val deletedItemIds = dataManager.getDeletedItemIds()
            val filteredExpressList = savedExpressList.filter { !deletedItemIds.contains(it.id) }.toMutableList()
            
            // 更新取件状态
            val pickupStatusManager = PickupStatusManager.getInstance(this)
            for (i in filteredExpressList.indices) {
                val expressInfo = filteredExpressList[i]
                val savedStatus = pickupStatusManager.getPickupStatus(expressInfo.id)
                if (savedStatus != expressInfo.isPickedUp) {
                    filteredExpressList[i] = expressInfo.copy(isPickedUp = savedStatus)
                }
            }
            
            expressList.clear()
            expressList.addAll(filteredExpressList)
            sortAndRefreshList()
            updateToolbarTitle()
        }
    }

    /**
     * 检查短信权限并加载数据
     */
    private fun checkSMSPermissionAndLoadData() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            == PackageManager.PERMISSION_GRANTED) {
            // 如果已有权限，直接加载数据
            readSMSAndExtractInfo()
        } else {
            // 如果没有权限，请求权限
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_SMS),
                REQUEST_SMS_PERMISSION
            )
            // 没有权限时停止刷新动画
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_layout)
        recyclerView = findViewById(R.id.recycler_express)
        fabAddExpress = findViewById(R.id.fab_add_express)
        
        setSupportActionBar(toolbar)
        updateToolbarTitle()
        
        // 添加双击应用栏返回顶部的功能
        var lastClickTime: Long = 0
        toolbar.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < 300) { // 双击间隔小于300毫秒
                // 双击直接返回顶部
                recyclerView.scrollToPosition(0)
            }
            lastClickTime = currentTime
        }
    }

    /**
     * 更新应用栏标题，显示未取件数量
     */
    private fun updateToolbarTitle() {
        val unpickedCount = expressList.count { !it.isPickedUp }
        supportActionBar?.title = "待取件: $unpickedCount"
    }

    private fun setListeners() {
        swipeRefreshLayout.setOnRefreshListener {
            // 检查是否有下载的规则，如果有则重新解析现有数据
            val downloadedRules = SettingsActivity.loadDownloadedRules(this)
            if (downloadedRules.isNotEmpty()) {
                reparseWithDownloadedRules()
            } else {
                checkSMSPermissionAndLoadData()
            }
        }

        // 浮动按钮点击事件
        fabAddExpress.setOnClickListener {
            showAddExpressDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.menu_clear_list -> {
                expressList.clear()
                sortAndRefreshList()
                // 清除已删除项目ID列表
                dataManager.clearDeletedItemIds()
                // 清除保存的快递信息列表
                dataManager.clearExpressInfoList()
                Toast.makeText(this, "列表已清空", Toast.LENGTH_SHORT).show()
                updateToolbarTitle() // 更新应用栏标题
                true
            }
            R.id.menu_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(SpaceItemDecoration(2)) // 减小间距装饰器从4到2

        // 初始化分组适配器
        expressGroupAdapter = ExpressGroupAdapter(
            this,
            mutableListOf(),
            { id ->
                // 查找对应的ExpressInfo并显示原始短信
                val position = expressList.indexOfFirst { it.id == id }
                if (position != -1) {
                    showOriginalSms(position)
                } else {
                    // 如果直接查找失败，尝试查找拆分后的ID
                    val splitPosition = expressList.indexOfFirst { expressInfo ->
                        id.startsWith("${expressInfo.id}_")
                    }
                    if (splitPosition != -1) {
                        showOriginalSms(splitPosition)
                    }
                }
            },
            { id, _ ->
                // 查找对应的ExpressInfo并显示添加备注对话框
                val position = expressList.indexOfFirst { it.id == id }
                if (position != -1) {
                    showAddNoteDialog(position)
                } else {
                    // 如果直接查找失败，尝试查找拆分后的ID
                    val splitPosition = expressList.indexOfFirst { expressInfo ->
                        id.startsWith("${expressInfo.id}_")
                    }
                    if (splitPosition != -1) {
                        showAddNoteDialog(splitPosition)
                    }
                }
            },
            { id, isPickedUp ->
                // 更新数据库中的取件状态
                val pickupStatusManager = PickupStatusManager.getInstance(this)
                pickupStatusManager.savePickupStatus(id, isPickedUp)
                
                // 更新列表中的取件状态
                val position = expressList.indexOfFirst { it.id == id }
                if (position != -1) {
                    // 直接匹配到ID的情况
                    expressList[position] = expressList[position].copy(isPickedUp = isPickedUp)
                    sortAndRefreshList() // 重新排序列表
                } else {
                    // 如果直接查找失败，尝试查找拆分后的ID
                    val splitPosition = expressList.indexOfFirst { expressInfo ->
                        id.startsWith("${expressInfo.id}_")
                    }
                    if (splitPosition != -1) {
                        // 创建一个新的ExpressInfo对象来替代原始对象
                        val originalExpressInfo = expressList[splitPosition]
                        
                        // 创建新的ExpressInfo对象，更新取件状态
                        val updatedExpressInfo = originalExpressInfo.copy(
                            id = id, // 使用拆分后的ID
                            isPickedUp = isPickedUp
                        )
                        
                        // 用新的ExpressInfo对象替换原始对象
                        expressList[splitPosition] = updatedExpressInfo
                        sortAndRefreshList() // 重新排序列表
                    }
                }
                
                updateToolbarTitle() // 更新应用栏标题
                Toast.makeText(this, if (isPickedUp) "标记为已取件" else "标记为未取件", Toast.LENGTH_SHORT).show()
            },
            { id ->
                // 删除项目
                val position = expressList.indexOfFirst { it.id == id }
                if (position != -1) {
                    showDeleteConfirmationDialog(position)
                } else {
                    // 如果直接查找失败，尝试查找拆分后的ID
                    val splitPosition = expressList.indexOfFirst { expressInfo ->
                        id.startsWith("${expressInfo.id}_")
                    }
                    if (splitPosition != -1) {
                        // 创建一个新的ExpressInfo对象，ID使用拆分后的ID
                        val originalExpressInfo = expressList[splitPosition]
                        val splitExpressInfo = originalExpressInfo.copy(
                            id = id // 使用拆分后的ID
                        )
                        
                        // 用新的ExpressInfo对象替换原始对象
                        expressList[splitPosition] = splitExpressInfo
                        showDeleteConfirmationDialog(splitPosition)
                    }
                }
            }
        )

        recyclerView.adapter = expressGroupAdapter
    }

    /**
     * 显示删除确认对话框（微信样式）
     */
    private fun showDeleteConfirmationDialog(position: Int) {
        val expressInfo = expressList[position]
        // 删除不必要的null检查
        // 创建自定义对话框
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirm, null)
        val dialog = Dialog(this, R.style.CustomDialog)
        
        // 设置对话框内容
        val titleText = dialogView.findViewById<TextView>(R.id.dialog_title)
        val messageText = dialogView.findViewById<TextView>(R.id.dialog_message)
        val cancelButton = dialogView.findViewById<Button>(R.id.btn_cancel)
        val deleteButton = dialogView.findViewById<Button>(R.id.btn_delete)
        
        titleText.text = "确认删除"
        messageText.text = "确定要删除取件码 ${expressInfo.pickupCode} 吗？"
        
        // 设置按钮点击事件
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        deleteButton.setOnClickListener {
            val pickupCode = expressInfo.pickupCode
            // 从列表中移除项目
            expressList.removeAt(position)
            // 重新刷新列表
            sortAndRefreshList()
            
            // 记录删除状态
            dataManager.addDeletedItemId(expressInfo.id)
            
            // 如果是手动添加的快递信息，则从持久化存储中删除
            if (expressInfo.smsContent == "手动添加的取件信息") {
                dataManager.removeManualExpressInfo(expressInfo.id)
            }
            
            // 更新工具栏标题
            updateToolbarTitle()
            
            Toast.makeText(this, "已删除取件码 $pickupCode", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        
        // 设置对话框窗口属性
        val window = dialog.window
        if (window != null) {
            val layoutParams = window.attributes
            layoutParams.width = (resources.displayMetrics.widthPixels * 00.8).toInt() // 宽度为屏幕的80%
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            window.attributes = layoutParams
            window.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
        }
        
        // 确保对话框在当前Activity上下文中正确显示
        if (!isFinishing) {
            dialog.show()
        }
    }
    


    /**
     * 更新短信计数显示
     */
    private fun updateSMSCount() {
        // 这里可以添加更新短信计数的逻辑
        // 目前留空以解决编译错误
    }

    /**
     * 读取短信并提取信息
     */
    private fun readSMSAndExtractInfo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            // 没有权限时也要停止刷新动画
            swipeRefreshLayout.isRefreshing = false
            return
        }

        try {
            // 检查是否是首次授权
            val isFirstAuthorization = !dataManager.isFirstAuthorizationDone()
            
            // 如果不是首次授权，数据应该已经在onCreate中加载了
            // 只有在首次授权时才需要从SharedPreferences加载数据（如果有的话）
            if (isFirstAuthorization) {
                // 首次授权时，检查是否有已保存的数据
                val savedExpressList = dataManager.getExpressInfoList()
                if (savedExpressList.isNotEmpty()) {
                    // 过滤掉已删除的项目
                    val deletedItemIds = dataManager.getDeletedItemIds()
                    val filteredExpressList = savedExpressList.filter { !deletedItemIds.contains(it.id) }.toMutableList()
                    
                    // 更新取件状态
                    val pickupStatusManager = PickupStatusManager.getInstance(this)
                    for (i in filteredExpressList.indices) {
                        val expressInfo = filteredExpressList[i]
                        val savedStatus = pickupStatusManager.getPickupStatus(expressInfo.id)
                        if (savedStatus != expressInfo.isPickedUp) {
                            filteredExpressList[i] = expressInfo.copy(isPickedUp = savedStatus)
                        }
                    }
                    
                    expressList.clear()
                    expressList.addAll(filteredExpressList)
                    sortAndRefreshList()
                    updateToolbarTitle()
                }
            }
            
            // 构建查询条件
            var selection: String? = null
            var selectionArgs: Array<String>? = null
            var sortOrder = "date DESC"
            
            // 如果不是首次授权，则只读取最近的短信和当天的短信
            if (!isFirstAuthorization) {
                val lastReadTime = dataManager.getLastSmsReadTime()
                val currentTime = System.currentTimeMillis()
                val startOfToday = getStartOfToday()
                
                // 取最近读取时间之后的短信和今天的所有短信中的较早时间作为查询起点
                val queryStartTime = Math.min(lastReadTime, startOfToday)
                selection = "date >= ?"
                selectionArgs = arrayOf(queryStartTime.toString())
                
                // 限制查询结果数量以提高性能
                sortOrder = "date DESC LIMIT 100"
            }

            val cursor = contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("_id", "address", "body", "date"),
                selection,
                selectionArgs,
                sortOrder
            )

            val newExpressList = mutableListOf<ExpressInfo>()
            val expressSet = mutableSetOf<String>() // 用于去重，存储已添加的ID
            
            // 获取自定义规则
            val customRules = SettingsActivity.loadRules(this)
            
            // 获取已删除的项目ID列表
            val deletedItemIds = dataManager.getDeletedItemIds()
            
            // 获取取件状态管理器
            val pickupStatusManager = PickupStatusManager.getInstance(this)

            cursor?.use {
                while (it.moveToNext()) {
                    val smsBody = it.getString(it.getColumnIndexOrThrow("body"))
                    val smsSender = it.getString(it.getColumnIndexOrThrow("address"))
                    val smsDate = it.getLong(it.getColumnIndexOrThrow("date"))

                    // 处理服务号短信格式（服务号: 短信内容）
                    var processedSmsBody = smsBody
                    if (smsSender.startsWith("106") && smsSender.length >= 8 && smsSender.length <= 20 && 
                        smsBody.contains(": ")) {
                        val parts = smsBody.split(": ", limit = 2)
                        if (parts.size == 2) {
                            processedSmsBody = parts[1]
                        }
                    }

                    // 使用ExpressParser解析短信，传入context参数以支持下载的规则
                    val expressInfo = ExpressParser.parse(processedSmsBody, smsDate, this)
                    if (expressInfo != null) {
                        // 使用ID作为唯一标识进行去重
                        if (!expressSet.contains(expressInfo.id)) {
                            // 检查该项目是否已被删除
                            if (!deletedItemIds.contains(expressInfo.id)) {
                                expressSet.add(expressInfo.id)
                                newExpressList.add(expressInfo)
                            }
                        }
                    }
                }
            }
        
        // 添加手动添加的快递信息
        val manualExpressList = dataManager.getManualExpressInfoList()
        for (manualExpress in manualExpressList) {
            // 检查该项目是否已被删除
            if (!deletedItemIds.contains(manualExpress.id)) {
                newExpressList.add(manualExpress)
            }
        }
        
        // 更新取件状态
        for (i in newExpressList.indices) {
            val expressInfo = newExpressList[i]
            val savedStatus = pickupStatusManager.getPickupStatus(expressInfo.id)
            if (savedStatus != expressInfo.isPickedUp) {
                newExpressList[i] = expressInfo.copy(isPickedUp = savedStatus)
            }
        }

        // 合并新数据到现有列表中
        mergeNewExpressData(newExpressList)
        
        // 更新UI
        sortAndRefreshList()
        updateToolbarTitle()
        
        // 保存当前快递信息列表之前，过滤掉已删除的项目
        val filteredExpressList = expressList.filter { !deletedItemIds.contains(it.id) }
        dataManager.saveExpressInfoList(filteredExpressList)
        
        // 如果是首次授权，标记为已完成首次授权
        if (isFirstAuthorization) {
            dataManager.setFirstAuthorizationDone()
        }
        
        // 保存当前时间为下次读取的参考时间
        dataManager.saveLastSmsReadTime(System.currentTimeMillis())

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "读取短信失败: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            // 停止下拉刷新动画
            try {
                swipeRefreshLayout.isRefreshing = false
            } catch (e: UninitializedPropertyAccessException) {
                // 忽略未初始化的属性异常
            }
        }
    }
    
    /**
     * 合并新数据到现有列表中
     */
    private fun mergeNewExpressData(newExpressList: List<ExpressInfo>) {
        // 创建现有数据的ID映射以便快速查找
        val existingItemsMap = expressList.associateBy { it.id }.toMutableMap()
        
        // 更新现有项目的取件状态
        val pickupStatusManager = PickupStatusManager.getInstance(this)
        for (newExpress in newExpressList) {
            if (existingItemsMap.containsKey(newExpress.id)) {
                // 如果项目已存在，更新其取件状态
                val savedStatus = pickupStatusManager.getPickupStatus(newExpress.id)
                if (savedStatus != newExpress.isPickedUp) {
                    val updatedExpress = newExpress.copy(isPickedUp = savedStatus)
                    existingItemsMap[newExpress.id] = updatedExpress
                }
            } else {
                // 如果是新项目，直接添加
                existingItemsMap[newExpress.id] = newExpress
            }
        }
        
        // 更新expressList
        expressList.clear()
        expressList.addAll(existingItemsMap.values)
    }
    
    /**
     * 获取今天的开始时间（00:00:00）
     */
    private fun getStartOfToday(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * 使用下载的规则重新解析现有数据
     */
    private fun reparseWithDownloadedRules() {
        // 获取下载的规则
        val downloadedRules = SettingsActivity.loadDownloadedRules(this)
        
        // 如果没有下载的规则，直接返回
        if (downloadedRules.isEmpty()) {
            return
        }
        
        // 重新解析现有数据
        val newExpressList = mutableListOf<ExpressInfo>()
        val expressSet = mutableSetOf<String>() // 用于去重，存储已添加的ID
        
        // 获取已删除的项目ID列表
        val deletedItemIds = dataManager.getDeletedItemIds()
        
        // 获取取件状态管理器
        val pickupStatusManager = PickupStatusManager.getInstance(this)
        
        // 遍历现有列表，使用下载的规则重新解析
        for (expressInfo in expressList) {
            // 使用ExpressParser和下载的规则重新解析短信内容
            val reparsedInfo = ExpressParser.parse(expressInfo.smsContent, expressInfo.timestamp, this)
            if (reparsedInfo != null) {
                // 使用ID作为唯一标识进行去重
                if (!expressSet.contains(reparsedInfo.id)) {
                    // 检查该项目是否已被删除
                    if (!deletedItemIds.contains(reparsedInfo.id)) {
                        expressSet.add(reparsedInfo.id)
                        newExpressList.add(reparsedInfo)
                    }
                }
            } else {
                // 如果无法重新解析，保留原始信息
                if (!expressSet.contains(expressInfo.id)) {
                    if (!deletedItemIds.contains(expressInfo.id)) {
                        expressSet.add(expressInfo.id)
                        newExpressList.add(expressInfo)
                    }
                }
            }
        }
        
        // 更新取件状态
        for (i in newExpressList.indices) {
            val expressInfo = newExpressList[i]
            val savedStatus = pickupStatusManager.getPickupStatus(expressInfo.id)
            if (savedStatus != expressInfo.isPickedUp) {
                newExpressList[i] = expressInfo.copy(isPickedUp = savedStatus)
            }
        }

        // 合并新数据到现有列表中
        mergeNewExpressData(newExpressList)
        
        // 更新UI
        sortAndRefreshList()
        updateToolbarTitle()
        
        // 保存当前快递信息列表之前，过滤掉已删除的项目
        val filteredExpressList = expressList.filter { !deletedItemIds.contains(it.id) }
        dataManager.saveExpressInfoList(filteredExpressList)
        
        // 停止下拉刷新动画
        swipeRefreshLayout.isRefreshing = false
    }
    
    /**
     * 对快递列表进行排序：未取件在前，已取件在后，都按时间倒序排列（最新的在前）
     */
    private fun sortExpressList() {
        expressList.sortWith(
            compareBy<ExpressInfo> { it.isPickedUp } // false (未取件) 在前，true (已取件) 在后
                .thenByDescending { it.timestamp } // 都按时间倒序排列（最新的在前）
        )
    }
    
    /**
     * 排序并刷新列表
     */
    private fun sortAndRefreshList() {
        try {
            // 先对列表进行排序：未取件的在前，已取件的在后，同状态下按时间倒序排列
            expressList.sortWith(
                compareByDescending<ExpressInfo> { if (it.isPickedUp) 0 else 1 }
                    .thenByDescending { it.timestamp }
            )
            
            // 处理ExpressList，将包含多个取件码的ExpressInfo拆分为多个独立的ExpressInfo
            val processedExpressList = processExpressList(expressList)
            
            // 使用处理后的列表创建分组并刷新适配器
            val groups = expressGroupAdapter.createGroupsFromExpressList(processedExpressList)
            expressGroupAdapter.refreshGroups(groups)
            
            // 更新工具栏标题
            updateToolbarTitle()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "数据处理出错: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_SMS_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 权限被授予，刷新短信列表
                    readSMSAndExtractInfo()
                } else {
                    // 权限被拒绝，显示提示
                    Toast.makeText(this, "权限被拒绝，无法读取短信", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }

    /**
     * 获取状态栏高度
     */
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    /**
     * 显示原始短信内容
     */
    private fun showOriginalSms(position: Int) {
        val expressInfo = expressList[position]
        // 删除不必要的null检查
        // 创建自定义对话框
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_original_sms, null)
        val dialog = Dialog(this, R.style.CustomDialog)
        
        // 设置对话框内容
        val titleText = dialogView.findViewById<TextView>(R.id.dialog_title)
        val messageText = dialogView.findViewById<TextView>(R.id.dialog_message)
        val copyButton = dialogView.findViewById<Button>(R.id.btn_copy)
        val cancelButton = dialogView.findViewById<Button>(R.id.btn_cancel)
        
        titleText.text = "原始短信内容"
        messageText.text = expressInfo.smsContent
        
        // 设置按钮点击事件
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        copyButton.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("原始短信", expressInfo.smsContent)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "短信内容已复制到剪贴板", Toast.LENGTH_SHORT).show()
            // 复制后也关闭对话框
            dialog.dismiss()
        }
        
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        
        // 设置对话框窗口属性
        val window = dialog.window
        if (window != null) {
            val layoutParams = window.attributes
            layoutParams.width = (resources.displayMetrics.widthPixels * 0.8).toInt() // 宽度为屏幕的80%
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            window.attributes = layoutParams
            window.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
        }
        
        // 确保对话框在当前Activity上下文中正确显示
        if (!isFinishing) {
            dialog.show()
        }
    }
    
    /**
     * 显示原始短信内容（通过ID查找）
     */
    private fun showOriginalSms(id: String) {
        // 查找对应的ExpressInfo
        var position = expressList.indexOfFirst { it.id == id }
        if (position == -1) {
            // 如果未找到，可能是多取件码情况，尝试查找拆分后的ID
            position = expressList.indexOfFirst { expressInfo ->
                if (id.contains("_")) {
                    // 如果ID包含下划线，表示是拆分后的ID，提取原始ID进行匹配
                    val originalId = id.substring(0, id.lastIndexOf("_"))
                    originalId == expressInfo.id
                } else {
                    id == expressInfo.id
                }
            }
        }
        if (position != -1) {
            showOriginalSms(position)
        }
    }
    
    /**
     * 显示添加备注对话框
     */
    private fun showAddNoteDialog(id: String) {
        // 查找对应的ExpressInfo（处理多取件码情况）
        var position = expressList.indexOfFirst { it.id == id }
        if (position == -1) {
            // 如果未找到，可能是多取件码情况，尝试查找拆分后的ID
            position = expressList.indexOfFirst { expressInfo ->
                if (id.contains("_")) {
                    // 如果ID包含下划线，表示是拆分后的ID，提取原始ID进行匹配
                    val originalId = id.substring(0, id.lastIndexOf("_"))
                    originalId == expressInfo.id
                } else {
                    id == expressInfo.id
                }
            }
        }
        if (position != -1) {
            showAddNoteDialog(position)
        }
    }
    
    /**
     * 显示添加备注对话框
     */
    private fun showAddNoteDialog(position: Int) {
        val expressInfo = expressList[position]
        // 删除不必要的null检查
        // 创建自定义对话框
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_note, null)
        val dialog = Dialog(this, R.style.CustomDialog)
        
        // 设置对话框内容
        val titleText = dialogView.findViewById<TextView>(R.id.dialog_title)
        val messageText = dialogView.findViewById<TextView>(R.id.dialog_message)
        val noteEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edit_note)
        val cancelButton = dialogView.findViewById<Button>(R.id.btn_cancel)
        val confirmButton = dialogView.findViewById<Button>(R.id.btn_confirm)
        
        titleText.text = "添加备注"
        messageText.text = "为取件码 ${expressInfo.pickupCode} 添加备注"
        noteEditText.setText(expressInfo.note)
        
        // 设置按钮点击事件
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        confirmButton.setOnClickListener {
            val newNote = noteEditText.text.toString()
            // 更新ExpressInfo对象的备注
            expressList[position] = expressList[position].copy(note = newNote)
            sortAndRefreshList() // 重新排序列表
            
            // 保存更新后的列表之前，过滤掉已删除的项目
            val deletedItemIds = dataManager.getDeletedItemIds()
            val filteredExpressList = expressList.filter { !deletedItemIds.contains(it.id) }
            dataManager.saveExpressInfoList(filteredExpressList)
            
            Toast.makeText(this, "备注已更新", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        
        // 设置对话框窗口属性
        val window = dialog.window
        if (window != null) {
            val layoutParams = window.attributes
            layoutParams.width = (resources.displayMetrics.widthPixels * 0.8).toInt() // 宽度为屏幕的80%
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            window.attributes = layoutParams
            window.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
        }
        
        // 确保对话框在当前Activity上下文中正确显示
        if (!isFinishing) {
            dialog.show()
            
            // 自动弹出软键盘
            noteEditText.post {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(noteEditText, InputMethodManager.SHOW_IMPLICIT)
            }
            
            // 请求焦点
            noteEditText.requestFocus()
        }
    }
    
    /**
     * 显示添加备注对话框（通过ID查找）
     */
    private fun showAddNoteDialog(id: String, currentNote: String) {
        // 查找对应的ExpressInfo
        val position = expressList.indexOfFirst { it.id == id }
        if (position != -1) {
            showAddNoteDialog(position)
        }
    }
    
    /**
     * 显示添加快递对话框
     */
    private fun showAddExpressDialog() {
        // 创建自定义对话框
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_express, null)
        val dialog = Dialog(this, R.style.CustomDialog)
        
        // 设置对话框内容
        val titleText = dialogView.findViewById<TextView>(R.id.dialog_title)
        val pickupCodeEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_pickup_code)
        val stationNameEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_station_name)
        val addressEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_address)
        val cancelButton = dialogView.findViewById<Button>(R.id.btn_cancel)
        val addButton = dialogView.findViewById<Button>(R.id.btn_add)
        
        // 新增的短信解析相关控件
        val smsContentEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_sms_content)
        val parseSmsButton = dialogView.findViewById<Button>(R.id.btn_parse_sms)
        
        titleText.text = "手动添加取件信息"
        
        // 设置解析按钮点击事件
        parseSmsButton.setOnClickListener {
            val smsContent = smsContentEditText.text.toString().trim()
            if (smsContent.isNotEmpty()) {
                // 使用ExpressParser解析短信内容
                val parsedInfo = ExpressParser.parse(smsContent, System.currentTimeMillis(), this)
                
                if (parsedInfo != null) {
                    // 填充解析结果到对应输入框
                    pickupCodeEditText.setText(parsedInfo.pickupCode)
                    stationNameEditText.setText(parsedInfo.stationName)
                    addressEditText.setText(parsedInfo.address)
                    
                    Toast.makeText(this, "短信解析成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "未能从短信中解析出有效信息，请检查短信格式", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "请先粘贴短信内容", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 设置按钮点击事件
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        addButton.setOnClickListener {
            val pickupCode = pickupCodeEditText.text.toString().trim()
            val stationName = stationNameEditText.text.toString().trim()
            val address = addressEditText.text.toString().trim()
            
            // 验证输入
            if (pickupCode.isEmpty()) {
                Toast.makeText(this, "请输入取件码", Toast.LENGTH_SHORT).show()
                pickupCodeEditText.requestFocus()
                return@setOnClickListener
            }
            
            if (stationName.isEmpty()) {
                Toast.makeText(this, "请输入驿站名称", Toast.LENGTH_SHORT).show()
                stationNameEditText.requestFocus()
                return@setOnClickListener
            }
            
            if (address.isEmpty()) {
                Toast.makeText(this, "请输入地址", Toast.LENGTH_SHORT).show()
                addressEditText.requestFocus()
                return@setOnClickListener
            }
            
            // 创建新的ExpressInfo对象
            val newExpressInfo = ExpressInfo(
                id = ExpressInfo.generateStableId(pickupCode, stationName, address, System.currentTimeMillis()),
                pickupCode = pickupCode,
                pickupCodes = listOf(pickupCode),
                stationName = stationName,
                address = address,
                smsContent = "手动添加的取件信息",
                timestamp = System.currentTimeMillis()
            )
            
            // 保存到持久化存储
            dataManager.saveManualExpressInfo(newExpressInfo)
            
            // 添加到列表开头
            expressList.add(0, newExpressInfo)
            sortAndRefreshList()
            
            // 滚动到顶部
            recyclerView.scrollToPosition(0)
            
            // 保存更新后的列表之前，过滤掉已删除的项目
            val deletedItemIds = dataManager.getDeletedItemIds()
            val filteredExpressList = expressList.filter { !deletedItemIds.contains(it.id) }
            dataManager.saveExpressInfoList(filteredExpressList)
            
            Toast.makeText(this, "已添加取件信息", Toast.LENGTH_SHORT).show()
            updateToolbarTitle() // 更新应用栏标题
            dialog.dismiss()
        }
        
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        
        // 设置对话框窗口属性
        val window = dialog.window
        if (window != null) {
            val layoutParams = window.attributes
            layoutParams.width = (resources.displayMetrics.widthPixels * 0.8).toInt() // 宽度为屏幕的80%
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            window.attributes = layoutParams
            window.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
        }
        
        // 确保对话框在当前Activity上下文中正确显示
        if (!isFinishing) {
            dialog.show()
            
            // 自动弹出软键盘
            pickupCodeEditText.post {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(pickupCodeEditText, InputMethodManager.SHOW_IMPLICIT)
            }
            
            // 请求焦点
            pickupCodeEditText.requestFocus()
        }
    }
    
    /**
     * 显示设置菜单
     */
    private fun showSettingsMenu() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }
    
    /**
     * 显示关于对话框
     */
    private fun showAboutDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_about, null)
        val dialog = Dialog(this, R.style.CustomDialog)
        
        val versionText = dialogView.findViewById<TextView>(R.id.version_text)
        val closeButton = dialogView.findViewById<Button>(R.id.btn_close)
        
        // 获取应用版本信息
        try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            versionText.text = "版本号: ${packageInfo.versionName}"
        } catch (e: Exception) {
            versionText.text = "版本号: 未知"
        }
        
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        
        // 设置对话框窗口属性
        val window = dialog.window
        if (window != null) {
            val layoutParams = window.attributes
            layoutParams.width = (resources.displayMetrics.widthPixels * 0.8).toInt() // 宽度为屏幕的80%
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            window.attributes = layoutParams
            window.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
        }
        
        // 确保对话框在当前Activity上下文中正确显示
        if (!isFinishing) {
            dialog.show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_MANAGE_RULES -> {
                    // 规则管理返回，重新读取短信以应用新规则
                    readSMSAndExtractInfo()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private var exitTime: Long = 0
    
    override fun onBackPressed() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - exitTime < 2000) { // 2秒内再次按返回键
            super.onBackPressed()
        } else {
            Toast.makeText(this, "再按一次退出应用", Toast.LENGTH_SHORT).show()
            exitTime = currentTime
        }
    }
    
    private val onDeleteItem: (Int) -> Unit = { position ->
        if (position >= 0 && position < expressList.size) {
            val expressInfo = expressList[position]
            
            // 从数据库中删除
            // 如果是手动添加的快递信息，则从持久化存储中删除
            if (expressInfo.smsContent == "手动添加的取件信息") {
                dataManager.removeManualExpressInfo(expressInfo.id)
            }
            
            // 从列表中删除
            expressList.removeAt(position)
            sortAndRefreshList() // 使用sortAndRefreshList而不是expressAdapter.notifyItemRemoved
            
            // 记录删除状态
            dataManager.addDeletedItemId(expressInfo.id)
            
            // 保存更新后的列表之前，过滤掉已删除的项目
            val deletedItemIds = dataManager.getDeletedItemIds()
            val filteredExpressList = expressList.filter { !deletedItemIds.contains(it.id) }
            dataManager.saveExpressInfoList(filteredExpressList)
            
            updateToolbarTitle() // 更新应用栏标题
            Toast.makeText(this, "已删除取件信息", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 删除快递信息（通过ID查找）
     */
    private fun deleteExpress(id: String) {
        // 查找对应的ExpressInfo
        var position = expressList.indexOfFirst { it.id == id }
        if (position == -1) {
            // 如果未找到，可能是多取件码情况，尝试查找拆分后的ID
            position = expressList.indexOfFirst { expressInfo ->
                // 对于拆分后的取件码，使用generateStableId生成的ID进行匹配
                expressInfo.pickupCodes.size > 1 && expressInfo.pickupCodes.any { code ->
                    val independentId = ExpressInfo.generateStableId(
                        code,
                        expressInfo.stationName,
                        expressInfo.address,
                        expressInfo.timestamp
                    )
                    independentId == id
                }
            }
        }
        
        if (position != -1) {
            onDeleteItem(position)
        }
    }
    
    /**
     * 切换取件状态（通过位置）
     */
    private fun togglePickupStatus(position: Int, isPickedUp: Boolean) {
        if (position in expressList.indices) {
            val expressInfo = expressList[position]
            val updatedExpressInfo = expressInfo.copy(isPickedUp = isPickedUp)
            expressList[position] = updatedExpressInfo
            
            // 保存取件状态
            val pickupStatusManager = PickupStatusManager.getInstance(this)
            pickupStatusManager.savePickupStatus(expressInfo.id, isPickedUp)
            
            sortAndRefreshList()
            updateToolbarTitle()
            Toast.makeText(this, if (isPickedUp) "标记为已取件" else "标记为未取件", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 切换取件状态（通过ID查找）
     */
    private fun togglePickupStatus(id: String, isPickedUp: Boolean) {
        // 查找对应的ExpressInfo
        var position = expressList.indexOfFirst { it.id == id }
        if (position == -1) {
            // 如果未找到，可能是多取件码情况，尝试查找拆分后的ID
            position = expressList.indexOfFirst { expressInfo ->
                // 对于拆分后的取件码，使用generateStableId生成的ID进行匹配
                expressInfo.pickupCodes.size > 1 && expressInfo.pickupCodes.any { code ->
                    val independentId = ExpressInfo.generateStableId(
                        code,
                        expressInfo.stationName,
                        expressInfo.address,
                        expressInfo.timestamp
                    )
                    independentId == id
                }
            }
        }
        
        if (position != -1) {
            togglePickupStatus(position, isPickedUp)
        }
    }
    
    /**
     * 根据原始ExpressInfo对象列表，生成处理后的ExpressInfo对象列表
     * 如果一个ExpressInfo对象包含多个取件码，则将其拆分为多个独立的ExpressInfo对象
     */
    private fun processExpressList(expressList: List<ExpressInfo>): List<ExpressInfo> {
        val processedList = mutableListOf<ExpressInfo>()
        
        for (expressInfo in expressList) {
            // 如果有多个取件码，为每个取件码创建独立的ExpressInfo对象
            if (expressInfo.pickupCodes.size > 1) {
                for (i in expressInfo.pickupCodes.indices) {
                    val code = expressInfo.pickupCodes[i]
                    // 为每个取件码生成独立的原始ID，添加索引以确保唯一性
                    val independentId = ExpressInfo.generateStableId(
                        code, 
                        expressInfo.stationName, 
                        expressInfo.address, 
                        expressInfo.timestamp + i // 添加索引以确保唯一性
                    )
                    val singleCodeExpressInfo = expressInfo.copy(
                        id = independentId, // 为每个取件码生成独立的原始ID
                        pickupCode = code,
                        pickupCodes = listOf(code) // 只包含当前取件码
                    )
                    processedList.add(singleCodeExpressInfo)
                }
            } else {
                processedList.add(expressInfo)
            }
        }
        
        return processedList
    }
    
    class SpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            with(outRect) {
                left = space
                right = space
                bottom = space
                // 顶部间距统一设置，避免首行特殊处理
                top = space
            }
        }
    }
    
    /**
     * 自定义分割线装饰器
     */
    class DividerItemDecoration(context: Context) : RecyclerView.ItemDecoration() {
        private val dividerPaint = Paint()
        private val dividerHeight = 1 // 1像素分割线
        
        init {
            dividerPaint.color = ContextCompat.getColor(context, R.color.light_grey)
            dividerPaint.isAntiAlias = true
        }
        
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            super.getItemOffsets(outRect, view, parent, state)
            outRect.bottom = dividerHeight
        }
        
        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            super.onDraw(c, parent, state)
            val childCount = parent.childCount
            for (i in 0 until childCount - 1) {
                val child = parent.getChildAt(i)
                val params = child.layoutParams as RecyclerView.LayoutParams
                val top = child.bottom + params.bottomMargin
                val bottom = top + dividerHeight
                c.drawRect(
                    parent.paddingLeft.toFloat(),
                    top.toFloat(),
                    (parent.width - parent.paddingRight).toFloat(),
                    bottom.toFloat(),
                    dividerPaint
                )
            }
        }
    }

    /**
     * 检查并从服务器同步数据
     */
    private fun checkAndSyncFromServer() {
        // 检查是否需要同步（例如：距离上次同步超过4小时）
        if (serverSyncManager.shouldSync()) {
            // 检查网络连接
            if (!NetworkUtils.isNetworkAvailable(this)) {
                runOnUiThread {
                    Toast.makeText(this, "网络不可用，请检查网络连接", Toast.LENGTH_LONG).show()
                }
                return
            }

            // 从SharedPreferences获取API密钥
            val apiKey = getSavedApiKey()
            if (apiKey.isNotEmpty()) {
                // 自动同步数据
                serverSyncManager.syncFromServer(apiKey) { success, message ->
                    runOnUiThread {
                        if (success) {
                            Log.d("MainActivity", "Auto sync successful: $message")
                            // 重新加载规则
                            loadRules()
                            Toast.makeText(this, "规则同步成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.e("MainActivity", "Auto sync failed: $message")
                            Toast.makeText(this, "规则同步失败: $message", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                // 如果没有API密钥，提醒用户配置
                runOnUiThread {
                    Toast.makeText(this, "请在设置中配置API密钥以启用规则同步", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * 检查网络是否可用
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            networkCapabilities != null && networkCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo != null && networkInfo.isConnected
        }
    }

    /**
     * 从SharedPreferences获取保存的API密钥
     */
    private fun getSavedApiKey(): String {
        val sharedPrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val savedKey = sharedPrefs.getString("api_key", "") ?: ""
        // 如果没有保存的API密钥，则使用默认API密钥
        return if (savedKey.isNotEmpty()) savedKey else DataManager.DEFAULT_API_KEY
    }

    /**
     * 保存API密钥到SharedPreferences
     */
    private fun saveApiKey(apiKey: String) {
        val sharedPrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("api_key", apiKey).apply()
    }
}