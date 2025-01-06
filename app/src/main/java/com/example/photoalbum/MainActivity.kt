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
import androidx.lifecycle.ViewModelProvider
import com.example.photoalbum.data.model.Settings
import com.example.photoalbum.ui.action.UserAction
import com.example.photoalbum.ui.screen.BaseViewModel
import com.example.photoalbum.ui.theme.PhotoAlbumTheme
import com.example.photoalbum.ui.screen.MainScreen
import com.example.photoalbum.ui.screen.MainScreenViewModel
import com.example.photoalbum.utils.decodeBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
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
        }
        //decodeBitmap("/storage/emulated/0/DCIM/Camera/20231008_154438.jpg")
        setContent {
            PhotoAlbumTheme {
                MainScreen(viewModel)
            }
        }
    }
}

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