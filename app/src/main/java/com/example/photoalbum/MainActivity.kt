package com.example.photoalbum

import android.app.Activity
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Size
import android.view.View
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.photoalbum.data.SyncDatabaseWork
import com.example.photoalbum.data.model.Settings
import com.example.photoalbum.enums.ScanResult
import com.example.photoalbum.enums.WorkTag
import com.example.photoalbum.ui.action.UserAction
import com.example.photoalbum.ui.screen.BaseViewModel
import com.example.photoalbum.ui.screen.MainScreen
import com.example.photoalbum.ui.screen.MainScreenViewModel
import com.example.photoalbum.ui.theme.PhotoAlbumTheme
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var workManager: WorkManager = WorkManager.getInstance(application)

    private var createWorkFlag = true

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val myapplication = application as MediaApplication
        val factory = BaseViewModel.Companion.MyViewModelFactory(
            application = myapplication,
            userAction = MutableStateFlow(UserAction.NoneAction),
            settings = Settings(),
            activity = this
        )

        val viewModel =
            ViewModelProvider.create(this, factory = factory)[MainScreenViewModel::class.java]
        val myActivity = this
        //监听是否隐藏状态栏
        CoroutineScope(context = Dispatchers.IO).launch {
            viewModel.userAction.collect {
                if (it is UserAction.ExpandStatusBarAction) {
                    CoroutineScope(context = Dispatchers.Main).launch {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val controller = window.insetsController
                            if (it.expand) {
                                controller?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                            } else {
                                controller?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            if (it.expand) {
                                window.decorView.systemUiVisibility =
                                    View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            } else {
                                window.decorView.systemUiVisibility =
                                    View.SYSTEM_UI_FLAG_VISIBLE or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            }
                        }
                    }
                }
            }
        }
        CoroutineScope(context = Dispatchers.IO).launch {
            viewModel.userAction.collect {
                if (it is UserAction.ScanAction) {
                    if (it.scanState == ScanResult.SCANNING){
                        println("work log:扫描中，取消同步work")
                        workManager.cancelAllWork()
                        createWorkFlag = false
                    }
                    if (it.scanState == ScanResult.SUCCESS || it.scanState == ScanResult.FAILED){
                        println("work log:扫描结束，启动同步work")
                        val work = OneTimeWorkRequest.Builder(SyncDatabaseWork::class.java)
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .addTag(WorkTag.SYNC_DATABASE.value)
                            .build()
                        workManager.enqueue(work)
                        createWorkFlag = true
                    }
                }
            }
        }
        //初始化设置
        CoroutineScope(context = Dispatchers.IO).launch {
            val isFirstRun = viewModel.checkFirstRunApp()
            if (isFirstRun) {
                viewModel.checkAndRequestPermissions(myActivity)
                viewModel.setFirstRunState()
                viewModel.application.mediaDatabase.settingsDao.insertOrUpdate(viewModel.settings)
            } else {
                val settings = viewModel.application.mediaDatabase.settingsDao.getUserSettings()
                viewModel.settings.openLocalNetStorageThumbnail =
                    settings?.openLocalNetStorageThumbnail ?: true
                viewModel.settings.gridColumnNumState.intValue = settings?.gridColumnNum ?: 3
            }
            viewModel.settings.phoneSize = getPhysicalResolution(this@MainActivity)
            println("屏幕参数${viewModel.settings.phoneSize}")
        }
        //getImageRatio("/storage/emulated/0/DCIM/Camera/20231008_154438.jpg", 0f)
        setContent {
            PhotoAlbumTheme {
                MainScreen(viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val result = workManager.getWorkInfosByTag(WorkTag.SYNC_DATABASE.value)
        Futures.addCallback(
            result, object : FutureCallback<List<WorkInfo>> {
                override fun onSuccess(result: List<WorkInfo>) {
                    val hasRunningWork = result.any { !it.state.isFinished }
                    println("work log:有work在运行: $hasRunningWork")

                    if (!hasRunningWork && createWorkFlag) {
                        println("work log:没有work运行，创建work")
                        val work = OneTimeWorkRequest.Builder(SyncDatabaseWork::class.java)
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .addTag(WorkTag.SYNC_DATABASE.value)
                            .build()
                        workManager.enqueue(work)
                    }
                }

                override fun onFailure(t: Throwable) {
                }
            },
            ContextCompat.getMainExecutor(application)
        )
    }

    /*    override fun onRestart() {
            super.onRestart()
            val info = workManager.getWorkInfoById(syncDatabaseWorkId)
            if (info.isDone || info.isCancelled) {
                println("work意外结束")
                syncDatabaseWorkId = workRequest.id
                workManager.enqueue(workRequest)
            }
        }*/

    /*private fun observeWorkStatus() {
        // 观察 WorkInfo 的变化
        workManager.getWorkInfoByIdLiveData(syncDatabaseWorkId).observe(this, Observer { workInfo ->
            if (workInfo != null) {
                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED -> {
                        println("work log:Task is in the queue. id:${workInfo.id}")
                    }

                    WorkInfo.State.RUNNING -> {
                        println("work log:Task is running. id:${workInfo.id}")
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        println("work log:Task succeeded. id:${workInfo.id}")
                    }

                    WorkInfo.State.FAILED -> {
                        println("work log:Task failed. id:${workInfo.id}")
                    }

                    WorkInfo.State.CANCELLED -> {
                        println("work log:Task was cancelled. id:${workInfo.id}")
                    }

                    WorkInfo.State.BLOCKED -> {
                        println("work log:Task was blocked. id:${workInfo.id}")
                    }
                }
            }
        })
    }*/
}

@Suppress("DEPRECATION")
fun getPhysicalResolution(activity: Activity): Size {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Android 11 (API 30)及以上
        val windowManager = activity.windowManager
        val metrics = windowManager.currentWindowMetrics
        val bounds = metrics.bounds // 获取窗口的边界
        Size(bounds.width(), bounds.height())
    } else {
        // Android 10 (API 29)及以下
        val display = activity.windowManager.defaultDisplay
        val metrics = DisplayMetrics()
        try {
            display.getRealMetrics(metrics) // 获取完整分辨率，包括系统栏
            Size(metrics.widthPixels, metrics.heightPixels)
        } catch (e: Exception) {
            // Fallback：对于部分特殊设备，使用旧方法获取
            val size = Point()
            display.getRealSize(size)
            Size(size.x, size.y)
        }
    }
}